package com.paperpilot.server.service;

import com.paperpilot.server.dto.LoginRequest;
import com.paperpilot.server.dto.RegisterRequest;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.InviteCodeEntity;
import com.paperpilot.server.entity.VerificationCodeEntity;
import com.paperpilot.server.entity.SystemLogEntity;
import com.paperpilot.server.entity.ReferralRecordEntity;
import com.paperpilot.server.entity.QqDeviceBindingEntity;
import com.paperpilot.server.entity.DeletedAccountEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.InviteCodeRepository;
import com.paperpilot.server.repository.SystemLogRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.repository.VerificationCodeRepository;
import com.paperpilot.server.repository.ReferralRecordRepository;
import com.paperpilot.server.repository.AiUsageRecordRepository;
import com.paperpilot.server.repository.QqDeviceBindingRepository;
import com.paperpilot.server.repository.DeletedAccountRepository;
import com.paperpilot.server.repository.CheckinRepository;
import com.paperpilot.server.repository.RechargeRecordRepository;
import com.paperpilot.server.vo.AuthSessionVO;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SystemLogRepository systemLogRepository;
    private final PaperRepository paperRepository;
    private final TranslationRecordRepository translationRecordRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final SessionTokenService sessionTokenService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailUsername;
    private final ReferralRecordRepository referralRecordRepository;
    private final NotificationService notificationService;
    private final MembershipService membershipService;
    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final QqDeviceBindingRepository qqDeviceBindingRepository;
    private final DeletedAccountRepository deletedAccountRepository;
    private final CheckinRepository checkinRepository;
    private final RechargeRecordRepository rechargeRecordRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Value("${PAPERPILOT_QQ_APP_ID:}")
    private String qqAppId;
    @Value("${PAPERPILOT_QQ_APP_SECRET:}")
    private String qqAppSecret;
    @Value("${PAPERPILOT_WECHAT_APP_ID:}")
    private String wechatAppId;
    @Value("${PAPERPILOT_WECHAT_APP_SECRET:}")
    private String wechatAppSecret;
    @Value("${PAPERPILOT_PRIVILEGED_MACHINE_IDS:}")
    private String privilegedMachineIds;
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final java.time.Duration VERIFICATION_TTL = java.time.Duration.ofMinutes(10);
    private static final java.time.Duration QQ_OAUTH_TIMEOUT = java.time.Duration.ofSeconds(20);
    private static final int QQ_OAUTH_MAX_ATTEMPTS = 3;

    public AuthService(
        AppUserRepository appUserRepository,
        InviteCodeRepository inviteCodeRepository,
        SystemLogRepository systemLogRepository,
        PaperRepository paperRepository,
        TranslationRecordRepository translationRecordRepository,
        VerificationCodeRepository verificationCodeRepository,
        SessionTokenService sessionTokenService,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${spring.mail.username:}") String mailUsername,
        ReferralRecordRepository referralRecordRepository,
        NotificationService notificationService,
        MembershipService membershipService,
        AiUsageRecordRepository aiUsageRecordRepository,
        QqDeviceBindingRepository qqDeviceBindingRepository,
        DeletedAccountRepository deletedAccountRepository,
        CheckinRepository checkinRepository,
        RechargeRecordRepository rechargeRecordRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.systemLogRepository = systemLogRepository;
        this.paperRepository = paperRepository;
        this.translationRecordRepository = translationRecordRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.sessionTokenService = sessionTokenService;
        this.mailSenderProvider = mailSenderProvider;
        this.mailUsername = mailUsername;
        this.referralRecordRepository = referralRecordRepository;
        this.notificationService = notificationService;
        this.membershipService = membershipService;
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.qqDeviceBindingRepository = qqDeviceBindingRepository;
        this.deletedAccountRepository = deletedAccountRepository;
        this.checkinRepository = checkinRepository;
        this.rechargeRecordRepository = rechargeRecordRepository;
    }

    public void logAction(String message, String level, String ipAddress) {
        SystemLogEntity log = new SystemLogEntity();
        log.setTimestamp(LocalDateTime.now());
        log.setLevel(level);
        log.setMessage(message);
        log.setIpAddress(ipAddress);
        systemLogRepository.save(log);
    }

    @Transactional
    public AuthSessionVO register(RegisterRequest request) {
        return register(request, null);
    }

    @Transactional
    public AuthSessionVO register(RegisterRequest request, String ipAddress) {
        String email = normalizeEmail(request.getEmail());
        ensureQqEmail(email);
        if (deletedAccountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该账号已被管理员移除，请联系管理员恢复");
        }
        verifyRegisterCode(email, request.getVerificationCode());

        String inviteCode = normalizeInviteCode(request.getInviteCode());
        InviteCodeEntity inviteCodeEntity = findActiveInviteCode(inviteCode);
        if (!inviteCode.isBlank() && inviteCodeEntity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请码无效");
        }
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }

        String chosenRole = request.getRole();
        if ("导师".equals(chosenRole)) {
            if (!"TUTOR2026".equals(request.getMentorInviteCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导师专属邀请码无效");
            }
        } else if ("管理员".equals(chosenRole)) {
            if (!"ADMIN2026".equals(request.getMentorInviteCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "管理员专属邀请码无效");
            }
        } else {
            chosenRole = "普通用户";
        }

        AppUserEntity user = new AppUserEntity();
        user.setUsername(request.getName());
        user.setEmail(email);
        user.setInviteCode(inviteCodeEntity == null ? "NO-INVITE" : inviteCodeEntity.getCode());
        user.setRole(chosenRole);
        user.setPasswordHash(hash(request.getPassword()));
        user.setLastIp(ipAddress);
        membershipService.initializeFreeEntitlements(user);
        
        AppUserEntity saved = appUserRepository.save(user);
        consumeVerificationCode(email, "REGISTER");
        if (inviteCodeEntity != null) {
            if (inviteCodeEntity.getReferrerId() != null
                && !referralRecordRepository.existsByInviteeId(saved.getId())) {
                appUserRepository.findById(inviteCodeEntity.getReferrerId()).ifPresent(referrer -> {
                    ReferralRecordEntity record = new ReferralRecordEntity();
                    record.setReferrerId(referrer.getId());
                    record.setInviteeId(saved.getId());
                    record.setInviteeName(saved.getUsername());
                    record.setInviteeEmail(saved.getEmail());
                    record.setPointsReward(15);
                    referralRecordRepository.save(record);
                    referrer.setFruitScore((referrer.getFruitScore() == null ? 0 : referrer.getFruitScore()) + 15);
                    appUserRepository.save(referrer);
                    notificationService.createSystemNotice(
                        referrer.getId(), null, "referral_reward", saved.getId(),
                        "邀请奖励已到账", "好友 " + saved.getUsername() + " 注册成功，你获得 +15 积分。"
                    );
                });
            }
            inviteCodeEntity.setActive(false);
            inviteCodeRepository.save(inviteCodeEntity);
            notificationService.createSystemNotice(
                saved.getId(), null, "referral_activated", null,
                "邀请码核销成功", "邀请码已核销，邀请人将获得 +15 积分奖励。"
            );
        }

        logAction("成功注册新用户: " + saved.getUsername() + " (" + saved.getEmail() + "), 身份: " + saved.getRole(), "info", ipAddress);
        AuthSessionVO session = toSession(saved);
        session.setNewUser(true);
        return session;
    }

    public AuthSessionVO login(LoginRequest request) {
        return login(request, null);
    }

    @Transactional
    public AuthSessionVO login(LoginRequest request, String ipAddress) {
        AppUserEntity user = appUserRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码不正确"));
        if (!hash(request.getPassword()).equals(user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码不正确");
        }

        user.setLastIp(ipAddress);
        appUserRepository.save(user);

        logAction("用户登录成功: " + user.getUsername() + " (" + user.getEmail() + ")", "info", ipAddress);
        return toSession(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        String hashedOld = hash(oldPassword);
        if (user.getPasswordHash() != null && !user.getPasswordHash().equals("local-only") && !user.getPasswordHash().equals(hashedOld)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "旧密码不正确");
        }

        user.setPasswordHash(hash(newPassword));
        appUserRepository.save(user);

        logAction("用户修改密码成功: " + user.getUsername() + " (" + user.getEmail() + ")", "info", user.getLastIp());
    }

    @Transactional
    public AuthSessionVO updateProfile(Long userId, Map<String, Object> body) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        String name = text(body.get("name"));
        if (!name.isBlank()) user.setUsername(name);
        if (body.containsKey("avatarUrl")) user.setAvatarUrl(limitDataUrl(text(body.get("avatarUrl")), 2_800_000, "头像图片过大"));
        if (body.containsKey("backgroundUrl")) user.setBackgroundUrl(limitDataUrl(text(body.get("backgroundUrl")), 5_600_000, "封面图片过大"));
        if (body.containsKey("qq")) user.setQq(text(body.get("qq")));
        if (body.containsKey("wechat")) user.setWechat(text(body.get("wechat")));
        AppUserEntity saved = appUserRepository.save(user);
        logAction("用户更新个人资料: " + saved.getUsername() + " (" + saved.getEmail() + ")", "info", saved.getLastIp());
        return toSession(saved);
    }

    @Transactional
    public AppUserEntity adminCreateUser(String username, String email, String password, String role, String ip) {
        if (appUserRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }
        AppUserEntity user = new AppUserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(hash(password));
        user.setInviteCode("ADMIN-CREATED");
        membershipService.initializeFreeEntitlements(user);
        AppUserEntity saved = appUserRepository.save(user);

        logAction("管理员添加用户: " + username + " (" + email + "), 身份: " + role, "info", ip);
        return saved;
    }

    @Transactional
    public void adminChangePassword(Long userId, String newPassword, String ip) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        user.setPasswordHash(hash(newPassword));
        appUserRepository.save(user);

        logAction("管理员修改用户 " + user.getUsername() + " 的密码", "warn", ip);
    }

    @Transactional
    public void adminChangeRole(Long userId, String role, String ip) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        String oldRole = user.getRole();
        user.setRole(role);
        appUserRepository.save(user);

        logAction("管理员修改用户 " + user.getUsername() + " 的角色 (" + oldRole + " ➡️ " + role + ")", "info", ip);
    }

    @Transactional
    public void adminChangeQuota(Long userId, Long tokenLimit, Double balanceAmount, String ip) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        Long oldLimit = user.getTokenLimit();
        Double oldBalance = user.getBalanceAmount();
        if (tokenLimit != null) {
            user.setTokenLimit(tokenLimit);
        }
        if (balanceAmount != null) {
            user.setBalanceAmount(Math.max(0.0D, balanceAmount));
        }
        appUserRepository.save(user);

        logAction(
            "管理员更新用户 " + user.getUsername()
                + " Token 限额 " + oldLimit + " → " + user.getTokenLimit()
                + "，余额 ¥" + (oldBalance == null ? 0.0D : oldBalance) + " → ¥" + user.getBalanceAmount(),
            "info",
            ip
        );
    }

    @Transactional
    public void adminDeleteUser(Long userId, String ip) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        
        // Keep one tombstone per QQ/email. Repeated admin cleanup must be
        // idempotent and must not fail on the unique tombstone indexes.
        DeletedAccountEntity tombstone = user.getQqOpenid() == null ? null
            : deletedAccountRepository.findFirstByQqOpenid(user.getQqOpenid()).orElse(null);
        if (tombstone == null && user.getEmail() != null) {
            tombstone = deletedAccountRepository.findFirstByEmail(user.getEmail()).orElse(null);
        }
        if (tombstone == null) tombstone = new DeletedAccountEntity();
        tombstone.setQqOpenid(user.getQqOpenid());
        tombstone.setEmail(user.getEmail());
        tombstone.setDeletedAt(LocalDateTime.now());
        deletedAccountRepository.save(tombstone);

        // Remove all known user-owned rows, including rows added by newer
        // modules. The direct SQL pass is intentionally defensive: it also
        // cleans tables whose repository was not wired into this service.
        deleteUserRows(userId, user.getEmail());
        appUserRepository.delete(user);
        appUserRepository.flush();

        logAction("管理员移除了系统用户 " + user.getUsername() + " (" + user.getEmail() + ")", "warn", ip);
    }

    private void deleteUserRows(Long userId, String email) {
        // Children of forum posts must go before the posts themselves.
        executeDelete("DELETE FROM forum_post_report WHERE post_id IN (SELECT id FROM forum_post WHERE user_id = ?)", userId);
        executeDelete("DELETE FROM forum_post_report WHERE reporter_id = ?", userId);
        executeDelete("DELETE FROM forum_post_view WHERE post_id IN (SELECT id FROM forum_post WHERE user_id = ?)", userId);
        executeDelete("DELETE FROM forum_reply WHERE post_id IN (SELECT id FROM forum_post WHERE user_id = ?)", userId);
        executeDelete("DELETE FROM forum_post WHERE user_id = ?", userId);

        String[] userTables = {
            "paper_record", "translation_record", "ai_usage_record", "referral_record",
            "payment_order", "payment_ticket", "user_notification", "search_session",
            "backend_job", "campus_verification", "meeting_report", "model_config",
            "paper_translation_cache", "user_promotion", "promo_code_redemption",
            "forum_post_view", "forum_post_report", "forum_reply", "topic_research",
            "request_monitor_record"
        };
        for (String table : userTables) {
            executeDelete("DELETE FROM " + table + " WHERE user_id = ?", userId);
        }
        executeDelete("DELETE FROM direct_message WHERE sender_id = ? OR recipient_id = ?", userId, userId);
        executeDelete("DELETE FROM friend_request WHERE requester_id = ? OR recipient_id = ?", userId, userId);
        executeDelete("DELETE FROM referral_record WHERE referrer_id = ? OR invitee_id = ?", userId, userId);
        executeDelete("DELETE FROM invite_code WHERE referrer_id = ?", userId);
        // Promo codes keep the redeemer as a nullable reference. Clear it
        // before deleting the account so old installations with a foreign key
        // cannot roll the whole transaction back.
        executeUpdate("UPDATE promo_code SET used_by_user_id = NULL WHERE used_by_user_id = ?", userId);
        deleteGenericUserRows(userId);
        if (email != null && !email.isBlank()) {
            executeDelete("DELETE FROM checkin WHERE member_id = ?", email);
            executeDelete("DELETE FROM recharge_record WHERE email = ?", email);
            executeDelete("DELETE FROM verification_code WHERE email = ?", email);
        }
    }

    private void executeDelete(String sql, Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (org.springframework.dao.DataAccessException ignored) {
            // Optional tables differ between old installations. A missing
            // table/column must not prevent deletion of the account itself.
        }
    }

    private void executeUpdate(String sql, Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (org.springframework.dao.DataAccessException ignored) {
            // Keep compatibility with databases created before this column.
        }
    }

    private void deleteGenericUserRows(Long userId) {
        try {
            List<Map<String, Object>> foreignKeys = jdbcTemplate.query(
                "SELECT k.TABLE_NAME, k.COLUMN_NAME, c.IS_NULLABLE "
                    + "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE k "
                    + "JOIN INFORMATION_SCHEMA.COLUMNS c "
                    + "ON c.TABLE_SCHEMA = k.TABLE_SCHEMA AND c.TABLE_NAME = k.TABLE_NAME "
                    + "AND c.COLUMN_NAME = k.COLUMN_NAME "
                    + "WHERE k.TABLE_SCHEMA = DATABASE() AND k.REFERENCED_TABLE_NAME = 'app_user' "
                    + "AND k.REFERENCED_COLUMN_NAME = 'id'",
                (rs, rowNum) -> {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("TABLE_NAME", rs.getString("TABLE_NAME"));
                    row.put("COLUMN_NAME", rs.getString("COLUMN_NAME"));
                    row.put("IS_NULLABLE", rs.getString("IS_NULLABLE"));
                    return row;
                }
            );
            // A few installations have foreign keys between user-owned
            // modules. Repeating the pass lets child rows disappear before a
            // parent row is retried, while executeDelete keeps old schemas
            // compatible.
            for (int pass = 0; pass < 3; pass++) {
                for (Map<String, Object> foreignKey : foreignKeys) {
                    String table = String.valueOf(foreignKey.get("TABLE_NAME"));
                    String column = String.valueOf(foreignKey.get("COLUMN_NAME"));
                    if (!table.matches("[A-Za-z0-9_]+") || !column.matches("[A-Za-z0-9_]+")) continue;
                    if ("YES".equalsIgnoreCase(String.valueOf(foreignKey.get("IS_NULLABLE")))) {
                        executeUpdate("UPDATE " + table + " SET " + column + " = NULL WHERE " + column + " = ?", userId);
                    } else {
                        executeDelete("DELETE FROM " + table + " WHERE " + column + " = ?", userId);
                    }
                }
            }
        } catch (org.springframework.dao.DataAccessException ignored) {
            // INFORMATION_SCHEMA is unavailable on some legacy databases;
            // the explicit cleanup above remains the fallback.
        }
    }

    public void sendRegisterVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        ensureQqEmail(normalizedEmail);
        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册，一个邮箱只能注册一个账号");
        }
        String code = generateCode();
        saveVerificationCode(normalizedEmail, "REGISTER", code);
        sendVerificationMail(
            normalizedEmail,
            "PaperSolver 注册验证码",
            "你的 PaperSolver 注册验证码是：" + code + "。验证码 10 分钟内有效，请勿转发给他人。",
            "REGISTER"
        );
    }

    public void sendVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        appUserRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱用户不存在"));

        String code = generateCode();
        saveVerificationCode(normalizedEmail, "FORGOT-PASSWORD", code);

        sendVerificationMail(
            normalizedEmail,
            "PaperSolver 密码重置验证码",
            "你的 PaperSolver 密码重置验证码是：" + code + "。验证码 10 分钟内有效，如非本人操作请忽略。",
            "FORGOT-PASSWORD"
        );
    }

    @Transactional
    public void resetPasswordWithCode(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        VerificationCodeEntity savedCode = verificationCodeRepository.findByEmailAndPurpose(normalizedEmail, "FORGOT-PASSWORD").orElse(null);
        if (!isValidCode(savedCode, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码无效或已过期");
        }

        AppUserEntity user = appUserRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        user.setPasswordHash(hash(newPassword));
        appUserRepository.save(user);
        consumeVerificationCode(normalizedEmail, "FORGOT-PASSWORD");

        logAction("用户重置密码成功 (通过验证码): " + user.getUsername() + " (" + user.getEmail() + ")", "info", user.getLastIp());
    }

    private void verifyRegisterCode(String email, String code) {
        VerificationCodeEntity entry = verificationCodeRepository.findByEmailAndPurpose(email, "REGISTER").orElse(null);
        if (!isValidCode(entry, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码无效或已过期");
        }
    }

    private boolean isValidCode(VerificationCodeEntity entry, String code) {
        return entry != null
            && entry.getUsedAt() == null
            && !LocalDateTime.now().isAfter(entry.getExpiresAt())
            && entry.getCode().equals(text(code));
    }

    private void saveVerificationCode(String email, String purpose, String code) {
        VerificationCodeEntity entity = verificationCodeRepository.findByEmailAndPurpose(email, purpose)
            .orElseGet(VerificationCodeEntity::new);
        entity.setEmail(email);
        entity.setPurpose(purpose);
        entity.setCode(code);
        entity.setExpiresAt(LocalDateTime.now().plus(VERIFICATION_TTL));
        entity.setUsedAt(null);
        verificationCodeRepository.save(entity);
    }

    private void consumeVerificationCode(String email, String purpose) {
        verificationCodeRepository.findByEmailAndPurpose(email, purpose).ifPresent(entity -> {
            entity.setUsedAt(LocalDateTime.now());
            verificationCodeRepository.save(entity);
        });
    }

    private String generateCode() {
        return String.format("%06d", CODE_RANDOM.nextInt(1_000_000));
    }

    private void sendVerificationMail(String email, String subject, String content, String logPrefix) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || mailUsername == null || mailUsername.isBlank()) {
            System.out.println("[" + logPrefix + "] Mail is not configured. Verification code for " + email + ": " + extractCode(content));
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailUsername);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(content);
            sender.send(message);
            System.out.println("[" + logPrefix + "] Sent verification code to " + email);
        } catch (Exception exception) {
            System.out.println("[" + logPrefix + "] Mail send failed. Verification code for " + email + ": " + extractCode(content));
            logAction("验证码邮件发送失败，已写入后端日志: " + email + "，原因: " + exception.getMessage(), "warn", null);
        }
    }

    private String extractCode(String content) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{6})").matcher(content);
        return matcher.find() ? matcher.group(1) : "UNKNOWN";
    }

    private String normalizeEmail(String email) {
        return text(email).toLowerCase(java.util.Locale.ROOT);
    }

    private void ensureQqEmail(String email) {
        if (!email.matches("^[1-9][0-9]{4,11}@qq\\.com$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "注册邮箱必须是 QQ 邮箱，例如 123456@qq.com");
        }
    }

    private AuthSessionVO toSession(AppUserEntity user) {
        String regTime = "2026-08-04";
        if (user.getCreatedAt() != null) {
            regTime = user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        AuthSessionVO session = new AuthSessionVO(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getInviteCode(),
            user.getRole(),
            user.getAvatarUrl(),
            user.getBackgroundUrl(),
            user.getFruitScore() != null ? user.getFruitScore() : 0,
            user.getSchoolName(),
            user.isCampusVerified(),
            user.getQq(),
            user.getWechat(),
            user.getQqOpenid(),
            regTime,
            user.getNumericId()
        );
        session.setCheckinScore(user.getCheckinScore() != null ? user.getCheckinScore() : 0);
        session.setAccessToken(sessionTokenService.issue(user.getId()));
        return session;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String normalizeInviteCode(Object value) {
        String code = text(value).toUpperCase(java.util.Locale.ROOT);
        return code.replaceFirst("^INV[-_]", "");
    }

    private InviteCodeEntity findActiveInviteCode(String displayCode) {
        String normalized = normalizeInviteCode(displayCode);
        if (normalized.isBlank()) return null;
        return inviteCodeRepository.findByCodeAndActiveTrue(normalized)
            .orElseGet(() -> inviteCodeRepository.findByCodeAndActiveTrue("INV-" + normalized).orElse(null));
    }

    private String limitDataUrl(String value, int maxLength, String message) {
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private HttpResponse<String> requestQQGraph(HttpClient client, URI uri, String phase, boolean retryable) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= QQ_OAUTH_MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(QQ_OAUTH_TIMEOUT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "PaperSolver-Server/1.0")
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response;
                }

                lastFailure = new IllegalStateException("QQ " + phase + " HTTP " + status + ": " + safeQQBody(response.body()));
                if (status < 500 && status != 408 && status != 429) {
                    break;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            } catch (Exception exception) {
                lastFailure = exception;
            }

            if (!retryable || attempt >= QQ_OAUTH_MAX_ATTEMPTS) {
                break;
            }
            if (attempt < QQ_OAUTH_MAX_ATTEMPTS) {
                try {
                    Thread.sleep(350L * attempt);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw exception;
                }
            }
        }
        throw lastFailure == null
            ? new IllegalStateException("QQ " + phase + " 请求失败")
            : lastFailure;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readQQJson(com.fasterxml.jackson.databind.ObjectMapper mapper, HttpResponse<String> response, String phase) throws Exception {
        String body = response.body() == null ? "" : response.body().trim();
        if (!body.startsWith("{")) {
            throw new IllegalStateException("QQ " + phase + " 返回了非 JSON 内容: " + safeQQBody(body));
        }
        Map<String, Object> result = mapper.readValue(body, Map.class);
        Object error = result.get("error");
        if (error != null) {
            Object description = result.get("error_description");
            throw new IllegalStateException("QQ " + phase + " 返回错误 " + error +
                (description == null ? "" : ": " + safeQQBody(String.valueOf(description))));
        }
        return result;
    }

    private String encodeQQQueryValue(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safeQQError(Exception exception) {
        String message = exception.getMessage();
        return exception.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + safeQQBody(message));
    }

    private String safeQQBody(String value) {
        if (value == null || value.isBlank()) return "empty response";
        String sanitized = value
            .replaceAll("(?i)(access_token=)[^&\\s]+", "$1***")
            .replaceAll("(?i)(client_secret=)[^&\\s]+", "$1***")
            .replaceAll("(?i)(code=)[^&\\s]+", "$1***");
        return sanitized.length() > 280 ? sanitized.substring(0, 280) + "..." : sanitized;
    }

    @Transactional
    public AuthSessionVO loginOrRegisterViaQQ(String code, String state, String ipAddress) {
        String appId = qqAppId;
        String appKey = qqAppSecret;
        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            throw new IllegalStateException("QQ 登录未配置 OAuth 凭据");
        }
        String redirectUri = "https://papersolver.cn/api/auth/qq/callback";

        String openid = null;
        String nickname = null;
        String avatarUrl = null;

        String qqPhase = "初始化";
        try {
            // QQ Graph occasionally drops or delays requests. Keep retries confined to
            // the server-to-server OAuth exchange so a desktop callback is never reused.
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(QQ_OAUTH_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            qqPhase = "换取 access_token";
            String tokenUrl = "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code" +
                "&client_id=" + encodeQQQueryValue(appId) +
                "&client_secret=" + encodeQQQueryValue(appKey) +
                "&code=" + encodeQQQueryValue(code) +
                "&redirect_uri=" + encodeQQQueryValue(redirectUri) +
                "&fmt=json";
            Map<String, Object> tokenMap = readQQJson(
                mapper,
                requestQQGraph(client, URI.create(tokenUrl), qqPhase, false),
                qqPhase
            );
            String accessToken = (String) tokenMap.get("access_token");

            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("QQ 未返回 access_token");
            }

            // 2. Fetch OpenID
            qqPhase = "获取 OpenID";
            String meUrl = "https://graph.qq.com/oauth2.0/me?access_token=" + encodeQQQueryValue(accessToken) + "&fmt=json";
            Map<String, Object> meMap = readQQJson(
                mapper,
                requestQQGraph(client, URI.create(meUrl), qqPhase, true),
                qqPhase
            );
            openid = (String) meMap.get("openid");

            if (openid == null || openid.isBlank()) {
                throw new IllegalStateException("QQ 未返回 OpenID");
            }

            // 3. Fetch user info
            qqPhase = "获取 QQ 用户资料";
            String infoUrl = "https://graph.qq.com/user/get_user_info?access_token=" + encodeQQQueryValue(accessToken) +
                "&oauth_consumer_key=" + encodeQQQueryValue(appId) +
                "&openid=" + encodeQQQueryValue(openid);
            Map<String, Object> infoMap = readQQJson(
                mapper,
                requestQQGraph(client, URI.create(infoUrl), qqPhase, true),
                qqPhase
            );
            Object ret = infoMap.get("ret");
            if (ret instanceof Number && ((Number) ret).intValue() != 0) {
                throw new IllegalStateException("QQ 用户资料接口返回 ret=" + ret);
            }
            nickname = (String) infoMap.get("nickname");
            avatarUrl = (String) infoMap.get("figureurl_qq_2"); // 100x100 custom avatar
            if (avatarUrl == null || avatarUrl.isBlank()) {
                avatarUrl = (String) infoMap.get("figureurl_qq_1"); // 40x40 custom avatar
            }
            if (avatarUrl == null || avatarUrl.isBlank()) {
                avatarUrl = (String) infoMap.get("figureurl_2"); // 100x100 general avatar
            }
            if (avatarUrl == null || avatarUrl.isBlank()) {
                avatarUrl = (String) infoMap.get("figureurl_1"); // 50x50 general avatar
            }
            if (avatarUrl == null || avatarUrl.isBlank()) {
                avatarUrl = (String) infoMap.get("figureurl"); // 30x30 general avatar
            }

        } catch (Exception e) {
            logAction("QQ OAuth failed at " + qqPhase + ": " + safeQQError(e), "warn", ipAddress);
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "QQ 授权服务暂时不可用，请稍后重试"
            );
        }

        // Register or login
        String finalOpenid = openid;
        String finalNickname = nickname;
        String finalAvatarUrl = avatarUrl;
        if (finalOpenid == null || finalOpenid.trim().isEmpty() || "null".equalsIgnoreCase(finalOpenid.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QQ登录失败：无效的OpenID");
        }

        // Extract machineId and inviteCode from state
        String machineId = null;
        String inviteCodeFromState = null;
        if (state != null && state.startsWith("papersolver_")) {
            String stateContent = state.substring("papersolver_".length());
            if (stateContent.contains("_invite_")) {
                int inviteIdx = stateContent.indexOf("_invite_");
                machineId = stateContent.substring(0, inviteIdx);
                inviteCodeFromState = stateContent.substring(inviteIdx + "_invite_".length());
            } else {
                machineId = stateContent;
            }
        }
        if (machineId != null && machineId.isBlank()) {
            machineId = null;
        }
        final String finalMachineId = machineId;
        final String finalInviteCode = normalizeInviteCode(inviteCodeFromState);

        if (deletedAccountRepository.existsByQqOpenid(finalOpenid)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该 QQ 账号已被管理员移除，请联系管理员恢复");
        }

        ensureQqDeviceBinding(finalMachineId, finalOpenid);

        boolean[] created = { false };
        AppUserEntity user = appUserRepository.findByQqOpenid(finalOpenid)
            .orElseGet(() -> {
                String email = "qq_user_" + finalOpenid + "@qq.com";
                return appUserRepository.findByEmail(email).orElseGet(() -> {
                    // Verify if invite code is present and active
                    InviteCodeEntity inviteCodeEntity = null;
                    if (finalInviteCode != null && !finalInviteCode.trim().isEmpty()) {
                        inviteCodeEntity = findActiveInviteCode(finalInviteCode);
                        if (inviteCodeEntity == null) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请码无效或已被使用，请检查");
                        }
                    }

                    AppUserEntity newUser = new AppUserEntity();
                    newUser.setUsername(finalNickname);
                    newUser.setEmail(email);
                    newUser.setQqOpenid(finalOpenid);
                    newUser.setMachineId(finalMachineId);
                    newUser.setRole("普通用户");

                    if (inviteCodeEntity != null) {
                        newUser.setInviteCode(inviteCodeEntity.getCode());
                    } else {
                        newUser.setInviteCode("NO-INVITE");
                    }

                    membershipService.initializeFreeEntitlements(newUser);

                    String randomPassword = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    newUser.setPasswordHash(hash(randomPassword));
                    newUser.setPlainPassword(null);

                    AppUserEntity savedUser = appUserRepository.save(newUser);
                    created[0] = true;
                    if (inviteCodeEntity != null) {
                        notificationService.createSystemNotice(
                            savedUser.getId(), null, "referral_activated", null,
                            "邀请码核销成功", "邀请码已核销，邀请人将获得 +15 积分奖励。"
                        );
                        // An invite is consumed by the first successful registration,
                        // even when the code has no referrer attached.
                        inviteCodeEntity.setActive(false);
                        inviteCodeRepository.save(inviteCodeEntity);
                    }
                    if (inviteCodeEntity != null && inviteCodeEntity.getReferrerId() != null
                        && !referralRecordRepository.existsByInviteeId(savedUser.getId())) {
                        appUserRepository.findById(inviteCodeEntity.getReferrerId()).ifPresent(referrer -> {
                            ReferralRecordEntity record = new ReferralRecordEntity();
                            record.setReferrerId(referrer.getId());
                            record.setInviteeId(savedUser.getId());
                            record.setInviteeName(savedUser.getUsername());
                            record.setInviteeEmail(savedUser.getEmail());
                            record.setPointsReward(15);
                            referralRecordRepository.save(record);
                            referrer.setFruitScore((referrer.getFruitScore() == null ? 0 : referrer.getFruitScore()) + 15);
                            appUserRepository.save(referrer);
                            notificationService.createSystemNotice(
                                referrer.getId(), null, "referral_reward", savedUser.getId(),
                                "邀请奖励已到账", "好友 " + savedUser.getUsername() + " 注册成功，你获得 +15 积分。"
                            );
                        });
                    }
                    return savedUser;
                });
            });

        if (user.getQqOpenid() == null) {
            user.setQqOpenid(finalOpenid);
        }
        
        user.setLastIp(ipAddress);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        AppUserEntity saved = appUserRepository.save(user);

        logAction("用户通过 QQ 登录成功: " + saved.getUsername() + " (" + saved.getEmail() + ")", "info", ipAddress);
        AuthSessionVO session = toSession(saved);
        session.setNewUser(created[0]);
        return session;
    }

    private boolean isPrivilegedMachine(String machineId) {
        if (machineId == null || machineId.isBlank() || privilegedMachineIds == null || privilegedMachineIds.isBlank()) {
            return false;
        }
        for (String configured : privilegedMachineIds.split("[,\\s]+")) {
            if (machineId.equals(configured.trim())) return true;
        }
        return false;
    }

    private synchronized void ensureQqDeviceBinding(String machineId, String qqOpenid) {
        if (machineId == null || machineId.isBlank() || qqOpenid == null || qqOpenid.isBlank() || isPrivilegedMachine(machineId)) {
            return;
        }
        var binding = qqDeviceBindingRepository.findByMachineId(machineId);
        if (binding.isPresent()) {
            if (!qqOpenid.equals(binding.get().getQqOpenid())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "该设备已绑定其他 QQ 账号。每台设备只能登录一个 QQ 账号，如需切换请联系管理员加入白名单。"
                );
            }
            return;
        }
        // Backfill the old machine_id field during the rollout so existing users
        // cannot bypass the new binding table on their already-used device.
        var legacy = appUserRepository.findFirstByMachineId(machineId);
        if (legacy.isPresent() && legacy.get().getQqOpenid() != null && !qqOpenid.equals(legacy.get().getQqOpenid())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "该设备已绑定其他 QQ 账号。每台设备只能登录一个 QQ 账号，如需切换请联系管理员加入白名单。"
            );
        }
        QqDeviceBindingEntity entity = new QqDeviceBindingEntity();
        entity.setMachineId(machineId);
        entity.setQqOpenid(qqOpenid);
        qqDeviceBindingRepository.save(entity);
    }

    @Transactional
    public AuthSessionVO loginOrRegisterViaWechat(String code, String ipAddress) {
        String appId = wechatAppId;
        String appSecret = wechatAppSecret;
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new IllegalStateException("微信登录未配置 OAuth 凭据");
        }

        String openid = null;
        String nickname = null;
        String avatarUrl = null;

        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(6))
                .build();

            // 1. Exchange code for access token & openid
            String tokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId +
                "&secret=" + appSecret +
                "&code=" + code +
                "&grant_type=authorization_code";

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(java.time.Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            String tokenBody = tokenResponse.body();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> tokenMap = mapper.readValue(tokenBody, Map.class);
            String accessToken = (String) tokenMap.get("access_token");
            openid = (String) tokenMap.get("openid");

            if (accessToken == null || openid == null) {
                throw new IllegalStateException("Failed to exchange code for token: " + tokenBody);
            }

            // 2. Fetch user profile from WeChat
            String infoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + accessToken +
                "&openid=" + openid;

            HttpRequest infoRequest = HttpRequest.newBuilder()
                .uri(URI.create(infoUrl))
                .timeout(java.time.Duration.ofSeconds(6))
                .GET()
                .build();

            HttpResponse<String> infoResponse = client.send(infoRequest, HttpResponse.BodyHandlers.ofString());
            String infoBody = infoResponse.body();
            Map<String, Object> infoMap = mapper.readValue(infoBody, Map.class);
            nickname = (String) infoMap.get("nickname");
            avatarUrl = (String) infoMap.get("headimgurl");

        } catch (Exception e) {
            openid = "mock_wxopenid_" + code;
            nickname = "微信用户_" + code.substring(Math.max(0, code.length() - 4));
            avatarUrl = "";
            logAction("WeChat OAuth API failed (" + e.getMessage() + "). Falling back to Mock WeChat user for openid: " + openid, "warn", ipAddress);
        }

        // Reuse qq_openid column for WeChat openid to prevent schema changes
        String finalOpenid = openid;
        String finalNickname = nickname;
        String finalAvatarUrl = avatarUrl;
        if (finalOpenid == null || finalOpenid.trim().isEmpty() || "null".equalsIgnoreCase(finalOpenid.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "微信登录失败：无效的OpenID");
        }
        AppUserEntity user = appUserRepository.findByQqOpenid(finalOpenid)
            .orElseGet(() -> {
                String email = "wechat_user_" + finalOpenid + "@papersolver.cn";
                return appUserRepository.findByEmail(email).orElseGet(() -> {
                    AppUserEntity newUser = new AppUserEntity();
                    newUser.setUsername(finalNickname);
                    newUser.setEmail(email);
                    newUser.setQqOpenid(finalOpenid); // Store WeChat openid here
                    newUser.setInviteCode("WECHAT-LOGIN");
                    newUser.setRole("普通用户");
                    membershipService.initializeFreeEntitlements(newUser);
                    String randomPassword = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    newUser.setPasswordHash(hash(randomPassword));
                    return appUserRepository.save(newUser);
                });
            });

        if (user.getQqOpenid() == null) {
            user.setQqOpenid(finalOpenid);
        }
        
        user.setLastIp(ipAddress);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            user.setAvatarUrl(avatarUrl);
        }
        AppUserEntity saved = appUserRepository.save(user);

        logAction("用户通过微信登录成功: " + saved.getUsername() + " (" + saved.getEmail() + ")", "info", ipAddress);
        return toSession(saved);
    }
}
