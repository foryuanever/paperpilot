package com.paperpilot.server.controller;

import com.paperpilot.server.dto.TranslateRequest;
import com.paperpilot.server.service.TranslateService;
import com.paperpilot.server.vo.TranslateResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/translate")
public class TranslateController {

    private final TranslateService translateService;

    public TranslateController(TranslateService translateService) {
        this.translateService = translateService;
    }

    @GetMapping("/providers")
    public List<Map<String, String>> providers() {
        return translateService.listProviders();
    }

    @PostMapping
    public TranslateResultVO translate(@Valid @RequestBody TranslateRequest request) {
        return translateService.translate(request);
    }
}
