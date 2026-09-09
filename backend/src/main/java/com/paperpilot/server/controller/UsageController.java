package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.service.AiUsageService;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.MembershipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
    private final AiUsageService aiUsageService;
    private final MembershipService membershipService;
    private final CurrentUserService currentUserService;

    public UsageController(
        AiUsageService aiUsageService,
        MembershipService membershipService,
        CurrentUserService currentUserService
    ) {
        this.aiUsageService = aiUsageService;
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return aiUsageService.summary();
    }

    @GetMapping("/details")
    public Map<String, Object> details(
        @org.springframework.web.bind.annotation.RequestParam(required = false) String benefit,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String startDate,
        @org.springframework.web.bind.annotation.RequestParam(required = false) String endDate,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "1") int page,
        @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "10") int pageSize
    ) {
        return aiUsageService.details(benefit, startDate, endDate, page, pageSize);
    }

    @PostMapping("/consume")
    public Map<String, Object> consume(@RequestBody Map<String, String> body) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        if (user == null || "Local User".equals(user.getUsername())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "请先登录"
            );
        }
        String action = body.get("action");
        if (action == null || action.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "action 不能为空"
            );
        }
        membershipService.consume(user, action, body);
        return Map.of("success", true);
    }
}
