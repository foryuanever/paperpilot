package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.AiUsageRecordEntity;
import com.paperpilot.server.entity.MembershipPlanEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.MembershipPlanRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.entity.TranslationRecordEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.util.StringUtils;

@Service
public class MembershipService {
    private static final String DAILY_TRANSLATION_EXHAUSTED_MESSAGE = "翻译次数不足，请明天再来";
    public static final String PLAN_LITE = "lite";
    public static final String PLAN_PLUS = "plus";
    public static final String PLAN_PRO = "pro";
    public static final String PLAN_MAX = "max";
    public static final String PLAN_TEAM_PLUS = "team_plus";
    public static final String PLAN_TEAM_PRO = "team_pro";

    private final AppUserRepository users;
    private final AiUsageRecordRepository usageRecords;
    private final MembershipPlanRepository planRepository;
    private final TranslationRecordRepository translationRecords;

    public record MembershipGrant(
        String planId,
        boolean topUpPack,
        int reviewQuota,
        int pptQuota,
        int chatQuota,
        int researchQuota,
        int reportQuota,
        int translateQuota,
        int immersiveQuota,
        long points
    ) {}

    @Autowired
    public MembershipService(AppUserRepository users, AiUsageRecordRepository usageRecords, MembershipPlanRepository planRepository, TranslationRecordRepository translationRecords) {
        this.users = users;
        this.usageRecords = usageRecords;
        this.planRepository = planRepository;
        this.translationRecords = translationRecords;
    }

    public MembershipService(AppUserRepository users, AiUsageRecordRepository usageRecords, MembershipPlanRepository planRepository) {
        this(users, usageRecords, planRepository, null);
    }

    public List<Map<String, Object>> catalog() {
        ensureDefaultPlans();
        return planRepository.findAllByOrderBySortOrderAscIdAsc().stream()
            .map(this::planToMap).toList();
    }

    /** Public catalog deliberately excludes plans that an administrator has taken offline. */
    public List<Map<String, Object>> publicCatalog() {
        return catalog().stream()
            .filter(item -> !Boolean.FALSE.equals(item.get("activeFlag")))
            .toList();
    }

    public Map<String, Object> membership(AppUserEntity user) {
        if (user == null) {
            return Map.of("id", "free", "name", "未开通会员", "benefits", Map.of());
        }
        expireIfNeeded(user);
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }

        evaluateMembershipStack(owner);
        syncDailyTranslationCounters(owner);

