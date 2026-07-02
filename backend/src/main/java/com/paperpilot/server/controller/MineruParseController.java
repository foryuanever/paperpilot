package com.paperpilot.server.controller;

import com.paperpilot.server.service.MineruParseService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/mineru")
public class MineruParseController {

    private final MineruParseService service;

    public MineruParseController(MineruParseService service) {
        this.service = service;
    }

    @PostMapping("/{workspaceId}/parse")
    public Map<String, Object> start(
        @PathVariable String workspaceId,
        @RequestParam(defaultValue = "false") boolean force
    ) {
        return service.start(workspaceId, force);
    }

    @GetMapping("/{workspaceId}/status")
    public Map<String, Object> status(@PathVariable String workspaceId) {
        return service.status(workspaceId);
    }

    @GetMapping("/{workspaceId}/document")
    public Map<String, Object> document(@PathVariable String workspaceId) {
        return service.document(workspaceId);
    }

    @GetMapping("/{workspaceId}/asset")
    public ResponseEntity<byte[]> asset(
        @PathVariable String workspaceId,
        @RequestParam String path
    ) {
        MineruParseService.Asset asset = service.asset(workspaceId, path);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(asset.mediaType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
            .contentType(mediaType)
            .body(asset.bytes());
    }
}
