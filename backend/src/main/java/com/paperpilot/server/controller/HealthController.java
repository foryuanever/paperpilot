package com.paperpilot.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final String BUILD_TAG = "2.2.0";

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "ok", true,
            "service", "PaperSolver Backend",
            "build", BUILD_TAG,
            "pptMasterMode", "official-agent-only",
            "time", Instant.now().toString()
        );
    }
}
