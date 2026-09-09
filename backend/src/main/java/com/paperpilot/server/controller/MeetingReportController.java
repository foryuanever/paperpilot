package com.paperpilot.server.controller;

import com.paperpilot.server.service.MeetingReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meeting-reports")
public class MeetingReportController {
    private final MeetingReportService meetingReportService;

    public MeetingReportController(MeetingReportService meetingReportService) {
        this.meetingReportService = meetingReportService;
    }

    @GetMapping("/qa/queue")
    public Map<String, Object> qaQueueStatus() {
        return meetingReportService.paperQaQueueStatus();
    }

    @GetMapping("/model-options")
    public Map<String, Object> modelOptions() {
        return meetingReportService.paperQaModelOptions();
    }

    @GetMapping("/{workspaceId}")
    public Map<String, Object> get(@PathVariable("workspaceId") String workspaceId) {
        return meetingReportService.get(workspaceId);
    }

    @PostMapping("/{workspaceId}/generate")
    public Map<String, Object> generate(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        return meetingReportService.startGenerate(workspaceId, body);
    }

    @PostMapping("/{workspaceId}/generate-section")
    public Map<String, Object> generateSection(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        return meetingReportService.generateSection(workspaceId, body == null ? Map.of() : body);
    }

    @GetMapping("/{workspaceId}/generate/status")
    public Map<String, Object> generateStatus(@PathVariable("workspaceId") String workspaceId) {
        return meetingReportService.generateStatus(workspaceId);
    }

    @PutMapping("/{workspaceId}")
    public Map<String, Object> save(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody Map<String, Object> body
    ) {
        return meetingReportService.save(workspaceId, body);
    }

    @PostMapping("/{workspaceId}/ask")
    public Map<String, Object> askSelection(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody Map<String, Object> body
    ) {
        return meetingReportService.askSelection(workspaceId, body);
    }

    @PostMapping("/{workspaceId}/meeting-note")
    public Map<String, Object> generateMeetingNote(
        @PathVariable("workspaceId") String workspaceId,
        @RequestBody Map<String, Object> body
    ) {
        return meetingReportService.generateMeetingNote(workspaceId, body);
    }

    @PostMapping("/deck/generate")
    public Map<String, Object> generateDeck(@RequestBody Map<String, Object> body) {
        return meetingReportService.prepareDeckGeneration(body);
    }

    @PostMapping(value = "/deck/generate", consumes = "multipart/form-data")
    public Map<String, Object> generateDeckWithReportPaper(
        @RequestPart("payload") String payload,
        @RequestPart("reportPaper") MultipartFile reportPaper
    ) {
        return meetingReportService.prepareDeckGeneration(payload, reportPaper);
    }

    @PostMapping("/deck/analyze")
    public Map<String, Object> analyzeDeck(@RequestBody Map<String, Object> body) {
        return meetingReportService.analyzeDeckComparison(body);
    }

    @PostMapping("/fuse")
    public Map<String, Object> fuseMeetingReport(@RequestBody Map<String, Object> body) {
        return meetingReportService.fuseMeetingReport(body);
    }

    @GetMapping("/deck/jobs/{jobId}/status")
    public Map<String, Object> deckStatus(@PathVariable("jobId") String jobId) {
        return meetingReportService.deckGenerationStatus(jobId);
    }

    @GetMapping("/deck/jobs/{jobId}/download")
    public ResponseEntity<byte[]> downloadDeck(@PathVariable("jobId") String jobId) {
        MeetingReportService.GeneratedDeck deck = meetingReportService.readGeneratedDeck(jobId);
        String encoded = URLEncoder.encode(deck.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .body(deck.bytes());
    }

    @GetMapping("/deck/jobs/{jobId}/previews")
    public Map<String, Object> deckPreviews(@PathVariable("jobId") String jobId) {
        List<String> urls = meetingReportService.generatedDeckPreviewUrls(jobId);
        return Map.of("jobId", jobId, "count", urls.size(), "previewUrls", urls);
    }

    @GetMapping("/deck/jobs/{jobId}/preview/{index}")
    public ResponseEntity<byte[]> previewDeck(
        @PathVariable("jobId") String jobId,
        @PathVariable("index") int index
    ) {
        MeetingReportService.GeneratedDeckPreview preview = meetingReportService.readGeneratedDeckPreview(jobId, index);
        String encoded = URLEncoder.encode(preview.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/svg+xml"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
            .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
            .header("Content-Security-Policy", "sandbox; default-src 'none'; img-src data:; style-src 'unsafe-inline'")
            .body(preview.bytes());
    }
}
