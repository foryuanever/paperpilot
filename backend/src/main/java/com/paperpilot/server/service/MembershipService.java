package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
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

    public MembershipService(AppUserRepository users) { this.users = users; }

    public List<Map<String, Object>> catalog() {
        return List.of(plan(PLAN_LITE), plan(PLAN_PLUS), plan(PLAN_PRO), plan(PLAN_MAX), plan(PLAN_TEAM_PLUS), plan(PLAN_TEAM_PRO));
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
        result.put("benefits", Map.of(
            "translation", Map.of("label", "论文翻译与文献导入", "unlimited", true),
            "review", allowance(owner.getReviewQuota(), owner.getReviewUsed()),
            "ppt", allowance(owner.getPptQuota(), owner.getPptUsed()),
            "chat", allowance(owner.getChatQuota(), owner.getChatUsed()),
            "teamSeats", Map.of("quota", teamSeats(id), "shared", isTeamPlan(id))
        ));
        return result;
    }

    public void activate(AppUserEntity user, String planId, String cycle) {
        planId = normalizePlanId(planId);
        if (!List.of(PLAN_LITE, PLAN_PLUS, PLAN_PRO, PLAN_MAX, PLAN_TEAM_PLUS, PLAN_TEAM_PRO).contains(planId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效会员套餐");
        }
        int months = cycleMonths(cycle);
        LocalDateTime startsAt = user.getMembershipExpiresAt() != null && user.getMembershipExpiresAt().isAfter(LocalDateTime.now())
            ? user.getMembershipExpiresAt() : LocalDateTime.now();
        Map<String, Object> plan = plan(planId);
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

    private Map<String, Object> plan(String id) {
        id = normalizePlanId(id);
        Map<String, Object> item = new LinkedHashMap<>();
        if (PLAN_LITE.equals(id)) {
            item.put("id", id); item.put("name", "个人 Lite"); item.put("monthlyPrice", 9.9); item.put("reviewQuota", 3); item.put("pptQuota", 0); item.put("chatQuota", 60); item.put("teamSeats", 0); item.put("teamShared", false);
        } else if (PLAN_PLUS.equals(id)) {
            item.put("id", id); item.put("name", "个人 Plus"); item.put("monthlyPrice", 19.9); item.put("reviewQuota", 10); item.put("pptQuota", 1); item.put("chatQuota", 180); item.put("teamSeats", 0); item.put("teamShared", false);
        } else if (PLAN_PRO.equals(id)) {
            item.put("id", id); item.put("name", "个人 Pro"); item.put("monthlyPrice", 39.9); item.put("reviewQuota", 25); item.put("pptQuota", 4); item.put("chatQuota", 500); item.put("teamSeats", 0); item.put("teamShared", false);
        } else if (PLAN_MAX.equals(id)) {
            item.put("id", id); item.put("name", "个人 Max"); item.put("monthlyPrice", 69.9); item.put("reviewQuota", 60); item.put("pptQuota", 10); item.put("chatQuota", 1200); item.put("teamSeats", 0); item.put("teamShared", false);
        } else if (PLAN_TEAM_PLUS.equals(id)) {
            item.put("id", id); item.put("name", "团队 Plus"); item.put("monthlyPrice", 129.0); item.put("reviewQuota", 120); item.put("pptQuota", 16); item.put("chatQuota", 2600); item.put("teamSeats", 8); item.put("teamShared", true);
        } else {
            item.put("id", PLAN_TEAM_PRO); item.put("name", "团队 Pro"); item.put("monthlyPrice", 229.0); item.put("reviewQuota", 260); item.put("pptQuota", 36); item.put("chatQuota", 6000); item.put("teamSeats", 15); item.put("teamShared", true);
        }
        return item;
    }

    public double price(String planId, String cycle) {
        double monthly = ((Number) plan(planId).get("monthlyPrice")).doubleValue();
        return Math.round(monthly * ("quarterly".equals(cycle) ? 2.7D : "yearly".equals(cycle) ? 9D : 1D) * 100D) / 100D;
    }
    private int cycleMonths(String cycle) { return "quarterly".equals(cycle) ? 3 : "yearly".equals(cycle) ? 12 : 1; }
    private String entitlementFor(String action) { String a = safe(action, ""); if (a.contains("PPT")) return "ppt"; if (a.contains("综述") || a.contains("汇报")) return "review"; if (a.contains("问答") || a.contains("对话")) return "chat"; return null; }
    private String label(String kind) { return Map.of("review", "论文综述", "ppt", "PPT 生成", "chat", "AI 问答").get(kind); }
    private Map<String, Object> allowance(Integer quota, Integer used) { int q = number(quota); int u = number(used); return Map.of("quota", q, "used", u, "remaining", Math.max(0, q - u)); }
    private int number(Integer value) { return value == null ? 0 : value; }
    private String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private boolean isTeamPlan(String id) { String normalized = normalizePlanId(id); return PLAN_TEAM_PLUS.equals(normalized) || PLAN_TEAM_PRO.equals(normalized); }
    private int teamSeats(String id) { String normalized = normalizePlanId(id); return PLAN_TEAM_PRO.equals(normalized) ? 15 : PLAN_TEAM_PLUS.equals(normalized) ? 8 : 0; }
    private String normalizePlanId(String id) {
        String value = safe(id, "free");
        if ("light".equals(value)) return PLAN_LITE;
        if ("study".equals(value)) return PLAN_PLUS;
        if ("lab".equals(value)) return PLAN_PRO;
        if ("team".equals(value)) return PLAN_TEAM_PLUS;
        return value;
    }
    private String planName(String id) {
        return switch (normalizePlanId(id)) {
            case PLAN_LITE -> "个人 Lite";
            case PLAN_PLUS -> "个人 Plus";
            case PLAN_PRO -> "个人 Pro";
            case PLAN_MAX -> "个人 Max";
            case PLAN_TEAM_PLUS -> "团队 Plus";
            case PLAN_TEAM_PRO -> "团队 Pro";
            default -> "未开通会员";
        };
    }
}
