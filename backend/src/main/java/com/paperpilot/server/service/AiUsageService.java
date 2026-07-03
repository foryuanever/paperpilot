package com.paperpilot.server.service;

import com.paperpilot.server.entity.AiUsageRecordEntity;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.AppUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiUsageService {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final AiUsageRecordRepository repository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;

    public AiUsageService(
        AiUsageRecordRepository repository,
        AppUserRepository appUserRepository,
        CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
    }

    public void record(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens
    ) {
        AiUsageRecordEntity entity = new AiUsageRecordEntity();
        entity.setUserId(userId);
        entity.setModelName(blankTo(modelName, "unknown-model"));
        entity.setScene(blankTo(scene, "analyze"));
        entity.setAction(blankTo(action, "论文解析"));
        entity.setPaperTitle(blankTo(paperTitle, "当前论文"));
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setTotalTokens(totalTokens);
        repository.save(entity);
    }

    public void recordAndCharge(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens
    ) {
        if (userId == null || totalTokens <= 0) return;
        appUserRepository.findById(userId).ifPresent(user -> {
            long current = user.getTokenUsed() == null ? 0L : user.getTokenUsed();
            user.setTokenUsed(current + totalTokens);
            appUserRepository.save(user);
        });
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens);
    }

    public Map<String, Object> summary() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        List<AiUsageRecordEntity> recent = repository.findTop240ByUserIdOrderByCreatedAtDesc(user.getId());
        boolean showingAllUsers = false;
        if (recent.isEmpty()) {
            recent = repository.findTop240ByOrderByCreatedAtDesc();
            showingAllUsers = !recent.isEmpty();
        }
        long promptTokens = recent.stream().mapToLong(r -> safe(r.getPromptTokens())).sum();
        long completionTokens = recent.stream().mapToLong(r -> safe(r.getCompletionTokens())).sum();
        long totalTokens = recent.stream().mapToLong(r -> safe(r.getTotalTokens())).sum();
        LocalDateTime weekStart = LocalDateTime.now().minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime minuteStart = LocalDateTime.now().minusMinutes(1);
        long weekTokens = (showingAllUsers
            ? repository.findByCreatedAtAfterOrderByCreatedAtDesc(weekStart)
            : repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), weekStart)
        ).stream().mapToLong(r -> safe(r.getTotalTokens())).sum();
        List<AiUsageRecordEntity> todayRecords = showingAllUsers
            ? repository.findByCreatedAtAfterOrderByCreatedAtDesc(todayStart)
            : repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), todayStart);
        List<AiUsageRecordEntity> minuteRecords = showingAllUsers
            ? repository.findByCreatedAtAfterOrderByCreatedAtDesc(minuteStart)
            : repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), minuteStart);
        long todayTokens = todayRecords.stream().mapToLong(r -> safe(r.getTotalTokens())).sum();
        long minuteTokens = minuteRecords.stream().mapToLong(r -> safe(r.getTotalTokens())).sum();
        long totalRequests = showingAllUsers ? repository.count() : repository.countByUserId(user.getId());
        long todayRequests = showingAllUsers
            ? repository.countByCreatedAtAfter(todayStart)
            : repository.countByUserIdAndCreatedAtAfter(user.getId(), todayStart);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", inferPlanId(safe(user.getTokenLimit())));
        result.put("planName", inferPlanName(safe(user.getTokenLimit())));
        result.put("tokenQuota", safe(user.getTokenLimit()));
        result.put("tokenUsed", safe(user.getTokenUsed()));
        result.put("tokenRemaining", Math.max(0L, safe(user.getTokenLimit()) - safe(user.getTokenUsed())));
        result.put("resetAt", LocalDate.now().plusMonths(1).withDayOfMonth(1).toString());
        result.put("promptTokens", promptTokens);
        result.put("completionTokens", completionTokens);
        result.put("weekTokens", weekTokens);
        result.put("estimatedCost", ((double) totalTokens / 1000D) * 0.02D);
        result.put("totalRequests", totalRequests);
        result.put("todayRequests", todayRequests);
        result.put("todayTokens", todayTokens);
        result.put("rpm", minuteRecords.size());
        result.put("tpm", minuteTokens);
        result.put("mpm", ((double) minuteTokens / 1000D) * 0.02D);
        result.put("currentMinuteCost", ((double) minuteTokens / 1000D) * 0.02D);
        result.put("usageScope", showingAllUsers ? "all" : "current");
        result.put("dailyUsage", buildDailyUsage(recent));
        result.put("modelBreakdown", buildBreakdown(recent, "model"));
        result.put("sceneBreakdown", buildBreakdown(recent, "scene"));
        result.put("actionBreakdown", buildBreakdown(recent, "action"));
        result.put("recentCalls", buildRecentCalls(recent));
        return result;
    }

    private List<Map<String, Object>> buildDailyUsage(List<AiUsageRecordEntity> recent) {
        Map<LocalDate, Long> daily = new LinkedHashMap<>();
        Map<LocalDate, Long> calls = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            daily.put(day, 0L);
            calls.put(day, 0L);
        }
        for (AiUsageRecordEntity record : recent) {
            LocalDate day = record.getCreatedAt() == null ? null : record.getCreatedAt().toLocalDate();
            if (day != null && daily.containsKey(day)) {
                daily.put(day, daily.get(day) + safe(record.getTotalTokens()));
                calls.put(day, calls.get(day) + 1L);
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : daily.entrySet()) {
            rows.add(Map.of(
                "label", entry.getKey().format(DAY_LABEL),
                "tokens", entry.getValue(),
                "calls", calls.getOrDefault(entry.getKey(), 0L)
            ));
        }
        return rows;
    }

    private List<Map<String, Object>> buildBreakdown(List<AiUsageRecordEntity> recent, String type) {
        Map<String, Long> bucket = new LinkedHashMap<>();
        long total = 0L;
        for (AiUsageRecordEntity record : recent) {
            String key = switch (type) {
                case "scene" -> blankTo(record.getScene(), "analyze");
                case "action" -> blankTo(record.getAction(), "论文解析");
                default -> blankTo(record.getModelName(), "unknown-model");
            };
            long tokens = safe(record.getTotalTokens());
            total += tokens;
            bucket.put(key, bucket.getOrDefault(key, 0L) + tokens);
        }
        long denominator = Math.max(total, 1L);
        return bucket.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
            .limit(6)
            .map(entry -> {
                Map<String, Object> row = new LinkedHashMap<>();
                if ("action".equals(type)) row.put("action", entry.getKey());
                else if ("scene".equals(type)) row.put("scene", entry.getKey());
                else row.put("model", entry.getKey());
                row.put("tokens", entry.getValue());
                row.put("share", Math.max(1, Math.round((entry.getValue() * 100f) / denominator)));
                return row;
            })
            .toList();
    }

    private List<Map<String, Object>> buildRecentCalls(List<AiUsageRecordEntity> recent) {
        return recent.stream().limit(12).map(record -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", record.getCreatedAt() == null ? "" : record.getCreatedAt().format(TIME_LABEL));
            row.put("action", blankTo(record.getAction(), "论文解析"));
            row.put("paper", blankTo(record.getPaperTitle(), "当前论文"));
            row.put("model", blankTo(record.getModelName(), "unknown-model"));
            row.put("tokens", safe(record.getTotalTokens()));
            row.put("promptTokens", safe(record.getPromptTokens()));
            row.put("completionTokens", safe(record.getCompletionTokens()));
            return row;
        }).toList();
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String blankTo(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private String inferPlanId(long tokenLimit) {
        if (tokenLimit >= 1_000_000L) return "elite";
        if (tokenLimit >= 500_000L) return "pro";
        if (tokenLimit >= 150_000L) return "plus";
        return "starter";
    }

    private String inferPlanName(long tokenLimit) {
        if (tokenLimit >= 1_000_000L) return "Elite 课题组";
        if (tokenLimit >= 500_000L) return "Pro 深度阅读";
        if (tokenLimit >= 150_000L) return "Plus 论文冲刺";
        return "Starter 入门版";
    }
}
