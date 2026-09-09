package com.paperpilot.server.controller;

import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.entity.BackendJobEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.BackendJobRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/translation-issues")
public class AdminTranslationIssueController {

    private final TranslationRecordRepository translationRecordRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final BackendJobRepository backendJobRepository;

    public AdminTranslationIssueController(
        TranslationRecordRepository translationRecordRepository,
        AppUserRepository appUserRepository,
        CurrentUserService currentUserService,
        BackendJobRepository backendJobRepository
    ) {
        this.translationRecordRepository = translationRecordRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
        this.backendJobRepository = backendJobRepository;
    }

    @ModelAttribute
    public void requireAdminAccess() {
        currentUserService.requireAdmin();
    }

    @GetMapping
    public Map<String, Object> getTranslationIssues(
        @RequestParam(value = "slowMs", defaultValue = "1800") long slowMs,
        @RequestParam(value = "limit", defaultValue = "500") int limit
    ) {
        long safeSlowMs = Math.max(300L, Math.min(slowMs, 60000L));
        int safeLimit = Math.max(20, Math.min(limit, 1000));
        // Include successful records so a newly completed PDF translation is
        // visible immediately instead of leaving the admin panel looking stale.
        List<TranslationRecordEntity> records = translationRecordRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(0, safeLimit)
        );
        List<Map<String, Object>> rows = records.stream()
            .map(this::translationIssueToMap)
            .toList();
        long failedCount = translationRecordRepository.countBySuccessFalse();
        long slowCount = translationRecordRepository.countByLatencyMsGreaterThanEqual(safeSlowMs);
        long googleIssues = records.stream()
            .filter(record -> String.valueOf(record.getProvider()).toLowerCase().contains("google"))
            .count();
        long p95Latency = percentile(records.stream()
            .map(TranslationRecordEntity::getLatencyMs)
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList(), 0.95);

        return Map.of(
            "rows", rows,
            "jobs", backendJobRepository.findTop200ByJobTypeInOrderByUpdatedAtDesc(
                List.of("PDF_MATH_TRANSLATE_V2", "MINERU_PARSE")
            ).stream().map(this::backendJobToMap).toList(),
            "summary", Map.of(
                "totalRecords", translationRecordRepository.count(),
            "problemCount", records.stream().filter(record -> !record.isSuccess()
                || (record.getLatencyMs() != null && record.getLatencyMs() >= safeSlowMs)).count(),
                "failedCount", failedCount,
                "slowCount", slowCount,
                "googleIssues", googleIssues,
                "p95LatencyMs", p95Latency,
                "slowThresholdMs", safeSlowMs
            )
        );
    }

    private Map<String, Object> backendJobToMap(BackendJobEntity job) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", job.getId());
        row.put("type", job.getJobType());
        row.put("userId", job.getUserId());
        row.put("resourceId", job.getResourceId());
        row.put("taskId", job.getExternalTaskId());
        row.put("status", job.getStatus());
        row.put("progress", job.getProgress() == null ? 0 : job.getProgress());
        row.put("message", job.getMessage() == null ? "" : job.getMessage());
        row.put("detail", job.getDetail() == null ? "" : job.getDetail());
        row.put("createdAt", job.getCreatedAt());
        row.put("updatedAt", job.getUpdatedAt());
        return row;
    }

    private Map<String, Object> translationIssueToMap(TranslationRecordEntity record) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", record.getId());
        row.put("userId", record.getUserId());
        appUserRepository.findById(record.getUserId()).ifPresent(user -> {
            row.put("username", user.getUsername());
            row.put("email", user.getEmail());
            row.put("numericId", user.getNumericId());
        });
        row.put("provider", record.getProvider());
        row.put("paperTitle", record.getPaperTitle());
        row.put("translationMode", record.getTranslationMode() != null ? record.getTranslationMode()
            : String.valueOf(record.getRoute()).startsWith("immersive") ? "沉浸式翻译"
            : String.valueOf(record.getRoute()).startsWith("translate") ? "对照翻译" : "未记录");
        row.put("quotaRecord", "membership-quota".equals(record.getProvider()));
        row.put("route", record.getRoute());
        row.put("sourceLang", record.getSourceLang());
        row.put("targetLang", record.getTargetLang());
        row.put("clientType", record.getClientType());
        row.put("charCount", record.getCharCount());
        row.put("latencyMs", record.getLatencyMs());
        row.put("success", record.isSuccess());
        row.put("errorMessage", record.getErrorMessage());
        row.put("networkProfile", record.getNetworkProfile());
        row.put("ipAddress", record.getIpAddress());
        row.put("userAgent", record.getUserAgent());
        row.put("createdAt", record.getCreatedAt());
        return row;
    }

    private long percentile(List<Long> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }
}
