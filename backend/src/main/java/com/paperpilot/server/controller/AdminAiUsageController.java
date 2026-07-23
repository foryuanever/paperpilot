package com.paperpilot.server.controller;

import com.paperpilot.server.service.AiUsageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai-usage")
public class AdminAiUsageController {
    private final AiUsageService aiUsageService;

    public AdminAiUsageController(AiUsageService aiUsageService) {
        this.aiUsageService = aiUsageService;
    }

    @GetMapping("/calls")
    public Map<String, Object> calls(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "scene", required = false) String scene,
        @RequestParam(value = "model", required = false) String model,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate,
        @RequestParam(value = "page", defaultValue = "1") int page,
        @RequestParam(value = "pageSize", defaultValue = "20") int pageSize
    ) {
        return aiUsageService.adminCalls(keyword, scene, model, status, startDate, endDate, page, pageSize);
    }

    @DeleteMapping("/calls")
    public Map<String, Object> clearCalls() {
        return aiUsageService.clearAdminCalls();
    }
}
