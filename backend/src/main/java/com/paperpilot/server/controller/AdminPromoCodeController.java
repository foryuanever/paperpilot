package com.paperpilot.server.controller;

import com.paperpilot.server.entity.PromoCodeEntity;
import com.paperpilot.server.repository.PromoCodeRepository;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.MembershipService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/admin/promo-code")
public class AdminPromoCodeController {

    private final PromoCodeRepository promoCodeRepository;
    private final CurrentUserService currentUserService;
    private final MembershipService membershipService;
    private final SecureRandom random = new SecureRandom();

    public AdminPromoCodeController(
        PromoCodeRepository promoCodeRepository,
        CurrentUserService currentUserService,
        MembershipService membershipService
    ) {
        this.promoCodeRepository = promoCodeRepository;
        this.currentUserService = currentUserService;
        this.membershipService = membershipService;
    }

    @ModelAttribute
    public void requireAdminAccess() {
        currentUserService.requireAdmin();
    }

    @PostConstruct
    public void loadPromoCodesBackup() {
        java.io.File file = new java.io.File("promo_codes_backup.json");
        if (!file.exists()) {
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            List<Map<String, Object>> list = mapper.readValue(file, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> map : list) {
                String code = (String) map.get("code");
                if (code == null || code.isBlank() || promoCodeRepository.findByCode(code).isPresent()) {
                    continue;
                }
                PromoCodeEntity entity = new PromoCodeEntity();
                entity.setCode(code);
                entity.setPlanId((String) map.get("planId"));
                entity.setPlanCycle((String) map.get("planCycle"));
                entity.setUsed(Boolean.TRUE.equals(map.get("used")));
                entity.setMaxUses(0);
                if (map.get("usedCount") instanceof Number usedCount) {
                    entity.setUsedCount(Math.max(0, usedCount.intValue()));
                } else if (Boolean.TRUE.equals(map.get("used"))) {
                    entity.setUsedCount(1);
                }
                if (map.get("usedByUserId") != null) {
                    entity.setUsedByUserId(((Number) map.get("usedByUserId")).longValue());
                }
                if (map.get("usedAt") != null) {
                    entity.setUsedAt(LocalDateTime.parse((String) map.get("usedAt")));
                }
                if (map.get("createdAt") != null) {
                    entity.setCreatedAt(LocalDateTime.parse((String) map.get("createdAt")));
                }
                promoCodeRepository.save(entity);
            }
        } catch (Exception e) {
            System.err.println("Failed to load promo codes backup: " + e.getMessage());
        }
    }

    @PostConstruct
    public void normalizeExistingPromoCodeLimits() {
        promoCodeRepository.findAll().forEach(code -> {
            boolean changed = false;
            if (!Integer.valueOf(0).equals(code.getMaxUses())) {
                code.setMaxUses(0);
                changed = true;
            }
            if (Boolean.TRUE.equals(code.getUsed()) || (code.getUsedCount() != null && code.getUsedCount() > 0)) {
                if (!Boolean.TRUE.equals(code.getUsed())) {
                    code.setUsed(true);
                    changed = true;
                }
                if (code.getUsedCount() == null || code.getUsedCount() < 1) {
                    code.setUsedCount(1);
                    changed = true;
                }
            }
            if (changed) promoCodeRepository.save(code);
        });
    }

    @PostMapping("/generate")
    public Map<String, Object> generatePromoCode(@RequestBody Map<String, Object> body) {
        String planId = String.valueOf(body.getOrDefault("planId", "")).trim();
        if (planId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "套餐ID不能为空");
        }
        membershipService.assertPurchasablePlan(planId);
        String code = null;
        for (int attempts = 0; attempts < 50; attempts += 1) {
            String candidate = generateRandomCode();
            if (promoCodeRepository.findByCode(candidate).isEmpty()) {
                code = candidate;
                break;
            }
        }
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "生成兑换码失败，请重试");
        }
        PromoCodeEntity entity = new PromoCodeEntity();
        entity.setCode(code);
        entity.setPlanId(planId);
        entity.setPlanCycle(String.valueOf(body.getOrDefault("planCycle", "monthly")));
        int maxUses = 0;
        entity.setMaxUses(maxUses);
        entity.setUsedCount(0);
        promoCodeRepository.save(entity);
        AdminController.saveBackup(promoCodeRepository);
        return Map.of("success", true, "id", entity.getId(), "code", code, "planId", planId, "maxUses", "∞", "invalidated", false);
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listPromoCodes() {
        return promoCodeRepository.findAll().stream()
            .map(code -> {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("id", code.getId());
                row.put("code", code.getCode());
                row.put("planId", code.getPlanId());
                row.put("planCycle", code.getPlanCycle());
                row.put("used", code.getUsed());
                row.put("invalidated", Boolean.TRUE.equals(code.getInvalidated()));
                // beta.91 used `maxUses || 1`; a numeric zero was therefore shown as 1/1.
                row.put("maxUses", "∞");
                row.put("usedCount", code.getUsedCount() != null ? code.getUsedCount() : 0);
                row.put("usedByUserId", code.getUsedByUserId());
                row.put("usedAt", code.getUsedAt());
                row.put("createdAt", code.getCreatedAt());
                return row;
            })
            .toList();
    }

    @PostMapping("/{id}/invalidate")
    public Map<String, Object> invalidatePromoCode(@org.springframework.web.bind.annotation.PathVariable Long id) {
        PromoCodeEntity code = promoCodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "兑换码不存在"));
        code.setInvalidated(true);
        promoCodeRepository.save(code);
        AdminController.saveBackup(promoCodeRepository);
        return Map.of("success", true, "id", id, "invalidated", true);
    }

    private String generateRandomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i += 1) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
