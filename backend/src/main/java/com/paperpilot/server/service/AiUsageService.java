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
import com.paperpilot.server.entity.CheckinEntity;
import com.paperpilot.server.entity.PaymentOrderEntity;
import com.paperpilot.server.entity.RechargeRecordEntity;
import com.paperpilot.server.entity.ReferralRecordEntity;
import com.paperpilot.server.repository.CheckinRepository;
import com.paperpilot.server.repository.PaymentOrderRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import com.paperpilot.server.repository.ReferralRecordRepository;
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
    private final CheckinRepository checkinRepository;
    private final RechargeRecordRepository rechargeRecordRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final ReferralRecordRepository referralRecordRepository;

    public AiUsageService(
        AiUsageRecordRepository repository,
        AppUserRepository appUserRepository,
        ModelConfigRepository modelConfigRepository,
        CurrentUserService currentUserService,
        BillingService billingService,
        MembershipService membershipService,
        CheckinRepository checkinRepository,
        RechargeRecordRepository rechargeRecordRepository,
        PaymentOrderRepository paymentOrderRepository,
        ReferralRecordRepository referralRecordRepository
    ) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.currentUserService = currentUserService;
        this.billingService = billingService;
        this.membershipService = membershipService;
        this.checkinRepository = checkinRepository;
        this.rechargeRecordRepository = rechargeRecordRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.referralRecordRepository = referralRecordRepository;
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
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens, "success", "", 0L, null);
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
        record(userId, modelName, scene, action, paperTitle, promptTokens, completionTokens, totalTokens, status, errorMessage, latencyMs, null);
    }

    private void record(
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
        long latencyMs,
        String requestKey
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
        entity.setRequestKey(StringUtils.hasText(requestKey) ? clip(requestKey.trim(), 150) : null);
        entity.setPaperTitle(blankTo(paperTitle, "当前论文"));
        long safePromptTokens = Math.max(0L, promptTokens);
        long safeCompletionTokens = Math.max(0L, completionTokens);
        long safeTotalTokens = Math.max(0L, totalTokens);
        if (safeTotalTokens <= 0L) safeTotalTokens = safePromptTokens + safeCompletionTokens;
        if (safeTotalTokens > 0L && safeCompletionTokens <= 0L && safePromptTokens > 0L) {
            safeCompletionTokens = Math.max(0L, safeTotalTokens - safePromptTokens);
        }
        if (safeTotalTokens > 0L && safePromptTokens <= 0L && safeCompletionTokens > 0L) {
            safePromptTokens = Math.max(0L, safeTotalTokens - safeCompletionTokens);
        }
        entity.setPromptTokens(safePromptTokens);
        entity.setCompletionTokens(safeCompletionTokens);
        entity.setTotalTokens(safeTotalTokens);
        entity.setStatus(blankTo(status, "success"));
        entity.setErrorMessage(clip(errorMessage, 760));
        entity.setLatencyMs(Math.max(0L, latencyMs));
        entity.setUnitPrice(billingService.unitPrice());
        entity.setBillingMultiplier(billingService.multiplier());
        entity.setChargeAmount("success".equalsIgnoreCase(status) ? billingService.calculateCharge(action, safePromptTokens, safeCompletionTokens) : 0.0D);
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
        long safePromptTokens = Math.max(0L, promptTokens);
        long safeCompletionTokens = Math.max(0L, completionTokens);
        long safeTotalTokens = Math.max(0L, totalTokens);
        if (safeTotalTokens <= 0L) safeTotalTokens = safePromptTokens + safeCompletionTokens;
        if (userId == null) return;
        appUserRepository.deductPoints(userId, 1);
        record(userId, modelName, scene, action, paperTitle, safePromptTokens, safeCompletionTokens, safeTotalTokens);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAndChargeSpecial(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        int pointsToDeduct
    ) {
        recordAndChargeSpecial(userId, modelName, scene, action, paperTitle, promptTokens,
            completionTokens, totalTokens, pointsToDeduct, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized void recordAndChargeSpecial(
        Long userId,
        String modelName,
        String scene,
        String action,
        String paperTitle,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        int pointsToDeduct,
        String requestKey
    ) {
        long safePromptTokens = Math.max(0L, promptTokens);
        long safeCompletionTokens = Math.max(0L, completionTokens);
        long safeTotalTokens = Math.max(0L, totalTokens);
        if (safeTotalTokens <= 0L) safeTotalTokens = safePromptTokens + safeCompletionTokens;
        if (userId == null) return;
        String normalizedRequestKey = StringUtils.hasText(requestKey) ? clip(requestKey.trim(), 150) : null;
        if (normalizedRequestKey != null && repository.existsByUserIdAndRequestKey(userId, normalizedRequestKey)) return;
        appUserRepository.deductPoints(userId, pointsToDeduct);
        record(userId, modelName, scene, action, paperTitle, safePromptTokens, safeCompletionTokens,
            safeTotalTokens, "success", "", 0L, normalizedRequestKey);
    }

    public void assertAgentAvailable(Long userId) {
        membershipService.assertAgentAvailable(userId);
    }

    public void assertPointsAvailable(Long userId, int required, String feature) {
        if (userId == null || required <= 0) return;
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("用户不存在"));
        int available = Math.max(0, user.getFruitScore() == null ? 0 : user.getFruitScore());
        if (available < required) {
            throw new IllegalStateException((feature == null ? "该功能" : feature) + "需要 " + required + " 积分，当前积分不足");
        }
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
        long safePromptTokens = Math.max(0L, promptTokens);
        long safeCompletionTokens = Math.max(0L, completionTokens);
        long safeTotalTokens = Math.max(0L, totalTokens);
        if (safeTotalTokens <= 0L) safeTotalTokens = safePromptTokens + safeCompletionTokens;
        if (userId == null) return;
        appUserRepository.deductPoints(userId, 1);
        record(userId, modelName, scene, action, paperTitle, safePromptTokens, safeCompletionTokens, safeTotalTokens, "success", "", latencyMs);
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

    public Map<String, Object> adminCalls(String keyword, String scene, String model, String status, String startDate, String endDate, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(5, pageSize <= 0 ? 10 : pageSize));
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : "";
        LocalDateTime startAt = parseStartDate(startDate);
        LocalDateTime endBefore = parseEndDate(endDate);
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
            predicates.add(cb.not(root.get("scene").in("translate", "immersive")));
            if (StringUtils.hasText(status) && !"全部".equals(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (startAt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startAt));
            }
            if (endBefore != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), endBefore));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<AiUsageRecordEntity> result = repository.findAll(
            spec,
            PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<AiUsageRecordEntity> rows = result.getContent();
        List<AiUsageRecordEntity> summaryRows = repository.findAll(spec);
        long inputTokens = summaryRows.stream().mapToLong(r -> safe(r.getPromptTokens())).sum();
        long outputTokens = summaryRows.stream().mapToLong(r -> safe(r.getCompletionTokens())).sum();
        long failed = summaryRows.stream().filter(r -> "failed".equalsIgnoreCase(blankTo(r.getStatus(), ""))).count();
        double cost = summaryRows.stream().mapToDouble(this::chargeOf).sum();
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
            "matchedCalls", summaryRows.size(),
            "cost", money(cost)
        ));
        response.put("rows", rows.stream().map(this::adminCallRow).toList());
        return response;
    }

    private LocalDateTime parseStartDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String value) {
        if (!StringUtils.hasText(value)) return null;
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay();
    }

    public Map<String, Object> clearAdminCalls() {
        currentUserService.requireAdmin();
        long removed = repository.count();
        repository.deleteAllInBatch();
        return Map.of("removed", removed);
    }

    public Map<String, Object> deleteAdminCall(Long id) {
        currentUserService.requireAdmin();
        if (id == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "ID不能为空");
        }
        boolean exists = repository.existsById(id);
        if (exists) {
            repository.deleteById(id);
        }
        return Map.of("success", exists);
    }

    public Map<String, Object> summary() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        boolean showingAllUsers = false;
        List<AiUsageRecordEntity> recent = showingAllUsers
            ? repository.findTop240ByOrderByCreatedAtDesc()
            : repository.findTop240ByUserIdOrderByCreatedAtDesc(user.getId());
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
        long tokenQuota = user.getTokenLimit() == null ? 0L : Math.max(0L, user.getTokenLimit());
        long tokenUsed = user.getTokenUsed() == null ? 0L : Math.max(0L, user.getTokenUsed());
        result.put("tokenQuota", tokenQuota);
        result.put("tokenUsed", tokenUsed);
        result.put("tokenRemaining", Math.max(0L, tokenQuota - tokenUsed));
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
        result.put("modelBreakdown", List.of());
        result.put("sceneBreakdown", buildBreakdown(recent, "scene"));
        result.put("actionBreakdown", buildBreakdown(recent, "action"));
        result.put("activeModels", List.of());
        result.put("recentCalls", buildRecentCalls(recent));
        return result;
    }

    public Map<String, Object> details(String benefit, String startDate, String endDate, int page, int pageSize) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        LocalDateTime oldestAllowed = LocalDateTime.now().minusDays(30);
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        if (start == null || start.isBefore(oldestAllowed)) start = oldestAllowed;
        if (end == null) end = LocalDateTime.now().plusDays(1);
        String wanted = StringUtils.hasText(benefit) ? benefit.trim().toLowerCase() : "";
        final LocalDateTime endAt = end;

        if ("agent".equals(wanted)) {
            List<Map<String, Object>> agentLogs = new ArrayList<>();
            final LocalDateTime startAt = start;
            
            // 1. AI Usage Point Deductions (special actions, hide model name)
            List<AiUsageRecordEntity> aiUsage = repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), startAt);
            for (AiUsageRecordEntity row : aiUsage) {
                if (!endAt.isBefore(row.getCreatedAt())) {
                    int pointsDelta = usagePointsDelta(row);
                    boolean isPointsDeduction = pointsDelta < 0;
                    if (isPointsDeduction) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", "ai-" + row.getId());
                        item.put("time", row.getCreatedAt());
                        item.put("paper", row.getPaperTitle());
                        item.put("scene", row.getScene());
                        item.put("sceneLabel", sceneLabel(row.getScene(), row.getAction()));
                        item.put("action", row.getAction());
                        item.put("delta", pointsDelta);
                        item.put("status", isSuccessfulUsage(row) ? "扣减成功" : "不计次");
                        item.put("effective", isSuccessfulUsage(row));
                        agentLogs.add(item);
                    }
                }
            }
            
            // 2. Check-ins (Points Gain)
            List<String> memberIds = new ArrayList<>();
            if (user.getEmail() != null && !user.getEmail().isBlank()) memberIds.add(user.getEmail());
            if (user.getUsername() != null && !user.getUsername().isBlank()) memberIds.add(user.getUsername());
            if (user.getId() != null) memberIds.add("m-" + user.getId());
            
            java.util.Set<Long> seenCheckinIds = new java.util.HashSet<>();
            for (String mId : memberIds) {
                List<CheckinEntity> checkins = checkinRepository.findAllByMemberIdOrderByDateDesc(mId);
                for (CheckinEntity checkin : checkins) {
                    if (checkin.getId() != null && !seenCheckinIds.add(checkin.getId())) continue;
                    LocalDateTime cTime = checkin.getCreatedAt();
                    if (cTime != null && !cTime.isBefore(startAt) && !cTime.isAfter(endAt)) {
                        String awardType = blankTo(checkin.getAwardType(), "");
                        String awardName = blankTo(checkin.getAwardName(), "");
                        if ("nothing".equalsIgnoreCase(awardType)
                            || "遗憾未中奖".equals(awardName)) {
                            continue;
                        }
                        // Translation/full-reading check-in rewards are quota grants,
                        // not AI points. Keep them in their own entitlement ledger.
                        boolean isTranslationQuotaAward = "bilingual_translate".equalsIgnoreCase(awardType)
                            || "full_translate".equalsIgnoreCase(awardType)
                            || awardName.contains("对照翻译")
                            || awardName.contains("全文翻译")
                            || awardName.contains("沉浸翻译");
                        if (isTranslationQuotaAward) continue;
                        int pts = checkin.getFruitAward() != null && checkin.getFruitAward() > 0 ? checkin.getFruitAward() : 0;
                        if (pts > 0 || !awardName.isBlank()) {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", "checkin-" + checkin.getId());
                            item.put("time", cTime);
                            item.put("paper", "每日签到奖励");
                            item.put("scene", "checkin");
                            item.put("sceneLabel", "签到福利 (" + (!awardName.isBlank() ? awardName : ("+" + pts + "积分")) + ")");
                            item.put("action", "checkin");
                            item.put("delta", pts > 0 ? pts : 1);
                            item.put("status", "发放成功");
                            item.put("effective", true);
                            agentLogs.add(item);
                        }
                    }
                }
            }

            // 3. Successful invitations are also entitlement records and must appear
            // in the same points ledger as check-in and administrator gifts.
            for (ReferralRecordEntity referral : referralRecordRepository.findAllByReferrerIdOrderByCreatedAtDesc(user.getId())) {
                LocalDateTime rTime = referral.getCreatedAt();
                if (rTime != null && !rTime.isBefore(startAt) && !rTime.isAfter(endAt)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", "referral-" + referral.getId());
                    item.put("time", rTime);
                    item.put("paper", "邀请奖励");
                    item.put("scene", "referral");
                    item.put("sceneLabel", "邀请奖励");
                    item.put("action", "referral-reward");
                    item.put("delta", referral.getPointsReward());
                    item.put("status", "发放成功");
                    item.put("effective", true);
                    agentLogs.add(item);
                }
            }

            // The invitee receives the same reward and must see its own ledger entry.
            for (ReferralRecordEntity referral : referralRecordRepository.findAllByInviteeIdOrderByCreatedAtDesc(user.getId())) {
                LocalDateTime rTime = referral.getCreatedAt();
                if (rTime != null && !rTime.isBefore(startAt) && !rTime.isAfter(endAt)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", "referral-invitee-" + referral.getId());
                    item.put("time", rTime);
                    item.put("paper", "邀请奖励");
                    item.put("scene", "referral");
                    item.put("sceneLabel", "邀请奖励");
                    item.put("action", "referral-reward");
                    item.put("delta", referral.getPointsReward());
                    item.put("status", "发放成功");
                    item.put("effective", true);
                    agentLogs.add(item);
                }
            }
            
            // 4. Recharge & Promo Codes (Points Gain)
            List<RechargeRecordEntity> recharges = rechargeRecordRepository.findAll();
            for (RechargeRecordEntity recharge : recharges) {
                // Legacy recharge rows stored model-token quotas in `tokens`.
                // Only explicit point grants belong in the AI points ledger.
                if ("points".equals(recharge.getRecordType())
                    && user.getEmail().equalsIgnoreCase(recharge.getEmail())) {
                    LocalDateTime rTime = recharge.getCreatedAt();
                    if (rTime != null && !rTime.isBefore(startAt) && !rTime.isAfter(endAt)) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", "recharge-" + recharge.getId());
                        item.put("time", rTime);
                        item.put("paper", "积分充值与兑换");
                        item.put("scene", "recharge");
                        item.put("sceneLabel", "套餐充值");
                        item.put("action", "recharge");
                        item.put("delta", recharge.getPointsGranted() != null ? recharge.getPointsGranted().intValue() : 0);
                        item.put("status", "到账成功");
                        item.put("effective", true);
                        agentLogs.add(item);
                    }
                }
            }
            
            // 5. Admin gift records, Penalty/Deduct records & Restore records
            List<PaymentOrderEntity> giftOrders = new ArrayList<>();
            giftOrders.addAll(paymentOrderRepository.findByUserIdAndPlanIdStartingWithOrderByCreatedAtDesc(user.getId(), "gift-"));
            giftOrders.addAll(paymentOrderRepository.findByUserIdAndPlanIdStartingWithOrderByCreatedAtDesc(user.getId(), "deduct-"));
            for (PaymentOrderEntity order : giftOrders) {
                LocalDateTime oTime = order.getCreatedAt();
                if (oTime != null && !oTime.isBefore(startAt) && !oTime.isAfter(endAt)) {
                    String msg = order.getMessage();
                    if ("gift-restore-all".equals(order.getPlanId()) || (msg != null && msg.contains("恢复满血"))) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", "restore-" + order.getOrderNo());
                        item.put("time", oTime);
                        item.put("paper", "全站会员额度恢复");
                        item.put("scene", "restore-benefit");
                        item.put("sceneLabel", "重置福利");
                        item.put("action", "restore-benefit");
                        item.put("delta", 0);
                        item.put("deltaLabel", "满血");
                        item.put("status", "恢复成功");
                        item.put("effective", true);
                        agentLogs.add(item);
                    } else if (msg != null && (msg.contains("AI积分") || order.getPlanId().contains("fruitScore") || order.getPlanId().contains("score"))) {
                        int delta = 0;
                        try {
                            // parse delta from message like "管理员赠送：AI积分 +5 积分" or "管理员扣减：AI积分 -50 积分"
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[+-]?\\d+").matcher(msg);
                            if (m.find()) delta = Integer.parseInt(m.group());
                        } catch (Exception ignored) {}
                        boolean isDeduct = order.getPlanId().startsWith("deduct-") || delta < 0 || (msg != null && msg.contains("扣减"));
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", (isDeduct ? "deduct-" : "gift-") + order.getOrderNo());
                        item.put("time", oTime);
                        item.put("paper", null);
                        item.put("scene", isDeduct ? "admin-penalty" : "admin-gift");
                        item.put("sceneLabel", isDeduct ? "管理员扣除" : "管理员赠送");
                        item.put("action", isDeduct ? "admin-penalty" : "admin-gift");
                        item.put("delta", delta);
                        item.put("status", isDeduct ? "扣减成功" : "到账成功");
                        item.put("effective", true);
                        agentLogs.add(item);
                    }
                }
            }

            // Sort by time desc
            agentLogs.sort((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")));
            
            int safePage = Math.max(1, page);
            int safeSize = Math.min(50, Math.max(5, pageSize <= 0 ? 10 : pageSize));
            int from = Math.min(agentLogs.size(), (safePage - 1) * safeSize);
            int to = Math.min(agentLogs.size(), from + safeSize);
            return Map.of(
                "page", safePage,
                "pageSize", safeSize,
                "total", agentLogs.size(),
                "totalPages", Math.max(1, (int) Math.ceil(agentLogs.size() / (double) safeSize)),
                "rows", agentLogs.subList(from, to)
            );
        }

        List<Map<String, Object>> matched = new ArrayList<>(
            repository.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(user.getId(), start).stream()
                .filter(row -> !endAt.isBefore(row.getCreatedAt()))
                .filter(row -> wanted.isBlank() || matchesBenefit(row, wanted))
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", row.getId());
                    item.put("time", row.getCreatedAt());
                    item.put("paper", row.getPaperTitle());
                    item.put("scene", row.getScene());
                    item.put("sceneLabel", sceneLabel(row.getScene(), row.getAction()));
                    item.put("action", row.getAction());
                    item.put("delta", "success".equalsIgnoreCase(row.getStatus()) && isCountBenefit(wanted) ? -1 : 0);
                    item.put("status", "success".equalsIgnoreCase(row.getStatus()) ? "生效" : "故障，不计入次数");
                    item.put("effective", "success".equalsIgnoreCase(row.getStatus()));
                    item.put("latencyMs", row.getLatencyMs());
                    return item;
                }).toList()
        );

        // Include restore benefit records and admin deduct/gift records in benefit tab
        List<PaymentOrderEntity> adminBenefitOrders = new ArrayList<>();
        adminBenefitOrders.addAll(paymentOrderRepository.findByUserIdAndPlanIdStartingWithOrderByCreatedAtDesc(user.getId(), "gift-"));
        adminBenefitOrders.addAll(paymentOrderRepository.findByUserIdAndPlanIdStartingWithOrderByCreatedAtDesc(user.getId(), "deduct-"));
        for (PaymentOrderEntity order : adminBenefitOrders) {
            LocalDateTime oTime = order.getCreatedAt();
            if (oTime != null && !oTime.isBefore(start) && !oTime.isAfter(endAt)) {
                String msg = order.getMessage();
                if ("gift-restore-all".equals(order.getPlanId()) || (msg != null && msg.contains("恢复满血"))) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", "restore-" + order.getOrderNo());
                    item.put("time", oTime);
                    item.put("paper", "全站会员额度恢复");
                    item.put("scene", "restore-benefit");
                    item.put("sceneLabel", "重置福利");
                    item.put("action", "restore-benefit");
                    item.put("delta", 0);
                    item.put("deltaLabel", "满血");
                    item.put("status", "恢复成功");
                    item.put("effective", true);
                    matched.add(item);
                } else if (order.getPlanId().startsWith("deduct-") || order.getPlanId().startsWith("gift-")) {
                    // Check if this matches the current wanted benefit
                    boolean matchesCurrent = false;
                    String targetKey = order.getPlanId().replace("deduct-", "").replace("gift-", "");
                    if ("translation".equals(wanted) && "translate".equalsIgnoreCase(targetKey)) matchesCurrent = true;
                    if ("immersive".equals(wanted) && "immersive".equalsIgnoreCase(targetKey)) matchesCurrent = true;
                    if ("ppt".equals(wanted) && "ppt".equalsIgnoreCase(targetKey)) matchesCurrent = true;
                    if (wanted.isBlank()) matchesCurrent = true;

                    if (matchesCurrent && !targetKey.contains("fruitScore") && !targetKey.contains("score") && !targetKey.contains("agent") && !targetKey.contains("restore")) {
                        int delta = 0;
                        try {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[+-]?\\d+").matcher(msg != null ? msg : "");
                            if (m.find()) delta = Integer.parseInt(m.group());
                        } catch (Exception ignored) {}
                        boolean isDeduct = order.getPlanId().startsWith("deduct-") || delta < 0;
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", (isDeduct ? "deduct-" : "gift-") + order.getOrderNo());
                        item.put("time", oTime);
                        item.put("paper", null);
                        item.put("scene", isDeduct ? "admin-penalty" : "admin-gift");
                        item.put("sceneLabel", isDeduct ? "管理员扣除" : "管理员赠送");
                        item.put("action", isDeduct ? "admin-penalty" : "admin-gift");
                        item.put("delta", delta);
                        item.put("status", isDeduct ? "扣减成功" : "到账成功");
                        item.put("effective", true);
                        matched.add(item);
                    }
                }
            }
        }

        // If queried for translation or immersive, also include check-in bonus rewards
        if ("translation".equals(wanted) || "immersive".equals(wanted) || wanted.isBlank()) {
            List<String> memberIds = new ArrayList<>();
            if (user.getEmail() != null && !user.getEmail().isBlank()) memberIds.add(user.getEmail());
            if (user.getUsername() != null && !user.getUsername().isBlank()) memberIds.add(user.getUsername());
            if (user.getId() != null) memberIds.add("m-" + user.getId());

            java.util.Set<Long> seenCheckinIds = new java.util.HashSet<>();
            for (String mId : memberIds) {
                List<CheckinEntity> checkins = checkinRepository.findAllByMemberIdOrderByDateDesc(mId);
                for (CheckinEntity checkin : checkins) {
                    if (checkin.getId() != null && !seenCheckinIds.add(checkin.getId())) continue;
                    LocalDateTime cTime = checkin.getCreatedAt();
                    if (cTime != null && !cTime.isBefore(start) && !cTime.isAfter(endAt)) {
                        String awardType = checkin.getAwardType();
                        boolean isTranslationAward = "bilingual_translate".equalsIgnoreCase(awardType) || "full_translate".equalsIgnoreCase(awardType);
                        if (isTranslationAward) {
                            if ("translation".equals(wanted) && !"bilingual_translate".equalsIgnoreCase(awardType)) continue;
                            if ("immersive".equals(wanted) && !"full_translate".equalsIgnoreCase(awardType)) continue;

                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", "checkin-trans-" + checkin.getId());
                            item.put("time", cTime);
                            item.put("paper", "每日签到奖励");
                            item.put("scene", "checkin");
                            String awardName = blankTo(checkin.getAwardName(), "full_translate".equalsIgnoreCase(awardType) ? "沉浸翻译 1 次" : "对照翻译 1 次");
                            item.put("sceneLabel", "签到赠送 (" + awardName + ")");
                            item.put("action", "checkin");
                            item.put("delta", 1);
                            item.put("status", "发放成功");
                            item.put("effective", true);
                            matched.add(item);
                        }
                    }
                }
            }
        }
        matched.sort((a, b) -> ((LocalDateTime) b.get("time")).compareTo((LocalDateTime) a.get("time")));
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(5, pageSize <= 0 ? 10 : pageSize));
        int from = Math.min(matched.size(), (safePage - 1) * safeSize);
        int to = Math.min(matched.size(), from + safeSize);
        return Map.of(
            "page", safePage,
            "pageSize", safeSize,
            "total", matched.size(),
            "totalPages", Math.max(1, (int) Math.ceil(matched.size() / (double) safeSize)),
            "rows", matched.subList(from, to)
        );
    }

    private boolean matchesBenefit(AiUsageRecordEntity row, String benefit) {
        String value = (blankTo(row.getAction(), "") + " " + blankTo(row.getScene(), "")).toLowerCase();
        return switch (benefit) {
            case "translation" -> (value.contains("对照") || value.contains("translate")) && !value.contains("immersive") && !value.contains("沉浸");
            case "immersive" -> value.contains("沉浸") || value.contains("immersive");
            case "review" -> value.contains("综述") || value.contains("review");
            case "chat" -> value.contains("对话") || value.contains("chat");
            case "ppt" -> value.contains("ppt") || value.contains("汇报");
            case "research" -> value.contains("调研") || value.contains("research");
            default -> true;
        };
    }

    private boolean isCountBenefit(String benefit) {
        return "translation".equals(benefit) || "immersive".equals(benefit) || "ppt".equals(benefit);
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
                else row.put("label", entry.getKey());
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
        row.put("sceneLabel", sceneLabel(record.getScene(), record.getAction()));
        row.put("action", normalizeAction(record.getAction()));
        row.put("rawAction", blankTo(record.getAction(), ""));
        row.put("modelName", blankTo(record.getModelName(), "未记录模型"));
        row.put("paper", displayPaperTitle(record.getPaperTitle(), "未关联论文"));
        row.put("promptTokens", safe(record.getPromptTokens()));
        row.put("completionTokens", safe(record.getCompletionTokens()));
        row.put("inputTokens", safe(record.getPromptTokens()));
        row.put("outputTokens", safe(record.getCompletionTokens()));
        row.put("totalTokens", safe(record.getTotalTokens()));
        row.put("chargeAmount", chargeOf(record));
        row.put("unitPrice", unitPriceOf(record));
        row.put("outputUnitPrice", billingService.outputUnitPrice(unitPriceOf(record), multiplierOf(record)));
        row.put("billingMultiplier", multiplierOf(record));
        row.put("billingFormula", "$" + unitPriceOf(record) + " / 1000 × (" + safe(record.getPromptTokens()) + " + " + safe(record.getCompletionTokens()) + " × " + multiplierOf(record) + ")");
        row.put("accountingNote", usageAccountingNote(record));
        row.put("status", blankTo(record.getStatus(), "success"));
        row.put("latencyMs", safe(record.getLatencyMs()));
        row.put("errorMessage", blankTo(record.getErrorMessage(), ""));
        Map<String, Object> fallback = fallbackResolution(record);
        row.put("fallbackResolved", fallback.get("resolved"));
        row.put("fallbackTime", fallback.get("time"));
        return row;
    }

    private String usageAccountingNote(AiUsageRecordEntity record) {
        String scene = blankTo(record.getScene(), "");
        String action = blankTo(record.getAction(), "");
        if (ModelConfigService.SCENE_MEETING_DECK.equalsIgnoreCase(scene) && action.contains("PPT")) {
            return "PPTMaster 是多轮 Agent 执行：中转站会把每轮请求的缓存输入单独计费；本站记录按 Codex 日志汇总 tokens，真实扣费请以中转站账单为准。";
        }
        return "";
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
            "time", success.getCreatedAt() == null ? "" : success.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }

    private String normalizeAction(String action) {
        String value = blankTo(action, "");
        if (value.contains("AI 笔记") || value.contains("AI笔记")) return "AI 笔记";
        if (value.contains("图片分析")) return "图片分析";
        if (value.contains("AI 解析") || value.contains("AI解析") || value.contains("AI 解读") || value.contains("AI解读")) return "AI 解析";
        if (value.contains("AI 对话") || value.contains("AI对话") || value.contains("论文问答")) return "AI 对话";
        if (value.contains("文献综述") || value.contains("论文综述")) return "文献综述";
        if (value.contains("PPT") || value.contains("Agent")) return "组会PPT Agent执行";
        if (value.contains("审核")) return "AI发帖审核";
        if (value.contains("选题")) return "选题调研";
        if (value.contains("融合")) return "组会一键融合";
        if (value.contains("组会")) return "论文综述生成";
        if (value.contains("翻译")) return "论文翻译";
        return "研读解析与对话";
    }

    private String sceneLabel(String scene, String action) {
        if ("paper_quiz".equals(scene)) return "形成考卷";
        String s = blankTo(scene, "").toLowerCase();
        if ("meeting_fusion".equals(s) || "review".equals(s)) {
            return "组会一键融合";
        }
        if ("reading_notes".equals(s) || "reading-notes".equals(s)) {
            return "AI 笔记";
        }
        if ("paper_review".equals(s) || "paper_review_section".equals(s) || "report".equals(s)) {
            return "文献综述";
        }
        if ("paper_qa".equals(s)) {
            String act = blankTo(action, "");
            if (act.contains("图片分析")) return "图片分析";
            if (act.contains("解析") || act.contains("解读")) return "AI 解析";
            return "AI 对话";
        }
        if ("meeting_deck".equals(s)) {
            return "PPT生成";
        }
        if ("forum_moderation".equals(s)) {
            return "AI发帖审核";
        }
        if ("topic_research".equals(s)) {
            return "选题大厅";
        }
        if ("translate".equals(s)) {
            return "对照翻译";
        }
        if ("immersive".equals(s)) {
            return "沉浸全文翻译";
        }

        // Fallbacks for compatibility or older records
        String act = blankTo(action, "");
        if (act.contains("AI 笔记") || act.contains("AI笔记")) return "AI 笔记";
        if (act.contains("图片分析")) return "图片分析";
        if (act.contains("AI 解析") || act.contains("AI解析") || act.contains("AI 解读") || act.contains("AI解读")) return "AI 解析";
        if (act.contains("AI 对话") || act.contains("AI对话") || act.contains("论文问答")) return "AI 对话";
        if (act.contains("PPT") || act.contains("ppt")) {
            return "PPT生成";
        }
        if (act.contains("融合")) {
            return "组会一键融合";
        }
        if (act.contains("综述") || act.contains("汇报") || act.contains("组会")) {
            return "论文综述";
        }
        if ("translate".equalsIgnoreCase(scene)) {
            return "对照翻译";
        }
        if ("immersive".equalsIgnoreCase(scene)) {
            return "沉浸全文翻译";
        }
        if (ModelConfigService.SCENE_TOPIC_RESEARCH.equalsIgnoreCase(scene)) {
            return "选题大厅";
        }
        if (ModelConfigService.SCENE_MEETING_DECK.equalsIgnoreCase(scene)) {
            return "PPT生成";
        }
        if (ModelConfigService.SCENE_FORUM_MODERATION.equalsIgnoreCase(scene)) {
            return "AI发帖审核";
        }
        return "研读解析与对话";
    }

    private int usagePointsDelta(AiUsageRecordEntity row) {
        if (row == null || !isSuccessfulUsage(row)) return 0;
        String scene = blankTo(row.getScene(), "").toLowerCase();
        String action = blankTo(row.getAction(), "");
        String normalizedAction = action.toLowerCase(java.util.Locale.ROOT).replace(" ", "");
        if ("paper_quiz".equals(scene)) {
            // Keep legacy “学术自测（2积分）” rows visible while standardizing
            // new records on the user-facing “形成考卷” label.
            return action.startsWith("形成考卷") || action.startsWith("学术自测") ? -2 : 0;
        }
        // Prefer the persisted action label over broad legacy scene names. Older
        // review/notes rows were recorded under paper_qa, which made their ledger
        // display as a one-point Q&A charge even though the feature charged 2.
        if ("image_analysis".equals(scene) || "image-analysis".equals(scene)
            || "imageanalysis".equals(scene)
            || normalizedAction.contains("图片分析") || normalizedAction.contains("imageanalysis")) {
            return -2;
        }
        if (normalizedAction.contains("ai笔记") || normalizedAction.contains("阅读笔记")
            || normalizedAction.contains("组会汇报") || normalizedAction.contains("readingnotes")
            || normalizedAction.contains("ainotes")) return -2;
        if (normalizedAction.contains("文献综述") || normalizedAction.contains("论文综述")
            || normalizedAction.contains("literaturereview") || normalizedAction.contains("reviewgeneration")) {
            return -1;
        }
        if ("paper_review_section".equals(scene)) return -1;
        if ("paper_review".equals(scene)
            || "paper-review".equals(scene) || "review".equals(scene)) {
            return -1;
        }
        if ("reading_notes".equals(scene) || "reading-notes".equals(scene) || "readingnotes".equals(scene)) return -2;
        if ("paper_qa".equals(scene) || "paper-qa".equals(scene)) return -1;
        if (normalizedAction.contains("ai对话") || normalizedAction.contains("ai解析")
            || normalizedAction.contains("ai解读") || normalizedAction.contains("研读解析")
            || normalizedAction.contains("论文问答") || normalizedAction.contains("paperqa")) {
            return -1;
        }
        if ("meeting-fusion".equals(scene) || "meeting-note".equals(scene)) {
            return -2;
        }
        return 0;
    }

    /** Older usage rows did not always persist a status; treat those completed
     * records as successful so their point deductions remain visible. */
    private boolean isSuccessfulUsage(AiUsageRecordEntity row) {
        String status = blankTo(row == null ? "" : row.getStatus(), "").trim();
        return status.isBlank() || "success".equalsIgnoreCase(status);
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
        double calculated = calculateCharge(unitPriceOf(record), multiplierOf(record), safe(record.getPromptTokens()), safe(record.getCompletionTokens()));
        return calculated > 0 ? calculated : saved;
    }

    private double calculateCharge(double unitPrice, double multiplier, long promptTokens, long completionTokens) {
        long safePrompt = Math.max(0L, promptTokens);
        long safeCompletion = Math.max(0L, completionTokens);
        if (unitPrice <= 0 || safePrompt + safeCompletion <= 0) return 0.0D;
        double billableTokens = safePrompt + safeCompletion * Math.max(1.0D, multiplier);
        return Math.round((unitPrice * billableTokens / 1000.0D) * 1_000_000D) / 1_000_000D;
    }

    private double unitPriceOf(AiUsageRecordEntity record) {
        double saved = money(record.getUnitPrice());
        return billingService.normalizeInputUnitPrice(saved, multiplierOf(record));
    }

    private double multiplierOf(AiUsageRecordEntity record) {
        double saved = money(record.getBillingMultiplier());
        return saved > 1.0D ? saved : billingService.multiplier();
    }

}
