package com.paperpilot.server.controller;

import com.paperpilot.server.entity.SiteMessageEntity;
import com.paperpilot.server.repository.SiteMessageRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/site-messages")
public class SiteMessageController {
    private final SiteMessageRepository repository;

    public SiteMessageController(SiteMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/active")
    public List<SiteMessageEntity> activeMessages() {
        return repository.findByActiveFlagTrueOrderByCreatedAtDesc();
    }
}
