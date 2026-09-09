package com.paperpilot.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits user-initiated AI jobs before they reach an upstream model provider.
 * A model fallback remains part of the same user action and is therefore counted once.
 */
@Service
public class AiRequestRateLimiter {
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final ConcurrentHashMap<Long, Deque<Long>> requestTimes = new ConcurrentHashMap<>();
    private final int requestsPerMinute;
    private final boolean enabled;

    public AiRequestRateLimiter(
        @Value("${PAPERPILOT_AI_REQUESTS_PER_MINUTE:8}") int requestsPerMinute,
        @Value("${PAPERPILOT_AI_RATE_LIMIT_ENABLED:true}") boolean enabled
    ) {
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
        this.enabled = enabled;
    }

    public void checkAndRecord(Long userId) {
        if (!enabled || userId == null) return;

        long now = System.currentTimeMillis();
        Deque<Long> times = requestTimes.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() >= WINDOW_MILLIS) {
                times.removeFirst();
            }
            if (times.size() >= requestsPerMinute) {
                long retryAfterSeconds = Math.max(1L, (WINDOW_MILLIS - (now - times.peekFirst()) + 999L) / 1000L);
                throw new AiRateLimitExceededException(requestsPerMinute, retryAfterSeconds);
            }
            times.addLast(now);
        }
    }

    /** Failed or timed-out actions should not consume a user's anti-abuse allowance. */
    public void rollbackLast(Long userId) {
        if (!enabled || userId == null) return;
        Deque<Long> times = requestTimes.get(userId);
        if (times == null) return;
        synchronized (times) {
            if (!times.isEmpty()) times.removeLast();
            if (times.isEmpty()) requestTimes.remove(userId, times);
        }
    }

    public static class AiRateLimitExceededException extends ResponseStatusException {
        private final long retryAfterSeconds;

        public AiRateLimitExceededException(int requestsPerMinute, long retryAfterSeconds) {
            super(HttpStatus.TOO_MANY_REQUESTS,
                "AI 请求过于频繁。为保证所有用户可用，每分钟最多发起 " + requestsPerMinute + " 次，请 " + retryAfterSeconds + " 秒后再试。");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
