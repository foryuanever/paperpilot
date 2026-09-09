package com.paperpilot.server.controller;

import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/translation-issues")
public class TranslationIssueController {

    private final TranslationRecordRepository translationRecordRepository;
    private final CurrentUserService currentUserService;

    public TranslationIssueController(
        TranslationRecordRepository translationRecordRepository,
        CurrentUserService currentUserService
    ) {
        this.translationRecordRepository = translationRecordRepository;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/report")
    public Map<String, Object> report(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        TranslationRecordEntity record = new TranslationRecordEntity();
        record.setPaperTitle(clip(asString(body.get("paperTitle"), null), 512));
        record.setTranslationMode(clip(asString(body.get("translationMode"), null), 64));
        record.setUserId(currentUserService.getOrCreateDefaultUserId());
        record.setProvider(clip(asString(body.get("provider"), "unknown"), 64));
        record.setRoute(clip(asString(body.get("route"), null), 128));
        record.setSourceLang(clip(asString(body.get("sourceLang"), null), 32));
        record.setTargetLang(clip(asString(body.get("targetLang"), null), 32));
        record.setClientType(clip(asString(body.get("clientType"), "desktop"), 64));
        record.setCharCount(Math.max(0L, asLong(body.get("charCount"), 0L)));
        record.setLatencyMs(Math.max(0L, asLong(body.get("latencyMs"), 0L)));
        record.setSuccess(Boolean.TRUE.equals(body.get("success")));
        record.setErrorMessage(clip(asString(body.get("errorMessage"), null), 1000));
        record.setNetworkProfile(clip(asString(body.get("networkProfile"), null), 512));
        record.setIpAddress(clip(clientIp(request), 128));
        record.setUserAgent(clip(request.getHeader("User-Agent"), 255));
        translationRecordRepository.save(record);
        return Map.of("success", true, "id", record.getId());
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private long asLong(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String clip(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
