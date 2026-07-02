package com.paperpilot.server.controller;

import com.paperpilot.server.service.PdfMathTranslateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pdfmathtranslate")
public class PdfMathTranslateController {

    private final PdfMathTranslateService service;

    public PdfMathTranslateController(PdfMathTranslateService service) {
        this.service = service;
    }

    @PostMapping("/{workspaceId}/translate")
    public Map<String, Object> start(
        @PathVariable String workspaceId,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        String provider = body == null ? "google" : String.valueOf(body.getOrDefault("service", "google"));
        return service.start(workspaceId, provider);
    }

    @GetMapping("/{workspaceId}/status")
    public Map<String, Object> status(@PathVariable String workspaceId) {
        return service.status(workspaceId);
    }

    @GetMapping("/{workspaceId}/dual.pdf")
    public ResponseEntity<byte[]> dual(@PathVariable String workspaceId) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + workspaceId + "-dual.pdf\"")
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.APPLICATION_PDF)
            .body(service.bilingualPdf(workspaceId));
    }
}
