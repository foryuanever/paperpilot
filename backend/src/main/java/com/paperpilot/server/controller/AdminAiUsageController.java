package com.paperpilot.server.controller;

import com.paperpilot.server.service.AiUsageService;
import com.paperpilot.server.service.AuthService;
import com.paperpilot.server.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai-usage")
public class AdminAiUsageController {
    private final AiUsageService aiUsageService;
    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public AdminAiUsageController(AiUsageService aiUsageService, CurrentUserService currentUserService, AuthService authService) {
        this.aiUsageService = aiUsageService;
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @ModelAttribute
    public void requireAdminAccess() {
        currentUserService.requireAdmin();
    }

    @GetMapping("/calls")
    public Map<String, Object> calls(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "scene", required = false) String scene,
        @RequestParam(value = "model", required = false) String model,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return aiUsageService.adminCalls(keyword, scene, model, status, startDate, endDate, page, pageSize);
    }

    @DeleteMapping("/calls")
    public Map<String, Object> clearCalls(HttpServletRequest request) {
        Map<String, Object> result = aiUsageService.clearAdminCalls();
        authService.logAction("管理员清空 AI 调用记录", "warn", getClientIp(request));
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        for (String header : new String[] {"CF-Connecting-IP", "X-Real-IP", "X-Forwarded-For"}) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return normalizeIp(value.split(",")[0].trim());
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private String normalizeIp(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::ffff:127.0.0.1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