        Map<String, Object> result = new LinkedHashMap<>();
        String id = normalizePlanId(owner.getMembershipPlan());
        result.put("id", id);
        result.put("name", owner.getId() != null && !owner.getId().equals(user.getId()) ? planName(id) + "（团队共享）" : planName(id));
        result.put("cycle", safe(owner.getMembershipCycle(), "monthly"));
        LocalDateTime membershipStart = owner.getMembershipExpiresAt() != null
            ? owner.getMembershipExpiresAt().minusMonths(cycleMonths(owner.getMembershipCycle()))
            : owner.getCreatedAt();
        result.put("startedAt", membershipStart);
        result.put("expiresAt", owner.getMembershipExpiresAt());
        result.put("active", !"free".equals(id));
        result.put("sharedFromTeam", owner.getId() != null && !owner.getId().equals(user.getId()));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        List<Map<String, Object>> stack = new java.util.ArrayList<>();
        if (StringUtils.hasText(owner.getMembershipStack())) {
            try {
                stack = mapper.readValue(owner.getMembershipStack(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {}
        }

        List<Map<String, Object>> stackedList = new java.util.ArrayList<>();
        for (Map<String, Object> sub : stack) {
            Map<String, Object> subMap = new java.util.LinkedHashMap<>();
            subMap.put("planId", sub.get("planId"));
            subMap.put("planName", planName((String) sub.get("planId")));
            subMap.put("cycle", sub.get("cycle"));
            subMap.put("expiresAt", sub.get("expiresAt"));
            subMap.put("status", sub.get("status"));
            subMap.put("remainingSeconds", sub.get("remainingSeconds"));
            stackedList.add(subMap);
        }
        result.put("stacked", stackedList);

        // Auto-repair: if paid plan user has 0 immersive/translate quota (legacy activation bug), backfill from plan defaults
        if (!"free".equals(id)) {
            boolean needsSave = false;
            if (number(owner.getImmersiveQuota()) <= 0) {
                owner.setImmersiveQuota(immersiveDailyQuota(id));
                owner.setImmersiveUsed(0);
                owner.setImmersiveDailyQuotaBase(immersiveDailyQuota(id));
                needsSave = true;
            }
            if (number(owner.getTranslateQuota()) <= 0) {
                owner.setTranslateQuota(translateDailyQuota(id));
                owner.setTranslateUsed(0);
                owner.setTranslateDailyQuotaBase(translateDailyQuota(id));
                needsSave = true;
            }
            Map<String, Object> currentPlan = plan(id);
            long planAgentQuota = longNumber(currentPlan.get("agentTokenQuota"), 0L);
            if (planAgentQuota > 0 && numberLong(owner.getTokenLimit()) <= 0) {
                owner.setTokenLimit(planAgentQuota);
                owner.setTokenUsed(0L);
                needsSave = true;
            }
            if (needsSave) {
                users.save(owner);
            }
        }

        result.put("benefits", Map.of(
            "translation", allowance(owner.getTranslateQuota(), owner.getTranslateUsed()),
            "immersive", allowance(owner.getImmersiveQuota(), owner.getImmersiveUsed()),
            "review", allowance(owner.getReviewQuota(), owner.getReviewUsed()),
            "ppt", allowance(owner.getPptQuota(), owner.getPptUsed()),
            "chat", allowance(owner.getChatQuota(), owner.getChatUsed()),
            "research", allowance(owner.getResearchQuota(), owner.getResearchUsed()),
            "report", allowance(owner.getReportQuota(), owner.getReportUsed()),
            "agent", tokenAllowance(owner),
            "teamSeats", Map.of("quota", teamSeats(id), "shared", isTeamPlan(id))
        ));
        result.put("agentTokenQuota", numberLong(owner.getTokenLimit()));
        result.put("agentTokenUsed", numberLong(owner.getTokenUsed()));
        result.put("agentTokenRemaining", Math.max(0L, numberLong(owner.getTokenLimit()) - numberLong(owner.getTokenUsed())));
        return result;
    }

    private int translateDailyQuota(String planId) {
        return ((Number) plan(planId).getOrDefault("translateQuotaDaily", 5)).intValue();
    }

    private int immersiveDailyQuota(String planId) {
        return ((Number) plan(planId).getOrDefault("immersiveQuotaDaily", 3)).intValue();
    }

    public int dailyTranslateQuotaForUser(AppUserEntity user) {
        return translateDailyQuota(normalizePlanId(user == null ? "free" : user.getMembershipPlan()));
    }

    public int dailyImmersiveQuotaForUser(AppUserEntity user) {
        return immersiveDailyQuota(normalizePlanId(user == null ? "free" : user.getMembershipPlan()));
    }

    public void initializeFreeEntitlements(AppUserEntity user) {
        initializeFreeEntitlements(user, 0);
    }

    public void initializeFreeEntitlements(AppUserEntity user, int bonusPoints) {
        if (user == null) return;
        Map<String, Object> freePlan = plan("free");
        user.setMembershipPlan("free");
        user.setMembershipCycle("monthly");
        user.setMembershipExpiresAt(null);
        user.setReviewQuota(integer(freePlan.get("reviewQuota"), 0));
        user.setReviewUsed(0);
        user.setPptQuota(integer(freePlan.get("pptQuota"), 0));
        user.setPptUsed(0);
        user.setChatQuota(integer(freePlan.get("chatQuota"), 0));
        user.setChatUsed(0);
        user.setResearchQuota(integer(freePlan.get("researchQuota"), 0));
        user.setResearchUsed(0);
        user.setReportQuota(integer(freePlan.get("reportQuota"), 0));
        user.setReportUsed(0);
        user.setTranslateQuota(integer(freePlan.get("translateQuota"), 0));
        user.setTranslateUsed(0);
        user.setTranslateDailyQuotaBase(translateDailyQuota("free"));
        user.setTranslateDailyBonus(0);
        user.setTranslateDailyBonusDate(LocalDate.now());
        user.setImmersiveQuota(integer(freePlan.get("immersiveQuota"), 0));
        user.setImmersiveUsed(0);
        user.setTranslateOneOffQuota(0);
        user.setImmersiveOneOffQuota(0);
        user.setImmersiveDailyQuotaBase(immersiveDailyQuota("free"));
        user.setImmersiveDailyBonus(0);
        user.setImmersiveDailyBonusDate(LocalDate.now());
        long freePoints = longNumber(freePlan.get("agentTokenQuota"), 0L);
        user.setTokenLimit(freePoints);
        user.setTokenUsed(0L);
        user.setFruitScore(safeScore(freePoints + Math.max(0, bonusPoints)));
    }

    public MembershipGrant activate(AppUserEntity user, String planId, String cycle) {
        Map<String, Object> plan = activePlanOrThrow(planId);
        // Keep the database ID returned by the catalog. This matters for legacy
        // promo codes and administrator-created plans whose stored ID may not
        // be identical to the normalized alias sent by an older client.
        planId = String.valueOf(plan.getOrDefault("id", normalizePlanId(planId)));
        if ("free".equals(planId) || Boolean.FALSE.equals(plan.get("activeFlag"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐");
        }

        if (Boolean.TRUE.equals(plan.get("topUpPack"))) {
            applyTopUpPack(user, plan);
            users.save(user);
            return grantFromPlan(plan, true);
        }

        evaluateMembershipStack(user);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        List<Map<String, Object>> stack = new java.util.ArrayList<>();
        if (StringUtils.hasText(user.getMembershipStack())) {
            try {
                stack = mapper.readValue(user.getMembershipStack(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {}
        }

        Map<String, Object> newSub = new java.util.LinkedHashMap<>();
        newSub.put("planId", planId);
        newSub.put("cycle", cycle);
        long secs = (long) cycleMonths(cycle) * 30L * 24 * 3600; // 30 days per month
        newSub.put("remainingSeconds", secs);
        newSub.put("status", "paused");
        newSub.put("expiresAt", LocalDateTime.now().plusSeconds(secs).toString());
        stack.add(newSub);

        user.setMembershipStack(serializeStack(stack, mapper));
        evaluateMembershipStack(user);

        String activePlanId = user.getMembershipPlan();
        Map<String, Object> activePlan = activePlanId != null && !"free".equals(activePlanId) ? entitlementPlanOrThrow(activePlanId) : plan;

        user.setReviewQuota((Integer) activePlan.get("reviewQuota"));
        user.setReviewUsed(0);
        user.setPptQuota((Integer) activePlan.get("pptQuota"));
        user.setPptUsed(0);
        user.setChatQuota((Integer) activePlan.get("chatQuota"));
        user.setChatUsed(0);
        user.setResearchQuota((Integer) activePlan.get("researchQuota"));
        user.setResearchUsed(0);
        user.setReportQuota((Integer) activePlan.get("reportQuota"));
        user.setReportUsed(0);
        user.setTranslateQuota(integer(activePlan.get("translateQuota"), 0));
        user.setTranslateUsed(0);
        user.setTranslateDailyQuotaBase(integer(activePlan.get("translateQuotaDaily"), translateDailyQuota(activePlanId)));
        user.setTranslateDailyBonus(0);
        user.setTranslateDailyBonusDate(LocalDate.now());
        user.setImmersiveQuota(integer(activePlan.get("immersiveQuota"), 0));
        user.setImmersiveUsed(0);
        user.setTranslateOneOffQuota(0);
        user.setImmersiveOneOffQuota(0);
        user.setImmersiveDailyQuotaBase(integer(activePlan.get("immersiveQuotaDaily"), immersiveDailyQuota(activePlanId)));
        user.setImmersiveDailyBonus(0);
        user.setImmersiveDailyBonusDate(LocalDate.now());
        user.setTokenLimit(longNumber(activePlan.get("agentTokenQuota"), 0L));
        user.setTokenUsed(0L);
        // AI points are a balance. Purchasing or redeeming another plan must add
        // the new allowance instead of replacing points already earned or bought.
        // Points belong to this purchase/redemption. Never derive them from the
        // currently active stack entry, which may be a different older plan.
        long planPoints = longNumber(plan.get("agentTokenQuota"), 0L);
        user.setFruitScore(safeScore((long) number(user.getFruitScore()) + planPoints));
        users.save(user);
        return grantFromPlan(plan, false);
    }

    private MembershipGrant grantFromPlan(Map<String, Object> plan, boolean topUpPack) {
        return new MembershipGrant(
            String.valueOf(plan.get("id")),
            topUpPack,
            integer(plan.get("reviewQuota"), 0),
            integer(plan.get("pptQuota"), 0),
            integer(plan.get("chatQuota"), 0),
            integer(plan.get("researchQuota"), 0),
            integer(plan.get("reportQuota"), 0),
            integer(plan.get("translateQuota"), 0),
            integer(plan.get("immersiveQuota"), 0),
            longNumber(plan.get("agentTokenQuota"), 0L)
        );
    }

    /** Add a one-off pack without changing the active plan, expiry, queue, or used counters. */
    private void applyTopUpPack(AppUserEntity user, Map<String, Object> plan) {
        user.setReviewQuota(number(user.getReviewQuota()) + integer(plan.get("reviewQuota"), 0));
        user.setPptQuota(number(user.getPptQuota()) + integer(plan.get("pptQuota"), 0));
        user.setChatQuota(number(user.getChatQuota()) + integer(plan.get("chatQuota"), 0));
        user.setResearchQuota(number(user.getResearchQuota()) + integer(plan.get("researchQuota"), 0));
        user.setReportQuota(number(user.getReportQuota()) + integer(plan.get("reportQuota"), 0));
        user.setTranslateOneOffQuota(number(user.getTranslateOneOffQuota()) + integer(plan.get("translateQuota"), 0));
        user.setImmersiveOneOffQuota(number(user.getImmersiveOneOffQuota()) + integer(plan.get("immersiveQuota"), 0));
        long agentPoints = longNumber(plan.get("agentTokenQuota"), 0L);
        user.setTokenLimit(numberLong(user.getTokenLimit()) + agentPoints);
        user.setFruitScore(safeScore((long) (user.getFruitScore() == null ? 0 : user.getFruitScore()) + agentPoints));
    }

    private int intScore(Object value, int fallback) {
        return safeScore(longNumber(value, fallback));
    }

    private int safeScore(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    public boolean isTopUpPack(String planId) {
        return Boolean.TRUE.equals(plan(planId).get("topUpPack"));
    }

    /** True only when the administrator-defined plan exists in the catalog. */
    public boolean hasConfiguredPlan(String planId) {
        ensureDefaultPlans();
        return planRepository.findById(normalizePlanId(planId)).isPresent();
    }

    /** Validate a plan before an administrator exposes it through a payment or redemption channel. */
    public void assertPurchasablePlan(String planId) {
        Map<String, Object> item = activePlanOrThrow(planId);
        if ("free".equals(item.get("id")) || Boolean.FALSE.equals(item.get("activeFlag"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择当前上架的付费套餐或加油包");
        }
    }

    /** Resolve a client or legacy promo-code plan ID to the exact active catalog ID. */
    public String resolvePurchasablePlanId(String planId) {
        Map<String, Object> item = activePlanOrThrow(planId);
        return String.valueOf(item.get("id"));
    }

    /** Restore every metered entitlement to the current plan's configured value. */
    public AppUserEntity restoreCurrentPlanEntitlements(AppUserEntity user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        evaluateMembershipStack(user);
        String planId = normalizePlanId(user.getMembershipPlan());
        Map<String, Object> currentPlan = plan(planId);

        user.setReviewQuota(integer(currentPlan.get("reviewQuota"), 0));
        user.setReviewUsed(0);
        user.setPptQuota(integer(currentPlan.get("pptQuota"), 0));
        user.setPptUsed(0);
        user.setChatQuota(integer(currentPlan.get("chatQuota"), 0));
        user.setChatUsed(0);
        user.setResearchQuota(integer(currentPlan.get("researchQuota"), 0));
        user.setResearchUsed(0);
        user.setReportQuota(integer(currentPlan.get("reportQuota"), 0));
        user.setReportUsed(0);
        user.setTranslateQuota(integer(currentPlan.get("translateQuota"), 0));
        user.setTranslateUsed(0);
        user.setTranslateDailyBonus(0);
        user.setTranslateDailyBonusDate(LocalDate.now());
        user.setImmersiveQuota(integer(currentPlan.get("immersiveQuota"), 0));
        user.setImmersiveUsed(0);
        user.setImmersiveDailyBonus(0);
        user.setImmersiveDailyBonusDate(LocalDate.now());
        user.setTokenLimit(longNumber(currentPlan.get("agentTokenQuota"), 0L));
        user.setTokenUsed(0L);
        return users.save(user);
    }

    public void evaluateMembershipStack(AppUserEntity user) {
        if (user == null || user.getId() == null) return;
        LocalDateTime now = LocalDateTime.now();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        List<Map<String, Object>> stack = null;
        if (StringUtils.hasText(user.getMembershipStack())) {
            try {
                stack = mapper.readValue(user.getMembershipStack(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception ignored) {}
        }

        if ((stack == null || stack.isEmpty()) && user.getMembershipPlan() != null && !"free".equals(user.getMembershipPlan())) {
            stack = new java.util.ArrayList<>();
            Map<String, Object> sub = new java.util.LinkedHashMap<>();
            sub.put("planId", user.getMembershipPlan());
            sub.put("cycle", user.getMembershipCycle() == null ? "monthly" : user.getMembershipCycle());
            long secs = 30L * 24 * 3600;
            if (user.getMembershipExpiresAt() != null && user.getMembershipExpiresAt().isAfter(now)) {
                secs = java.time.Duration.between(now, user.getMembershipExpiresAt()).toSeconds();
            }
            sub.put("remainingSeconds", secs);
            sub.put("status", "active");
            sub.put("expiresAt", (user.getMembershipExpiresAt() != null ? user.getMembershipExpiresAt() : now.plusSeconds(secs)).toString());
            stack.add(sub);
        }

        if (stack == null) {
            stack = new java.util.ArrayList<>();
        }

        LocalDateTime lastEval = user.getLastMembershipEvaluationTime();
        if (lastEval == null) {
            lastEval = now;
        }
        user.setLastMembershipEvaluationTime(now);

        long elapsedSeconds = Math.max(0, java.time.Duration.between(lastEval, now).toSeconds());

        if (elapsedSeconds > 0 && !stack.isEmpty()) {
            Map<String, Object> activeSub = null;
            for (Map<String, Object> sub : stack) {
                if ("active".equals(sub.get("status"))) {
                    activeSub = sub;
                    break;
                }
            }
            if (activeSub == null) {
                activeSub = stack.get(0);
                activeSub.put("status", "active");
            }

            long rem = ((Number) activeSub.getOrDefault("remainingSeconds", 0L)).longValue();
            rem -= elapsedSeconds;
            if (rem <= 0) {
                stack.remove(activeSub);
                long remainingElapsed = -rem;
                user.setMembershipStack(serializeStack(stack, mapper));
                user.setLastMembershipEvaluationTime(now.minusSeconds(remainingElapsed));
                evaluateMembershipStack(user);
                return;
            } else {
                activeSub.put("remainingSeconds", rem);
            }
        }

        if (!stack.isEmpty()) {
            Map<String, Object> bestSub = stack.get(0);
            int bestPriority = planPriority((String) bestSub.get("planId"));
            for (Map<String, Object> sub : stack) {
                int p = planPriority((String) sub.get("planId"));
                // On equal priority, prefer the most recently appended plan so a
                // newly redeemed custom plan does not remain paused behind an old one.
                if (p >= bestPriority) {
                    bestPriority = p;
                    bestSub = sub;
                }
            }

            for (Map<String, Object> sub : stack) {
                if (sub == bestSub) {
                    sub.put("status", "active");
                } else {
                    sub.put("status", "paused");
                }
            }

            long activeRem = ((Number) bestSub.get("remainingSeconds")).longValue();
            LocalDateTime activeExpires = now.plusSeconds(activeRem);
            bestSub.put("expiresAt", activeExpires.toString());

            LocalDateTime currentAnchor = activeExpires;
            for (Map<String, Object> sub : stack) {
                if (sub == bestSub) continue;
                long rem = ((Number) sub.get("remainingSeconds")).longValue();
                currentAnchor = currentAnchor.plusSeconds(rem);
                sub.put("expiresAt", currentAnchor.toString());
            }

            user.setMembershipPlan((String) bestSub.get("planId"));
            user.setMembershipCycle((String) bestSub.get("cycle"));
            user.setMembershipExpiresAt(activeExpires);
        } else {
            user.setMembershipPlan("free");
            user.setMembershipExpiresAt(null);
        }

        user.setMembershipStack(serializeStack(stack, mapper));
        users.save(user);
    }

    private int planPriority(String planId) {
        if ("team".equals(planId)) return 3;
        if ("pro".equals(planId)) return 2;
        if ("lite".equals(planId)) return 1;
        return 0;
    }

    private String serializeStack(List<Map<String, Object>> stack, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(stack);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    public void consume(AppUserEntity user, String action) {
        consume(user, action, Map.of());
    }

    public synchronized void consume(AppUserEntity user, String action, Map<String, String> metadata) {
        expireIfNeeded(user);
        String kind = entitlementFor(action);
        if (kind == null) return;
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        boolean isAdmin = "管理员".equals(owner.getRole()) || "admin".equalsIgnoreCase(owner.getRole());
        if ("ppt".equals(kind)) {
            if (!isAdmin) {
                int quota = owner.getPptQuota() != null ? owner.getPptQuota() : 0;
                int used = owner.getPptUsed() != null ? owner.getPptUsed() : 0;
                if (quota - used <= 0) {
                    throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "PPT 生成次数已用尽（当前剩余 0 次），请升级会员套餐或联系管理员补给！");
                }
                owner.setPptUsed(used + 1);
                users.save(owner);
                recordCountUsage(owner, "ppt", metadata);
            }
            return;
        }
        syncDailyTranslationCounters(owner);
        if ("translate".equals(kind)) {
            if (alreadyConsumedForWorkspace(owner, "translate", metadata)) return;
            if (consumeOneOffQuota(owner, "translate", metadata)) return;
            consumeCountQuota(
                owner.getTranslateQuota(),
                owner.getTranslateUsed(),
                DAILY_TRANSLATION_EXHAUSTED_MESSAGE,
                (nextUsed) -> owner.setTranslateUsed(nextUsed),
                owner,
                "translate",
                metadata
            );
            return;
        }
        if ("immersive".equals(kind)) {
            if (alreadyConsumedForWorkspace(owner, "immersive", metadata)) return;
            if (consumeOneOffQuota(owner, "immersive", metadata)) return;
            consumeCountQuota(
                owner.getImmersiveQuota(),
                owner.getImmersiveUsed(),
                DAILY_TRANSLATION_EXHAUSTED_MESSAGE,
                (nextUsed) -> owner.setImmersiveUsed(nextUsed),
                owner,
                "immersive",
                metadata
            );
            return;
        }
        int points = owner.getFruitScore() != null ? owner.getFruitScore() : 0;
        if (!isAdmin && points <= 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "积分不足，请每日签到获取积分");
        }
        if (!isAdmin) {
            owner.setFruitScore(Math.max(0, points - 1));
            users.save(owner);
        }
    }

    private void consumeCountQuota(
        Integer quotaValue,
        Integer usedValue,
        String emptyMessage,
        java.util.function.IntConsumer setUsed,
        AppUserEntity owner,
        String kind,
        Map<String, String> metadata
    ) {
        int quota = quotaValue != null ? quotaValue : 0;
        int used = usedValue != null ? usedValue : 0;
        if (quota - used <= 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, emptyMessage);
        }
        setUsed.accept(used + 1);
        users.save(owner);
        recordCountUsage(owner, kind, metadata);
    }

    private void recordCountUsage(AppUserEntity owner, String kind, Map<String, String> metadata) {
        recordCountUsage(owner, kind, metadata, kind + "-quota");
    }

    private void recordCountUsage(AppUserEntity owner, String kind, Map<String, String> metadata, String route) {
        if (owner == null || owner.getId() == null) return;
        if ("translate".equals(kind) || "immersive".equals(kind)) {
            if (translationRecords == null) return;
            TranslationRecordEntity record = new TranslationRecordEntity();
            record.setUserId(owner.getId());
            record.setProvider("membership-quota");
            record.setPaperTitle(limitText(metadata == null ? "" : metadata.get("paperTitle"), 512));
            record.setTranslationMode("immersive".equals(kind) ? "沉浸式翻译" : "对照翻译");
            record.setRoute(route);
            record.setSourceLang("auto");
            record.setTargetLang("zh-CN");
            record.setClientType("membership");
            record.setCharCount(0L);
            record.setLatencyMs(0L);
            record.setSuccess(true);
            record.setNetworkProfile("daily-quota");
            record.setErrorMessage(limitText(metadata == null ? "" : metadata.get("workspaceId"), 1000));
            translationRecords.save(record);
            return;
        }
        AiUsageRecordEntity record = new AiUsageRecordEntity();
        record.setUserId(owner.getId());
        record.setUsername(safe(owner.getUsername(), ""));
        record.setUserEmail(safe(owner.getEmail(), ""));
        record.setModelName("membership-quota");
        record.setScene(kind);
        record.setAction(switch (kind) {
            case "translate" -> "对照翻译（首次生成）";
            case "immersive" -> "沉浸全文翻译";
            case "ppt" -> "PPT生成";
            default -> kind;
        });
        record.setPaperTitle(limitText(metadata == null ? "" : metadata.get("paperTitle"), 255));
        record.setPromptTokens(0L);
        record.setCompletionTokens(0L);
        record.setTotalTokens(0L);
        record.setStatus("success");
        record.setErrorMessage(limitText(metadata == null ? "" : metadata.get("workspaceId"), 800));
        record.setLatencyMs(0L);
        record.setChargeAmount(0.0D);
        record.setUnitPrice(0.0D);
        record.setBillingMultiplier(1.0D);
        usageRecords.save(record);
    }

    private boolean consumeOneOffQuota(AppUserEntity owner, String kind, Map<String, String> metadata) {
        int available = "translate".equals(kind) ? number(owner.getTranslateOneOffQuota()) : number(owner.getImmersiveOneOffQuota());
        if (available <= 0) return false;
        if ("translate".equals(kind)) owner.setTranslateOneOffQuota(available - 1);
        else owner.setImmersiveOneOffQuota(available - 1);
        users.save(owner);
        recordCountUsage(owner, kind, metadata, kind + "-once");
        return true;
    }

    private boolean alreadyConsumedForWorkspace(AppUserEntity owner, String kind, Map<String, String> metadata) {
        if (owner == null || owner.getId() == null || metadata == null) return false;
        String workspaceId = limitText(metadata.get("workspaceId"), 800);
        if (!StringUtils.hasText(workspaceId)) return false;
        if (translationRecords != null && ("translate".equals(kind) || "immersive".equals(kind))) {
            return translationRecords.existsByUserIdAndRouteAndSuccessTrueAndErrorMessage(owner.getId(), kind + "-quota", workspaceId)
                || translationRecords.existsByUserIdAndRouteAndSuccessTrueAndErrorMessage(owner.getId(), kind + "-once", workspaceId);
        }
        return usageRecords.existsByUserIdAndSceneAndStatusAndErrorMessage(owner.getId(), kind, "success", workspaceId);
    }

    private void syncDailyTranslationCounters(AppUserEntity owner) {
        if (owner == null || owner.getId() == null) return;
        LocalDate today = LocalDate.now();
        boolean bonusChanged = false;
        if (!today.equals(owner.getTranslateDailyBonusDate())) {
            owner.setTranslateDailyBonus(0);
            owner.setTranslateDailyBonusDate(today);
            bonusChanged = true;
        }
        if (!today.equals(owner.getImmersiveDailyBonusDate())) {
            owner.setImmersiveDailyBonus(0);
            owner.setImmersiveDailyBonusDate(today);
            bonusChanged = true;
        }
        String planId = normalizePlanId(owner.getMembershipPlan());
        int translateBase = owner.getTranslateDailyQuotaBase() == null
            ? Math.max(0, number(owner.getTranslateQuota()) - number(owner.getTranslateDailyBonus()))
            : owner.getTranslateDailyQuotaBase();
        int immersiveBase = owner.getImmersiveDailyQuotaBase() == null
            ? Math.max(0, number(owner.getImmersiveQuota()) - number(owner.getImmersiveDailyBonus()))
            : owner.getImmersiveDailyQuotaBase();
        if (owner.getTranslateDailyQuotaBase() == null) {
            owner.setTranslateDailyQuotaBase(translateBase);
            bonusChanged = true;
        }
        if (owner.getImmersiveDailyQuotaBase() == null) {
            owner.setImmersiveDailyQuotaBase(immersiveBase);
            bonusChanged = true;
        }
        int translateQuota = Math.max(0, translateBase + number(owner.getTranslateDailyBonus()));
        int immersiveQuota = Math.max(0, immersiveBase + number(owner.getImmersiveDailyBonus()));
        // Repair legacy users created while daily quota was incorrectly tied
        // to the one-off quota field. Do not overwrite actual usage or an
        // administrator's non-zero daily adjustment.
        int configuredTranslateDaily = translateDailyQuota(planId);
        int configuredImmersiveDaily = immersiveDailyQuota(planId);
        if (translateBase <= 0 && configuredTranslateDaily > 0 && number(owner.getTranslateUsed()) == 0
            && number(owner.getTranslateDailyBonus()) == 0 && number(owner.getTranslateQuota()) == 0) {
            translateBase = configuredTranslateDaily;
            owner.setTranslateDailyQuotaBase(translateBase);
            translateQuota = translateBase;
            bonusChanged = true;
        }
        if (immersiveBase <= 0 && configuredImmersiveDaily > 0 && number(owner.getImmersiveUsed()) == 0
            && number(owner.getImmersiveDailyBonus()) == 0 && number(owner.getImmersiveQuota()) == 0) {
            immersiveBase = configuredImmersiveDaily;
            owner.setImmersiveDailyQuotaBase(immersiveBase);
            immersiveQuota = immersiveBase;
            bonusChanged = true;
        }
        if (number(owner.getTranslateQuota()) != translateQuota) {
            owner.setTranslateQuota(translateQuota);
            bonusChanged = true;
        }
        if (number(owner.getImmersiveQuota()) != immersiveQuota) {
            owner.setImmersiveQuota(immersiveQuota);
            bonusChanged = true;
        }
        LocalDateTime todayStart = java.time.LocalDate.now().atStartOfDay();
        int translateToday = translationRecords == null
            ? safeScore(usageRecords.countByUserIdAndSceneAndStatusAndCreatedAtAfter(owner.getId(), "translate", "success", todayStart))
            : safeScore(translationRecords.countByUserIdAndRouteAndSuccessTrueAndCreatedAtAfter(owner.getId(), "translate-quota", todayStart));
        int immersiveToday = translationRecords == null
            ? safeScore(usageRecords.countByUserIdAndSceneAndStatusAndCreatedAtAfter(owner.getId(), "immersive", "success", todayStart))
            : safeScore(translationRecords.countByUserIdAndRouteAndSuccessTrueAndCreatedAtAfter(owner.getId(), "immersive-quota", todayStart));
        boolean changed = bonusChanged;
        if (number(owner.getTranslateUsed()) != translateToday) {
            owner.setTranslateUsed(translateToday);
            changed = true;
        }
        if (number(owner.getImmersiveUsed()) != immersiveToday) {
            owner.setImmersiveUsed(immersiveToday);
            changed = true;
        }
        if (changed) {
            users.save(owner);
        }
    }

    public void assertAvailable(Long userId, String action) {
        AppUserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        expireIfNeeded(user);
        String kind = entitlementFor(action);
        if (kind == null) return;
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        boolean isAdmin = "管理员".equals(owner.getRole()) || "admin".equalsIgnoreCase(owner.getRole());
        if (isAdmin) return;
        syncDailyTranslationCounters(owner);

        if ("ppt".equals(kind)) {
            int quota = owner.getPptQuota() != null ? owner.getPptQuota() : 0;
            int used = owner.getPptUsed() != null ? owner.getPptUsed() : 0;
            if (quota - used <= 0) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "PPT 生成次数已用尽（当前剩余 0 次），请升级会员套餐或联系管理员补给！");
            }
            return;
        }
        if ("translate".equals(kind)) {
            int quota = owner.getTranslateQuota() != null ? owner.getTranslateQuota() : 0;
            int used = owner.getTranslateUsed() != null ? owner.getTranslateUsed() : 0;
            if (quota - used <= 0) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, DAILY_TRANSLATION_EXHAUSTED_MESSAGE);
            }
            return;
        }
        if ("immersive".equals(kind)) {
            int quota = owner.getImmersiveQuota() != null ? owner.getImmersiveQuota() : 0;
            int used = owner.getImmersiveUsed() != null ? owner.getImmersiveUsed() : 0;
            if (quota - used <= 0) {
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, DAILY_TRANSLATION_EXHAUSTED_MESSAGE);
            }
            return;
        }

        int points = owner.getFruitScore() != null ? owner.getFruitScore() : 0;
        if (points <= 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "积分不足，请每日签到获取积分");
        }
    }

    @Transactional
    public void addTranslateDailyBonus(AppUserEntity user, int amount) {
        if (user == null || amount <= 0) return;
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getTranslateDailyBonusDate())) {
            user.setTranslateDailyBonus(0);
            user.setTranslateDailyBonusDate(today);
        }
        user.setTranslateDailyBonus(number(user.getTranslateDailyBonus()) + amount);
    }

    @Transactional
    public void addImmersiveDailyBonus(AppUserEntity user, int amount) {
        if (user == null || amount <= 0) return;
        LocalDate today = LocalDate.now();
        if (!today.equals(user.getImmersiveDailyBonusDate())) {
            user.setImmersiveDailyBonus(0);
            user.setImmersiveDailyBonusDate(today);
        }
        user.setImmersiveDailyBonus(number(user.getImmersiveDailyBonus()) + amount);
    }

    public void assertAgentAvailable(Long userId) {
        AppUserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        expireIfNeeded(user);
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) expireIfNeeded(owner);
        boolean isAdmin = "管理员".equals(owner.getRole()) || "admin".equalsIgnoreCase(owner.getRole());
        if (isAdmin) return;
        int points = owner.getFruitScore() != null ? owner.getFruitScore() : 0;
        if (points <= 0) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "积分不足，请每日签到获取积分");
        }
    }

    private void expireIfNeeded(AppUserEntity user) {
        if (user.getMembershipExpiresAt() != null && user.getMembershipExpiresAt().isBefore(LocalDateTime.now())) {
            Map<String, Object> freePlan = plan("free");
            user.setMembershipPlan("free");
            user.setMembershipCycle("monthly");
            user.setMembershipExpiresAt(null);
            user.setReviewQuota(integer(freePlan.get("reviewQuota"), 0)); user.setReviewUsed(0);
            user.setPptQuota(integer(freePlan.get("pptQuota"), 0)); user.setPptUsed(0);
            user.setChatQuota(integer(freePlan.get("chatQuota"), 0)); user.setChatUsed(0);
            user.setResearchQuota(integer(freePlan.get("researchQuota"), 0)); user.setResearchUsed(0);
            user.setReportQuota(integer(freePlan.get("reportQuota"), 0)); user.setReportUsed(0);
            user.setTranslateQuota(translateDailyQuota("free")); user.setTranslateUsed(0);
            user.setImmersiveQuota(immersiveDailyQuota("free")); user.setImmersiveUsed(0);
            user.setTranslateOneOffQuota(0); user.setImmersiveOneOffQuota(0);
            user.setTranslateDailyQuotaBase(translateDailyQuota("free"));
            user.setImmersiveDailyQuotaBase(immersiveDailyQuota("free"));
            user.setTranslateDailyBonus(0); user.setTranslateDailyBonusDate(LocalDate.now());
            user.setImmersiveDailyBonus(0); user.setImmersiveDailyBonusDate(LocalDate.now());
            users.save(user);
        }
    }

    private AppUserEntity entitlementOwner(AppUserEntity user) {
        if (user.getTeamId() != null) {
            LocalDateTime now = LocalDateTime.now();
            AppUserEntity teamSponsor = users.findByTeamIdOrderByCreatedAtAsc(user.getTeamId()).stream()
                .filter(item -> "导师".equals(item.getRole()))
                .filter(item -> isTeamPlan(item.getMembershipPlan()))
                .filter(item -> item.getMembershipExpiresAt() != null && item.getMembershipExpiresAt().isAfter(now))
                .findFirst()
                .orElse(null);
            if (teamSponsor != null) {
                return teamSponsor;
            }
        }
        return user;
    }

    public Map<String, Object> savePlan(String id, Map<String, Object> body) {
        id = normalizePlanId(id);
        ensureDefaultPlans();
        MembershipPlanEntity entity = planRepository.findById(id).orElse(defaultPlan(id, 99));
        if (body.containsKey("name")) entity.setName(text(body.get("name"), entity.getName()));
        if (body.containsKey("subtitle")) entity.setSubtitle(text(body.get("subtitle"), entity.getSubtitle()));
        if (body.containsKey("monthlyPrice")) entity.setMonthlyPrice(decimal(body.get("monthlyPrice"), entity.getMonthlyPrice()));
        if (body.containsKey("originalMonthlyPrice")) entity.setOriginalMonthlyPrice(decimal(body.get("originalMonthlyPrice"), entity.getOriginalMonthlyPrice()));
        if (body.containsKey("agentTokenQuota")) entity.setAgentTokenQuota(longNumber(body.get("agentTokenQuota"), entity.getAgentTokenQuota() == null ? 0L : entity.getAgentTokenQuota()));
        if (body.containsKey("pluginImportEnabled")) entity.setPluginImportEnabled(bool(body.get("pluginImportEnabled"), entity.getPluginImportEnabled()));
        if (body.containsKey("agentEnabled")) entity.setAgentEnabled(bool(body.get("agentEnabled"), entity.getAgentEnabled()));
        if (body.containsKey("reviewEnabled")) entity.setReviewEnabled(bool(body.get("reviewEnabled"), entity.getReviewEnabled()));
        if (body.containsKey("chatEnabled")) entity.setChatEnabled(bool(body.get("chatEnabled"), entity.getChatEnabled()));
        if (body.containsKey("reviewQuota")) entity.setReviewQuota(integer(body.get("reviewQuota"), entity.getReviewQuota()));
        if (body.containsKey("pptQuota")) entity.setPptQuota(integer(body.get("pptQuota"), entity.getPptQuota()));
        if (body.containsKey("chatQuota")) entity.setChatQuota(integer(body.get("chatQuota"), entity.getChatQuota()));
        if (body.containsKey("translateQuota")) entity.setTranslateQuota(integer(body.get("translateQuota"), entity.getTranslateQuota()));
        if (body.containsKey("immersiveQuota")) entity.setImmersiveQuota(integer(body.get("immersiveQuota"), entity.getImmersiveQuota()));
        if (body.containsKey("translateQuotaDaily")) entity.setTranslateDailyQuota(integer(body.get("translateQuotaDaily"), entity.getTranslateDailyQuota()));
        if (body.containsKey("immersiveQuotaDaily")) entity.setImmersiveDailyQuota(integer(body.get("immersiveQuotaDaily"), entity.getImmersiveDailyQuota()));
        if (body.containsKey("researchQuota")) entity.setResearchQuota(integer(body.get("researchQuota"), entity.getResearchQuota()));
        if (body.containsKey("reportQuota")) entity.setReportQuota(integer(body.get("reportQuota"), entity.getReportQuota()));
        if (body.containsKey("teamSeats")) entity.setTeamSeats(integer(body.get("teamSeats"), entity.getTeamSeats()));
        if (body.containsKey("teamShared")) entity.setTeamShared(bool(body.get("teamShared"), entity.getTeamShared()));
        if (body.containsKey("forumSpecial")) entity.setForumSpecial(bool(body.get("forumSpecial"), entity.getForumSpecial()));
        if (body.containsKey("forumTopDaily")) entity.setForumTopDaily(integer(body.get("forumTopDaily"), entity.getForumTopDaily()));
        if (body.containsKey("peakPriority")) entity.setPeakPriority(bool(body.get("peakPriority"), entity.getPeakPriority()));
        if (body.containsKey("activeFlag")) entity.setActiveFlag(bool(body.get("activeFlag"), entity.getActiveFlag()));
        if (body.containsKey("topUpPack")) entity.setTopUpPack(bool(body.get("topUpPack"), entity.getTopUpPack()));
        if (body.containsKey("sortOrder")) entity.setSortOrder(integer(body.get("sortOrder"), entity.getSortOrder()));
        if (body.containsKey("seckillEnabled")) entity.setSeckillEnabled(bool(body.get("seckillEnabled"), entity.getSeckillEnabled()));
        if (body.containsKey("seckillPrice")) {
            Object value = body.get("seckillPrice");
            entity.setSeckillPrice(value == null || String.valueOf(value).isBlank() ? null : decimal(value, entity.getSeckillPrice() == null ? 0D : entity.getSeckillPrice()));
        }
        if (body.containsKey("seckillStartsAt")) entity.setSeckillStartsAt(dateTime(body.get("seckillStartsAt"), entity.getSeckillStartsAt()));
        if (body.containsKey("seckillEndsAt")) entity.setSeckillEndsAt(dateTime(body.get("seckillEndsAt"), entity.getSeckillEndsAt()));
        if (body.containsKey("seckillLabel")) entity.setSeckillLabel(text(body.get("seckillLabel"), entity.getSeckillLabel()));
        return planToMap(planRepository.save(entity));
    }

    public Map<String, Object> createPlan(Map<String, Object> body) {
        String id = normalizePlanId(text(body.get("id"), ""));
        if (id.isBlank() || "free".equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写有效套餐标识");
        }
        ensureDefaultPlans();
        if (planRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "套餐标识已存在，请换一个 ID");
        }
        MembershipPlanEntity entity = defaultPlan(id, integer(body.get("sortOrder"), 99));
        entity.setName(text(body.get("name"), "新会员套餐"));
        entity.setSubtitle(text(body.get("subtitle"), "自定义上架套餐"));
        entity.setMonthlyPrice(decimal(body.get("monthlyPrice"), 19.9D));
        entity.setOriginalMonthlyPrice(decimal(body.get("originalMonthlyPrice"), entity.getMonthlyPrice()));
        entity.setActiveFlag(bool(body.get("activeFlag"), true));
        entity.setTopUpPack(bool(body.get("topUpPack"), false));
        if (body.containsKey("teamShared")) entity.setTeamShared(bool(body.get("teamShared"), false));
        return planToMap(planRepository.save(entity));
    }

    public void deletePlan(String id) {
        String planId = normalizePlanId(id);
        if ("free".equals(planId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "免费套餐不可删除");
        }
        if (isBuiltInPlan(planId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "系统预置套餐只能隐藏，不能删除");
        }
        if (!planRepository.existsById(planId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "套餐不存在或已删除");
        }
        planRepository.deleteById(planId);
    }

    public Map<String, Object> plan(String id) {
        id = normalizePlanId(id);
        ensureDefaultPlans();
        String planId = id;
        return planRepository.findById(planId).map(this::planToMap).orElseGet(() -> planToMap(defaultPlan(planId, 99)));
    }

    public double price(String planId, String cycle) {
        return price(planId, cycle, 1);
    }

    public double price(String planId, String cycle, int quantity) {
        Map<String, Object> item = activePlanOrThrow(planId);
        double monthly = ((Number) item.getOrDefault("effectiveMonthlyPrice", item.get("monthlyPrice"))).doubleValue();
        int members = Boolean.TRUE.equals(item.get("teamShared")) && !Boolean.TRUE.equals(item.get("topUpPack")) ? Math.max(1, quantity) : 1;
        double base = monthly * members;
        return Math.round(base * 100D) / 100D;
    }
    private int cycleMonths(String cycle) {
        if ("yearly".equals(cycle)) return 12;
        if ("quarterly".equals(cycle)) return 3;
        return 1;
    }
    private String entitlementFor(String action) {
        String a = safe(action, "").trim().toLowerCase(java.util.Locale.ROOT);
        if (a.isBlank()) return null;
        if (a.contains("ppt")) return "ppt";
        if (a.contains("综述") || a.contains("review")) return "review";
        if (a.contains("问答") || a.contains("对话") || a.contains("chat") || a.contains("qa")) return "chat";
        if (a.contains("调研") || a.contains("广场") || a.contains("research")) return "research";
        if (a.contains("汇报") || a.contains("report")) return "report";
        if (a.contains("对照") || a.contains("双栏") || a.contains("translate") || a.contains("translation")) {
            if (a.contains("沉浸") || a.contains("immersive")) {
                return "immersive";
            }
            return "translate";
        }
        if (a.contains("沉浸") || a.contains("immersive")) return "immersive";
        return null;
    }
    private String label(String kind) { return Map.of("review", "论文综述", "ppt", "PPT 生成", "chat", "研读解析与对话", "research", "调研广场", "report", "组会一键汇报", "translate", "对照翻译", "immersive", "沉浸翻译").get(kind); }
    private Map<String, Object> allowance(Integer quota, Integer used) { int q = number(quota); int u = number(used); return Map.of("quota", q, "used", u, "remaining", Math.max(0, q - u)); }
    private int number(Integer value) { return value == null ? 0 : value; }
    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private String limitText(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.length() <= maxLength) return text;
        return text.substring(0, Math.max(0, maxLength));
    }
    private boolean isTeamPlan(String id) { return Boolean.TRUE.equals(plan(id).get("teamShared")); }
    private int teamSeats(String id) { return ((Number) plan(id).getOrDefault("teamSeats", 0)).intValue(); }
    private boolean isBuiltInPlan(String id) {
        String planId = normalizePlanId(id);
        return PLAN_LITE.equals(planId)
            || PLAN_PLUS.equals(planId)
            || PLAN_PRO.equals(planId)
            || PLAN_MAX.equals(planId)
            || PLAN_TEAM_PLUS.equals(planId)
            || PLAN_TEAM_PRO.equals(planId);
    }
    public String normalizePlanId(String id) {
        String value = safe(id, "free");
        if ("light".equals(value)) return PLAN_LITE;
        if ("study".equals(value)) return PLAN_PLUS;
        if ("lab".equals(value)) return PLAN_PRO;
        if ("team".equals(value)) return PLAN_TEAM_PLUS;
        return value.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
    }
    private String planName(String id) {
        return String.valueOf(plan(id).getOrDefault("name", "未开通会员"));
    }

    private Map<String, Object> activePlanOrThrow(String id) {
        Map<String, Object> item = entitlementPlanOrThrow(id);
        if ("free".equals(item.get("id")) || Boolean.FALSE.equals(item.get("activeFlag"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐");
        }
        return item;
    }

    /** Existing paid entitlements remain readable after an administrator takes a plan off sale. */
    private Map<String, Object> entitlementPlanOrThrow(String id) {
        String rawPlanId = safe(id, "free").trim();
        String planId = normalizePlanId(rawPlanId);
        ensureDefaultPlans();
        MembershipPlanEntity entity = planRepository.findById(rawPlanId)
            .orElseGet(() -> planRepository.findById(planId).orElse(null));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "兑换码绑定的会员套餐不存在，请让管理员重新生成兑换码");
        }
        return planToMap(entity);
    }

    private void ensureDefaultPlans() {
        List<MembershipPlanEntity> defaults = new ArrayList<>();
        defaults.add(defaultPlan("free", 0));
        defaults.add(defaultPlan(PLAN_LITE, 1));
        defaults.add(defaultPlan(PLAN_PLUS, 2));
        defaults.add(defaultPlan(PLAN_PRO, 3));
        defaults.add(defaultPlan(PLAN_TEAM_PLUS, 4));
        defaults.add(defaultPlan(PLAN_TEAM_PRO, 5));
        for (MembershipPlanEntity item : defaults) {
            java.util.Optional<MembershipPlanEntity> existingOpt = planRepository.findById(item.getId());
            if (existingOpt.isEmpty()) {
                planRepository.save(item);
            } else {
                MembershipPlanEntity existing = existingOpt.get();
                boolean changed = false;
                if (!item.getName().equals(existing.getName())) {
                    existing.setName(item.getName());
                    changed = true;
                }
                if (existing.getSubtitle() == null || !existing.getSubtitle().equals(item.getSubtitle())) {
                    existing.setSubtitle(item.getSubtitle());
                    changed = true;
                }
                if (existing.getMonthlyPrice() == null || !existing.getMonthlyPrice().equals(item.getMonthlyPrice())) {
                    existing.setMonthlyPrice(item.getMonthlyPrice());
                    changed = true;
                }
                if (existing.getReviewQuota() == null) {
                    existing.setReviewQuota(item.getReviewQuota());
                    changed = true;
                }
                if (existing.getPptQuota() == null) {
                    existing.setPptQuota(item.getPptQuota());
                    changed = true;
                }
                if (existing.getChatQuota() == null) {
                    existing.setChatQuota(item.getChatQuota());
                    changed = true;
                }
                if (existing.getResearchQuota() == null) {
                    existing.setResearchQuota(item.getResearchQuota());
                    changed = true;
                }
                if (existing.getReportQuota() == null) {
                    existing.setReportQuota(item.getReportQuota());
                    changed = true;
                }
                // Only legacy rows without a value receive the default. An explicit false is
                // the administrator's off-shelf choice and must survive every catalog refresh.
                if (existing.getActiveFlag() == null) {
                    existing.setActiveFlag(true);
                    changed = true;
                }
                if (existing.getTranslateDailyQuota() == null) {
                    existing.setTranslateDailyQuota(integer(item.getTranslateDailyQuota(), integer(item.getTranslateQuota(), 0)));
                    changed = true;
                }
                if (existing.getImmersiveDailyQuota() == null) {
                    existing.setImmersiveDailyQuota(integer(item.getImmersiveDailyQuota(), integer(item.getImmersiveQuota(), 0)));
                    changed = true;
                }
                if (changed) {
                    planRepository.save(existing);
                }
            }
        }
    }

    private MembershipPlanEntity defaultPlan(String id, int sortOrder) {
        id = normalizePlanId(id);
        MembershipPlanEntity item = new MembershipPlanEntity();
        item.setId(id);
        item.setSortOrder(sortOrder);
        item.setActiveFlag(true);
        item.setSeckillEnabled(false);
        item.setOriginalMonthlyPrice(null);
        item.setImmersiveQuota(3);
        if ("free".equals(id)) {
            item.setName("个人 Free"); item.setSubtitle("永久免费版"); item.setMonthlyPrice(0D);
            item.setReviewQuota(90); item.setPptQuota(0); item.setChatQuota(150); item.setTranslateQuota(5); item.setImmersiveQuota(3);
            item.setResearchQuota(90); item.setReportQuota(1);
        } else if (PLAN_LITE.equals(id)) {
            item.setName("个人 Lite"); item.setSubtitle("一杯瑞幸咖啡价"); item.setMonthlyPrice(9.9D);
            item.setReviewQuota(450); item.setPptQuota(0); item.setChatQuota(900); item.setTranslateQuota(10); item.setImmersiveQuota(10);
            item.setResearchQuota(300); item.setReportQuota(5);
        } else if (PLAN_PLUS.equals(id)) {
            item.setName("个人 Plus"); item.setSubtitle("热销推荐"); item.setMonthlyPrice(19.9D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setResearchQuota(600); item.setReportQuota(10);
            item.setForumSpecial(true);
        } else if (PLAN_PRO.equals(id)) {
            item.setName("个人 Pro"); item.setSubtitle("极速进阶"); item.setMonthlyPrice(29.9D);
            item.setReviewQuota(1800); item.setPptQuota(6); item.setChatQuota(3600); item.setTranslateQuota(50); item.setImmersiveQuota(50);
            item.setResearchQuota(1200); item.setReportQuota(20);
            item.setForumSpecial(true); item.setForumTopDaily(1); item.setPeakPriority(true);
        } else if (PLAN_TEAM_PLUS.equals(id)) {
            item.setName("课题组团队 Plus"); item.setSubtitle("导师购买分配"); item.setMonthlyPrice(17.91D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setResearchQuota(600); item.setReportQuota(10);
            item.setTeamShared(true); item.setTeamSeats(10); item.setForumSpecial(true); item.setPeakPriority(true);
        } else if (PLAN_TEAM_PRO.equals(id)) {
            item.setId(PLAN_TEAM_PRO); item.setName("课题组团队 Pro"); item.setSubtitle("实验室旗舰"); item.setMonthlyPrice(26.91D);
            item.setReviewQuota(1800); item.setPptQuota(6); item.setChatQuota(3600); item.setTranslateQuota(50); item.setImmersiveQuota(50);
            item.setResearchQuota(1200); item.setReportQuota(20);
            item.setTeamShared(true); item.setTeamSeats(20); item.setForumSpecial(true); item.setForumTopDaily(1); item.setPeakPriority(true);
        } else {
            item.setName("新会员套餐"); item.setSubtitle("自定义上架套餐"); item.setMonthlyPrice(19.9D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setResearchQuota(600); item.setReportQuota(10);
            item.setForumSpecial(true);
        }
        if (item.getTranslateDailyQuota() == null) item.setTranslateDailyQuota(item.getTranslateQuota());
        if (item.getImmersiveDailyQuota() == null) item.setImmersiveDailyQuota(item.getImmersiveQuota());
        setDefaultAgentTokenQuota(item, id);
        return item;
    }

    private Map<String, Object> planToMap(MembershipPlanEntity entity) {
        Map<String, Object> item = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        boolean seckillActive = Boolean.TRUE.equals(entity.getSeckillEnabled())
            && entity.getSeckillPrice() != null
            && entity.getSeckillPrice() >= 0
            && (entity.getSeckillStartsAt() == null || !entity.getSeckillStartsAt().isAfter(now))
            && (entity.getSeckillEndsAt() == null || entity.getSeckillEndsAt().isAfter(now));
        double monthly = decimal(entity.getMonthlyPrice(), 0D);
        double original = decimal(entity.getOriginalMonthlyPrice(), monthly);
        double effective = seckillActive ? decimal(entity.getSeckillPrice(), monthly) : monthly;
        item.put("id", entity.getId());
        item.put("name", safe(entity.getName(), entity.getId()));
        item.put("subtitle", safe(entity.getSubtitle(), ""));
        item.put("monthlyPrice", monthly);
        item.put("originalMonthlyPrice", original);
        long agentQuota = Boolean.FALSE.equals(entity.getAgentEnabled()) ? 0L : longNumber(entity.getAgentTokenQuota());
        item.put("agentTokenQuota", agentQuota);
        item.put("tokenQuota", agentQuota);
        item.put("pluginImportEnabled", !Boolean.FALSE.equals(entity.getPluginImportEnabled()));
        item.put("agentEnabled", !Boolean.FALSE.equals(entity.getAgentEnabled()));
        item.put("reviewEnabled", !Boolean.FALSE.equals(entity.getReviewEnabled()));
        item.put("chatEnabled", !Boolean.FALSE.equals(entity.getChatEnabled()));
        item.put("effectiveMonthlyPrice", effective);
        item.put("reviewQuota", Boolean.FALSE.equals(entity.getReviewEnabled()) ? 0 : integer(entity.getReviewQuota(), 0));
        item.put("pptQuota", integer(entity.getPptQuota(), 0));
        item.put("chatQuota", Boolean.FALSE.equals(entity.getChatEnabled()) ? 0 : integer(entity.getChatQuota(), 0));
        item.put("translateQuota", integer(entity.getTranslateQuota(), 0));
        item.put("immersiveQuota", integer(entity.getImmersiveQuota(), 0));
        item.put("researchQuota", integer(entity.getResearchQuota(), 0));
        item.put("reportQuota", integer(entity.getReportQuota(), 0));
        item.put("reviewQuotaDaily", Math.max(0, integer(entity.getReviewQuota(), 0) / 30));
        item.put("pptQuotaMonthly", integer(entity.getPptQuota(), 0));
        item.put("chatQuotaDaily", Math.max(0, integer(entity.getChatQuota(), 0) / 30));
        item.put("translateQuotaDaily", integer(entity.getTranslateDailyQuota(), integer(entity.getTranslateQuota(), 0)));
        item.put("immersiveQuotaDaily", integer(entity.getImmersiveDailyQuota(), integer(entity.getImmersiveQuota(), 0)));
        item.put("researchQuotaDaily", Math.max(0, integer(entity.getResearchQuota(), 0) / 30));
        item.put("reportQuotaMonthly", integer(entity.getReportQuota(), 0));
        item.put("teamSeats", integer(entity.getTeamSeats(), 0));
        item.put("teamShared", Boolean.TRUE.equals(entity.getTeamShared()));
        item.put("forumSpecial", Boolean.TRUE.equals(entity.getForumSpecial()));
        item.put("forumTopDaily", integer(entity.getForumTopDaily(), 0));
        item.put("peakPriority", Boolean.TRUE.equals(entity.getPeakPriority()));
        item.put("activeFlag", !Boolean.FALSE.equals(entity.getActiveFlag()));
        item.put("topUpPack", Boolean.TRUE.equals(entity.getTopUpPack()));
        item.put("sortOrder", integer(entity.getSortOrder(), 99));
        item.put("seckillEnabled", Boolean.TRUE.equals(entity.getSeckillEnabled()));
        item.put("seckillActive", seckillActive);
        item.put("seckillPrice", entity.getSeckillPrice());
        item.put("seckillStartsAt", entity.getSeckillStartsAt());
        item.put("seckillEndsAt", entity.getSeckillEndsAt());
        item.put("seckillLabel", safe(entity.getSeckillLabel(), "限时秒杀"));
        item.put("seckillRemainingSeconds", seckillActive && entity.getSeckillEndsAt() != null ? Math.max(0, java.time.Duration.between(now, entity.getSeckillEndsAt()).toSeconds()) : 0);
        return item;
    }

    private int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    private long longNumber(Object value, long fallback) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return fallback;
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    private long longNumber(Object value) { return longNumber(value, 0L); }

    private long numberLong(Long value) { return value == null ? 0L : Math.max(0L, value); }

    private Map<String, Object> tokenAllowance(AppUserEntity owner) {
        long quota = numberLong(owner.getTokenLimit());
        long used = numberLong(owner.getTokenUsed());
        return Map.of("quota", quota, "used", used, "remaining", Math.max(0L, quota - used), "unit", "tokens");
    }

    private void setDefaultAgentTokenQuota(MembershipPlanEntity item, String id) {
        if (item.getAgentTokenQuota() != null) return;
        long quota = switch (id) {
            case "free" -> 20L;
            case PLAN_LITE -> 300L;
            case PLAN_PLUS -> 600L;
            case PLAN_PRO, PLAN_TEAM_PLUS -> 1_200L;
            case PLAN_TEAM_PRO -> 2_400L;
            default -> 0L;
        };
        item.setAgentTokenQuota(quota);
    }

    private double decimal(Object value, double fallback) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return fallback;
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }

    private boolean bool(Object value, Boolean fallback) {
        if (value instanceof Boolean bool) return bool;
        if (value == null) return Boolean.TRUE.equals(fallback);
        return "true".equalsIgnoreCase(String.valueOf(value)) || "1".equals(String.valueOf(value));
    }

    private String text(Object value, String fallback) {
        if (value == null) return fallback;
        return String.valueOf(value);
    }

    private LocalDateTime dateTime(Object value, LocalDateTime fallback) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        try { return LocalDateTime.parse(String.valueOf(value)); } catch (DateTimeParseException ignored) { return fallback; }
    }
}
