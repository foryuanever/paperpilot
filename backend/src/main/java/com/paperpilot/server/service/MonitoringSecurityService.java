package com.paperpilot.server.service;

import com.paperpilot.server.entity.RequestMonitorRecordEntity;
import com.paperpilot.server.repository.RequestMonitorRecordRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MonitoringSecurityService {

    public static class RequestLog {
        public long timestamp;
        public Long userId;
        public String ipAddress;
        public String method;
        public String url;
        public int status = 0;
        public long latencyMs = 0L;
        public boolean blocked = false;

        public RequestLog(long timestamp, Long userId, String ipAddress, String method, String url) {
            this.timestamp = timestamp;
            this.userId = userId;
            this.ipAddress = ipAddress;
            this.method = method;
            this.url = url;
        }
    }

    public static class RequestDecision {
        public final boolean allowed;
        public final RequestLog log;

        public RequestDecision(boolean allowed, RequestLog log) {
            this.allowed = allowed;
            this.log = log;
        }
    }

    public static class SecurityEvent {
        public String id;
        public long timestamp;
        public String type; // "IP_ABUSE", "USER_ABUSE", "MANUAL_BAN", "MANUAL_UNBAN"
        public String target;
        public String message;

        public SecurityEvent(String type, String target, String message) {
            this.id = UUID.randomUUID().toString();
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.target = target;
            this.message = message;
        }
    }

    private final ConcurrentLinkedQueue<RequestLog> requestHistory = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SecurityEvent> securityLogs = new ConcurrentLinkedQueue<>();

    private final ConcurrentHashMap<String, Boolean> bannedIps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> bannedUsers = new ConcurrentHashMap<>();

    // Keep track of active users (last 2 minutes)
    private final ConcurrentHashMap<Long, Long> activeUserHeartbeats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> ipToUsername = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userIdToUsername = new ConcurrentHashMap<>();
    private final RequestMonitorRecordRepository requestMonitorRecordRepository;

    public MonitoringSecurityService(RequestMonitorRecordRepository requestMonitorRecordRepository) {
        this.requestMonitorRecordRepository = requestMonitorRecordRepository;
    }

    public RequestDecision registerRequest(Long userId, String ipAddress, String method, String url) {
        long now = System.currentTimeMillis();
        RequestLog log = new RequestLog(now, userId, ipAddress, method, url);
        boolean trustedLocalIp = isTrustedLocalIp(ipAddress);

        // 1. Check bans
        if (!trustedLocalIp && ipAddress != null && bannedIps.containsKey(ipAddress)) {
            logSecurityAlert("ATTACK_ATTEMPT", ipAddress, "已被封禁的IP尝试访问接口: " + url);
            log.blocked = true;
            log.status = 403;
            requestHistory.add(log);
            cleanupOldRequests();
            return new RequestDecision(false, log);
        }
        if (userId != null && isUserBanned(userId)) {
            logSecurityAlert("ATTACK_ATTEMPT", "user-" + userId, "已被封禁的账号尝试访问接口: " + url);
            log.blocked = true;
            log.status = 403;
            requestHistory.add(log);
            cleanupOldRequests();
            return new RequestDecision(false, log);
        }

        // 2. Log request
        requestHistory.add(log);

        // 3. Heartbeat tracking
        if (userId != null) {
            activeUserHeartbeats.put(userId, now);
        }

        // 4. Rate checks
        cleanupOldRequests();
        if (!trustedLocalIp) {
            checkRateLimits(userId, ipAddress);
        }

        return new RequestDecision(true, log);
    }

    public void completeRequest(RequestLog log, int status, long latencyMs) {
        if (log == null) return;
        log.status = status;
        log.latencyMs = Math.max(0L, latencyMs);
        persistRequestLog(log);
    }

    public void recordIpUsername(String ip, String username) {
        if (ip != null && username != null) {
            ipToUsername.put(ip, username);
        }
    }

    public void recordUserName(Long userId, String username) {
        if (userId != null && username != null) {
            userIdToUsername.put(userId, username);
        }
    }

    private void checkRateLimits(Long userId, String ipAddress) {
        long oneMinuteAgo = System.currentTimeMillis() - 60 * 1000;

        if (ipAddress != null) {
            long ipCount = requestHistory.stream()
                .filter(log -> ipAddress.equals(log.ipAddress) && log.timestamp >= oneMinuteAgo)
                .count();
            if (ipCount > 100) { // Limit: 100 requests per minute
                logSecurityAlert("IP_ABUSE", ipAddress, "IP请求速率过高: " + ipCount + "次/分钟，系统已拦截其高频动作。");
                if (ipCount > 150) {
                    banIp(ipAddress, "系统自动风控：每分钟请求超150次");
                }
            }
        }

        if (userId != null) {
            long userCount = requestHistory.stream()
                .filter(log -> userId.equals(log.userId) && log.timestamp >= oneMinuteAgo)
                .count();
            if (userCount > 50) { // Limit: 50 requests per minute
                logSecurityAlert("USER_ABUSE", "user-" + userId, "用户账号频次异常: " + userCount + "次/分钟，请核对是否在使用刷单脚本。");
            }
        }
    }

    public int getRealtimeOnlineCount() {
        long twoMinutesAgo = System.currentTimeMillis() - 2 * 60 * 1000;
        activeUserHeartbeats.entrySet().removeIf(entry -> entry.getValue() < twoMinutesAgo);
        return activeUserHeartbeats.size();
    }

    public void banIp(String ip, String reason) {
        if (isTrustedLocalIp(ip)) {
            logSecurityAlert("LOCAL_IP_SKIP", ip, "本机开发地址不执行封禁：" + reason);
            return;
        }
        bannedIps.put(ip, true);
        logSecurityAlert("MANUAL_BAN", ip, "管理员手动封禁IP：" + reason);
    }

    public void unbanIp(String ip) {
        bannedIps.remove(ip);
        logSecurityAlert("MANUAL_UNBAN", ip, "管理员解封IP");
    }

    public boolean isUserBanned(Long userId) {
        if (userId == null) return false;
        Long bannedUntil = bannedUsers.get(userId);
        if (bannedUntil != null) {
            if (bannedUntil == -1L || bannedUntil > System.currentTimeMillis()) {
                return true;
            } else {
                bannedUsers.remove(userId);
            }
        }
        return false;
    }

    public void banUser(Long userId, String reason) {
        banUser(userId, reason, -1);
    }

    public void banUser(Long userId, String reason, int days) {
        long expireTime = days < 0 ? -1L : System.currentTimeMillis() + days * 24L * 60 * 60 * 1000;
        bannedUsers.put(userId, expireTime);
        String durationStr = days < 0 ? "永久" : days + "天";
        logSecurityAlert("MANUAL_BAN", "user-" + userId, "管理员手动封禁账号（时效：" + durationStr + "）：" + reason);
    }

    public void unbanUser(Long userId) {
        bannedUsers.remove(userId);
        logSecurityAlert("MANUAL_UNBAN", "user-" + userId, "管理员解封账号");
    }

    public List<SecurityEvent> getSecurityLogs() {
        List<SecurityEvent> list = new ArrayList<>(securityLogs);
        Collections.reverse(list);
        return list;
    }

    public List<Map<String, Object>> getTopRequestIps() {
        long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
        Map<String, List<RequestLog>> grouped = new HashMap<>();
        for (RequestLog log : requestHistory) {
            if (log.timestamp >= tenMinutesAgo && log.ipAddress != null) {
                grouped.computeIfAbsent(log.ipAddress, ignored -> new ArrayList<>()).add(log);
            }
        }
        List<Map<String, Object>> top = new ArrayList<>();
        grouped.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(10)
            .forEach(e -> {
                List<RequestLog> logs = e.getValue();
                Map<String, Object> map = new HashMap<>();
                map.put("ip", e.getKey());
                map.put("count", logs.size());
                map.put("banned", bannedIps.containsKey(e.getKey()));
                map.put("username", ipToUsername.getOrDefault(e.getKey(), "游客访问"));
                map.put("rpm", round(logs.size() / 10.0));
                long blocked = logs.stream().filter(log -> log.blocked).count();
                long errors = logs.stream().filter(log -> log.status >= 400).count();
                map.put("blocked", blocked);
                map.put("errors", errors);
                map.put("avgLatencyMs", avgLatency(logs));
                map.put("lastUrl", logs.stream().max(Comparator.comparingLong(log -> log.timestamp)).map(log -> log.url).orElse(""));
                map.put("riskLevel", riskLevel(logs.size(), errors, blocked));
                top.add(map);
            });
        return top;
    }

    public List<Map<String, Object>> getTopRequestUsers() {
        long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
        Map<Long, List<RequestLog>> grouped = new HashMap<>();
        for (RequestLog log : requestHistory) {
            if (log.timestamp >= tenMinutesAgo && log.userId != null) {
                grouped.computeIfAbsent(log.userId, ignored -> new ArrayList<>()).add(log);
            }
        }
        List<Map<String, Object>> top = new ArrayList<>();
        grouped.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(10)
            .forEach(e -> {
                List<RequestLog> logs = e.getValue();
                Map<String, Object> map = new HashMap<>();
                map.put("userId", e.getKey());
                map.put("username", userIdToUsername.getOrDefault(e.getKey(), "用户-" + e.getKey()));
                map.put("count", logs.size());
                map.put("banned", isUserBanned(e.getKey()));
                map.put("rpm", round(logs.size() / 10.0));
                long errors = logs.stream().filter(log -> log.status >= 400).count();
                map.put("errors", errors);
                map.put("avgLatencyMs", avgLatency(logs));
                map.put("lastUrl", logs.stream().max(Comparator.comparingLong(log -> log.timestamp)).map(log -> log.url).orElse(""));
                map.put("riskLevel", riskLevel(logs.size(), errors, 0L));
                top.add(map);
            });
        return top;
    }

    public List<Map<String, Object>> getRealtimeTrafficSeries() {
        // Returns last 60 minutes data points
        List<Map<String, Object>> points = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 59; i >= 0; i--) {
            long minStart = now - (i + 1) * 60 * 1000;
            long minEnd = now - i * 60 * 1000;

            List<RequestLog> logs = requestHistory.stream()
                .filter(log -> log.timestamp >= minStart && log.timestamp < minEnd)
                .toList();

            Map<String, Object> pt = new HashMap<>();
            java.time.Instant instant = java.time.Instant.ofEpochMilli(minEnd);
            java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            pt.put("time", String.format("%02d:%02d", ldt.getHour(), ldt.getMinute()));
            pt.put("requests", logs.size());
            pt.put("errors", logs.stream().filter(log -> log.status >= 400).count());
            pt.put("blocked", logs.stream().filter(log -> log.blocked).count());
            pt.put("uniqueIps", logs.stream().map(log -> log.ipAddress).filter(Objects::nonNull).distinct().count());
            pt.put("avgLatencyMs", avgLatency(logs));
            points.add(pt);
        }
        return points;
    }

    public List<Map<String, Object>> getHourlyTrafficSeries(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        List<RequestMonitorRecordEntity> records = requestMonitorRecordRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
        List<Map<String, Object>> points = new ArrayList<>();

        for (int h = 0; h < 24; h++) {
            final int hour = h;
            List<RequestMonitorRecordEntity> logs = records.stream()
                .filter(record -> record.getCreatedAt() != null && record.getCreatedAt().getHour() == hour)
                .toList();

            long errors = logs.stream().filter(record -> record.getStatusCode() != null && record.getStatusCode() >= 400).count();
            long blocked = logs.stream().filter(record -> Boolean.TRUE.equals(record.getBlocked())).count();
            long uniqueIps = logs.stream().map(RequestMonitorRecordEntity::getIpAddress).filter(Objects::nonNull).filter(ip -> !ip.isBlank()).distinct().count();
            long avgLatency = Math.round(logs.stream()
                .mapToLong(record -> record.getLatencyMs() == null ? 0L : record.getLatencyMs())
                .filter(v -> v > 0)
                .average()
                .orElse(0));

            Map<String, Object> pt = new LinkedHashMap<>();
            pt.put("time", String.format("%02d:00", h));
            pt.put("requests", logs.size());
            pt.put("errors", errors);
            pt.put("blocked", blocked);
            pt.put("uniqueIps", uniqueIps);
            pt.put("avgLatencyMs", avgLatency);
            points.add(pt);
        }
        return points;
    }

    public List<Map<String, Object>> getEndpointHotspots() {
        long tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000;
        Map<String, List<RequestLog>> grouped = new HashMap<>();
        for (RequestLog log : requestHistory) {
            if (log.timestamp >= tenMinutesAgo && log.url != null) {
                grouped.computeIfAbsent(log.method + " " + log.url, ignored -> new ArrayList<>()).add(log);
            }
        }
        return grouped.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(8)
            .map(e -> {
                List<RequestLog> logs = e.getValue();
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("endpoint", e.getKey());
                map.put("count", logs.size());
                map.put("errors", logs.stream().filter(log -> log.status >= 400).count());
                map.put("avgLatencyMs", avgLatency(logs));
                map.put("uniqueIps", logs.stream().map(log -> log.ipAddress).filter(Objects::nonNull).distinct().count());
                return map;
            })
            .toList();
    }

    public Map<String, Object> getWindowSummary() {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60 * 1000;
        long tenMinutesAgo = now - 10 * 60 * 1000;
        List<RequestLog> oneMinute = requestHistory.stream().filter(log -> log.timestamp >= oneMinuteAgo).toList();
        List<RequestLog> tenMinutes = requestHistory.stream().filter(log -> log.timestamp >= tenMinutesAgo).toList();
        long blocked = tenMinutes.stream().filter(log -> log.blocked).count();
        long errors = tenMinutes.stream().filter(log -> log.status >= 400).count();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("requestsLastMinute", oneMinute.size());
        map.put("requestsLastTenMinutes", tenMinutes.size());
        map.put("uniqueIpsLastTenMinutes", tenMinutes.stream().map(log -> log.ipAddress).filter(Objects::nonNull).distinct().count());
        map.put("avgLatencyMsLastTenMinutes", avgLatency(tenMinutes));
        map.put("errorRateLastTenMinutes", tenMinutes.isEmpty() ? 0 : round(errors * 100.0 / tenMinutes.size()));
        map.put("blockedLastTenMinutes", blocked);
        map.put("activeBannedIps", bannedIps.size());
        map.put("activeBannedUsers", bannedUsers.size());
        return map;
    }

    private void logSecurityAlert(String type, String target, String message) {
        if (securityLogs.size() > 150) {
            securityLogs.poll();
        }
        securityLogs.add(new SecurityEvent(type, target, message));
    }

    private void cleanupOldRequests() {
        long threshold = System.currentTimeMillis() - 2 * 60 * 60 * 1000;
        requestHistory.removeIf(log -> log.timestamp < threshold);
    }

    private void persistRequestLog(RequestLog log) {
        try {
            RequestMonitorRecordEntity record = new RequestMonitorRecordEntity();
            record.setUserId(log.userId);
            record.setUsername(log.userId == null ? ipToUsername.getOrDefault(log.ipAddress, "") : userIdToUsername.getOrDefault(log.userId, ""));
            record.setIpAddress(log.ipAddress == null ? "" : log.ipAddress);
            record.setMethod(log.method == null ? "" : log.method);
            record.setUrl(log.url == null ? "" : log.url);
            record.setStatusCode(log.status);
            record.setLatencyMs(log.latencyMs);
            record.setBlocked(log.blocked);
            record.setCreatedAt(LocalDateTime.now());
            requestMonitorRecordRepository.save(record);
        } catch (Exception ignored) {
            // Monitoring must never interrupt the business request path.
        }
    }

    private long avgLatency(List<RequestLog> logs) {
        return Math.round(logs.stream()
            .filter(log -> log.latencyMs > 0)
            .mapToLong(log -> log.latencyMs)
            .average()
            .orElse(0));
    }

    private String riskLevel(int count, long errors, long blocked) {
        if (blocked > 0 || count >= 150 || errors >= 30) return "critical";
        if (count >= 80 || errors >= 10) return "high";
        if (count >= 30 || errors >= 3) return "watch";
        return "normal";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isTrustedLocalIp(String ipAddress) {
        if (ipAddress == null) return false;
        String ip = ipAddress.trim();
        return "127.0.0.1".equals(ip)
            || "localhost".equalsIgnoreCase(ip)
            || "::1".equals(ip)
            || "0:0:0:0:0:0:0:1".equals(ip);
    }
}
