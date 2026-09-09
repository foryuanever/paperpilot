package com.paperpilot.server.controller;

import com.paperpilot.server.entity.InviteCodeEntity;
import com.paperpilot.server.entity.ReferralRecordEntity;
import com.paperpilot.server.repository.InviteCodeRepository;
import com.paperpilot.server.repository.ReferralRecordRepository;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {
    private static final int REFERRAL_REWARD_POINTS = 15;

    private final CurrentUserService currentUserService;
    private final InviteCodeRepository inviteCodeRepository;
    private final ReferralRecordRepository referralRecordRepository;
    private final AppUserRepository appUserRepository;

    public ReferralController(
            CurrentUserService currentUserService,
            InviteCodeRepository inviteCodeRepository,
            ReferralRecordRepository referralRecordRepository,
            AppUserRepository appUserRepository
    ) {
        this.currentUserService = currentUserService;
        this.inviteCodeRepository = inviteCodeRepository;
        this.referralRecordRepository = referralRecordRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/my-code")
    public Map<String, Object> getMyCode() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        InviteCodeEntity inviteCode = inviteCodeRepository
                .findFirstByReferrerIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .orElseGet(() -> {
                    // Automatically generate a default one
                    String seed = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                    InviteCodeEntity entity = new InviteCodeEntity();
                    entity.setCode(seed);
                    entity.setActive(true);
                    entity.setReferrerId(userId);
                    entity.setCreatedAt(LocalDateTime.now());
                    return inviteCodeRepository.save(entity);
                });

        Map<String, Object> result = new HashMap<>();
        result.put("code", displayCode(inviteCode.getCode()));
        result.put("active", inviteCode.isActive());
        result.put("createdAt", inviteCode.getCreatedAt());
        return result;
    }

    @PostMapping("/create")
    public Map<String, Object> createNewCode() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        // Deactivate old active codes first
        inviteCodeRepository.findFirstByReferrerIdAndActiveTrueOrderByCreatedAtDesc(userId).ifPresent(old -> {
            old.setActive(false);
            inviteCodeRepository.save(old);
        });

        // Create new invite code
        String seed = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        InviteCodeEntity entity = new InviteCodeEntity();
        entity.setCode(seed);
        entity.setActive(true);
        entity.setReferrerId(userId);
        entity.setCreatedAt(LocalDateTime.now());
        inviteCodeRepository.save(entity);

        Map<String, Object> result = new HashMap<>();
        result.put("code", displayCode(entity.getCode()));
        result.put("active", entity.isActive());
        result.put("createdAt", entity.getCreatedAt());
        return result;
    }

    @PostMapping("/delete")
    public void deleteCode() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        inviteCodeRepository.findFirstByReferrerIdAndActiveTrueOrderByCreatedAtDesc(userId).ifPresent(code -> {
            code.setActive(false);
            inviteCodeRepository.save(code);
        });
    }

    @GetMapping("/stats")
    public Map<String, Object> getReferralStats() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        long invitedCount = referralRecordRepository.countByReferrerId(userId);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("registered", invitedCount);
        stats.put("returned", invitedCount);
        stats.put("totalPointsReward", invitedCount * REFERRAL_REWARD_POINTS);
        return stats;
    }

    @GetMapping("/records")
    public List<Map<String, Object>> getReferralRecords() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        List<ReferralRecordEntity> records = referralRecordRepository.findAllByReferrerIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ReferralRecordEntity record : records) {
            Map<String, Object> map = new HashMap<>();
            map.put("time", record.getCreatedAt().format(formatter));
            // Show the QQ nickname first. Older records may not have a nickname,
            // so keep the QQ identifier as a compatibility fallback.
            String displayName = record.getInviteeName();
            if (displayName == null || displayName.isBlank()) {
                displayName = record.getInviteeEmail();
            }
            map.put("user", displayName);
            map.put("qq", record.getInviteeEmail());
            map.put("reward", record.getPointsReward());
            map.put("status", "已发放");
            result.add(map);
        }
        return result;
    }

    private String displayCode(String code) {
        if (code == null) return "";
        return code.trim().replaceFirst("(?i)^INV[-_]", "").toUpperCase(Locale.ROOT);
    }
}
