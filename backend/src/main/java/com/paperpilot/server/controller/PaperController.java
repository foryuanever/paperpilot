package com.paperpilot.server.controller;

import com.paperpilot.server.dto.PaperImportRequest;
import com.paperpilot.server.dto.PaperImportByUrlRequest;
import com.paperpilot.server.service.ExternalSearchService;
import com.paperpilot.server.vo.SearchPaperVO;
import com.paperpilot.server.service.PaperWorkspaceService;
import com.paperpilot.server.service.ZoteroImportService;
import com.paperpilot.server.vo.PaperWorkspaceVO;
import com.paperpilot.server.vo.LibraryPaperVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private final PaperWorkspaceService paperWorkspaceService;
    private final ZoteroImportService zoteroImportService;
    private final ExternalSearchService externalSearchService;
    private final com.paperpilot.server.service.ResearchDataService researchDataService;
    private final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    public PaperController(
            PaperWorkspaceService paperWorkspaceService,
            ZoteroImportService zoteroImportService,
            ExternalSearchService externalSearchService,
            com.paperpilot.server.service.ResearchDataService researchDataService
    ) {
        this.paperWorkspaceService = paperWorkspaceService;
        this.zoteroImportService = zoteroImportService;
        this.externalSearchService = externalSearchService;
        this.researchDataService = researchDataService;
    }

    @PostMapping("/import")
    public PaperWorkspaceVO importPaper(@Valid @RequestBody PaperImportRequest request) {
        return paperWorkspaceService.importPaper(request);
    }

    @PostMapping(value = "/import-zotero", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public java.util.Map<String, Object> importZotero(@RequestParam("file") MultipartFile file) throws IOException {
        return zoteroImportService.importFile(file);
    }

    @PostMapping("/import-zotero-online")
    public java.util.Map<String, Object> importZoteroOnline(@RequestBody java.util.Map<String, Object> body) throws IOException, InterruptedException {
        String userId = java.util.Objects.toString(body.get("userId"), "").trim();
        String apiKey = java.util.Objects.toString(body.get("apiKey"), "").trim();
        int limit = 100;
        Object rawLimit = body.get("limit");
        if (rawLimit instanceof Number number) {
            limit = number.intValue();
        } else if (rawLimit != null) {
            try {
                limit = Integer.parseInt(rawLimit.toString());
            } catch (NumberFormatException ignored) {
                limit = 100;
            }
        }
        return zoteroImportService.importOnline(userId, apiKey, limit);
    }

    @PostMapping("/import-zotero-local")
    public java.util.Map<String, Object> importZoteroLocal(@RequestBody(required = false) java.util.Map<String, Object> body) throws IOException, InterruptedException {
        int limit = 100;
        Object rawLimit = body == null ? null : body.get("limit");
        if (rawLimit instanceof Number number) {
            limit = number.intValue();
        } else if (rawLimit != null) {
            try {
                limit = Integer.parseInt(rawLimit.toString());
            } catch (NumberFormatException ignored) {
                limit = 100;
            }
        }
        return zoteroImportService.importLocal(limit);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LibraryPaperVO uploadNewPaper(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请上传 PDF 文件");
        }
        java.nio.file.Path temp = java.nio.file.Files.createTempFile("paperpilot-upload-", ".pdf");
        try {
            java.nio.file.Files.copy(file.getInputStream(), temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return researchDataService.createFromUploadedPdf(file.getOriginalFilename(), temp);
        } finally {
            java.nio.file.Files.deleteIfExists(temp);
        }
    }

    @PostMapping("/import-by-url")
    public ResponseEntity<SearchPaperVO> importByUrl(@Valid @RequestBody PaperImportByUrlRequest request) {
        SearchPaperVO result = externalSearchService.searchByUrlOrDoi(request.getUrl());
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        String inputUrl = request.getUrl() == null ? "" : request.getUrl().trim();
        if ((result.getPdfUrl() == null || result.getPdfUrl().isBlank()) && isLikelyPdfUrl(inputUrl)) {
            result = new SearchPaperVO(
                result.getId(),
                result.getTitle(),
                result.getSource(),
                result.getAuthors(),
                result.getYear(),
                result.getAbstractText(),
                inputUrl
            );
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/{workspaceId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPdf(
            @PathVariable("workspaceId") String workspaceId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
        if (!java.nio.file.Files.exists(uploadDir)) {
            java.nio.file.Files.createDirectories(uploadDir);
        }
        java.nio.file.Path filePath = uploadDir.resolve(workspaceId + ".pdf");
        java.nio.file.Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String paperUrl = "/api/papers/uploads/" + workspaceId + ".pdf";
        researchDataService.updatePaperUrl(workspaceId, paperUrl);
        Thread enrichThread = new Thread(() -> {
            try {
                researchDataService.enrichPaperFromUploadedPdf(workspaceId, filePath);
            } catch (Exception ignored) {
                // PDF 已保存并可阅读；题录补全失败不应阻塞插件上传。
            }
        }, "papersolver-pdf-enrich-" + workspaceId);
        enrichThread.setDaemon(true);
        enrichThread.start();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/uploads/{workspaceId}.pdf")
    public ResponseEntity<byte[]> getUploadedPdf(@PathVariable("workspaceId") String workspaceId) throws IOException {
        java.nio.file.Path filePath = java.nio.file.Paths.get("uploads").resolve(workspaceId + ".pdf");
        if (!java.nio.file.Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }
        byte[] content = java.nio.file.Files.readAllBytes(filePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + workspaceId + ".pdf\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.APPLICATION_PDF)
                .body(content);
    }

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxyPdf(@RequestParam("url") String url) throws IOException, InterruptedException {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String normalizedUrl = normalizePdfUrl(resolveReadablePdfUrl(url));
        HttpResponse<byte[]> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalizedUrl))
                .timeout(java.time.Duration.ofSeconds(25))
                .header(HttpHeaders.ACCEPT, "application/pdf,application/octet-stream,*/*")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.REFERER, refererFor(normalizedUrl))
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 PaperSolver/1.0")
                .GET()
                .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IllegalArgumentException error) {
            return ResponseEntity.badRequest().build();
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return ResponseEntity.status(502).build();
        }
        if (response.statusCode() >= 400) {
            return ResponseEntity.status(response.statusCode()).build();
        }
        String contentType = response.headers().firstValue("content-type").orElse(MediaType.APPLICATION_PDF_VALUE);
        boolean looksLikePdf = response.body().length >= 4
            && response.body()[0] == '%'
            && response.body()[1] == 'P'
            && response.body()[2] == 'D'
            && response.body()[3] == 'F';
        if (contentType.contains("text/html") && !looksLikePdf) {
            return ResponseEntity.badRequest().build();
        }
        String fileName = extractFileName(normalizedUrl);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .contentType(looksLikePdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(contentType))
            .body(response.body());
    }

    private String normalizePdfUrl(String url) {
        String normalized = url.trim()
            .replace("http://arxiv.org/", "https://arxiv.org/")
            .replace("http://export.arxiv.org/", "https://export.arxiv.org/");
        if (normalized.contains("arxiv.org/abs/")) {
            normalized = normalized.replace("/abs/", "/pdf/");
            if (!normalized.endsWith(".pdf")) {
                normalized = normalized + ".pdf";
            }
        }
        if (normalized.contains("aclanthology.org/") && !normalized.toLowerCase().endsWith(".pdf")) {
            normalized = normalized.replaceAll("/$", "") + ".pdf";
        }
        return normalized;
    }

    private String resolveReadablePdfUrl(String url) {
        String normalized = url == null ? "" : url.trim();
        try {
            SearchPaperVO paper = externalSearchService.searchByUrlOrDoi(normalized);
            if (paper != null && paper.getPdfUrl() != null && !paper.getPdfUrl().isBlank()) {
                return paper.getPdfUrl();
            }
        } catch (Exception ignored) {
        }
        return normalized;
    }

    private boolean isLikelyPdfUrl(String url) {
        String normalized = normalizePdfUrl(url).toLowerCase();
        return normalized.startsWith("http://") || normalized.startsWith("https://")
            ? normalized.contains(".pdf") || normalized.contains("/pdf/")
            : false;
    }

    private String extractFileName(String url) {
        String[] parts = url.split("/");
        String rawName = parts.length == 0 ? "paper.pdf" : parts[parts.length - 1];
        return rawName.isBlank() ? "paper.pdf" : rawName;
    }

    private String refererFor(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost() + "/";
        } catch (Exception error) {
            return "https://scholar.google.com/";
        }
    }
}
