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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public AuthSessionVO login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return authService.login(request, ip);
    }

    @PostMapping("/register")
    public AuthSessionVO register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        return authService.register(request, ip);
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
}
