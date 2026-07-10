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

    private final AppUserRepository users;

    public MembershipService(AppUserRepository users) { this.users = users; }

    public List<Map<String, Object>> catalog() {
        return List.of(plan(PLAN_LIGHT), plan(PLAN_STUDY), plan(PLAN_LAB));
    }

    public Map<String, Object> membership(AppUserEntity user) {
        expireIfNeeded(user);
        Map<String, Object> result = new LinkedHashMap<>();
        String id = safe(user.getMembershipPlan(), "free");
        result.put("id", id);
        result.put("name", planName(id));
        result.put("cycle", safe(user.getMembershipCycle(), "monthly"));
        result.put("expiresAt", user.getMembershipExpiresAt());
        result.put("active", !"free".equals(id));
        result.put("benefits", Map.of(
            "translation", Map.of("label", "论文翻译与文献导入", "unlimited", true),
            "review", allowance(user.getReviewQuota(), user.getReviewUsed()),
            "ppt", allowance(user.getPptQuota(), user.getPptUsed()),
            "chat", allowance(user.getChatQuota(), user.getChatUsed())
        ));
        return result;
    }

    public void activate(AppUserEntity user, String planId, String cycle) {
        if (!List.of(PLAN_LIGHT, PLAN_STUDY, PLAN_LAB).contains(planId)) {
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
        int quota = switch (kind) {
            case "review" -> number(user.getReviewQuota());
            case "ppt" -> number(user.getPptQuota());
            default -> number(user.getChatQuota());
        };
        int used = switch (kind) {
            case "review" -> number(user.getReviewUsed());
            case "ppt" -> number(user.getPptUsed());
            default -> number(user.getChatUsed());
        };
        if (used >= quota) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, planName(safe(user.getMembershipPlan(), "free")) + "的" + label(kind) + "额度已用完，请升级或续费后继续使用。");
        }
        if ("review".equals(kind)) user.setReviewUsed(used + 1);
        else if ("ppt".equals(kind)) user.setPptUsed(used + 1);
        else user.setChatUsed(used + 1);
        users.save(user);
    }

    public void assertAvailable(Long userId, String action) {
        AppUserEntity user = users.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        expireIfNeeded(user);
        String kind = entitlementFor(action);
        if (kind == null) return;
        int quota = "review".equals(kind) ? number(user.getReviewQuota()) : "ppt".equals(kind) ? number(user.getPptQuota()) : number(user.getChatQuota());
        int used = "review".equals(kind) ? number(user.getReviewUsed()) : "ppt".equals(kind) ? number(user.getPptUsed()) : number(user.getChatUsed());
        if (used >= quota) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, planName(safe(user.getMembershipPlan(), "free")) + "不含可用的" + label(kind) + "额度，请升级或续费后继续使用。");
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

    private Map<String, Object> plan(String id) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (PLAN_LIGHT.equals(id)) {
            item.put("id", id); item.put("name", "轻享会员"); item.put("monthlyPrice", 9.9); item.put("reviewQuota", 0); item.put("pptQuota", 0); item.put("chatQuota", 0);
        } else if (PLAN_STUDY.equals(id)) {
            item.put("id", id); item.put("name", "研读会员"); item.put("monthlyPrice", 19.9); item.put("reviewQuota", 8); item.put("pptQuota", 1); item.put("chatQuota", 30);
        } else {
            item.put("id", id); item.put("name", "课题会员"); item.put("monthlyPrice", 29.9); item.put("reviewQuota", 20); item.put("pptQuota", 4); item.put("chatQuota", 100);
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
    private String planName(String id) { return "light".equals(id) ? "轻享会员" : "study".equals(id) ? "研读会员" : "lab".equals(id) ? "课题会员" : "基础版"; }
}
