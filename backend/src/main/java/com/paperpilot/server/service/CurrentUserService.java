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

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public Long getOrCreateDefaultUserId() {
        return getOrCreateDefaultUser().getId();
    }

    @Transactional
    public AppUserEntity getOrCreateDefaultUser() {
        Long requestUserId = extractUserIdFromRequest();
        if (requestUserId != null && appUserRepository.existsById(requestUserId)) {
            return appUserRepository.findById(requestUserId).orElseThrow();
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

    private Long extractUserIdFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String header = request.getHeader("X-PaperPilot-User-Id");
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
