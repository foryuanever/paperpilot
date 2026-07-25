package com.paperpilot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.repository.PaperRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class MineruParseService {

    private final PaperRepository paperRepository;
    private final CurrentUserService currentUserService;
    private final BackendJobService backendJobService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private final Path inputRoot = Path.of("mineru-inputs");
    private final Path outputRoot = Path.of("mineru-output");

    @Value("${paperpilot.mineru.binary:../.mineru-venv/bin/mineru}")
    private String mineruBinary;

    @Value("${paperpilot.mineru.model-source:modelscope}")
    private String modelSource;

    public MineruParseService(
        PaperRepository paperRepository,
        CurrentUserService currentUserService,
        BackendJobService backendJobService,
        ObjectMapper objectMapper
    ) {
        this.paperRepository = paperRepository;
        this.currentUserService = currentUserService;
        this.backendJobService = backendJobService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> start(String workspaceId, boolean force) {
        PaperEntity paper = requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        if (!force && findContentList(workspaceId).isPresent()) {
            TaskState ready = new TaskState("SUCCESS", "结构化解析已就绪", "");
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, ready.state(), 100, ready.message(), ready.detail());
            return statusPayload(workspaceId, ready);
        }
        TaskState existing = tasks.get(workspaceId);
        if (!force && existing != null && "RUNNING".equals(existing.state())) {
            return statusPayload(workspaceId, existing);
        }

        TaskState running = new TaskState("RUNNING", "正在识别完整段落、阅读顺序与图表", "");
        tasks.put(workspaceId, running);
        backendJobService.upsert("MINERU_PARSE", userId, workspaceId, running.state(), 20, running.message(), running.detail());
        CompletableFuture.runAsync(() -> parseInBackground(workspaceId, paper));
        return statusPayload(workspaceId, running);
    }

    public Map<String, Object> status(String workspaceId) {
        PaperEntity paper = requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Optional<Path> contentList = findContentList(workspaceId);
        if (contentList.isPresent()) {
            TaskState ready = new TaskState("SUCCESS", "结构化解析已完成", "");
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, ready.state(), 100, ready.message(), ready.detail());
            return statusPayload(workspaceId, ready);
        }
        TaskState state = tasks.get(workspaceId);
        if (state == null) {
            state = backendJobService.find("MINERU_PARSE", userId, workspaceId)
                .map(job -> new TaskState(job.getStatus(), job.getMessage(), job.getDetail()))
                .orElse(null);
        }
        if (state == null) {
            state = new TaskState("RUNNING", "正在识别完整段落、阅读顺序与图表", "");
            tasks.put(workspaceId, state);
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, state.state(), 20, state.message(), state.detail());
            CompletableFuture.runAsync(() -> parseInBackground(workspaceId, paper));
        }
        return statusPayload(workspaceId, state);
    }

    public Map<String, Object> document(String workspaceId) {
        requirePaper(workspaceId);
        Path contentList = findContentList(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "结构化解析结果尚未生成"));
        try {
            List<Map<String, Object>> raw = objectMapper.readValue(
                Files.readString(contentList),
                new TypeReference<>() {}
            );
            Map<Integer, List<Map<String, Object>>> pageBlocks = new LinkedHashMap<>();
            int figures = 0;
            int tables = 0;
            int paragraphs = 0;
            int index = 0;

            for (Map<String, Object> item : raw) {
                String sourceType = string(item.get("type")).toLowerCase();
                if (sourceType.equals("header")
                    || sourceType.equals("footer")
                    || sourceType.equals("page_number")
                    || sourceType.equals("page_footnote")) {
                    continue;
                }
                int pageNumber = integer(item.get("page_idx"), 0) + 1;
                String kind = blockKind(sourceType, item);
                String text = itemText(item, kind);
                if (isPublicationNoise(text)) continue;
                String imageUrl = imageUrl(workspaceId, contentList, string(item.get("img_path")));
                String html = "table".equals(kind) ? sanitizeTableHtml(string(item.get("table_body"))) : "";
                if (isDecorativeFigure(kind, text, item)) continue;

                if ("paragraph".equals(kind) && isEquationNumberArtifact(text)) {
                    appendEquationNumber(pageBlocks.computeIfAbsent(pageNumber, ignored -> new ArrayList<>()), text);
                    continue;
                }
                if (text.isBlank() && imageUrl.isBlank() && html.isBlank()) continue;
                if ("figure".equals(kind)) figures++;
                if ("table".equals(kind)) tables++;
                if ("paragraph".equals(kind)) paragraphs++;

                Map<String, Object> block = new LinkedHashMap<>();
                block.put("id", "mineru-p" + pageNumber + "-b" + index++);
                block.put("kind", kind);
                block.put("text", text);
                block.put("imageUrl", imageUrl);
                block.put("html", html);
                block.put("equationNumber", "");
                block.put("textLevel", integer(item.get("text_level"), 0));
                block.put("bbox", item.getOrDefault("bbox", List.of()));
                block.put("translation", "");
                block.put("translationProvider", "google");
                block.put("translating", false);
                block.put("translationError", "");
                pageBlocks.computeIfAbsent(pageNumber, ignored -> new ArrayList<>()).add(block);
            }

            trimFirstPageMetadata(pageBlocks);

            List<Map<String, Object>> pages = pageBlocks.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> page = new LinkedHashMap<>();
                    page.put("pageNumber", entry.getKey());
                    page.put("blocks", entry.getValue());
                    return page;
                })
                .toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("engine", "MinerU");
            result.put("pages", pages);
            result.put("totalPages", pages.stream().mapToInt(page -> integer(page.get("pageNumber"), 0)).max().orElse(0));
            result.put("paragraphCount", paragraphs);
            result.put("figureCount", figures);
            result.put("tableCount", tables);
            return result;
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取结构化解析结果");
        }
    }

    public Asset asset(String workspaceId, String relativePath) {
        requirePaper(workspaceId);
        Path contentList = findContentList(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "解析结果不存在"));
        Path base = contentList.getParent().toAbsolutePath().normalize();
        Path requested = base.resolve(relativePath == null ? "" : relativePath).normalize();
        if (!requested.startsWith(base) || !Files.isRegularFile(requested)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图表资源不存在");
        }
        try {
            String mediaType = Files.probeContentType(requested);
            return new Asset(Files.readAllBytes(requested), mediaType == null ? "application/octet-stream" : mediaType);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图表资源读取失败");
        }
    }

    private void parseInBackground(String workspaceId, PaperEntity paper) {
        Path workspaceOutput = outputRoot.resolve(workspaceId);
        Long userId = paper.getUserId();
        try {
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, "RUNNING", 35, "正在准备 MinerU 解析环境", "");
            Files.createDirectories(inputRoot);
            Files.createDirectories(workspaceOutput);
            Path input = materializePdf(paper);
            Path log = workspaceOutput.resolve("mineru.log");
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, "RUNNING", 55, "MinerU 正在解析论文版面", "");
            ProcessBuilder builder = new ProcessBuilder(
                mineruBinary,
                "-p", input.toAbsolutePath().toString(),
                "-o", workspaceOutput.toAbsolutePath().toString(),
                "-b", "pipeline",
                "-f", "false",
                "-t", "true"
            );
            builder.environment().put("MINERU_MODEL_SOURCE", modelSource);
            builder.redirectErrorStream(true);
            builder.redirectOutput(log.toFile());
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0 || findContentList(workspaceId).isEmpty()) {
                String detail = tail(log, 1800);
                tasks.put(workspaceId, new TaskState("FAILURE", "论文结构化解析失败", detail));
                backendJobService.upsert("MINERU_PARSE", userId, workspaceId, "FAILURE", 100, "论文结构化解析失败", detail);
                return;
            }
            tasks.put(workspaceId, new TaskState("SUCCESS", "段落、图表与阅读顺序解析完成", ""));
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, "SUCCESS", 100, "段落、图表与阅读顺序解析完成", "");
        } catch (Exception error) {
            tasks.put(workspaceId, new TaskState("FAILURE", "论文结构化解析失败", error.getMessage()));
            backendJobService.upsert("MINERU_PARSE", userId, workspaceId, "FAILURE", 100, "论文结构化解析失败", error.getMessage());
        }
    }

    private Path materializePdf(PaperEntity paper) throws IOException, InterruptedException {
        Path target = inputRoot.resolve(paper.getWorkspaceId() + ".pdf");
        String source = string(paper.getPaperUrl()).trim();
        if (source.startsWith("/api/papers/uploads/")) {
            Path local = Path.of("uploads").resolve(paper.getWorkspaceId() + ".pdf");
            if (!Files.isRegularFile(local)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本地 PDF 不存在");
            }
            Files.copy(local, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
        if (!source.startsWith("http://") && !source.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前文献没有可解析的 PDF");
        }
        Files.deleteIfExists(target);
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizePdfUrl(source)))
            .timeout(Duration.ofMinutes(2))
            .header("Accept", "application/pdf,application/octet-stream,*/*")
            .header("User-Agent", "PaperPilot MinerU Bridge/1.0")
            .GET()
            .build();
        HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() >= 400 || Files.size(target) < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法获取论文 PDF");
        }
        return target;
    }

    private Optional<Path> findContentList(String workspaceId) {
        Path root = outputRoot.resolve(workspaceId);
        if (!Files.isDirectory(root)) return Optional.empty();
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("_content_list.json")
                    || path.getFileName().toString().equals("content_list.json"))
                .sorted(Comparator.comparing(Path::toString))
                .findFirst();
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    private Map<String, Object> statusPayload(String workspaceId, TaskState state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", workspaceId);
        payload.put("state", state.state());
        payload.put("message", state.message());
        payload.put("detail", Objects.toString(state.detail(), ""));
        payload.put("ready", "SUCCESS".equals(state.state()));
        return payload;
    }

    private PaperEntity requirePaper(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        PaperEntity paper = paperRepository.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文献不存在"));
        if (!userId.equals(paper.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该文献");
        }
        return paper;
    }

    private String blockKind(String sourceType, Map<String, Object> item) {
        if ("text".equals(sourceType)) {
            return integer(item.get("text_level"), 0) > 0 ? "heading" : "paragraph";
        }
        if (sourceType.contains("image") || sourceType.contains("chart")) return "figure";
        if (sourceType.contains("table")) return "table";
        if (sourceType.contains("equation") || sourceType.contains("formula")) return "equation";
        if (sourceType.contains("list")) {
            return "ref_text".equalsIgnoreCase(string(item.get("sub_type"))) ? "references" : "paragraph";
        }
        return "paragraph";
    }

    private String itemText(Map<String, Object> item, String kind) {
        String direct = string(item.get("text"));
        if (!direct.isBlank()) return normalizeText(direct);
        if ("equation".equals(kind)) {
            return joinText(
                item.get("latex"),
                item.get("text_format"),
                item.get("formula"),
                item.get("equation"),
                item.get("content")
            );
        }
        if ("figure".equals(kind)) {
            return joinText(item.get("image_caption"), item.get("image_footnote"));
        }
        if ("table".equals(kind)) {
            return joinText(item.get("table_caption"), item.get("table_footnote"));
        }
        return normalizeText(joinText(item.get("list_items"), item.get("content")));
    }

    private boolean isEquationNumberArtifact(String text) {
        String normalized = normalizeText(text)
            .replaceAll("\\s+", "")
            .replace("（", "(")
            .replace("）", ")")
            .replace("þ", "Þ");
        return normalized.matches("^[ð(]\\d{1,3}[Þ)]$");
    }

    private void appendEquationNumber(List<Map<String, Object>> blocks, String text) {
        if (blocks.isEmpty()) return;
        String number = extractEquationNumber(text);
        if (number.isBlank()) return;
        for (int i = blocks.size() - 1; i >= 0; i--) {
            Map<String, Object> block = blocks.get(i);
            if (!"equation".equals(block.get("kind"))) continue;
            block.put("equationNumber", "(" + number + ")");
            return;
        }
    }

    private String extractEquationNumber(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(\\d{1,3})")
            .matcher(normalizeText(text));
        return matcher.find() ? matcher.group(1) : "";
    }

    private String joinText(Object... values) {
        List<String> parts = new ArrayList<>();
        for (Object value : values) flattenText(value, parts);
        return normalizeText(String.join(" ", parts));
    }

    private void flattenText(Object value, List<String> output) {
        if (value == null) return;
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> flattenText(item, output));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> flattenText(item, output));
            return;
        }
        String text = string(value).trim();
        if (!text.isBlank()) output.add(text);
    }

    private String imageUrl(String workspaceId, Path contentList, String imgPath) {
        if (imgPath.isBlank()) return "";
        Path base = contentList.getParent().toAbsolutePath().normalize();
        Path image = base.resolve(imgPath).normalize();
        if (!image.startsWith(base) || !Files.isRegularFile(image)) return "";
        String relative = base.relativize(image).toString().replace('\\', '/');
        return "/api/mineru/" + workspaceId + "/asset?path="
            + URLEncoder.encode(relative, StandardCharsets.UTF_8);
    }

    private String sanitizeTableHtml(String html) {
        return html
            .replaceAll("(?is)<(script|iframe|object|embed|style)[^>]*>.*?</\\1>", "")
            .replaceAll("(?i)\\son\\w+\\s*=\\s*(['\"]).*?\\1", "")
            .replaceAll("(?i)javascript:", "");
    }

    private String normalizePdfUrl(String url) {
        String normalized = url
            .replace("http://arxiv.org/", "https://arxiv.org/")
            .replace("http://export.arxiv.org/", "https://export.arxiv.org/");
        if (normalized.contains("arxiv.org/abs/")) {
            normalized = normalized.replace("/abs/", "/pdf/");
            if (!normalized.endsWith(".pdf")) normalized += ".pdf";
        }
        return normalized;
    }

    private String tail(Path file, int maxChars) {
        try {
            String value = Files.readString(file);
            return value.length() <= maxChars ? value : value.substring(value.length() - maxChars);
        } catch (IOException ignored) {
            return "";
        }
    }

    private String normalizeText(String value) {
        return string(value)
            .replaceAll("(?is)<[^>]+>", "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean isPublicationNoise(String text) {
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        return normalized.startsWith("©")
            || normalized.equals("crossmark")
            || normalized.contains("check for updates")
            || normalized.contains("published by elsevier")
            || normalized.startsWith("peer-review under responsibility")
            || normalized.matches("^www\\..+/(locate|journal)/.*");
    }

    private boolean isDecorativeFigure(String kind, String text, Map<String, Object> item) {
        if (!"figure".equals(kind)) return false;
        String normalized = normalizeText(text).toLowerCase(Locale.ROOT);
        if (normalized.contains("check for updates")
            || normalized.contains("crossmark")
            || normalized.contains("publisher logo")
            || normalized.contains("journal logo")
            || normalized.contains("sciencedirect")
            || normalized.contains("elsevier")
            || normalized.contains("creative commons")
            || normalized.contains("open access")) {
            return true;
        }

        double left = bboxNumber(item.get("bbox"), 0);
        double top = bboxNumber(item.get("bbox"), 1);
        double right = bboxNumber(item.get("bbox"), 2);
        double bottom = bboxNumber(item.get("bbox"), 3);
        double width = Math.max(0, right - left);
        double height = Math.max(0, bottom - top);
        if (width <= 0 || height <= 0) return false;

        boolean hasCaption = !normalized.isBlank();
        boolean tinyIcon = width <= 90 && height <= 90;
        boolean smallUncaptionedAsset = !hasCaption && width * height <= 12_000 && Math.max(width, height) <= 160;
        boolean firstPageTopRightLogo = integer(item.get("page_idx"), 0) == 0
            && !hasCaption
            && left >= 300
            && top <= 260
            && width <= 220
            && height <= 180;
        return tinyIcon || smallUncaptionedAsset || firstPageTopRightLogo;
    }

    private double bboxNumber(Object bbox, int index) {
        if (!(bbox instanceof List<?> values) || values.size() <= index) return 0;
        Object value = values.get(index);
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(string(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void trimFirstPageMetadata(Map<Integer, List<Map<String, Object>>> pageBlocks) {
        // Keep title and author metadata visible so the reader can display and translate them.
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(string(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public record Asset(byte[] bytes, String mediaType) {}

    private record TaskState(String state, String message, String detail) {}
}
