package com.paperpilot.server.controller;

import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.PromotionEntity;
import com.paperpilot.server.repository.PromotionRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionRepository promotionRepository;
    private final CurrentUserService currentUserService;

    public PromotionController(PromotionRepository promotionRepository, CurrentUserService currentUserService) {
        this.promotionRepository = promotionRepository;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<PromotionEntity> getMyPromotions() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        return promotionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @PostMapping
    public PromotionEntity submitPromotion(@RequestBody Map<String, String> body) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        String link = body.getOrDefault("promotionLink", "").trim();
        String screenshot = body.getOrDefault("screenshotUrl", "");

        if (link.isBlank() || screenshot.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "推广链接和截图不能为空");
        }

        PromotionEntity promotion = new PromotionEntity();
        promotion.setUser(user);
        promotion.setPromotionLink(link);
        promotion.setScreenshotUrl(screenshot);
        promotion.setStatus("PENDING");

        return promotionRepository.save(promotion);
    }
}
