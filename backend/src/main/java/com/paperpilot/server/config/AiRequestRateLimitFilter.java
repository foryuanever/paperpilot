package com.paperpilot.server.config;

import com.paperpilot.server.service.AiRequestRateLimiter;
import com.paperpilot.server.service.SessionTokenService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/** Applies one shared AI-job limit to real user actions, not internal model retries or polling. */
@Component
@Order(2)
public class AiRequestRateLimitFilter implements Filter {
    private static final Set<String> AI_JOB_PATHS = Set.of(
        "/api/topics/generate",
        "/api/meeting-reports/deck/generate",
        "/api/meeting-reports/deck/analyze",
        "/api/meeting-reports/fuse"
    );

    private final AiRequestRateLimiter rateLimiter;
    private final SessionTokenService sessionTokenService;

    public AiRequestRateLimitFilter(AiRequestRateLimiter rateLimiter, SessionTokenService sessionTokenService) {
        this.rateLimiter = rateLimiter;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)
            || !"POST".equalsIgnoreCase(httpRequest.getMethod()) || !isAiJobPath(httpRequest.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        Long userId = sessionTokenService.verify(httpRequest.getHeader("X-PaperPilot-Session")).orElse(null);
        if (userId == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            rateLimiter.checkAndRecord(userId);
            chain.doFilter(request, response);
            if (httpResponse.getStatus() >= 400) {
                rateLimiter.rollbackLast(userId);
            }
        } catch (AiRequestRateLimiter.AiRateLimitExceededException error) {
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", String.valueOf(error.getRetryAfterSeconds()));
            httpResponse.setContentType("application/json;charset=UTF-8");
            String message = error.getReason().replace("\\", "\\\\").replace("\"", "\\\"");
            httpResponse.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
        }
    }

    private boolean isAiJobPath(String uri) {
        if (AI_JOB_PATHS.contains(uri)) return true;
        return uri.matches("/api/meeting-reports/[^/]+/(generate|generate-section|ask)");
    }
}
