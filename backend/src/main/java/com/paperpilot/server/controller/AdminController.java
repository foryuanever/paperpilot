package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.AiUsageRecordEntity;
import com.paperpilot.server.entity.RechargeRecordEntity;
import com.paperpilot.server.entity.TeamEntity;
import com.paperpilot.server.entity.SystemLogEntity;
import com.paperpilot.server.entity.SiteMessageEntity;
import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.entity.PaymentOrderEntity;
import com.paperpilot.server.entity.PaymentTicketEntity;
import com.paperpilot.server.entity.ForumPostEntity;
import com.paperpilot.server.entity.ForumPostReportEntity;
import com.paperpilot.server.entity.TutorialArticleEntity;
import com.paperpilot.server.entity.CampusVerificationEntity;
import com.paperpilot.server.entity.PromotionEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import com.paperpilot.server.repository.TeamRepository;
import com.paperpilot.server.repository.SystemLogRepository;
import com.paperpilot.server.repository.SiteMessageRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.repository.PaymentOrderRepository;
import com.paperpilot.server.repository.PaymentTicketRepository;
import com.paperpilot.server.repository.ForumPostRepository;
import com.paperpilot.server.repository.ForumPostReportRepository;
import com.paperpilot.server.repository.CheckinRepository;
import com.paperpilot.server.repository.TutorialArticleRepository;
import com.paperpilot.server.repository.CampusVerificationRepository;
import com.paperpilot.server.repository.PromotionRepository;
import com.paperpilot.server.service.AuthService;
import com.paperpilot.server.service.BillingService;
import com.paperpilot.server.service.MembershipService;
import com.paperpilot.server.service.NotificationService;
import com.paperpilot.server.service.MonitoringSecurityService;
import com.paperpilot.server.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
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
    private final MembershipService membershipService;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTicketRepository paymentTicketRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumPostReportRepository forumPostReportRepository;
    private final CheckinRepository checkinRepository;
    private final NotificationService notificationService;
    private final TutorialArticleRepository tutorialArticleRepository;
    private final CampusVerificationRepository campusVerificationRepository;
    private final PromotionRepository promotionRepository;
    private final MonitoringSecurityService monitoringSecurityService;
    private final CurrentUserService currentUserService;

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
        BillingService billingService,
        MembershipService membershipService,
        PaymentOrderRepository paymentOrderRepository,
        PaymentTicketRepository paymentTicketRepository,
        ForumPostRepository forumPostRepository,
        ForumPostReportRepository forumPostReportRepository,
        CheckinRepository checkinRepository,
        NotificationService notificationService,
        TutorialArticleRepository tutorialArticleRepository,
        CampusVerificationRepository campusVerificationRepository,
        PromotionRepository promotionRepository,
        MonitoringSecurityService monitoringSecurityService,
        CurrentUserService currentUserService
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
        this.membershipService = membershipService;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTicketRepository = paymentTicketRepository;
        this.forumPostRepository = forumPostRepository;
        this.forumPostReportRepository = forumPostReportRepository;
        this.checkinRepository = checkinRepository;
        this.notificationService = notificationService;
        this.tutorialArticleRepository = tutorialArticleRepository;
        this.campusVerificationRepository = campusVerificationRepository;
        this.promotionRepository = promotionRepository;
        this.monitoringSecurityService = monitoringSecurityService;
        this.currentUserService = currentUserService;
    }

    @ModelAttribute
    public void requireAdminAccess() {
        currentUserService.requireAdmin();
    }

    // --- Dynamic Global Statistics ---

    @GetMapping("/stats")
    public Map<String, Object> getGlobalStats() {
        long totalUsers = appUserRepository.count();
        List<AppUserEntity> allUsers = appUserRepository.findAll();

        long studentCount = allUsers.stream().filter(u -> "学生".equals(u.getRole())).count();
        long tutorCount = allUsers.stream().filter(u -> "导师".equals(u.getRole())).count();
        long adminCount = allUsers.stream().filter(u -> "管理员".equals(u.getRole())).count();
        LocalDateTime now = LocalDateTime.now();
        long activeMemberCount = allUsers.stream()
            .filter(user -> {
                String plan = user.getMembershipPlan() == null ? "free" : user.getMembershipPlan();
                return !"free".equalsIgnoreCase(plan)
                    && (user.getMembershipExpiresAt() == null || user.getMembershipExpiresAt().isAfter(now));
            })
            .count();

        long totalPapers = paperRepository.count();

        long totalTokensUsed = allUsers.stream()
            .mapToLong(u -> u.getTokenUsed() != null ? u.getTokenUsed() : 0L)
            .sum();
        long totalTokensLimit = allUsers.stream()
            .mapToLong(u -> u.getTokenLimit() != null ? u.getTokenLimit() : 5000000L)
            .sum();

        double usagePercentage = totalTokensLimit > 0 ? ((double) totalTokensUsed / totalTokensLimit) * 100 : 0.0;
        List<RechargeRecordEntity> rechargeRecords = rechargeRecordRepository.findAll();
        List<PaymentOrderEntity> paidOrders = paymentOrderRepository.findAll().stream()
            .filter(o -> "paid".equals(o.getStatus()))
            .toList();
        double totalRechargeAmount = paidOrders.stream()
            .mapToDouble(record -> record.getActualPayAmount() != null ? record.getActualPayAmount() : record.getAmount())
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
        stats.put("activeMemberCount", activeMemberCount);
        stats.put("totalPapers", totalPapers);
        stats.put("totalTokensUsed", totalTokensUsed);
        stats.put("totalTokensLimit", totalTokensLimit);
        stats.put("usagePercentage", usagePercentage);
        stats.put("totalRechargeAmount", totalRechargeAmount);
        stats.put("totalBalanceAmount", totalBalanceAmount);
        stats.put("totalRechargeTokens", totalRechargeTokens);
        stats.put("rechargeCount", paidOrders.size());
        stats.put("averageLatencyMs", Math.round(averageLatencyMs));
        stats.put("successRate", successRate);
        stats.put("engineStats", engineStats);

        return stats;
    }

    // --- Users CRUD ---

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        return appUserRepository.findAll().stream()
            .map(user -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", user.getId());
                map.put("username", user.getUsername());
                map.put("email", user.getEmail());
                map.put("role", user.getRole());
                map.put("plainPassword", user.getPlainPassword());
                map.put("tokenLimit", user.getTokenLimit());
                map.put("tokenUsed", user.getTokenUsed());
                map.put("balanceAmount", user.getBalanceAmount());
                map.put("membershipPlan", user.getMembershipPlan());
                map.put("membershipCycle", user.getMembershipCycle());
                map.put("membershipExpiresAt", user.getMembershipExpiresAt());
                map.put("reviewQuota", user.getReviewQuota());
                map.put("reviewUsed", user.getReviewUsed());
                map.put("pptQuota", user.getPptQuota());
                map.put("pptUsed", user.getPptUsed());
                map.put("chatQuota", user.getChatQuota());
                map.put("chatUsed", user.getChatUsed());
                map.put("fruitScore", user.getFruitScore());
                map.put("lastIp", user.getLastIp());
                map.put("createdAt", user.getCreatedAt());
                map.put("banned", monitoringSecurityService.isUserBanned(user.getId()));
                return map;
            })
            .toList();
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
    public void updateUserQuota(@PathVariable("id") Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long quota = body.get("tokenLimit") == null ? null : Long.valueOf(String.valueOf(body.get("tokenLimit")));
        Double balanceAmount = body.get("balanceAmount") == null ? null : Double.valueOf(String.valueOf(body.get("balanceAmount")));
        String ip = getClientIp(request);
        authService.adminChangeQuota(id, quota, balanceAmount, ip);
    }

    @PatchMapping("/users/{id}/membership")
    public AppUserEntity updateUserMembership(@PathVariable("id") Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        AppUserEntity user = appUserRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        String planId = body.getOrDefault("planId", "free");
        String cycle = body.getOrDefault("cycle", "monthly");
        String oldPlan = user.getMembershipPlan() == null ? "free" : user.getMembershipPlan();
        if ("free".equals(planId)) {
            user.setMembershipPlan("free");
            user.setMembershipCycle("monthly");
            user.setMembershipExpiresAt(null);
            user.setReviewQuota(0);
            user.setReviewUsed(0);
            user.setPptQuota(0);
            user.setPptUsed(0);
            user.setChatQuota(0);
            user.setChatUsed(0);
            AppUserEntity saved = appUserRepository.save(user);
            authService.logAction("管理员取消用户会员: " + saved.getUsername() + "，套餐 " + oldPlan + " → free", "warn", getClientIp(request));
            return saved;
        }
        membershipService.activate(user, planId, cycle);
        authService.logAction("管理员分配用户会员: " + user.getUsername() + "，套餐 " + oldPlan + " → " + planId, "info", getClientIp(request));
        return user;
    }

    @GetMapping("/membership-plans")
    public List<Map<String, Object>> getMembershipPlans() {
        return membershipService.catalog();
    }

    @PostMapping("/membership-plans")
    public Map<String, Object> createMembershipPlan(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> saved = membershipService.createPlan(body);
        authService.logAction("管理员上架新套餐: " + saved.getOrDefault("name", saved.get("id")), "info", getClientIp(request));
        return saved;
    }

    @PatchMapping("/membership-plans/{id}")
    public Map<String, Object> updateMembershipPlan(@PathVariable("id") String id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> saved = membershipService.savePlan(id, body);
        String action = body.size() == 1 && body.containsKey("activeFlag")
            ? (Boolean.FALSE.equals(body.get("activeFlag")) ? "隐藏套餐" : "上架套餐")
            : "更新套餐配置";
        authService.logAction("管理员" + action + ": " + saved.getOrDefault("name", id), "warn", getClientIp(request));
        return saved;
    }

    @DeleteMapping("/membership-plans/{id}")
    public void deleteMembershipPlan(@PathVariable("id") String id, HttpServletRequest request) {
        membershipService.deletePlan(id);
        authService.logAction("管理员彻底删除套餐: " + id, "warn", getClientIp(request));
    }

    @PostMapping("/checkins/reset")
    @jakarta.transaction.Transactional
    public Map<String, Object> resetAllCheckins(HttpServletRequest request) {
        long checkinCount = checkinRepository.count();
        checkinRepository.deleteAllInBatch();
        int userCount = appUserRepository.resetAllFruitScores();
        authService.logAction("管理员重置实验室签到: 删除签到 " + checkinCount + " 条，重置用户果值 " + userCount + " 人", "warn", getClientIp(request));
        return Map.of("deletedCheckins", checkinCount, "resetFruitUsers", userCount);
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
            row.put("action", normalizeBillingAction(record.getAction()));
            row.put("paper", record.getPaperTitle());
            row.put("tokens", record.getTotalTokens());
            row.put("promptTokens", record.getPromptTokens());
            row.put("completionTokens", record.getCompletionTokens());
            double multiplier = record.getBillingMultiplier() != null && record.getBillingMultiplier() > 1.0D ? record.getBillingMultiplier() : billingService.multiplier();
            double savedUnitPrice = record.getUnitPrice() != null && record.getUnitPrice() > 0 ? record.getUnitPrice() : billingService.unitPrice();
            double unitPrice = billingService.normalizeInputUnitPrice(savedUnitPrice, multiplier);
            double savedChargeAmount = record.getChargeAmount() != null && record.getChargeAmount() > 0
                ? record.getChargeAmount()
                : 0.0D;
            double billableTokens = (record.getPromptTokens() == null ? 0L : record.getPromptTokens())
                + (record.getCompletionTokens() == null ? 0L : record.getCompletionTokens()) * Math.max(1.0D, multiplier);
            double effectiveChargeAmount = unitPrice > 0 && billableTokens > 0
                ? Math.round((unitPrice * billableTokens / 1000.0D) * 1_000_000D) / 1_000_000D
                : 0.0D;
            double chargeAmount = effectiveChargeAmount > 0 ? effectiveChargeAmount : savedChargeAmount;
            row.put("chargeAmount", chargeAmount);
            row.put("unitPrice", unitPrice);
            row.put("outputUnitPrice", billingService.outputUnitPrice(unitPrice, multiplier));
            row.put("billingMultiplier", multiplier);
            return row;
        }).toList());
        return result;
    }

    @PatchMapping("/billing")
    public Map<String, Object> updateBillingSettings(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        double unitPrice = Double.parseDouble(String.valueOf(body.getOrDefault("unitPrice", billingService.unitPrice())));
        double multiplier = Double.parseDouble(String.valueOf(body.getOrDefault("multiplier", billingService.multiplier())));
        double pptAgentMinCharge = Double.parseDouble(String.valueOf(body.getOrDefault("pptAgentMinCharge", 0.0D)));
        try {
            Map<String, Object> result = billingService.update(unitPrice, multiplier, pptAgentMinCharge);
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

    @GetMapping("/payments")
    public Map<String, Object> getPaymentWorkdesk() {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orders", paymentOrderRepository.findTop80ByOrderByCreatedAtDesc().stream().map(this::paymentOrderToMap).toList());
        result.put("tickets", paymentTicketRepository.findTop80ByOrderByCreatedAtDesc().stream()
            .filter(this::isUsablePaymentTicket)
            .map(this::paymentTicketToMap)
            .toList());
        return result;
    }

    @PatchMapping("/payments/tickets/{id}")
    public Map<String, Object> updatePaymentTicket(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest request
    ) {
        PaymentTicketEntity ticket = paymentTicketRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在"));
        String status = String.valueOf(body.getOrDefault("status", "processed")).trim().toLowerCase();
        if (!List.of("open", "processed", "rejected").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工单状态仅支持 open、processed 或 rejected");
        }
        ticket.setStatus(status);
        ticket.setAdminNote(String.valueOf(body.getOrDefault("adminNote", "")).trim());
        ticket.setProcessedAt(status.equals("open") ? null : LocalDateTime.now());
        PaymentTicketEntity saved = paymentTicketRepository.save(ticket);
        authService.logAction("处理支付工单 #" + saved.getId() + ": " + paymentStatusLabel(status), "info", getClientIp(request));
        return paymentTicketToMap(saved);
    }

    @GetMapping("/forum/reports")
    public List<Map<String, Object>> getForumReports() {
        return forumPostReportRepository.findTop160ByOrderByCreatedAtDesc().stream()
            .map(this::forumReportToMap)
            .toList();
    }

    @PatchMapping("/forum/reports/{id}")
    public Map<String, Object> updateForumReport(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest request
    ) {
        ForumPostReportEntity report = forumPostReportRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "举报不存在"));
        String status = String.valueOf(body.getOrDefault("status", "processed")).trim().toLowerCase();
        if (!List.of("open", "processed", "rejected").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "举报状态仅支持 open、processed 或 rejected");
        }
        String note = String.valueOf(body.getOrDefault("adminNote", "")).trim();
        boolean banPost = Boolean.TRUE.equals(body.get("banPost")) || "true".equalsIgnoreCase(String.valueOf(body.get("banPost")));
        report.setStatus(status);
        report.setAdminNote(note);
        report.setProcessedAt(status.equals("open") ? null : LocalDateTime.now());

        ForumPostEntity post = forumPostRepository.findById(report.getPostId()).orElse(null);
        if (post != null && banPost) {
            post.setBanned(true);
            forumPostRepository.save(post);
            notificationService.createSystemNotice(post.getUserId(), null, "forum_ban", post.getId(),
                "你的帖子已被封禁", "管理员根据举报处理结果封禁了《" + post.getTitle() + "》。");
        }
        ForumPostReportEntity saved = forumPostReportRepository.save(report);
        if (saved.getReporterId() != null) {
            notificationService.createSystemNotice(saved.getReporterId(), null, "forum_report_processed", saved.getPostId(),
                "举报已处理", "你提交的举报已处理：" + forumReportStatusLabel(saved.getStatus()));
        }
        authService.logAction("处理论坛举报 #" + saved.getId() + ": " + forumReportStatusLabel(saved.getStatus()), "info", getClientIp(request));
        return forumReportToMap(saved);
    }

    @GetMapping("/campus-verifications")
    public List<Map<String, Object>> getCampusVerifications() {
        return campusVerificationRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::campusVerificationToMap)
            .toList();
    }

    @PatchMapping("/campus-verifications/{id}")
    public Map<String, Object> reviewCampusVerification(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest request
    ) {
        CampusVerificationEntity verification = campusVerificationRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "校园认证申请不存在"));
        String status = String.valueOf(body.getOrDefault("status", "approved")).trim().toLowerCase();
        if (!List.of("pending", "approved", "rejected").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "审核状态仅支持 pending、approved 或 rejected");
        }
        String note = String.valueOf(body.getOrDefault("adminNote", "")).trim();
        verification.setStatus(status);
        verification.setAdminNote(note);
        verification.setReviewedAt("pending".equals(status) ? null : LocalDateTime.now());

        if ("approved".equals(status)) {
            AppUserEntity user = appUserRepository.findById(verification.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "申请用户不存在"));
            user.setSchoolName(verification.getSchoolName());
            user.setCampusVerified(true);
            appUserRepository.save(user);
            notificationService.createSystemNotice(user.getId(), null, "campus_verified", verification.getId(),
                "校园认证已通过", "你的校园认证已通过，个人主页已显示学校：" + verification.getSchoolName());
        } else if ("rejected".equals(status)) {
            notificationService.createSystemNotice(verification.getUserId(), null, "campus_rejected", verification.getId(),
                "校园认证未通过", note.isBlank() ? "你的校园认证暂未通过，请检查资料后重新提交。" : note);
        }

        CampusVerificationEntity saved = campusVerificationRepository.save(verification);
        authService.logAction("处理校园认证 #" + saved.getId() + ": " + campusVerificationStatusLabel(saved.getStatus()), "info", getClientIp(request));
        return campusVerificationToMap(saved);
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
        message.setMessageType(normalizeSiteMessageType(body.get("messageType")));

        String imageUrl = body.get("imageUrl");
        if (imageUrl != null && !imageUrl.isBlank()) {
            message.setImageUrl(imageUrl);
        }

        message.setActiveFlag(true);
        SiteMessageEntity saved = siteMessageRepository.save(message);
        authService.logAction("发布全站消息: " + title, "info", getClientIp(request));
        return saved;
    }

    // --- Promotions ---

    @GetMapping("/promotions")
    public List<PromotionEntity> getPromotions() {
        return promotionRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/promotions/{id}/audit")
    public PromotionEntity auditPromotion(
        @PathVariable("id") Long id,
        @RequestBody Map<String, String> body,
        HttpServletRequest request
    ) {
        String action = body.getOrDefault("action", "").trim(); // "approve" or "reject"
        String note = body.getOrDefault("adminNote", "").trim();

        PromotionEntity promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "推广记录未找到"));

        if ("approve".equalsIgnoreCase(action)) {
            promotion.setStatus("APPROVED");
        } else if ("reject".equalsIgnoreCase(action)) {
            promotion.setStatus("REJECTED");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的审核操作");
        }

        if (!note.isBlank()) {
            promotion.setAdminNote(note);
        }

        PromotionEntity saved = promotionRepository.save(promotion);
        authService.logAction("审核推广(" + saved.getStatus() + "): " + saved.getId(), "info", getClientIp(request));
        return saved;
    }

    private String normalizeSiteMessageType(String value) {
        if ("timeline".equalsIgnoreCase(value)) return "timeline";
        return "notice";
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

    // --- Tutorial Articles ---

    @GetMapping("/tutorials")
    public List<TutorialArticleEntity> getTutorials() {
        return tutorialArticleRepository.findAllByOrderBySortOrderAscUpdatedAtDesc();
    }

    @PostMapping("/tutorials")
    public TutorialArticleEntity publishTutorial(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        TutorialArticleEntity article = new TutorialArticleEntity();
        applyTutorialBody(article, body);
        TutorialArticleEntity saved = tutorialArticleRepository.save(article);
        authService.logAction("发布使用教程: " + saved.getTitle(), "info", getClientIp(request));
        return saved;
    }

    @PatchMapping("/tutorials/{id}")
    public TutorialArticleEntity updateTutorial(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Object> body,
        HttpServletRequest request
    ) {
        TutorialArticleEntity article = tutorialArticleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教程不存在"));
        applyTutorialBody(article, body);
        TutorialArticleEntity saved = tutorialArticleRepository.save(article);
        authService.logAction("更新使用教程: " + saved.getTitle(), "info", getClientIp(request));
        return saved;
    }

    @PatchMapping("/tutorials/{id}/status")
    public TutorialArticleEntity updateTutorialStatus(
        @PathVariable("id") Long id,
        @RequestBody Map<String, Boolean> body,
        HttpServletRequest request
    ) {
        TutorialArticleEntity article = tutorialArticleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教程不存在"));
        article.setActiveFlag(Boolean.TRUE.equals(body.get("active")));
        TutorialArticleEntity saved = tutorialArticleRepository.save(article);
        authService.logAction((saved.isActiveFlag() ? "上架" : "下架") + "使用教程: " + saved.getTitle(), "info", getClientIp(request));
        return saved;
    }

    @DeleteMapping("/tutorials/{id}")
    public void deleteTutorial(@PathVariable("id") Long id, HttpServletRequest request) {
        TutorialArticleEntity article = tutorialArticleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教程不存在"));
        tutorialArticleRepository.delete(article);
        authService.logAction("删除使用教程: " + article.getTitle(), "warn", getClientIp(request));
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

    private void applyTutorialBody(TutorialArticleEntity article, Map<String, Object> body) {
        String title = textValue(body.get("title"));
        String content = textValue(body.get("content"));
        if (title.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "教程标题和 Markdown 内容不能为空");
        }
        article.setTitle(title);
        article.setContent(content);
        article.setCategory(defaultTextValue(body.get("category"), "使用教程"));
        article.setSortOrder(numberValue(body.get("sortOrder"), 0));
        if (body.containsKey("activeFlag")) {
            article.setActiveFlag(Boolean.TRUE.equals(body.get("activeFlag")));
        } else if (body.containsKey("active")) {
            article.setActiveFlag(Boolean.TRUE.equals(body.get("active")));
        }
    }

    private String textValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultTextValue(Object value, String fallback) {
        String result = textValue(value);
        return result.isBlank() ? fallback : result;
    }

    private Integer numberValue(Object value, Integer fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String normalizeIp(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::ffff:127.0.0.1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String normalizeBillingAction(String action) {
        String value = action == null ? "" : action;
        if (value.contains("PPT") || value.contains("Agent")) return "组会PPT Agent执行";
        if (value.contains("组会论文综述") || value.contains("生成文献综述")) return "论文综述生成";
        if (value.contains("问答") || value.contains("选区") || value.contains("研读") || value.contains("解读") || value.contains("对话") || value.contains("qa")) return "AI研读对话";
        if (value.contains("综述生成") || value.contains("综述")) return "AI研读对话"; // Fallback for historical misclassified ones
        return "AI研读对话";
    }

    private String getLevelTitle(int level) {
        if (level >= 15) return "科研主宰";
        if (level >= 10) return "科研宗师";
        if (level >= 6) return "学术专家";
        if (level >= 3) return "科研骨干";
        return "科研萌新";
    }

    private Map<String, Object> paymentOrderToMap(PaymentOrderEntity order) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("orderNo", order.getOrderNo());
        row.put("userId", order.getUserId());
        row.put("email", appUserRepository.findById(order.getUserId()).map(AppUserEntity::getEmail).orElse("—"));
        row.put("provider", order.getProvider());
        row.put("amount", order.getAmount());
        row.put("actualPayAmount", order.getActualPayAmount());
        row.put("status", order.getStatus());
        row.put("platformOrderNo", order.getPlatformOrderNo());
        row.put("message", order.getMessage());
        row.put("createdAt", order.getCreatedAt());
        row.put("paidAt", order.getPaidAt());
        return row;
    }

    private Map<String, Object> paymentTicketToMap(PaymentTicketEntity ticket) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", ticket.getId());
        row.put("userId", ticket.getUserId());
        row.put("email", appUserRepository.findById(ticket.getUserId()).map(AppUserEntity::getEmail).orElse("—"));
        row.put("type", ticket.getType());
        row.put("orderNo", ticket.getOrderNo());
        row.put("subject", ticket.getSubject());
        row.put("detail", ticket.getDetail());
        row.put("status", ticket.getStatus());
        row.put("adminNote", ticket.getAdminNote());
        row.put("createdAt", ticket.getCreatedAt());
        row.put("processedAt", ticket.getProcessedAt());
        return row;
    }

    private String paymentStatusLabel(String status) {
        return switch (status) {
            case "processed" -> "已处理";
            case "rejected" -> "已驳回";
            default -> "处理中";
        };
    }

    private Map<String, Object> forumReportToMap(ForumPostReportEntity report) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        ForumPostEntity post = forumPostRepository.findById(report.getPostId()).orElse(null);
        row.put("id", report.getId());
        row.put("postId", "post-" + report.getPostId());
        row.put("postTitle", post == null ? "帖子已删除" : post.getTitle());
        row.put("postContent", post == null ? "" : post.getContent());
        row.put("postType", post == null ? "" : post.getPostType());
        row.put("postTime", post == null || post.getCreatedAt() == null ? "" : post.getCreatedAt());
        row.put("postBanned", post != null && post.isBanned());
        row.put("author", post == null ? "—" : post.getAuthor());
        row.put("reporterId", report.getReporterId());
        row.put("reporterName", report.getReporterName());
        row.put("detail", report.getDetail());
        row.put("status", report.getStatus());
        row.put("adminNote", report.getAdminNote());
        row.put("createdAt", report.getCreatedAt());
        row.put("processedAt", report.getProcessedAt());
        row.put("screenshot", report.getScreenshot());
        return row;
    }

    private Map<String, Object> campusVerificationToMap(CampusVerificationEntity verification) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", verification.getId());
        row.put("userId", verification.getUserId());
        row.put("userName", verification.getUserName());
        row.put("email", verification.getEmail());
        row.put("schoolName", verification.getSchoolName());
        row.put("realName", verification.getRealName());
        row.put("studentCardFront", verification.getStudentCardFront());
        row.put("chsiScreenshot", verification.getChsiScreenshot());
        row.put("status", verification.getStatus());
        row.put("statusLabel", campusVerificationStatusLabel(verification.getStatus()));
        row.put("adminNote", verification.getAdminNote());
        row.put("createdAt", verification.getCreatedAt());
        row.put("reviewedAt", verification.getReviewedAt());
        appUserRepository.findById(verification.getUserId()).ifPresent(user -> {
            row.put("currentSchoolName", user.getSchoolName());
            row.put("campusVerified", user.isCampusVerified());
        });
        return row;
    }

    private String campusVerificationStatusLabel(String status) {
        return switch (status) {
            case "approved" -> "已通过";
            case "rejected" -> "未通过";
            default -> "待审核";
        };
    }

    private String forumReportStatusLabel(String status) {
        return switch (status) {
            case "processed" -> "已处理";
            case "rejected" -> "未采纳";
            default -> "待处理";
        };
    }

    private boolean isUsablePaymentTicket(PaymentTicketEntity ticket) {
        return ticket.getDetail() != null && !ticket.getDetail().isBlank()
            && ticket.getOrderNo() != null && !ticket.getOrderNo().isBlank();
    }

    @GetMapping("/monitoring/analytics")
    public Map<String, Object> getMonitoringAnalytics(@RequestParam(value = "date", required = false) String date) {
        List<AiUsageRecordEntity> aiRecords = aiUsageRecordRepository.findAll();
        List<TranslationRecordEntity> translateRecords = translationRecordRepository.findAll();
        java.time.LocalDate selectedDate;
        try {
            selectedDate = (date == null || date.isBlank()) ? java.time.LocalDate.now() : java.time.LocalDate.parse(date);
        } catch (Exception ignored) {
            selectedDate = java.time.LocalDate.now();
        }
        final java.time.LocalDate queryDate = selectedDate;

        List<Map<String, Object>> onlineUsers = new java.util.ArrayList<>();
        List<Map<String, Object>> aiCalls = new java.util.ArrayList<>();
        List<Map<String, Object>> translations = new java.util.ArrayList<>();

        for (int h = 0; h < 24; h++) {
            String timeStr = String.format("%02d:00", h);

            final int hour = h;
            long uniqueAiUsers = aiRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .map(AiUsageRecordEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
            long uniqueTranslateUsers = translateRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .map(TranslationRecordEntity::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
            long dbActiveCount = Math.max(uniqueAiUsers, uniqueTranslateUsers);

            Map<String, Object> onlineRow = new java.util.LinkedHashMap<>();
            onlineRow.put("time", timeStr);
            onlineRow.put("count", (int) dbActiveCount);
            onlineUsers.add(onlineRow);

            // 2. AI Calls
            long chatCalls = aiRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .filter(r -> {
                    String scene = r.getScene() == null ? "" : r.getScene();
                    String act = r.getAction() == null ? "" : r.getAction();
                    return scene.contains("qa") || act.contains("问答") || act.contains("对话");
                })
                .count();
            long reviewCalls = aiRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .filter(r -> {
                    String scene = r.getScene() == null ? "" : r.getScene();
                    String act = r.getAction() == null ? "" : r.getAction();
                    return scene.contains("review") || scene.contains("summary") || act.contains("综述") || act.contains("精读");
                })
                .count();
            long pptCalls = aiRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .filter(r -> {
                    String scene = r.getScene() == null ? "" : r.getScene();
                    String act = r.getAction() == null ? "" : r.getAction();
                    return scene.contains("meeting") || scene.contains("deck") || act.contains("PPT") || act.contains("Agent");
                })
                .count();
            long otherCalls = aiRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .count() - chatCalls - reviewCalls - pptCalls;

            Map<String, Object> aiRow = new java.util.LinkedHashMap<>();
            aiRow.put("time", timeStr);
            aiRow.put("chat", chatCalls);
            aiRow.put("review", reviewCalls);
            aiRow.put("ppt", pptCalls);
            aiRow.put("other", Math.max(0, otherCalls));
            aiRow.put("total", chatCalls + reviewCalls + pptCalls + Math.max(0, otherCalls));
            aiCalls.add(aiRow);

            // 3. Translations
            long transRequests = translateRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .count();
            long charCount = translateRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .mapToLong(r -> r.getCharCount() == null ? 0L : r.getCharCount())
                .sum();
            long avgLatency = Math.round(translateRecords.stream()
                .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate) && r.getCreatedAt().getHour() == hour)
                .mapToLong(r -> r.getLatencyMs() == null ? 0L : r.getLatencyMs())
                .filter(v -> v > 0)
                .average()
                .orElse(0));

            Map<String, Object> transRow = new java.util.LinkedHashMap<>();
            transRow.put("time", timeStr);
            transRow.put("requests", transRequests);
            transRow.put("charCount", charCount);
            transRow.put("avgLatencyMs", avgLatency);
            translations.add(transRow);
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("selectedDate", selectedDate.toString());
        result.put("onlineUsers", onlineUsers);
        result.put("aiCalls", aiCalls);
        result.put("translations", translations);
        result.put("todayAiTokens", aiRecords.stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate))
            .mapToLong(r -> r.getTotalTokens() == null ? 0L : r.getTotalTokens())
            .sum());
        result.put("todayAiFailures", aiRecords.stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate))
            .filter(r -> !"success".equalsIgnoreCase(r.getStatus()))
            .count());
        result.put("todayTranslationFailures", translateRecords.stream()
            .filter(r -> r.getCreatedAt() != null && r.getCreatedAt().toLocalDate().equals(queryDate))
            .filter(r -> !r.isSuccess())
            .count());

        // Security & High-Frequency Real-time stats
        result.put("realtimeOnline", monitoringSecurityService.getRealtimeOnlineCount());
        result.put("securityLogs", monitoringSecurityService.getSecurityLogs());
        result.put("topIps", monitoringSecurityService.getTopRequestIps());
        result.put("topUsers", monitoringSecurityService.getTopRequestUsers());
        result.put("realtimeTraffic", monitoringSecurityService.getRealtimeTrafficSeries());
        result.put("hourlyTraffic", monitoringSecurityService.getHourlyTrafficSeries(selectedDate));
        result.put("endpointHotspots", monitoringSecurityService.getEndpointHotspots());
        result.put("trafficSummary", monitoringSecurityService.getWindowSummary());

        return result;
    }

    @PostMapping("/monitoring/ban-ip")
    public Map<String, Object> banIp(@RequestParam("ip") String ip, @RequestParam(value = "reason", defaultValue = "管理员手动封禁") String reason) {
        monitoringSecurityService.banIp(ip, reason);
        return Map.of("success", true, "message", "已成功封禁 IP: " + ip);
    }

    @PostMapping("/monitoring/unban-ip")
    public Map<String, Object> unbanIp(@RequestParam("ip") String ip) {
        monitoringSecurityService.unbanIp(ip);
        return Map.of("success", true, "message", "已成功解封 IP: " + ip);
    }

    @PostMapping("/monitoring/ban-user")
    public Map<String, Object> banUser(
        @RequestParam("userId") Long userId,
        @RequestParam(value = "reason", defaultValue = "管理员手动封禁") String reason,
        @RequestParam(value = "days", defaultValue = "-1") int days
    ) {
        monitoringSecurityService.banUser(userId, reason, days);
        return Map.of("success", true, "message", "已成功封禁账号 ID: " + userId + " 时长: " + (days < 0 ? "永久" : days + "天"));
    }

    @PostMapping("/monitoring/unban-user")
    public Map<String, Object> unbanUser(@RequestParam("userId") Long userId) {
        monitoringSecurityService.unbanUser(userId);
        return Map.of("success", true, "message", "已成功解封账号 ID: " + userId);
    }
}
