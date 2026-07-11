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
    public static final String PLAN_LIGHT = "light";
    public static final String PLAN_STUDY = "study";
    public static final String PLAN_LAB = "lab";
    public static final String PLAN_TEAM = "team";

    private final AppUserRepository users;

    public MembershipService(AppUserRepository users) { this.users = users; }

    public List<Map<String, Object>> catalog() {
        return List.of(plan(PLAN_LIGHT), plan(PLAN_STUDY), plan(PLAN_LAB), plan(PLAN_TEAM));
    }

    public Map<String, Object> membership(AppUserEntity user) {
        expireIfNeeded(user);
        AppUserEntity owner = entitlementOwner(user);
        if (owner.getId() != null && !owner.getId().equals(user.getId())) {
            expireIfNeeded(owner);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        String id = safe(owner.getMembershipPlan(), "free");
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
            "teamSeats", Map.of("quota", teamSeats(id), "shared", PLAN_TEAM.equals(id))
        ));
        return result;
    }

    public void activate(AppUserEntity user, String planId, String cycle) {
        if (!List.of(PLAN_LIGHT, PLAN_STUDY, PLAN_LAB, PLAN_TEAM).contains(planId)) {
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
                .filter(item -> PLAN_TEAM.equals(item.getMembershipPlan()))
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
        Map<String, Object> item = new LinkedHashMap<>();
        if (PLAN_LIGHT.equals(id)) {
            item.put("id", id); item.put("name", "轻享会员"); item.put("monthlyPrice", 9.9); item.put("reviewQuota", 3); item.put("pptQuota", 0); item.put("chatQuota", 20);
        } else if (PLAN_STUDY.equals(id)) {
            item.put("id", id); item.put("name", "研读会员"); item.put("monthlyPrice", 19.9); item.put("reviewQuota", 10); item.put("pptQuota", 2); item.put("chatQuota", 80);
        } else if (PLAN_TEAM.equals(id)) {
            item.put("id", id); item.put("name", "导师车队会员"); item.put("monthlyPrice", 69.9); item.put("reviewQuota", 60); item.put("pptQuota", 12); item.put("chatQuota", 360); item.put("teamSeats", 20); item.put("teamShared", true);
        } else {
            item.put("id", id); item.put("name", "课题会员"); item.put("monthlyPrice", 29.9); item.put("reviewQuota", 25); item.put("pptQuota", 5); item.put("chatQuota", 180);
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
    private int teamSeats(String id) { return PLAN_TEAM.equals(id) ? 20 : 8; }
    private String planName(String id) { return "light".equals(id) ? "轻享会员" : "study".equals(id) ? "研读会员" : "lab".equals(id) ? "课题会员" : "team".equals(id) ? "导师车队会员" : "未开通会员"; }
}
