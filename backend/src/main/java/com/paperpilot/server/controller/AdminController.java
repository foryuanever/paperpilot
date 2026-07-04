package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.AiUsageRecordEntity;
import com.paperpilot.server.entity.RechargeRecordEntity;
import com.paperpilot.server.entity.TeamEntity;
import com.paperpilot.server.entity.SystemLogEntity;
import com.paperpilot.server.entity.SiteMessageEntity;
import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import com.paperpilot.server.repository.TeamRepository;
import com.paperpilot.server.repository.SystemLogRepository;
import com.paperpilot.server.repository.SiteMessageRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.service.AuthService;
import com.paperpilot.server.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AppUserRepository appUserRepository;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final RechargeRecordRepository rechargeRecordRepository;
    private final TeamRepository teamRepository;
    private final SystemLogRepository systemLogRepository;
    private final PaperRepository paperRepository;
    private final TranslationRecordRepository translationRecordRepository;
    private final AuthService authService;
    private final SiteMessageRepository siteMessageRepository;
    private final BillingService billingService;

    public AdminController(
        AppUserRepository appUserRepository,
        AiUsageRecordRepository aiUsageRecordRepository,
        RechargeRecordRepository rechargeRecordRepository,
        TeamRepository teamRepository,
        SystemLogRepository systemLogRepository,
        PaperRepository paperRepository,
        TranslationRecordRepository translationRecordRepository,
        AuthService authService,
        SiteMessageRepository siteMessageRepository,
        BillingService billingService
    ) {
        this.appUserRepository = appUserRepository;
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.rechargeRecordRepository = rechargeRecordRepository;
        this.teamRepository = teamRepository;
        this.systemLogRepository = systemLogRepository;
        this.paperRepository = paperRepository;
        this.translationRecordRepository = translationRecordRepository;
        this.authService = authService;
        this.siteMessageRepository = siteMessageRepository;
        this.billingService = billingService;
    }

    // --- Dynamic Global Statistics ---

    @GetMapping("/stats")
    public Map<String, Object> getGlobalStats() {
        long totalUsers = appUserRepository.count();
        List<AppUserEntity> allUsers = appUserRepository.findAll();
        
        long studentCount = allUsers.stream().filter(u -> "学生".equals(u.getRole())).count();
        long tutorCount = allUsers.stream().filter(u -> "导师".equals(u.getRole())).count();
        long adminCount = allUsers.stream().filter(u -> "管理员".equals(u.getRole())).count();

        long totalPapers = paperRepository.count();

        long totalTokensUsed = allUsers.stream()
            .mapToLong(u -> u.getTokenUsed() != null ? u.getTokenUsed() : 0L)
            .sum();
        long totalTokensLimit = allUsers.stream()
            .mapToLong(u -> u.getTokenLimit() != null ? u.getTokenLimit() : 5000000L)
            .sum();

        double usagePercentage = totalTokensLimit > 0 ? ((double) totalTokensUsed / totalTokensLimit) * 100 : 0.0;
        List<RechargeRecordEntity> rechargeRecords = rechargeRecordRepository.findAll();
        double totalRechargeAmount = rechargeRecords.stream()
            .mapToDouble(record -> record.getAmount() != null ? record.getAmount() : 0.0)
            .sum();
        double totalBalanceAmount = allUsers.stream()
            .mapToDouble(user -> user.getBalanceAmount() != null ? user.getBalanceAmount() : 0.0)
            .sum();
        long totalRechargeTokens = rechargeRecords.stream()
            .mapToLong(record -> record.getTokens() != null ? record.getTokens() : 0L)
            .sum();

        List<TranslationRecordEntity> allRecords = translationRecordRepository.findAll();
        long successfulCount = allRecords.stream().filter(TranslationRecordEntity::isSuccess).count();
        double successRate = allRecords.isEmpty() ? 100.0 : ((double) successfulCount / allRecords.size()) * 100;
        
        double averageLatencyMs = allRecords.isEmpty() ? 0.0 : allRecords.stream()
            .mapToLong(TranslationRecordEntity::getLatencyMs)
            .average()
            .orElse(0.0);
            
        Map<String, Long> engineStats = new java.util.HashMap<>();
        for (String p : List.of("google", "youdao", "deepl", "baidu", "microsoft", "ai")) {
            long sum = allRecords.stream()
                .filter(r -> p.equalsIgnoreCase(r.getProvider()))
                .mapToLong(TranslationRecordEntity::getCharCount)
                .sum();
            engineStats.put(p, sum);
        }

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("studentCount", studentCount);
        stats.put("tutorCount", tutorCount);
        stats.put("adminCount", adminCount);
        stats.put("totalPapers", totalPapers);
        stats.put("totalTokensUsed", totalTokensUsed);
        stats.put("totalTokensLimit", totalTokensLimit);
        stats.put("usagePercentage", usagePercentage);
        stats.put("totalRechargeAmount", totalRechargeAmount);
        stats.put("totalBalanceAmount", totalBalanceAmount);
        stats.put("totalRechargeTokens", totalRechargeTokens);
        stats.put("rechargeCount", rechargeRecords.size());
        stats.put("averageLatencyMs", Math.round(averageLatencyMs));
        stats.put("successRate", successRate);
        stats.put("engineStats", engineStats);

        return stats;
    }

    // --- Users CRUD ---

    @GetMapping("/users")
    public List<AppUserEntity> getAllUsers() {
        return appUserRepository.findAll();
    }

    @PostMapping("/users")
    public AppUserEntity createUser(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String role = body.get("role");
        String ip = getClientIp(request);
        return authService.adminCreateUser(username, email, password, role, ip);
    }

    @PatchMapping("/users/{id}/quota")
    public void updateUserQuota(@PathVariable("id") Long id, @RequestBody Map<String, Long> body, HttpServletRequest request) {
        Long quota = body.get("tokenLimit");
        String ip = getClientIp(request);
        authService.adminChangeQuota(id, quota, ip);
    }

    @PatchMapping("/users/{id}/role")
    public void updateUserRole(@PathVariable("id") Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String role = body.get("role");
        String ip = getClientIp(request);
        authService.adminChangeRole(id, role, ip);
    }

    @PatchMapping("/users/{id}/password")
    public void updateUserPassword(@PathVariable("id") Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String password = body.get("password");
        String ip = getClientIp(request);
        authService.adminChangePassword(id, password, ip);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable("id") Long id, HttpServletRequest request) {
        String ip = getClientIp(request);
        authService.adminDeleteUser(id, ip);
    }

    // --- Recharge & Quota ---

    @GetMapping("/recharges")
    public List<RechargeRecordEntity> getRecharges() {
        return rechargeRecordRepository.findAll();
    }

    @GetMapping("/billing")
    public Map<String, Object> getBillingSettings() {
        Map<String, Object> result = new java.util.LinkedHashMap<>(billingService.settings());
        List<AiUsageRecordEntity> records = aiUsageRecordRepository.findTop240ByOrderByCreatedAtDesc();
        result.put("recentCharges", records.stream().limit(60).map(record -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("time", record.getCreatedAt() == null ? "" : record.getCreatedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            row.put("action", record.getAction());
            row.put("paper", record.getPaperTitle());
            row.put("tokens", record.getTotalTokens());
            row.put("promptTokens", record.getPromptTokens());
            row.put("completionTokens", record.getCompletionTokens());
            double unitPrice = record.getUnitPrice() != null && record.getUnitPrice() > 0 ? record.getUnitPrice() : billingService.unitPrice();
            double multiplier = record.getBillingMultiplier() != null && record.getBillingMultiplier() > 0 ? record.getBillingMultiplier() : billingService.multiplier();
            double chargeAmount = record.getChargeAmount() != null && record.getChargeAmount() > 0
                ? record.getChargeAmount()
                : billingService.calculateCharge(record.getTotalTokens() == null ? 0L : record.getTotalTokens());
            row.put("chargeAmount", chargeAmount);
            row.put("unitPrice", unitPrice);
            row.put("billingMultiplier", multiplier);
            return row;
        }).toList());
        return result;
    }

    @PatchMapping("/billing")
    public Map<String, Object> updateBillingSettings(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        double unitPrice = Double.parseDouble(String.valueOf(body.getOrDefault("unitPrice", billingService.unitPrice())));
        double multiplier = Double.parseDouble(String.valueOf(body.getOrDefault("multiplier", billingService.multiplier())));
        try {
            Map<String, Object> result = billingService.update(unitPrice, multiplier);
            authService.logAction("更新计费规则: 单价 ¥" + unitPrice + " / 1K Token, 倍率 " + multiplier + "x", "info", getClientIp(request));
            return result;
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
    }

    @PostMapping("/recharges")
    public RechargeRecordEntity createRecharge(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String email = (String) body.get("email");
        Double amount = Double.valueOf(body.get("amount").toString());
        Long tokens = body.get("tokens") == null ? 0L : Long.valueOf(body.get("tokens").toString());
        String ip = getClientIp(request);
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "充值金额必须大于 0");
        }

        // Find user by email and update limit
        AppUserEntity user = appUserRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱对应的用户不存在"));

        double oldBalance = user.getBalanceAmount() == null ? 0.0D : user.getBalanceAmount();
        user.setBalanceAmount(oldBalance + amount);
        appUserRepository.save(user);

        // Create recharge record
        RechargeRecordEntity record = new RechargeRecordEntity();
        record.setEmail(email);
        record.setAmount(amount);
        record.setTokens(tokens);
        RechargeRecordEntity saved = rechargeRecordRepository.save(record);

        // Log recharge
        authService.logAction("充值入账成功: 用户 " + user.getUsername() + " (" + email + ") 充值 ¥" + amount + " (余额: ¥" + user.getBalanceAmount() + ")", "info", ip);

        return saved;
    }

    // --- Teams ---

    @GetMapping("/teams")
    public List<TeamEntity> getTeams() {
        return teamRepository.findAll();
    }

    @GetMapping("/teams/{id}/members")
    public List<Map<String, Object>> getTeamMembers(@PathVariable("id") Long id) {
        if (!teamRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "团队不存在");
        }
        return appUserRepository.findByTeamIdOrderByCreatedAtAsc(id).stream()
            .map(user -> {
                Map<String, Object> member = new java.util.LinkedHashMap<>();
                long activeTime = user.getActiveTime() != null ? user.getActiveTime() : 0L;
                int level = (int) (activeTime / 300L) + 1;
                member.put("id", user.getId());
                member.put("name", user.getUsername());
                member.put("email", user.getEmail());
                member.put("role", user.getRole() != null ? user.getRole() : "学生");
                member.put("tokenUsed", user.getTokenUsed() != null ? user.getTokenUsed() : 0L);
                member.put("tokenLimit", user.getTokenLimit() != null ? user.getTokenLimit() : 5000000L);
                member.put("activeTime", activeTime);
                member.put("level", level);
                member.put("levelTitle", getLevelTitle(level));
                member.put("createdAt", user.getCreatedAt());
                return member;
            })
            .toList();
    }

    @PostMapping("/teams")
    public TeamEntity createTeam(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String name = body.get("name");
        String identifier = body.get("identifier");
        String ip = getClientIp(request);

        if (teamRepository.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "团队名称已存在");
        }
        if (identifier != null && !identifier.isBlank() && teamRepository.findByIdentifier(identifier).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "团队标识已存在");
        }

        TeamEntity team = new TeamEntity();
        team.setName(name);
        if (identifier != null && !identifier.isBlank()) {
            team.setIdentifier(identifier.trim().toUpperCase());
        }
        team.setMemberCount(0);
        TeamEntity saved = teamRepository.save(team);

        authService.logAction("创建科研团队: " + name, "info", ip);
        return saved;
    }

    @DeleteMapping("/teams/{id}")
    public void deleteTeam(@PathVariable("id") Long id, HttpServletRequest request) {
        String ip = getClientIp(request);
        TeamEntity team = teamRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "团队不存在"));

        teamRepository.delete(team);
        authService.logAction("解散科研团队: " + team.getName(), "warn", ip);
    }

    // --- Logs ---

    @GetMapping("/logs")
    public List<SystemLogEntity> getLogs() {
        return systemLogRepository.findAllByOrderByTimestampDesc();
    }

    @DeleteMapping("/logs")
    public void clearLogs(HttpServletRequest request) {
        String ip = getClientIp(request);
        systemLogRepository.deleteAll();
        authService.logAction("管理员清空了所有系统运行日志", "warn", ip);
    }

    // --- Site-wide Messages ---

    @GetMapping("/site-messages")
    public List<SiteMessageEntity> getSiteMessages() {
        return siteMessageRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/site-messages")
    public SiteMessageEntity publishSiteMessage(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String title = body.getOrDefault("title", "").trim();
        String content = body.getOrDefault("content", "").trim();
        if (title.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息标题和内容不能为空");
        }

        SiteMessageEntity message = new SiteMessageEntity();
        message.setTitle(title);
        message.setContent(content);
        message.setActiveFlag(true);
        SiteMessageEntity saved = siteMessageRepository.save(message);
        authService.logAction("发布全站消息: " + title, "info", getClientIp(request));
        return saved;
    }

    @PatchMapping("/site-messages/{id}/status")
    public SiteMessageEntity updateSiteMessageStatus(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Boolean> body,
        HttpServletRequest request
    ) {
        SiteMessageEntity message = siteMessageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站内消息不存在"));
        message.setActiveFlag(Boolean.TRUE.equals(body.get("active")));
        SiteMessageEntity saved = siteMessageRepository.save(message);
        authService.logAction((saved.isActiveFlag() ? "重新发布" : "撤下") + "全站消息: " + saved.getTitle(), "info", getClientIp(request));
        return saved;
    }

    @DeleteMapping("/site-messages/{id}")
    public void deleteSiteMessage(@PathVariable("id") Long id, HttpServletRequest request) {
        SiteMessageEntity message = siteMessageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "站内消息不存在"));
        siteMessageRepository.delete(message);
        authService.logAction("删除全站消息: " + message.getTitle(), "warn", getClientIp(request));
    }

    // --- Helper ---

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

    private String getLevelTitle(int level) {
        if (level >= 15) return "科研主宰";
        if (level >= 10) return "科研宗师";
        if (level >= 6) return "学术专家";
        if (level >= 3) return "科研骨干";
        return "科研萌新";
    }
}
