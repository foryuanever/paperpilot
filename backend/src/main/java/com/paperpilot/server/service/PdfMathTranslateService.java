package com.paperpilot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.repository.PaperRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PdfMathTranslateService {

    private final PaperRepository paperRepository;
    private final CurrentUserService currentUserService;
    private final BackendJobService backendJobService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();
    private final Map<String, String> tasksByWorkspace = new ConcurrentHashMap<>();
    private final Path cacheDir = Path.of("translations");

    @Value("${paperpilot.pdfmathtranslate.base-url:http://127.0.0.1:11008}")
    private String baseUrl;

    public PdfMathTranslateService(
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

    public Map<String, Object> start(String workspaceId, String service) {
        PaperEntity paper = requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        if (Files.exists(cachedPdf(workspaceId))) {
            backendJobService.upsert("PDF_MATH_TRANSLATE", userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已生成", "");
            return Map.of("taskId", "cached", "state", "SUCCESS", "reused", true, "cached", true);
        }
        String existingTask = backendJobService.find("PDF_MATH_TRANSLATE", userId, workspaceId)
            .map(job -> job.getExternalTaskId())
            .orElseGet(() -> tasksByWorkspace.get(workspaceId));
        if (existingTask != null) {
            Map<String, Object> existingStatus = status(workspaceId);
            String state = String.valueOf(existingStatus.getOrDefault("state", ""));
            if (!"FAILURE".equalsIgnoreCase(state) && !"REVOKED".equalsIgnoreCase(state)) {
                return Map.of("taskId", existingTask, "state", state, "reused", true);
            }
        }

        try {
            byte[] pdf = readPaperPdf(paper);
            String boundary = "PaperSolver-" + UUID.randomUUID();
            String data = objectMapper.writeValueAsString(Map.of(
                "lang_in", "en",
                "lang_out", "zh",
                "service", service == null || service.isBlank() ? "google" : service,
                "thread", 8
            ));
            byte[] body = multipartBody(boundary, pdf, safeFileName(paper.getTitle()), data);
            HttpRequest request = HttpRequest.newBuilder(endpoint("/v1/translate"))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw unavailable("双栏翻译任务提交失败（HTTP " + response.statusCode() + "）");
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            String taskId = String.valueOf(payload.getOrDefault("id", ""));
            if (taskId.isBlank()) throw unavailable("双栏翻译服务未返回任务编号");
            tasksByWorkspace.put(workspaceId, taskId);
            backendJobService.externalTask("PDF_MATH_TRANSLATE", userId, workspaceId, taskId, "PENDING", 10, "双栏翻译任务已提交");
            return Map.of("taskId", taskId, "state", "PENDING", "reused", false);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("无法连接双栏翻译服务");
        }
    }

    public Map<String, Object> status(String workspaceId) {
        requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        if (Files.exists(cachedPdf(workspaceId))) {
            backendJobService.upsert("PDF_MATH_TRANSLATE", userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已生成", "");
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
                throw unavailable("双栏翻译状态查询失败");
            }
            Map<String, Object> payload = objectMapper.readValue(response.body(), new TypeReference<>() {});
            payload.put("taskId", taskId);
            String state = String.valueOf(payload.getOrDefault("state", payload.getOrDefault("status", "RUNNING")));
            int progress = "SUCCESS".equalsIgnoreCase(state) ? 100 : ("FAILURE".equalsIgnoreCase(state) ? 100 : 50);
            backendJobService.externalTask("PDF_MATH_TRANSLATE", userId, workspaceId, taskId, state.toUpperCase(), progress, "双栏翻译状态：" + state);
            return payload;
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("双栏翻译状态服务暂不可用");
        }
    }

    public byte[] bilingualPdf(String workspaceId) {
        requirePaper(workspaceId);
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Path cached = cachedPdf(workspaceId);
        if (Files.exists(cached)) {
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
                throw unavailable("双栏 PDF 尚未生成");
            }
            Files.createDirectories(cacheDir);
            Files.write(cached, response.body());
            backendJobService.upsert("PDF_MATH_TRANSLATE", userId, workspaceId, "SUCCESS", 100, "双栏翻译 PDF 已缓存", "");
            return response.body();
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw unavailable("下载双栏 PDF 失败");
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
        String taskId = backendJobService.find("PDF_MATH_TRANSLATE", userId, workspaceId)
            .map(job -> job.getExternalTaskId())
            .orElseGet(() -> tasksByWorkspace.get(workspaceId));
        if (taskId == null || taskId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "尚未创建双栏翻译任务");
        }
        return taskId;
    }

    private Path cachedPdf(String workspaceId) {
        return cacheDir.resolve(workspaceId + "-dual.pdf");
    }

    private byte[] readPaperPdf(PaperEntity paper) throws IOException, InterruptedException {
        String source = paper.getPaperUrl() == null ? "" : paper.getPaperUrl().trim();
        if (source.startsWith("/api/papers/uploads/")) {
            Path path = Path.of("uploads").resolve(paper.getWorkspaceId() + ".pdf");
            if (!Files.exists(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本地 PDF 不存在");
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
}
