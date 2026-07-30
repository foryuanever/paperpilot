package com.paperpilot.server.controller;

import com.paperpilot.server.dto.ModelConfigRequest;
import com.paperpilot.server.dto.ModelChatRequest;
import com.paperpilot.server.service.ModelConfigService;
import com.paperpilot.server.service.ModelRelayResearchService;
import com.paperpilot.server.service.CurrentUserService;
import com.paperpilot.server.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final CurrentUserService currentUserService;
    private final AuthService authService;

    public ModelController(
        ModelConfigService modelConfigService,
        ModelRelayResearchService modelRelayResearchService,
        CurrentUserService currentUserService,
        AuthService authService
    ) {
        this.modelConfigService = modelConfigService;
        this.modelRelayResearchService = modelRelayResearchService;
        this.currentUserService = currentUserService;
        this.authService = authService;
    }

    @PostMapping
    public ModelConfigVO save(@Valid @RequestBody ModelConfigRequest request, HttpServletRequest httpRequest) {
        currentUserService.requireAdmin();
        ModelConfigVO saved = modelConfigService.save(request);
        authService.logAction("管理员保存 AI 中转配置: " + saved.getProviderName() + " / " + saved.getModelName(), "warn", getClientIp(httpRequest));
        return saved;
    }

    @PostMapping("/test")
    public Map<String, Object> test(@Valid @RequestBody ModelConfigRequest request) {
        currentUserService.requireAdmin();
        return modelConfigService.test(request);
    }

    @PostMapping("/models")
    public Map<String, Object> models(@Valid @RequestBody ModelConfigRequest request) {
        currentUserService.requireAdmin();
        return modelConfigService.fetchModels(request);
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@Valid @RequestBody ModelChatRequest request) {
        currentUserService.requireAdmin();
        return modelConfigService.chat(request.getConfig(), request.getPrompt());
    }

    @GetMapping("/active")
    public ModelConfigVO active(@RequestParam(value = "scene", required = false) String scene) {
        return modelConfigService.getActive(scene);
    }

    @GetMapping("/pool")
    public List<Map<String, Object>> pool(@RequestParam(value = "scene", required = false) String scene) {
        currentUserService.requireAdmin();
        return modelConfigService.getPool(scene);
    }

    @PostMapping("/pool/refresh")
    public List<Map<String, Object>> refreshPool(@RequestParam(value = "scene", required = false) String scene) {
        currentUserService.requireAdmin();
        return modelConfigService.refreshPool(scene);
    }

    @PostMapping("/pool/seed")
    public List<Map<String, Object>> seedPool(@RequestParam(value = "scene", required = false) String scene) {
        currentUserService.requireAdmin();
        return modelConfigService.seedPool(scene);
    }

    @PostMapping("/pool/cleanup")
    public Map<String, Object> cleanupPool(@RequestParam(value = "scene", required = false) String scene, HttpServletRequest request) {
        currentUserService.requireAdmin();
        Map<String, Object> result = modelConfigService.cleanupPool(scene);
        authService.logAction("管理员清理不可用模型节点: " + safeScene(scene), "warn", getClientIp(request));
        return result;
    }

    @PostMapping("/pool/{id}/activate")
    public Map<String, Object> activatePoolRoute(
        @PathVariable("id") Long id,
        @RequestParam(value = "scene", required = false) String scene
    ) {
        currentUserService.requireAdmin();
        return modelConfigService.activatePoolRoute(id, scene);
    }

    @PostMapping("/pool/{id}/assign")
    public Map<String, Object> assignPoolRoute(
        @PathVariable("id") Long id,
        @RequestParam("scene") String scene,
        @RequestParam(value = "enabled", defaultValue = "true") boolean enabled
    ) {
        currentUserService.requireAdmin();
        return modelConfigService.assignPoolRoute(id, scene, enabled);
    }

    @PostMapping("/pool/{id}/assign-model")
    public Map<String, Object> assignPoolModelRoute(
        @PathVariable("id") Long id,
        @RequestParam("modelName") String modelName,
        @RequestParam("scene") String scene,
        @RequestParam(value = "enabled", defaultValue = "true") boolean enabled
    ) {
        currentUserService.requireAdmin();
        return modelConfigService.assignPoolModelRoute(id, modelName, scene, enabled);
    }

    @GetMapping("/pool/{id}/models")
    public Map<String, Object> routeModels(@PathVariable("id") Long id) {
        currentUserService.requireAdmin();
        return modelConfigService.fetchModelsForRoute(id);
    }

    @PostMapping("/pool/{id}/test-model")
    public Map<String, Object> testPoolModel(
        @PathVariable("id") Long id,
        @RequestParam("modelName") String modelName
    ) {
        currentUserService.requireAdmin();
        return modelConfigService.testPoolModel(id, modelName);
    }

    @DeleteMapping("/pool/{id}")
    public Map<String, Object> deletePoolRoute(@PathVariable("id") Long id, HttpServletRequest request) {
        currentUserService.requireAdmin();
        Map<String, Object> result = modelConfigService.deleteModelRoute(id);
        authService.logAction("管理员删除模型池节点: #" + id, "warn", getClientIp(request));
        return result;
    }

    @DeleteMapping("/pool/{id}/relay")
    public Map<String, Object> deleteRelay(@PathVariable("id") Long id, HttpServletRequest request) {
        currentUserService.requireAdmin();
        Map<String, Object> result = modelConfigService.deleteRelay(id);
        authService.logAction("管理员删除 AI 中转站及其模型池: #" + id, "warn", getClientIp(request));
        return result;
    }

    @GetMapping("/relay-research/top")
    public Map<String, Object> relayResearchTop() {
        currentUserService.requireAdmin();
        return modelRelayResearchService.topRelays();
    }

    @PostMapping("/pool/sort")
    public Map<String, Object> sortPoolRoutes(@RequestBody List<Long> ids) {
        currentUserService.requireAdmin();
        return modelConfigService.sortPoolRoutes(ids);
    }

    private String getClientIp(HttpServletRequest request) {
        for (String header : new String[] {"CF-Connecting-IP", "X-Real-IP", "X-Forwarded-For"}) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                return normalizeIp(value.split(",")[0].trim());
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private String normalizeIp(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::ffff:127.0.0.1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    private String safeScene(String scene) {
        return scene == null || scene.isBlank() ? "general" : scene;
    }
}
