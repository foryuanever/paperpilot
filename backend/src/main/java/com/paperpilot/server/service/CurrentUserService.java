package com.paperpilot.server.service;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CurrentUserService {

    private static final String DEFAULT_EMAIL = "local@paperpilot.app";

    private final AppUserRepository appUserRepository;
    private final SessionTokenService sessionTokenService;

    public CurrentUserService(AppUserRepository appUserRepository, SessionTokenService sessionTokenService) {
        this.appUserRepository = appUserRepository;
        this.sessionTokenService = sessionTokenService;
    }

    @Transactional
    public Long getOrCreateDefaultUserId() {
        return getOrCreateDefaultUser().getId();
    }

    @Transactional
    public AppUserEntity getOrCreateDefaultUser() {
        Long requestUserId = extractAuthenticatedUserId();
        if (requestUserId != null) {
            return appUserRepository.findById(requestUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
        }
        if (hasHttpRequest()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录后继续");
        }
        return appUserRepository.findByEmail(DEFAULT_EMAIL)
            .orElseGet(() -> {
                AppUserEntity user = new AppUserEntity();
                user.setUsername("Local User");
                user.setEmail(DEFAULT_EMAIL);
                user.setInviteCode("LOCAL-SEED");
                user.setPasswordHash("local-only");
                return appUserRepository.save(user);
            });
    }

    @Transactional
    public AppUserEntity requireAdmin() {
        AppUserEntity user = getOrCreateDefaultUser();
        if (!"管理员".equals(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅管理员可配置全局 AI 模型");
        }
        return user;
    }

    public boolean isCurrentSessionImpersonated() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("X-PaperPilot-Session");
        return sessionTokenService.isImpersonated(token);
    }

    private Long extractAuthenticatedUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return sessionTokenService.verify(request.getHeader("X-PaperPilot-Session")).orElse(null);
    }

    private boolean hasHttpRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
    }
}
