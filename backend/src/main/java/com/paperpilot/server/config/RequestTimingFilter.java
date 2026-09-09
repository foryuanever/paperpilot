package com.paperpilot.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Logs slow API requests without recording request bodies or credentials. */
public class RequestTimingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final long SLOW_REQUEST_MS = 1000L;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
            if (elapsedMs >= SLOW_REQUEST_MS && request.getRequestURI().startsWith("/api/")) {
                log.warn("Slow API request method={} uri={} status={} elapsedMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            }
        }
    }
}
