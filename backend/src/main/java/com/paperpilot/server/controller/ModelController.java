package com.paperpilot.server.controller;

import com.paperpilot.server.dto.ModelConfigRequest;
import com.paperpilot.server.dto.ModelChatRequest;
import com.paperpilot.server.service.ModelConfigService;
import com.paperpilot.server.service.ModelRelayResearchService;
import com.paperpilot.server.vo.ModelConfigVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin/model-config")
public class ModelController {

    private final ModelConfigService modelConfigService;
    private final ModelRelayResearchService modelRelayResearchService;

    public ModelController(ModelConfigService modelConfigService, ModelRelayResearchService modelRelayResearchService) {
        this.modelConfigService = modelConfigService;
        this.modelRelayResearchService = modelRelayResearchService;
    }

    @PostMapping
    public ModelConfigVO save(@Valid @RequestBody ModelConfigRequest request) {
        return modelConfigService.save(request);
    }

    @PostMapping("/test")
    public Map<String, Object> test(@Valid @RequestBody ModelConfigRequest request) {
        return modelConfigService.test(request);
    }

    @PostMapping("/models")
    public Map<String, Object> models(@Valid @RequestBody ModelConfigRequest request) {
        return modelConfigService.fetchModels(request);
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@Valid @RequestBody ModelChatRequest request) {
        return modelConfigService.chat(request.getConfig(), request.getPrompt());
    }

    @GetMapping("/active")
    public ModelConfigVO active(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.getActive(scene);
    }

    @GetMapping("/pool")
    public List<Map<String, Object>> pool(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.getPool(scene);
    }

    @PostMapping("/pool/refresh")
    public List<Map<String, Object>> refreshPool(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.refreshPool(scene);
    }

    @PostMapping("/pool/seed")
    public List<Map<String, Object>> seedPool(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.seedPool(scene);
    }

    @PostMapping("/pool/cleanup")
    public Map<String, Object> cleanupPool(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.cleanupPool(scene);
    }

    @PostMapping("/pool/{id}/activate")
    public Map<String, Object> activatePoolRoute(
        @PathVariable("id") Long id,
        @RequestParam(value = "scene", required = false) String scene
    ) {
        return modelConfigService.activatePoolRoute(id, scene);
    }

    @PostMapping("/pool/{id}/assign")
    public Map<String, Object> assignPoolRoute(
        @PathVariable("id") Long id,
        @RequestParam("scene") String scene,
        @RequestParam(value = "enabled", defaultValue = "true") boolean enabled
    ) {
        return modelConfigService.assignPoolRoute(id, scene, enabled);
    }

    @PostMapping("/pool/{id}/assign-model")
    public Map<String, Object> assignPoolModelRoute(
        @PathVariable("id") Long id,
        @RequestParam("modelName") String modelName,
        @RequestParam("scene") String scene,
        @RequestParam(value = "enabled", defaultValue = "true") boolean enabled
    ) {
        return modelConfigService.assignPoolModelRoute(id, modelName, scene, enabled);
    }

    @GetMapping("/pool/{id}/models")
    public Map<String, Object> routeModels(@PathVariable("id") Long id) {
        return modelConfigService.fetchModelsForRoute(id);
    }

    @PostMapping("/pool/{id}/test-model")
    public Map<String, Object> testPoolModel(
        @PathVariable("id") Long id,
        @RequestParam("modelName") String modelName
    ) {
        return modelConfigService.testPoolModel(id, modelName);
    }

    @DeleteMapping("/pool/{id}")
    public Map<String, Object> deletePoolRoute(@PathVariable("id") Long id) {
        return modelConfigService.deleteModelRoute(id);
    }

    @DeleteMapping("/pool/{id}/relay")
    public Map<String, Object> deleteRelay(@PathVariable("id") Long id) {
        return modelConfigService.deleteRelay(id);
    }

    @GetMapping("/relay-research/top")
    public Map<String, Object> relayResearchTop() {
        return modelRelayResearchService.topRelays();
    }
}
