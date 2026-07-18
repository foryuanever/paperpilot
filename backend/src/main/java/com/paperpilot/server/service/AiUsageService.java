package com.paperpilot.server.service;

import com.paperpilot.server.entity.AiUsageRecordEntity;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.ModelConfigRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AiUsageService {
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter TIME_LABEL = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final AiUsageRecordRepository repository;
    private final AppUserRepository appUserRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final CurrentUserService currentUserService;
    private final BillingService billingService;
    private final MembershipService membershipService;

    public AiUsageService(
        AiUsageRecordRepository repository,
        AppUserRepository appUserRepository,
        ModelConfigRepository modelConfigRepository,
        CurrentUserService currentUserService,
        BillingService billingService,
        MembershipService membershipService
    ) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.currentUserService = currentUserService;
        this.billingService = billingService;
        this.membershipService = membershipService;
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
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens, "success", "", 0L);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        String status,
        String errorMessage,
        long latencyMs
    ) {
        AiUsageRecordEntity entity = new AiUsageRecordEntity();
        entity.setUserId(userId);
        appUserRepository.findById(userId).ifPresent(user -> {
            entity.setUsername(blankTo(user.getUsername(), ""));
            entity.setUserEmail(blankTo(user.getEmail(), ""));
        });
        entity.setModelName(blankTo(modelName, "unknown-model"));
        entity.setScene(blankTo(scene, "analyze"));
        entity.setAction(blankTo(action, "论文解析"));
        entity.setPaperTitle(blankTo(paperTitle, "当前论文"));
        entity.setPromptTokens(promptTokens);
        entity.setCompletionTokens(completionTokens);
        entity.setTotalTokens(totalTokens);
        entity.setStatus(blankTo(status, "success"));
        entity.setErrorMessage(clip(errorMessage, 760));
        entity.setLatencyMs(Math.max(0L, latencyMs));
        entity.setUnitPrice(billingService.unitPrice());
        entity.setBillingMultiplier(billingService.multiplier());
        entity.setChargeAmount("success".equalsIgnoreCase(status) ? billingService.calculateCharge(action, totalTokens) : 0.0D);
        repository.save(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            membershipService.consume(user, action);
        });
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAndCharge(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        long latencyMs
    ) {
        if (userId == null || totalTokens <= 0) return;
        appUserRepository.findById(userId).ifPresent(user -> {
            long current = user.getTokenUsed() == null ? 0L : user.getTokenUsed();
            user.setTokenUsed(current + totalTokens);
            appUserRepository.save(user);
            membershipService.consume(user, action);
        });
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens, "success", "", latencyMs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        String errorMessage,
        long latencyMs
    ) {
        if (userId == null) return;
        record(userId, modelName, scene, action, paperTitle, Math.max(0L, promptTokens), 0L, Math.max(0L, promptTokens), "failed", errorMessage, latencyMs);
    }

    public Map<String, Object> adminCalls(String keyword, String scene, String model, String status, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(5, pageSize));
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        List<Long> matchedUserIds = StringUtils.hasText(trimmedKeyword)
            ? appUserRepository.findAll().stream()
                .filter(user ->
                    String.valueOf(user.getId()).contains(trimmedKeyword)
                        || blankTo(user.getUsername(), "").toLowerCase().contains(trimmedKeyword)
                        || blankTo(user.getEmail(), "").toLowerCase().contains(trimmedKeyword)
                )
                .map(AppUserEntity::getId)
                .toList()
            : List.of();
        Specification<AiUsageRecordEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(trimmedKeyword)) {
                String like = "%" + trimmedKeyword + "%";
                List<Predicate> keywordPredicates = new ArrayList<>(List.of(
                    cb.like(cb.lower(root.get("username")), like),
                    cb.like(cb.lower(root.get("userEmail")), like),
                    cb.like(cb.lower(root.get("paperTitle")), like),
                    cb.like(root.get("userId").as(String.class), like)
                ));
                if (!matchedUserIds.isEmpty()) {
                    keywordPredicates.add(root.get("userId").in(matchedUserIds));
                }
                predicates.add(cb.or(keywordPredicates.toArray(Predicate[]::new)));
            }
            if (StringUtils.hasText(scene) && !"全部".equals(scene)) {
                predicates.add(cb.equal(root.get("scene"), scene.trim()));
            }
            if (StringUtils.hasText(model)) {
                predicates.add(cb.like(cb.lower(root.get("modelName")), "%" + model.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(status) && !"全部".equals(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<AiUsageRecordEntity> result = repository.findAll(
            spec,
            PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<AiUsageRecordEntity> rows = result.getContent();
        long inputTokens = rows.stream().mapToLong(r -> safe(r.getPromptTokens())).sum();
        long outputTokens = rows.stream().mapToLong(r -> safe(r.getCompletionTokens())).sum();
        long failed = rows.stream().filter(r -> "failed".equalsIgnoreCase(blankTo(r.getStatus(), ""))).count();
        double cost = rows.stream().mapToDouble(this::chargeOf).sum();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("page", safePage);
        response.put("pageSize", safeSize);
        response.put("total", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("summary", Map.of(
            "inputTokens", inputTokens,
            "outputTokens", outputTokens,
            "totalTokens", inputTokens + outputTokens,
            "failed", failed,
            "cost", money(cost)
        ));
        response.put("rows", rows.stream().map(this::adminCallRow).toList());
        return response;
    }

    public Map<String, Object> clearAdminCalls() {
        currentUserService.requireAdmin();
        long removed = repository.count();
        repository.deleteAllInBatch();
        return Map.of("removed", removed);
    }

    public Map<String, Object> summary() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        boolean showingAllUsers = true;
        List<AiUsageRecordEntity> recent = repository.findTop240ByOrderByCreatedAtDesc();
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
        result.put("membership", membershipService.membership(user));
        result.put("plans", membershipService.catalog());
        result.put("promptTokens", promptTokens);
        result.put("completionTokens", completionTokens);
        result.put("weekTokens", weekTokens);
        result.put("totalRequests", totalRequests);
        result.put("todayRequests", todayRequests);
        result.put("todayTokens", todayTokens);
        result.put("rpm", minuteRecords.size());
        result.put("tpm", minuteTokens);
        result.put("usageScope", "current");
        result.put("dailyUsage", buildDailyUsage(recent));
        result.put("modelBreakdown", buildBreakdown(recent, "model"));
        result.put("sceneBreakdown", buildBreakdown(recent, "scene"));
        result.put("actionBreakdown", buildBreakdown(recent, "action"));
        result.put("activeModels", buildActiveModels(recent));
        result.put("recentCalls", buildRecentCalls(recent));
        return result;
    }

    private List<Map<String, Object>> buildActiveModels(List<AiUsageRecordEntity> recent) {
        Map<String, Long> tokensByModel = new LinkedHashMap<>();
        for (AiUsageRecordEntity record : recent) {
            String modelName = blankTo(record.getModelName(), "unknown-model");
            tokensByModel.put(modelName, tokensByModel.getOrDefault(modelName, 0L) + safe(record.getTotalTokens()));
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        addActiveModel(
            rows,
            tokensByModel,
            ModelConfigService.SCENE_MEETING_DECK,
            "组会 PPT Agent",
            "PPT 专用中转站",
            "gpt-5.4",
            "PPT 生成完成后按日志与材料入账"
        );
        addActiveModel(
            rows,
            tokensByModel,
            ModelConfigService.SCENE_GENERAL,
            "通用模型池",
            "OpenCode Free",
            "deepseek-v4-flash-free",
            "聊天、翻译、综述等普通调用按网关返回入账"
        );
        return rows;
    }

    private void addActiveModel(
        List<Map<String, Object>> rows,
        Map<String, Long> tokensByModel,
        String scene,
        String label,
        String fallbackProvider,
        String fallbackModel,
        String accountingRule
    ) {
        ModelConfigEntity active = modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(scene).orElse(null);
        String modelName = active == null ? fallbackModel : blankTo(active.getModelName(), fallbackModel);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("scene", scene);
        row.put("label", label);
        row.put("providerName", active == null ? fallbackProvider : blankTo(active.getProviderName(), fallbackProvider));
        row.put("modelName", modelName);
        row.put("apiFormat", active == null ? "openai_chat" : blankTo(active.getApiFormat(), "openai_chat"));
        row.put("baseUrl", active == null ? "" : blankTo(active.getBaseUrl(), ""));
        row.put("configured", active != null);
        row.put("recordedTokens", tokensByModel.getOrDefault(modelName, 0L));
        row.put("accountingRule", accountingRule);
        rows.add(row);
    }

    private List<Map<String, Object>> buildDailyUsage(List<AiUsageRecordEntity> recent) {
        Map<LocalDate, Long> daily = new LinkedHashMap<>();
        Map<LocalDate, Long> calls = new LinkedHashMap<>();
        Map<LocalDate, Double> costs = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            daily.put(day, 0L);
            calls.put(day, 0L);
            costs.put(day, 0.0D);
        }
        for (AiUsageRecordEntity record : recent) {
            LocalDate day = record.getCreatedAt() == null ? null : record.getCreatedAt().toLocalDate();
            if (day != null && daily.containsKey(day)) {
                daily.put(day, daily.get(day) + safe(record.getTotalTokens()));
                calls.put(day, calls.get(day) + 1L);
                costs.put(day, costs.get(day) + chargeOf(record));
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, Long> entry : daily.entrySet()) {
            rows.add(Map.of(
                "label", entry.getKey().format(DAY_LABEL),
                "tokens", entry.getValue(),
                "calls", calls.getOrDefault(entry.getKey(), 0L),
                "cost", money(costs.getOrDefault(entry.getKey(), 0.0D))
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
        String fallbackPaperTitle = recent.stream()
            .map(AiUsageRecordEntity::getPaperTitle)
            .filter(this::isSpecificPaperTitle)
            .findFirst()
            .orElse("未关联论文标题");
        return recent.stream().limit(12).map(record -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("time", record.getCreatedAt() == null ? "" : record.getCreatedAt().format(TIME_LABEL));
            row.put("action", normalizeAction(record.getAction()));
            row.put("paper", displayPaperTitle(record.getPaperTitle(), fallbackPaperTitle));
            row.put("model", blankTo(record.getModelName(), "unknown-model"));
            row.put("tokens", safe(record.getTotalTokens()));
            row.put("promptTokens", safe(record.getPromptTokens()));
            row.put("completionTokens", safe(record.getCompletionTokens()));
            row.put("status", blankTo(record.getStatus(), "success"));
            return row;
        }).toList();
    }

    private Map<String, Object> adminCallRow(AiUsageRecordEntity record) {
        AppUserEntity user = record.getUserId() == null ? null : appUserRepository.findById(record.getUserId()).orElse(null);
        String username = blankTo(record.getUsername(), user == null ? "未知用户" : blankTo(user.getUsername(), "未知用户"));
        String email = blankTo(record.getUserEmail(), user == null ? "" : blankTo(user.getEmail(), ""));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", record.getId());
        row.put("time", record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        row.put("userId", record.getUserId());
        row.put("username", username);
        row.put("userEmail", email);
        row.put("scene", blankTo(record.getScene(), "analyze"));
        row.put("sceneLabel", sceneLabel(record.getScene()));
        row.put("action", normalizeAction(record.getAction()));
        row.put("rawAction", blankTo(record.getAction(), ""));
        row.put("model", blankTo(record.getModelName(), "unknown-model"));
        row.put("paper", displayPaperTitle(record.getPaperTitle(), "未关联论文"));
        row.put("promptTokens", safe(record.getPromptTokens()));
        row.put("completionTokens", safe(record.getCompletionTokens()));
        row.put("totalTokens", safe(record.getTotalTokens()));
        row.put("chargeAmount", chargeOf(record));
        row.put("unitPrice", unitPriceOf(record));
        row.put("billingMultiplier", multiplierOf(record));
        row.put("status", blankTo(record.getStatus(), "success"));
        row.put("latencyMs", safe(record.getLatencyMs()));
        row.put("errorMessage", blankTo(record.getErrorMessage(), ""));
        Map<String, Object> fallback = fallbackResolution(record);
        row.put("fallbackResolved", fallback.get("resolved"));
        row.put("fallbackModel", fallback.get("model"));
        row.put("fallbackTime", fallback.get("time"));
        return row;
    }

    private Map<String, Object> fallbackResolution(AiUsageRecordEntity record) {
        if (!"failed".equalsIgnoreCase(blankTo(record.getStatus(), "")) || record.getCreatedAt() == null || record.getUserId() == null) {
            return Map.of("resolved", false, "model", "", "time", "");
        }
        LocalDateTime start = record.getCreatedAt();
        LocalDateTime end = start.plusMinutes(3);
        List<AiUsageRecordEntity> laterSuccess = repository.findTop3ByUserIdAndSceneAndActionAndStatusAndCreatedAtBetweenOrderByCreatedAtAsc(
            record.getUserId(),
            blankTo(record.getScene(), ""),
            blankTo(record.getAction(), ""),
            "success",
            start,
            end
        );
        AiUsageRecordEntity success = laterSuccess.stream()
            .filter(item -> item.getId() != null && record.getId() != null && item.getId() > record.getId())
            .findFirst()
            .orElse(null);
        if (success == null) return Map.of("resolved", false, "model", "", "time", "");
        return Map.of(
            "resolved", true,
            "model", blankTo(success.getModelName(), "后续模型"),
            "time", success.getCreatedAt() == null ? "" : success.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }

    private String normalizeAction(String action) {
        String value = blankTo(action, "");
        if (value.contains("PPT") || value.contains("Agent")) return "组会PPT Agent执行";
        if (value.contains("审核")) return "AI发帖审核";
        if (value.contains("选题")) return "选题调研";
        if (value.contains("综述") || value.contains("汇报") || value.contains("组会")) return "论文综述生成";
        if (value.contains("翻译")) return "论文翻译";
        return "AI文章对话";
    }

    private String sceneLabel(String scene) {
        return switch (blankTo(scene, "")) {
            case ModelConfigService.SCENE_PAPER_REVIEW, "summary", "report" -> "论文综述";
            case ModelConfigService.SCENE_PAPER_QA, "qa", "analyze" -> "AI论文问答";
            case ModelConfigService.SCENE_TOPIC_RESEARCH -> "选题研究";
            case ModelConfigService.SCENE_MEETING_DECK -> "PPT生成";
            case ModelConfigService.SCENE_FORUM_MODERATION -> "AI发帖审核";
            case "translate" -> "全文翻译";
            default -> blankTo(scene, "未知模块");
        };
    }

    private String displayPaperTitle(String paperTitle, String fallback) {
        if (isSpecificPaperTitle(paperTitle)) return paperTitle.trim();
        return fallback;
    }

    private boolean isSpecificPaperTitle(String paperTitle) {
        if (paperTitle == null || paperTitle.isBlank()) return false;
        String title = paperTitle.trim();
        return !"当前论文".equals(title)
            && !"未命名论文".equals(title)
            && !"当前任务".equals(title)
            && !"-".equals(title);
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String clip(String text, int maxLength) {
        String value = Objects.toString(text, "").trim();
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String blankTo(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text;
    }

    private double money(Double value) {
        return value == null ? 0.0D : value;
    }

    private double chargeOf(AiUsageRecordEntity record) {
        if ("failed".equalsIgnoreCase(blankTo(record.getStatus(), ""))) return 0.0D;
        double saved = money(record.getChargeAmount());
        double calculated = billingService.calculateCharge(record.getAction(), safe(record.getTotalTokens()));
        if (billingService.isPptAgentAction(record.getAction())) return calculated;
        if (saved > 0) return saved;
        return calculated;
    }

    private double unitPriceOf(AiUsageRecordEntity record) {
        double saved = money(record.getUnitPrice());
        return saved > 0 ? saved : billingService.unitPrice();
    }

    private double multiplierOf(AiUsageRecordEntity record) {
        double saved = money(record.getBillingMultiplier());
        return saved > 0 ? saved : billingService.multiplier();
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
