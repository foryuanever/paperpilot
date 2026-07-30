package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.MembershipPlanEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.MembershipPlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MembershipService {
    public static final String PLAN_LITE = "lite";
    public static final String PLAN_PLUS = "plus";
    public static final String PLAN_PRO = "pro";
    public static final String PLAN_MAX = "max";
    public static final String PLAN_TEAM_PLUS = "team_plus";
    public static final String PLAN_TEAM_PRO = "team_pro";

    private final AppUserRepository users;
    private final MembershipPlanRepository planRepository;

    public MembershipService(AppUserRepository users, MembershipPlanRepository planRepository) {
        this.users = users;
        this.planRepository = planRepository;
    }

    public List<Map<String, Object>> catalog() {
        ensureDefaultPlans();
        return planRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::planToMap).toList();
    }

    public Map<String, Object> membership(AppUserEntity user) {
        expireIfNeeded(user);
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        String id = normalizePlanId(owner.getMembershipPlan());
        result.put("id", id);
        result.put("name", owner.getId() != null && !owner.getId().equals(user.getId()) ? planName(id) + "（团队共享）" : planName(id));
        result.put("cycle", safe(owner.getMembershipCycle(), "monthly"));
        result.put("expiresAt", owner.getMembershipExpiresAt());
        result.put("active", !"free".equals(id));
        result.put("sharedFromTeam", owner.getId() != null && !owner.getId().equals(user.getId()));
        int translateDaily = translateDailyQuota(id);
        int immersiveDaily = immersiveDailyQuota(id);
        result.put("benefits", Map.of(
            "translation", Map.of("label", "对照翻译", "quota", translateDaily, "used", 0, "remaining", translateDaily),
            "immersive", Map.of("label", "沉浸翻译", "quota", immersiveDaily, "used", 0, "remaining", immersiveDaily),
            "review", allowance(owner.getReviewQuota(), owner.getReviewUsed()),
            "ppt", allowance(owner.getPptQuota(), owner.getPptUsed()),
            "chat", allowance(owner.getChatQuota(), owner.getChatUsed()),
            "teamSeats", Map.of("quota", teamSeats(id), "shared", isTeamPlan(id))
        ));
        return result;
    }

    private int translateDailyQuota(String planId) {
        return ((Number) plan(planId).getOrDefault("translateQuotaDaily", 5)).intValue();
    }

    private int immersiveDailyQuota(String planId) {
        return ((Number) plan(planId).getOrDefault("immersiveQuotaDaily", 3)).intValue();
    }

    public void activate(AppUserEntity user, String planId, String cycle) {
        planId = normalizePlanId(planId);
        Map<String, Object> plan = activePlanOrThrow(planId);
        if ("free".equals(planId) || Boolean.FALSE.equals(plan.get("activeFlag"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐");
        }
        int months = cycleMonths(cycle);
        LocalDateTime startsAt = user.getMembershipExpiresAt() != null && user.getMembershipExpiresAt().isAfter(LocalDateTime.now())
            ? user.getMembershipExpiresAt() : LocalDateTime.now();
        user.setMembershipPlan(planId);
        user.setMembershipCycle(cycle);
        user.setMembershipExpiresAt(startsAt.plusMonths(months));
        user.setReviewQuota((Integer) plan.get("reviewQuota"));
        user.setReviewUsed(0);
        user.setPptQuota((Integer) plan.get("pptQuota"));
        user.setPptUsed(0);
        user.setChatQuota((Integer) plan.get("chatQuota"));
        user.setChatUsed(0);
        users.save(user);
    }

    public void consume(AppUserEntity user, String action) {
        if ("管理员".equals(user.getRole())) return;
        expireIfNeeded(user);
        String kind = entitlementFor(action);
        if (kind == null) return;
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        int quota = switch (kind) {
            case "review" -> number(owner.getReviewQuota());
            case "ppt" -> number(owner.getPptQuota());
            default -> number(owner.getChatQuota());
        };
        int used = switch (kind) {
            case "review" -> number(owner.getReviewUsed());
            case "ppt" -> number(owner.getPptUsed());
            default -> number(owner.getChatUsed());
        };
        if (used >= quota) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, planName(safe(owner.getMembershipPlan(), "free")) + "的" + label(kind) + "额度已用完，请升级或续费后继续使用。");
        }
        if ("review".equals(kind)) owner.setReviewUsed(used + 1);
        else if ("ppt".equals(kind)) owner.setPptUsed(used + 1);
        else owner.setChatUsed(used + 1);
        users.save(owner);
    }

    public void assertAvailable(Long userId, String action) {
        AppUserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if ("管理员".equals(user.getRole())) return;
        expireIfNeeded(user);
        String kind = entitlementFor(action);
        if (kind == null) return;
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        int quota = "review".equals(kind) ? number(owner.getReviewQuota()) : "ppt".equals(kind) ? number(owner.getPptQuota()) : number(owner.getChatQuota());
        int used = "review".equals(kind) ? number(owner.getReviewUsed()) : "ppt".equals(kind) ? number(owner.getPptUsed()) : number(owner.getChatUsed());
        if (used >= quota) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, planName(safe(owner.getMembershipPlan(), "free")) + "不含可用的" + label(kind) + "额度，请升级或续费后继续使用。");
        }
    }

    private void expireIfNeeded(AppUserEntity user) {
        if (user.getMembershipExpiresAt() != null && user.getMembershipExpiresAt().isBefore(LocalDateTime.now())) {
            user.setMembershipPlan("free");
            user.setReviewQuota(0); user.setReviewUsed(0);
            user.setPptQuota(0); user.setPptUsed(0);
            user.setChatQuota(0); user.setChatUsed(0);
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
        if (body.containsKey("reviewQuota")) entity.setReviewQuota(integer(body.get("reviewQuota"), entity.getReviewQuota()));
        if (body.containsKey("pptQuota")) entity.setPptQuota(integer(body.get("pptQuota"), entity.getPptQuota()));
        if (body.containsKey("chatQuota")) entity.setChatQuota(integer(body.get("chatQuota"), entity.getChatQuota()));
        if (body.containsKey("translateQuota")) entity.setTranslateQuota(integer(body.get("translateQuota"), entity.getTranslateQuota()));
        if (body.containsKey("immersiveQuota")) entity.setImmersiveQuota(integer(body.get("immersiveQuota"), entity.getImmersiveQuota()));
        if (body.containsKey("teamSeats")) entity.setTeamSeats(integer(body.get("teamSeats"), entity.getTeamSeats()));
        if (body.containsKey("teamShared")) entity.setTeamShared(bool(body.get("teamShared"), entity.getTeamShared()));
        if (body.containsKey("forumSpecial")) entity.setForumSpecial(bool(body.get("forumSpecial"), entity.getForumSpecial()));
        if (body.containsKey("forumTopDaily")) entity.setForumTopDaily(integer(body.get("forumTopDaily"), entity.getForumTopDaily()));
        if (body.containsKey("peakPriority")) entity.setPeakPriority(bool(body.get("peakPriority"), entity.getPeakPriority()));
        if (body.containsKey("activeFlag")) entity.setActiveFlag(bool(body.get("activeFlag"), entity.getActiveFlag()));
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

    private Map<String, Object> plan(String id) {
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
        int members = Boolean.TRUE.equals(item.get("teamShared")) ? Math.max(1, quantity) : 1;
        double base = monthly * members;
        return Math.round(base * 100D) / 100D;
    }
    private int cycleMonths(String cycle) { return 1; }
    private String entitlementFor(String action) { String a = safe(action, ""); if (a.contains("PPT")) return "ppt"; if (a.contains("综述") || a.contains("汇报")) return "review"; if (a.contains("问答") || a.contains("对话")) return "chat"; return null; }
    private String label(String kind) { return Map.of("review", "论文综述", "ppt", "PPT 生成", "chat", "AI 问答").get(kind); }
    private Map<String, Object> allowance(Integer quota, Integer used) { int q = number(quota); int u = number(used); return Map.of("quota", q, "used", u, "remaining", Math.max(0, q - u)); }
    private int number(Integer value) { return value == null ? 0 : value; }
    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
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
    private String normalizePlanId(String id) {
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
        String planId = normalizePlanId(id);
        ensureDefaultPlans();
        MembershipPlanEntity entity = planRepository.findById(planId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐"));
        Map<String, Object> item = planToMap(entity);
        if ("free".equals(planId) || Boolean.FALSE.equals(item.get("activeFlag"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐");
        }
        return item;
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
            if (!planRepository.existsById(item.getId())) {
                planRepository.save(item);
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
        } else if (PLAN_LITE.equals(id)) {
            item.setName("个人 Lite"); item.setSubtitle("一杯瑞幸咖啡价"); item.setMonthlyPrice(9.9D);
            item.setReviewQuota(450); item.setPptQuota(2); item.setChatQuota(900); item.setTranslateQuota(10); item.setImmersiveQuota(10);
        } else if (PLAN_PLUS.equals(id)) {
            item.setName("个人 Plus"); item.setSubtitle("热销推荐"); item.setMonthlyPrice(19.9D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setForumSpecial(true);
        } else if (PLAN_PRO.equals(id)) {
            item.setName("个人 Pro"); item.setSubtitle("极速进阶"); item.setMonthlyPrice(29.9D);
            item.setReviewQuota(1800); item.setPptQuota(6); item.setChatQuota(3600); item.setTranslateQuota(50); item.setImmersiveQuota(50);
            item.setForumSpecial(true); item.setForumTopDaily(1); item.setPeakPriority(true);
        } else if (PLAN_TEAM_PLUS.equals(id)) {
            item.setName("课题组团队 Plus"); item.setSubtitle("导师购买分配"); item.setMonthlyPrice(17.91D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setTeamShared(true); item.setTeamSeats(10); item.setForumSpecial(true); item.setPeakPriority(true);
        } else if (PLAN_TEAM_PRO.equals(id)) {
            item.setId(PLAN_TEAM_PRO); item.setName("课题组团队 Pro"); item.setSubtitle("实验室旗舰"); item.setMonthlyPrice(26.91D);
            item.setReviewQuota(1800); item.setPptQuota(6); item.setChatQuota(3600); item.setTranslateQuota(50); item.setImmersiveQuota(50);
            item.setTeamShared(true); item.setTeamSeats(20); item.setForumSpecial(true); item.setForumTopDaily(1); item.setPeakPriority(true);
        } else {
            item.setName("新会员套餐"); item.setSubtitle("自定义上架套餐"); item.setMonthlyPrice(19.9D);
            item.setReviewQuota(900); item.setPptQuota(4); item.setChatQuota(1800); item.setTranslateQuota(20); item.setImmersiveQuota(20);
            item.setForumSpecial(true);
        }
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
        item.put("effectiveMonthlyPrice", effective);
        item.put("reviewQuota", integer(entity.getReviewQuota(), 0));
        item.put("pptQuota", integer(entity.getPptQuota(), 0));
        item.put("chatQuota", integer(entity.getChatQuota(), 0));
        item.put("translateQuota", integer(entity.getTranslateQuota(), 0));
        item.put("immersiveQuota", integer(entity.getImmersiveQuota(), 0));
        item.put("reviewQuotaDaily", Math.max(0, integer(entity.getReviewQuota(), 0) / 30));
        item.put("pptQuotaMonthly", integer(entity.getPptQuota(), 0));
        item.put("chatQuotaDaily", Math.max(0, integer(entity.getChatQuota(), 0) / 30));
        item.put("translateQuotaDaily", integer(entity.getTranslateQuota(), 0));
        item.put("immersiveQuotaDaily", integer(entity.getImmersiveQuota(), 0));
        item.put("teamSeats", integer(entity.getTeamSeats(), 0));
        item.put("teamShared", Boolean.TRUE.equals(entity.getTeamShared()));
        item.put("forumSpecial", Boolean.TRUE.equals(entity.getForumSpecial()));
        item.put("forumTopDaily", integer(entity.getForumTopDaily(), 0));
        item.put("peakPriority", Boolean.TRUE.equals(entity.getPeakPriority()));
        item.put("activeFlag", !Boolean.FALSE.equals(entity.getActiveFlag()));
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
