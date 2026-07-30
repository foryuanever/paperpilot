package com.paperpilot.server.config;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.service.MonitoringSecurityService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class SecurityMonitoringFilter implements Filter {

    private final MonitoringSecurityService monitoringSecurityService;
    private final AppUserRepository appUserRepository;
    private final ConcurrentHashMap<Long, String> usernameCache = new ConcurrentHashMap<>();

    public SecurityMonitoringFilter(
        MonitoringSecurityService monitoringSecurityService,
        AppUserRepository appUserRepository
    ) {
        this.monitoringSecurityService = monitoringSecurityService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        applyCorsHeaders(httpRequest, httpResponse);
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Skip monitoring polling itself, otherwise the dashboard creates its own traffic spike.
        if (uri.startsWith("/api/admin/monitoring")) {
            chain.doFilter(request, response);
            return;
        }

        // Skip static asset files to prevent spamming logs
        if (uri.contains(".") && !uri.endsWith(".json") && !uri.endsWith(".jsp") && !uri.endsWith(".do")) {
            chain.doFilter(request, response);
            return;
        }

        // Get Client IP (handling proxies)
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = httpRequest.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isBlank() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = httpRequest.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isBlank() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = httpRequest.getRemoteAddr();
        }

        // Split proxy IPs if multiple
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        // Get User ID
        Long userId = null;
        String userHeader = httpRequest.getHeader("X-PaperPilot-User-Id");
        if (userHeader != null && !userHeader.isBlank()) {
            try {
                userId = Long.parseLong(userHeader);
            } catch (NumberFormatException ignored) {}
        }

        // Cache Username lookup
        if (userId != null) {
            String cachedName = usernameCache.get(userId);
            if (cachedName == null) {
                final Long uid = userId;
                AppUserEntity user = appUserRepository.findById(uid).orElse(null);
                if (user != null) {
                    cachedName = user.getUsername();
                    usernameCache.put(uid, cachedName);
                } else {
                    cachedName = "用户-" + uid;
                }
            }
            monitoringSecurityService.recordIpUsername(ipAddress, cachedName);
            monitoringSecurityService.recordUserName(userId, cachedName);
        }

        // Register request and perform check
        long startedAt = System.currentTimeMillis();
        MonitoringSecurityService.RequestDecision decision = monitoringSecurityService.registerRequest(
            userId,
            ipAddress,
            httpRequest.getMethod(),
            uri
        );

        if (!decision.allowed) {
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"您的账号或IP由于异常的高频请求已被防火墙安全阻断，如有疑问请联系系统管理员。\"}");
            monitoringSecurityService.completeRequest(decision.log, HttpServletResponse.SC_FORBIDDEN, System.currentTimeMillis() - startedAt);
            return;
        }

        try {
            chain.doFilter(request, response);
        } finally {
            monitoringSecurityService.completeRequest(decision.log, httpResponse.getStatus(), System.currentTimeMillis() - startedAt);
        }
    }

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-PaperPilot-User-Id, X-Requested-With, Accept");
        response.setHeader("Access-Control-Max-Age", "3600");
    }
}
