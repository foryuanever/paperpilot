package com.paperpilot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.entity.TranslationRecordEntity;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdfwriter.ContentStreamWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Service
public class PdfMathTranslateService {

    private static final String TRANSLATION_JOB_TYPE = "PDF_MATH_TRANSLATE_V2";

    private final PaperRepository paperRepository;
    private final CurrentUserService currentUserService;
    private final BackendJobService backendJobService;
    private final TranslationRecordRepository translationRecordRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    private final Map<String, String> tasksByWorkspace = new ConcurrentHashMap<>();
    private final Map<String, Long> translationStartedAt = new ConcurrentHashMap<>();
    private final Map<String, String> translationProviders = new ConcurrentHashMap<>();
    private final Set<String> recordedTranslationTasks = new ConcurrentSkipListSet<>();
    private final Path cacheDir = Path.of("translations");

    @Value("${paperpilot.pdfmathtranslate.base-url:http://127.0.0.1:11008}")
    private String baseUrl;

    public PdfMathTranslateService(
        PaperRepository paperRepository,
        CurrentUserService currentUserService,
        BackendJobService backendJobService,
        TranslationRecordRepository translationRecordRepository,
        ObjectMapper objectMapper
    ) {
        this.paperRepository = paperRepository;
        this.currentUserService = currentUserService;
        this.backendJobService = backendJobService;
        this.translationRecordRepository = translationRecordRepository;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> start(String workspaceId, String service) {
        PaperEntity paper = requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        if (ensureCleanCachedPdf(workspaceId)) {
            backendJobService.upsert(TRANSLATION_JOB_TYPE, userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已生成", "");
            recordTranslation(workspaceId, userId, "cached", service, true, "双栏翻译 PDF 已缓存");
            return Map.of("taskId", "cached", "state", "SUCCESS", "reused", true, "cached", true);
        }
        String existingTask = backendJobService.find(TRANSLATION_JOB_TYPE, userId, workspaceId)
            .map(job -> job.getExternalTaskId())
            .orElseGet(() -> tasksByWorkspace.get(workspaceId));
        if (existingTask != null) {
            try {
                Map<String, Object> existingStatus = status(workspaceId);
                String state = String.valueOf(existingStatus.getOrDefault("state", ""));
                if (!"FAILURE".equalsIgnoreCase(state) && !"REVOKED".equalsIgnoreCase(state)) {
                    return Map.of("taskId", existingTask, "state", state, "reused", true);
                }
            } catch (ResponseStatusException error) {
                if (!isStaleRemoteTask(error)) throw error;
                tasksByWorkspace.remove(workspaceId);
                backendJobService.externalTask(TRANSLATION_JOB_TYPE, userId, workspaceId, "", "FAILURE", 100, "旧的双栏翻译任务已失效，正在重新提交");
            }
        }

        try {
            byte[] pdf = readPaperPdf(paper);
            String boundary = "PaperSolver-" + UUID.randomUUID();
            String data = objectMapper.writeValueAsString(Map.of(
                // Let pdf2zh/the selected translator detect Spanish, Japanese,
                // and other source languages instead of treating every paper
                // as English.
                "lang_in", "auto",
                "lang_out", "zh",
                "service", service == null || service.isBlank() ? "tencent-transmart" : service,
                "thread", 8,
                "skip_subset_fonts", true
            ));
            byte[] body = multipartBody(boundary, pdf, safeFileName(paper.getTitle()), data);
            HttpRequest request = HttpRequest.newBuilder(endpoint("/v1/translate"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw unavailable("对照翻译任务提交失败（HTTP " + response.statusCode() + "）：" + serviceErrorBody(response.body()));
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            String taskId = String.valueOf(payload.getOrDefault("id", ""));
            if (taskId.isBlank()) throw unavailable("双栏翻译服务未返回任务编号");
            tasksByWorkspace.put(workspaceId, taskId);
            translationStartedAt.put(workspaceId, System.currentTimeMillis());
            translationProviders.put(workspaceId, service == null || service.isBlank() ? "tencent-transmart" : service);
            backendJobService.externalTask(TRANSLATION_JOB_TYPE, userId, workspaceId, taskId, "PENDING", 10, "双栏翻译任务已提交");
            return Map.of("taskId", taskId, "state", "PENDING", "reused", false);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("无法连接对照翻译开源服务。请确认 PDFMathTranslate/pdf2zh 服务已在 11008 端口启动；如果服务正在下载或加载开源模型，请等待完成后重试。");
        }
    }

    public Map<String, Object> status(String workspaceId) {
        requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
            if (ensureCleanCachedPdf(workspaceId)) {
            backendJobService.upsert(TRANSLATION_JOB_TYPE, userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已生成", "");
            recordTranslation(workspaceId, userId, "cached", translationProviders.get(workspaceId), true, "双栏翻译 PDF 已缓存");
            return new java.util.LinkedHashMap<>(Map.of(
                "taskId", "cached",
                "state", "SUCCESS",
                "cached", true
            ));
        }
        String taskId = requireTask(workspaceId);
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint("/v1/translate/" + taskId))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                ResponseStatusException error = unavailable("对照翻译状态查询失败（HTTP " + response.statusCode() + "）：" + serviceErrorBody(response.body()));
                if (isStaleRemoteTask(error)) {
                    tasksByWorkspace.remove(workspaceId);
                    backendJobService.externalTask(TRANSLATION_JOB_TYPE, userId, workspaceId, "", "FAILURE", 100, "旧的双栏翻译任务已失效，请重新发起");
                }
                throw error;
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            payload.put("taskId", taskId);
            String state = String.valueOf(payload.getOrDefault("state", payload.getOrDefault("status", "RUNNING")));
            if ("FAILURE".equalsIgnoreCase(state)) {
                String message = serviceErrorBody(String.valueOf(payload.getOrDefault("error", payload.getOrDefault("message", ""))));
                if (message.isBlank()) message = "PDFMathTranslate/pdf2zh 任务失败。常见原因是开源服务处理该 PDF 的字体表或页面结构失败；系统已默认跳过字体子集化来规避 PyMuPDF bad value 问题，请重新发起对照翻译。";
                payload.put("message", message);
                recordTranslation(workspaceId, userId, taskId, translationProviders.get(workspaceId), false, message);
            }
            int progress = progressFromPayload(payload, state);
            backendJobService.externalTask(TRANSLATION_JOB_TYPE, userId, workspaceId, taskId, state.toUpperCase(), progress, "双栏翻译状态：" + state);
            if ("SUCCESS".equalsIgnoreCase(state) || "COMPLETED".equalsIgnoreCase(state)) {
                recordTranslation(workspaceId, userId, taskId, translationProviders.get(workspaceId), true, "双栏翻译 PDF 已生成");
            }
            return payload;
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("对照翻译状态服务暂不可用。请确认 PDFMathTranslate/pdf2zh 服务仍在运行。");
        }
    }

    public byte[] bilingualPdf(String workspaceId) {
        requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Path cached = cachedPdf(workspaceId);
        if (ensureCleanCachedPdf(workspaceId)) {
            try {
                return Files.readAllBytes(cached);
            } catch (IOException ignored) {
                // 缓存损坏时重新从任务服务获取。
            }
        }
        String taskId = requireTask(workspaceId);
        try {
            HttpRequest request = HttpRequest.newBuilder(endpoint("/v1/translate/" + taskId + "/dual"))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400 || response.body().length < 4) {
                throw unavailable("对照翻译 PDF 尚未生成，请等待任务完成后再打开。");
            }
            Files.createDirectories(cacheDir);
            byte[] cleaned = sanitizeBilingualPdf(response.body());
            Files.write(cached, cleaned);
            backendJobService.upsert(TRANSLATION_JOB_TYPE, userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已缓存", "");
            recordTranslation(workspaceId, userId, taskId, translationProviders.get(workspaceId), true, "双栏翻译 PDF 已缓存");
            return cleaned;
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("下载对照翻译 PDF 失败，请检查开源翻译服务是否仍在运行。");
        }
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

    private String requireTask(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String taskId = backendJobService.find(TRANSLATION_JOB_TYPE, userId, workspaceId)
            .map(job -> job.getExternalTaskId())
            .orElseGet(() -> tasksByWorkspace.get(workspaceId));
        if (taskId == null || taskId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "尚未创建双栏翻译任务");
        }
        return taskId;
    }

    private Path cachedPdf(String workspaceId) {
        return cacheDir.resolve(workspaceId + "-dual-v3.pdf");
    }

    private Path legacyCachedPdf(String workspaceId) {
        return cacheDir.resolve(workspaceId + "-dual-v2.pdf");
    }

    private boolean ensureCleanCachedPdf(String workspaceId) {
        Path current = cachedPdf(workspaceId);
        Path legacy = legacyCachedPdf(workspaceId);
        try {
            if (Files.exists(current) && Files.size(current) > 4) {
                byte[] original = Files.readAllBytes(current);
                byte[] cleaned = sanitizeBilingualPdf(original);
                if (!java.util.Arrays.equals(original, cleaned)) Files.write(current, cleaned);
                return true;
            }
            if (!Files.exists(legacy) || Files.size(legacy) <= 4) return false;
            Files.createDirectories(cacheDir);
            Files.write(current, sanitizeBilingualPdf(Files.readAllBytes(legacy)));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Remove date/time markers leaked by the external PDF translation bridge. */
    private byte[] sanitizeBilingualPdf(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            java.util.regex.Pattern timestamp = java.util.regex.Pattern.compile(
                "20\\d{2}\\s*[-—–/]\\s*\\d{1,2}\\s*[-—–/]\\s*\\d{1,2}(?:\\s+\\d{1,2}\\s*[:：]\\s*\\d{2}(?:\\s*[:：]\\s*\\d{2})?)?"
            );
            for (PDPage page : document.getPages()) {
                PDFStreamParser parser = new PDFStreamParser(page);
                java.util.List<Object> tokens = parser.parse();
                boolean changed = false;
                for (Object token : tokens) {
                    if (!(token instanceof COSString text)) continue;
                    byte[] raw = text.getBytes();
                    String value = new String(raw, StandardCharsets.ISO_8859_1);
                    String cleaned = timestamp.matcher(value).replaceAll("")
                        .replaceAll("(?m)^\\s*[\\[【(（]?\\s*\\d{3}\\s*[\\]】)）]?\\s*", "");
                    if (!cleaned.equals(value)) {
                        text.setValue(cleaned.getBytes(StandardCharsets.ISO_8859_1));
                        changed = true;
                    }
                }
                if (changed) {
                    PDStream stream = new PDStream(document);
                    new ContentStreamWriter(stream.createOutputStream()).writeTokens(tokens);
                    page.setContents(stream);
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception ignored) {
            return pdf;
        }
    }

    private void recordTranslation(String workspaceId, Long userId, String taskId, String provider, boolean success, String message) {
        String key = workspaceId + ":" + (taskId == null ? "" : taskId) + ":" + success;
        if (!recordedTranslationTasks.add(key)) return;
        try {
            TranslationRecordEntity record = new TranslationRecordEntity();
            record.setUserId(userId);
            record.setProvider("pdfmathtranslate/" + (provider == null || provider.isBlank() ? "unknown" : provider));
            record.setRoute("bilingual-translate/" + (provider == null || provider.isBlank() ? "unknown" : provider));
            record.setSourceLang("auto");
            record.setTargetLang("zh-CN");
            record.setClientType("desktop-pdf");
            record.setCharCount(0L);
            long started = translationStartedAt.getOrDefault(workspaceId, System.currentTimeMillis());
            record.setLatencyMs(Math.max(0L, System.currentTimeMillis() - started));
            record.setSuccess(success);
            record.setErrorMessage(success ? null : (message == null ? "双栏翻译失败" : message));
            record.setNetworkProfile("local-pdf2zh");
            translationRecordRepository.save(record);
        } catch (Exception ignored) {
            // Monitoring must never break the translation request itself.
        }
    }

    private boolean isStaleRemoteTask(ResponseStatusException error) {
        String message = String.valueOf(error.getReason()).toLowerCase();
        return error.getStatusCode().value() == 404
            || message.contains("task not found")
            || message.contains("任务不存在");
    }

    private byte[] readPaperPdf(PaperEntity paper) throws IOException, InterruptedException {
        String source = paper.getPaperUrl() == null ? "" : paper.getPaperUrl().trim();
        if (source.startsWith("/api/papers/uploads/")) {
            Path path = resolveLocalUploadPdf(paper.getWorkspaceId());
            if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本地 PDF 不存在");
            return Files.readAllBytes(path);
        }
        if (!source.startsWith("http://") && !source.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前文献没有可翻译的 PDF");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(normalizePdfUrl(source)))
            .timeout(Duration.ofSeconds(40))
            .header("Accept", "application/pdf,application/octet-stream,*/*")
            .header("User-Agent", "PaperSolver PDFMathTranslate Bridge/1.0")
            .GET()
            .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400 || response.body().length < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法获取论文 PDF");
        }
        return response.body();
    }

    private Path resolveLocalUploadPdf(String workspaceId) {
        String fileName = workspaceId + ".pdf";
        List<Path> candidates = List.of(
            Path.of("uploads").resolve(fileName),
            Path.of("backend").resolve("uploads").resolve(fileName),
            Path.of("../uploads").resolve(fileName)
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) return normalized;
        }
        return null;
    }

    private byte[] multipartBody(String boundary, byte[] pdf, String fileName, String data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, "--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n");
        write(out, "Content-Type: application/pdf\r\n\r\n");
        out.write(pdf);
        write(out, "\r\n--" + boundary + "\r\n");
        write(out, "Content-Disposition: form-data; name=\"data\"\r\n");
        write(out, "Content-Type: application/json; charset=UTF-8\r\n\r\n");
        write(out, data);
        write(out, "\r\n--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    private void write(ByteArrayOutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private URI endpoint(String path) {
        return URI.create(baseUrl.replaceAll("/+$", "") + path);
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

    private String safeFileName(String title) {
        String name = String.valueOf(title).replaceAll("[^\\p{L}\\p{N}._-]+", "_");
        if (name.length() > 80) name = name.substring(0, 80);
        return (name.isBlank() ? "paper" : name) + ".pdf";
    }

    private ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private String serviceErrorBody(String body) {
        String text = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (text.length() > 220) text = text.substring(0, 220) + "…";
        return text;
    }

    @SuppressWarnings("unchecked")
    private int progressFromPayload(Map<String, Object> payload, String state) {
        if ("SUCCESS".equalsIgnoreCase(state)) return 100;
        if ("FAILURE".equalsIgnoreCase(state) || "REVOKED".equalsIgnoreCase(state)) return 100;
        Object info = payload.get("info");
        if (info instanceof Map<?, ?> rawInfo) {
            double n = numericValue(rawInfo.get("n"));
            double total = numericValue(rawInfo.get("total"));
            if (total > 0 && n >= 0) {
                int value = (int) Math.round(Math.min(95, Math.max(8, (n / total) * 95)));
                return value;
            }
        }
        return 50;
    }

    private double numericValue(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return 0;
        }
    }
}
