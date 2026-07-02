package com.paperpilot.server.controller;

import com.paperpilot.server.service.AiUsageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
    private final AiUsageService aiUsageService;

    public UsageController(AiUsageService aiUsageService) {
        this.aiUsageService = aiUsageService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return aiUsageService.summary();
    }
}
