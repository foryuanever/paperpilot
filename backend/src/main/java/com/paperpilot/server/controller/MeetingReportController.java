package com.paperpilot.server.controller;

import com.paperpilot.server.service.MeetingReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    @GetMapping("/{workspaceId}")
    public Map<String, Object> get(@PathVariable("workspaceId") String workspaceId) {
        return meetingReportService.get(workspaceId);
    }

    @PostMapping("/{workspaceId}/generate")
    public Map<String, Object> generate(@PathVariable("workspaceId") String workspaceId) {
        return meetingReportService.startGenerate(workspaceId);
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
}
