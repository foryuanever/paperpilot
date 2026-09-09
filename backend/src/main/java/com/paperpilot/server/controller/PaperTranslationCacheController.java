package com.paperpilot.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.PaperTranslationCacheEntity;
import com.paperpilot.server.repository.PaperTranslationCacheRepository;
import com.paperpilot.server.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/translation-cache")
public class PaperTranslationCacheController {
    private static final int MAX_PAYLOAD_CHARS = 8_000_000;
    private final CurrentUserService currentUserService;
    private final PaperTranslationCacheRepository repository;
    private final ObjectMapper objectMapper;

    public PaperTranslationCacheController(
        CurrentUserService currentUserService,
        PaperTranslationCacheRepository repository,
        ObjectMapper objectMapper
    ) {
        this.currentUserService = currentUserService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{workspaceId}/{mode}")
    public Map<String, Object> get(@PathVariable String workspaceId, @PathVariable String mode) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String safeMode = mode(mode);
        return repository.findByUserIdAndWorkspaceIdAndMode(userId, workspace(workspaceId), safeMode)
            .map(entity -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("found", true);
                result.put("updatedAt", entity.getUpdatedAt());
                try {
                    result.put("payload", objectMapper.readValue(entity.getPayloadJson(), new TypeReference<Map<String, Object>>() {}));
                } catch (Exception ignored) {
                    result.put("payload", Map.of());
                }
                return result;
            })
            .orElseGet(() -> Map.of("found", false));
    }

    @PutMapping("/{workspaceId}/{mode}")
    @Transactional
    public Map<String, Object> put(
        @PathVariable String workspaceId,
        @PathVariable String mode,
        @RequestBody Map<String, Object> body
    ) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String safeWorkspace = workspace(workspaceId);
        String safeMode = mode(mode);
        Object payload = body.getOrDefault("payload", Map.of());
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (json.length() > MAX_PAYLOAD_CHARS) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "译文缓存过大，请分批保存");
            }
            PaperTranslationCacheEntity entity = repository
                .findByUserIdAndWorkspaceIdAndMode(userId, safeWorkspace, safeMode)
                .orElseGet(PaperTranslationCacheEntity::new);
            entity.setUserId(userId);
            entity.setWorkspaceId(safeWorkspace);
            entity.setMode(safeMode);
            entity.setPayloadJson(json);
            repository.save(entity);
            return Map.of("success", true);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "译文缓存格式无效");
        }
    }

    private String workspace(String value) {
        String result = StringUtils.hasText(value) ? value.trim() : "";
        if (!result.matches("[A-Za-z0-9_-]{1,96}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文献工作区编号无效");
        }
        return result;
    }

    private String mode(String value) {
        String result = StringUtils.hasText(value) ? value.trim().toLowerCase() : "";
        if (!"immersive".equals(result) && !"dual".equals(result)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "翻译缓存类型无效");
        }
        return result;
    }
}
