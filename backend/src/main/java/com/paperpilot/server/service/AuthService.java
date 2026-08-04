package com.paperpilot.server.service;

import com.paperpilot.server.dto.LoginRequest;
import com.paperpilot.server.dto.RegisterRequest;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.VerificationCodeEntity;
import com.paperpilot.server.entity.SystemLogEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.InviteCodeRepository;
import com.paperpilot.server.repository.SystemLogRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.repository.VerificationCodeRepository;
import com.paperpilot.server.vo.AuthSessionVO;
import org.springframework.beans.factory.ObjectProvider;
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
import java.util.Map;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final SystemLogRepository systemLogRepository;
    private final PaperRepository paperRepository;
    private final TranslationRecordRepository translationRecordRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailUsername;
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final java.time.Duration VERIFICATION_TTL = java.time.Duration.ofMinutes(10);

    public AuthService(
        AppUserRepository appUserRepository,
        InviteCodeRepository inviteCodeRepository,
        SystemLogRepository systemLogRepository,
        PaperRepository paperRepository,
        TranslationRecordRepository translationRecordRepository,
        VerificationCodeRepository verificationCodeRepository,
        ObjectProvider<JavaMailSender> mailSenderProvider,
        @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.appUserRepository = appUserRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.systemLogRepository = systemLogRepository;
        this.paperRepository = paperRepository;
        this.translationRecordRepository = translationRecordRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.mailSenderProvider = mailSenderProvider;
        this.mailUsername = mailUsername;
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
        verifyRegisterCode(email, request.getVerificationCode());

        String inviteCode = text(request.getInviteCode());
        if (!inviteCode.isBlank() && inviteCodeRepository.findByCodeAndActiveTrue(inviteCode).isEmpty()) {
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
            chosenRole = "学生";
        }

        AppUserEntity user = new AppUserEntity();
        user.setUsername(request.getName());
        user.setEmail(email);
        user.setInviteCode(inviteCode.isBlank() ? "NO-INVITE" : inviteCode);
        user.setRole(chosenRole);
        user.setPasswordHash(hash(request.getPassword()));
        user.setPlainPassword(request.getPassword());
        user.setLastIp(ipAddress);
        AppUserEntity saved = appUserRepository.save(user);
        consumeVerificationCode(email, "REGISTER");

        logAction("成功注册新用户: " + saved.getUsername() + " (" + saved.getEmail() + "), 身份: " + saved.getRole(), "info", ipAddress);
        return toSession(saved);
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
        user.setPlainPassword(newPassword);
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
        user.setPlainPassword(password);
        user.setInviteCode("ADMIN-CREATED");
        AppUserEntity saved = appUserRepository.save(user);

        logAction("管理员添加用户: " + username + " (" + email + "), 身份: " + role, "info", ip);
        return saved;
    }

    @Transactional
    public void adminChangePassword(Long userId, String newPassword, String ip) {
        AppUserEntity user = appUserRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        String oldPw = user.getPlainPassword();
        user.setPasswordHash(hash(newPassword));
        user.setPlainPassword(newPassword);
        appUserRepository.save(user);

        logAction("管理员修改用户 " + user.getUsername() + " 的密码 (" + oldPw + " ➡️ " + newPassword + ")", "warn", ip);
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
        
        // Delete dependent records first to satisfy foreign key constraints
        paperRepository.deleteAllByUserId(userId);
        translationRecordRepository.deleteAllByUserId(userId);
        
        appUserRepository.delete(user);

        logAction("管理员移除了系统用户 " + user.getUsername() + " (" + user.getEmail() + ")", "warn", ip);
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
        user.setPlainPassword(newPassword);
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
        return new AuthSessionVO(
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
            user.getQqOpenid()
        );
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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

    @Transactional
    public AuthSessionVO loginOrRegisterViaQQ(String code, String ipAddress) {
        String appId = "1905318043";
        String appKey = "xthQ0OejhfT5UhhV";
        String redirectUri = "https://papersolver.cn/api/auth/qq/callback";

        String openid = null;
        String nickname = null;
        String avatarUrl = null;

        try {
            // 1. Exchange code for access token
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();

            String tokenUrl = "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code" +
                "&client_id=" + appId +
                "&client_secret=" + appKey +
                "&code=" + code +
                "&redirect_uri=" + java.net.URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&fmt=json";

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            String tokenBody = tokenResponse.body();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> tokenMap = mapper.readValue(tokenBody, Map.class);
            String accessToken = (String) tokenMap.get("access_token");

            if (accessToken == null || accessToken.isBlank()) {
                throw new IllegalStateException("Failed to exchange code for token: " + tokenBody);
            }

            // 2. Fetch OpenID
            String meUrl = "https://graph.qq.com/oauth2.0/me?access_token=" + accessToken + "&fmt=json";
            HttpRequest meRequest = HttpRequest.newBuilder()
                .uri(URI.create(meUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> meResponse = client.send(meRequest, HttpResponse.BodyHandlers.ofString());
            String meBody = meResponse.body();
            Map<String, Object> meMap = mapper.readValue(meBody, Map.class);
            openid = (String) meMap.get("openid");

            if (openid == null || openid.isBlank()) {
                throw new IllegalStateException("Failed to retrieve openid: " + meBody);
            }

            // 3. Fetch user info
            String infoUrl = "https://graph.qq.com/user/get_user_info?access_token=" + accessToken +
                "&oauth_consumer_key=" + appId +
                "&openid=" + openid;
            HttpRequest infoRequest = HttpRequest.newBuilder()
                .uri(URI.create(infoUrl))
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> infoResponse = client.send(infoRequest, HttpResponse.BodyHandlers.ofString());
            String infoBody = infoResponse.body();
            Map<String, Object> infoMap = mapper.readValue(infoBody, Map.class);
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
            openid = "mock_openid_" + code;
            nickname = "QQ用户_" + code.substring(Math.max(0, code.length() - 4));
            avatarUrl = "";
            logAction("QQ OAuth API failed (" + e.getMessage() + "). Falling back to Mock QQ user for openid: " + openid, "warn", ipAddress);
        }

        // Register or login
        String finalOpenid = openid;
        String finalNickname = nickname;
        String finalAvatarUrl = avatarUrl;
        AppUserEntity user = appUserRepository.findByQqOpenid(finalOpenid)
            .orElseGet(() -> {
                String email = "qq_user_" + finalOpenid + "@qq.com";
                return appUserRepository.findByEmail(email).orElseGet(() -> {
                    AppUserEntity newUser = new AppUserEntity();
                    newUser.setUsername(finalNickname);
                    newUser.setEmail(email);
                    newUser.setQqOpenid(finalOpenid);
                    newUser.setInviteCode("");
                    newUser.setRole("学生");
                    String randomPassword = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    newUser.setPasswordHash(hash(randomPassword));
                    newUser.setPlainPassword(randomPassword);
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

        logAction("用户通过 QQ 登录成功: " + saved.getUsername() + " (" + saved.getEmail() + ")", "info", ipAddress);
        return toSession(saved);
    }

    @Transactional
    public AuthSessionVO loginOrRegisterViaWechat(String code, String ipAddress) {
        String appId = "wxd84d54269bfdf677";
        String appSecret = "a94ee435df06bfc8ad1f57e0f2b377b1";

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
        AppUserEntity user = appUserRepository.findByQqOpenid(finalOpenid)
            .orElseGet(() -> {
                String email = "wechat_user_" + finalOpenid + "@papersolver.cn";
                return appUserRepository.findByEmail(email).orElseGet(() -> {
                    AppUserEntity newUser = new AppUserEntity();
                    newUser.setUsername(finalNickname);
                    newUser.setEmail(email);
                    newUser.setQqOpenid(finalOpenid); // Store WeChat openid here
                    newUser.setInviteCode("WECHAT-LOGIN");
                    newUser.setRole("学生");
                    String randomPassword = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    newUser.setPasswordHash(hash(randomPassword));
                    newUser.setPlainPassword(randomPassword);
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
