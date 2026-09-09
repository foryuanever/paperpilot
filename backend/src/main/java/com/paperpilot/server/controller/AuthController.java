package com.paperpilot.server.controller;

import com.paperpilot.server.dto.LoginRequest;
import com.paperpilot.server.dto.RegisterRequest;
import com.paperpilot.server.dto.ChangePasswordRequest;
import com.paperpilot.server.service.AuthService;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.vo.AuthSessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Deque;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final ConcurrentHashMap<String, Deque<Long>> loginAttempts = new ConcurrentHashMap<>();
    private static final int LOGIN_ATTEMPT_LIMIT = 10;
    private static final long LOGIN_WINDOW_MS = 10 * 60 * 1000L;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public AuthSessionVO login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        checkLoginRate(ip, request.getEmail());
        return authService.login(request, ip);
    }

    private void checkLoginRate(String ip, String email) {
        long now = System.currentTimeMillis();
        String key = (ip == null ? "unknown" : ip) + "|" + (email == null ? "" : email.trim().toLowerCase());
        Deque<Long> attempts = loginAttempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst() < now - LOGIN_WINDOW_MS) attempts.removeFirst();
            if (attempts.size() >= LOGIN_ATTEMPT_LIMIT) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁，请 10 分钟后重试");
            }
            attempts.addLast(now);
        }
    }

    @PostMapping("/register")
    public AuthSessionVO register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return authService.register(request, ip);
    }

    @PostMapping("/register/send-code")
    public void sendRegisterCode(@org.springframework.web.bind.annotation.RequestParam String email) {
        authService.sendRegisterVerificationCode(email);
    }

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
    }

    @PatchMapping("/profile")
    public AuthSessionVO updateProfile(@RequestBody Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        return authService.updateProfile(userId, body);
    }

    @PostMapping("/forgot-password/send-code")
    public void sendVerificationCode(@org.springframework.web.bind.annotation.RequestParam String email) {
        authService.sendVerificationCode(email);
    }

    @PostMapping("/forgot-password/reset")
    public void resetPasswordWithCode(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordWithCode(request.getEmail(), request.getCode(), request.getNewPassword());
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

    public static class ResetPasswordRequest {
        @jakarta.validation.constraints.Email
        @jakarta.validation.constraints.NotBlank
        private String email;

        @jakarta.validation.constraints.NotBlank
        private String code;

        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(min = 6)
        private String newPassword;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
    @org.springframework.web.bind.annotation.GetMapping("/qq/callback")
    public void qqCallback(
            @org.springframework.web.bind.annotation.RequestParam("code") String code,
            @org.springframework.web.bind.annotation.RequestParam(value = "state", required = false) String state,
            jakarta.servlet.http.HttpServletResponse response,
            HttpServletRequest httpRequest) throws java.io.IOException {
        String ip = getClientIp(httpRequest);
        DesktopLocalOAuthCallback localCallback = parseDesktopLocalOAuthCallback(state);
        boolean desktopBrowserFlow = isDesktopBrowserFlow(state);
        String authState = localCallback != null
            ? localCallback.authState()
            : (desktopBrowserFlow ? state.substring("desktop_external_".length()) : state);
        try {
            AuthSessionVO session = authService.loginOrRegisterViaQQ(code, authState, ip);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // The OAuth result travels through a Location header. User-uploaded avatar
            // and background images can be multi-megabyte data URLs, so never put them
            // in that redirect. The client refreshes the complete profile after login.
            String json = mapper.writeValueAsString(compactOAuthSession(session));
            String base64 = java.util.Base64.getUrlEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            redirectOauth(response, "qqSession", base64, desktopBrowserFlow, localCallback);
        } catch (Exception e) {
            log.error("QQ OAuth callback failed: ip={}, state={}", ip, redactOauthState(state), e);
            redirectOauth(response, "error", oauthErrorMessage(e), desktopBrowserFlow, localCallback);
        }
    }
    @org.springframework.web.bind.annotation.GetMapping("/wechat/callback")
    public void wechatCallback(
            @org.springframework.web.bind.annotation.RequestParam("code") String code,
            @org.springframework.web.bind.annotation.RequestParam(value = "state", required = false) String state,
            jakarta.servlet.http.HttpServletResponse response,
            HttpServletRequest httpRequest) throws java.io.IOException {
        String ip = getClientIp(httpRequest);
        try {
            AuthSessionVO session = authService.loginOrRegisterViaWechat(code, ip);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(compactOAuthSession(session));
            String base64 = java.util.Base64.getUrlEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            redirectOauth(response, "qqSession", base64);
        } catch (Exception e) {
            log.error("WeChat OAuth callback failed: ip={}", ip, e);
            redirectOauth(response, "error", oauthErrorMessage(e));
        }
    }

    private AuthSessionVO compactOAuthSession(AuthSessionVO session) {
        AuthSessionVO compact = new AuthSessionVO(
            session.getUserId(),
            session.getName(),
            session.getEmail(),
            session.getInviteCode(),
            session.getRole(),
            "",
            "",
            session.getFruitScore(),
            session.getSchoolName(),
            session.isCampusVerified(),
            session.getQq(),
            session.getWechat(),
            session.getQqOpenid(),
            session.getRegisterTime(),
            session.getNumericId()
        );
        compact.setCheckinScore(session.getCheckinScore());
        compact.setAccessToken(session.getAccessToken());
        compact.setNewUser(session.isNewUser());
        return compact;
    }

    private boolean isDesktopBrowserFlow(String state) {
        return state != null && state.startsWith("desktop_external_");
    }

    private DesktopLocalOAuthCallback parseDesktopLocalOAuthCallback(String state) {
        if (state == null || !state.startsWith("desktop_local_")) return null;
        String[] parts = state.split("_", 5);
        if (parts.length != 5) return null;
        try {
            int port = Integer.parseInt(parts[2]);
            String token = parts[3];
            if (port < 1024 || port > 65535 || !token.matches("[a-fA-F0-9]{32}")) return null;
            String authState = new String(java.util.Base64.getUrlDecoder().decode(parts[4]), java.nio.charset.StandardCharsets.UTF_8);
            if (!authState.startsWith("papersolver_")) return null;
            return new DesktopLocalOAuthCallback(port, token, authState);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void redirectOauth(jakarta.servlet.http.HttpServletResponse response, String name, String value) throws java.io.IOException {
        redirectOauth(response, name, value, false);
    }

    private void redirectOauth(jakarta.servlet.http.HttpServletResponse response, String name, String value, boolean desktopBrowserFlow) throws java.io.IOException {
        redirectOauth(response, name, value, desktopBrowserFlow, null);
    }

    private void redirectOauth(
            jakarta.servlet.http.HttpServletResponse response,
            String name,
            String value,
            boolean desktopBrowserFlow,
            DesktopLocalOAuthCallback localCallback) throws java.io.IOException {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        String encodedValue = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        if (localCallback != null) {
            response.sendRedirect("http://127.0.0.1:" + localCallback.port() + "/oauth?token="
                + localCallback.token() + "&" + name + "=" + encodedValue);
            return;
        }
        String target = desktopBrowserFlow ? "papersolver://oauth?" : "https://papersolver.cn/?";
        response.sendRedirect(target + name + "=" + encodedValue);
    }

    private record DesktopLocalOAuthCallback(int port, String token, String authState) {}

    private String redactOauthState(String state) {
        if (state == null || state.isBlank()) return "";
        return state.length() <= 16 ? "[present]" : state.substring(0, 12) + "...";
    }

    private String oauthErrorMessage(Exception e) {
        if (e instanceof ResponseStatusException responseStatusException) {
            String reason = responseStatusException.getReason();
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return "第三方登录暂时不可用，请稍后重试";
    }
}
