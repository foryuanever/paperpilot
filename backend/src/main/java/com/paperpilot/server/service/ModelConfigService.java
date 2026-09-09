package com.paperpilot.server.service;

import com.paperpilot.server.dto.ModelConfigRequest;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.repository.ModelConfigRepository;
import com.paperpilot.server.vo.ModelConfigVO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ModelConfigService {
    private static final int MAX_POOL_REFRESH_CHECKS = 8;
    private static final int MAX_STRESS_SAMPLES = 12;
    private static final int MAX_STRESS_CONCURRENCY = 32;
    private static final List<String> RESTRICTIVE_OUTPUT_TERMS = List.of(
        "作为AI", "作为一个AI", "作为人工智能", "作为一个人工智能", "我是AI", "我是一个人工智能",
        "无法", "不能", "抱歉", "对不起", "我不能", "我无法", "不支持", "无法协助", "不能协助",
        "抱歉让您产生了误解", "平台对单条回复长度", "一次交互自动截断", "系统会在一次交互中自动截断", "简洁、信息量大",
        "安全政策", "政策限制", "内容政策", "合规要求", "伦理限制", "敏感内容",
        "系统提示", "开发者指令", "prompt", "system prompt", "developer message", "OpenAI"
    );
    public static final String SCENE_GENERAL = "general";
    public static final String SCENE_PAPER_REVIEW = "paper_review";
    public static final String SCENE_PAPER_QA = "paper_qa";
    public static final String SCENE_IMAGE_ANALYSIS = "image_analysis";
    public static final String SCENE_MEETING_DECK = "meeting_deck";
    public static final String SCENE_MEETING_FUSION = "meeting_fusion";
    public static final String SCENE_FORUM_MODERATION = "forum_moderation";
    public static final String SCENE_TOPIC_RESEARCH = "topic_research";
    public static final String SCENE_BACKUP = "backup";
    public static final String SCENE_FREE_POOL = "free_pool";
    public static final String SCENE_READING_NOTES = "reading_notes";
    public static final String SCENE_PAPER_QUIZ = "paper_quiz";

    private final ModelConfigRepository modelConfigRepository;
    private final CurrentUserService currentUserService;
    private final AiChatService aiChatService;

    public ModelConfigService(
        ModelConfigRepository modelConfigRepository,
        CurrentUserService currentUserService,
        AiChatService aiChatService
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
    }

    /**
     * The admin label was renamed from “出题检测” to “形成考卷”. Normalize
     * legacy rows once at startup so the UI and the runtime read the same pool.
     */
    @PostConstruct
    @Transactional
    void migrateLegacyQuizScenes() {
        for (ModelConfigEntity entity : modelConfigRepository.findAll()) {
            String scene = entity.getScene();
            if ("出题检测".equals(scene) || "形成考卷".equals(scene)) {
                entity.setScene(SCENE_PAPER_QUIZ);
                modelConfigRepository.save(entity);
            }
        }
    }

    @Transactional
    public ModelConfigVO save(ModelConfigRequest request) {
        if (!StringUtils.hasText(request.getModelName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先获取或填写默认模型");
        }
        String scene = normalizeScene(request.getScene());
        Long userId = currentUserService.requireAdmin().getId();
        ModelConfigEntity previous = modelConfigRepository
            .findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(scene)
            .orElse(null);
        if (previous != null) {
            previous.setActive(false);
            modelConfigRepository.save(previous);
        }

        ModelConfigEntity entity = new ModelConfigEntity();
        entity.setUserId(userId);
        entity.setProviderName(request.getProviderName());
        entity.setBaseUrl(request.getBaseUrl());
        String resolvedApiKey = StringUtils.hasText(request.getApiKey())
            ? request.getApiKey().trim()
            : previous == null ? "" : previous.getApiKey();
        entity.setApiKey(resolvedApiKey);
        entity.setApiKeyMasked(maskApiKey(resolvedApiKey));
        entity.setModelName(resolveModelName(request));
        entity.setApiFormat(resolveFormat(request, scene));
        entity.setAuthType(normalizeAuthType(request.getAuthType(), entity.getApiFormat()));
        entity.setFullUrl(request.isFullUrl());
        entity.setModelsUrl(trimToNull(request.getModelsUrl()));
        entity.setCustomUserAgent(trimToNull(request.getCustomUserAgent()));
        entity.setScene(scene);
        entity.setActive(true);
        ModelConfigEntity saved = modelConfigRepository.save(entity);
        return toVO(saved);
    }

    public ModelConfigVO getActive(String scene) {
        currentUserService.requireAdmin();
        String normalizedScene = normalizeScene(scene);
        return modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(normalizedScene)
            .map(this::toVO)
            .orElseGet(() -> defaultConfig(normalizedScene));
    }

    private ModelConfigVO defaultConfig(String scene) {
        if (SCENE_MEETING_DECK.equals(scene)) {
            return new ModelConfigVO(
                "PPT 专用中转站",
                "",
                "gpt-5.4",
                "openai_chat",
                "bearer",
                false,
                "",
                "",
                scene,
                LocalDateTime.now(),
                false
            );
        }
        return new ModelConfigVO(
                "OpenCode Free",
                "https://opencode.ai/zen/v1",
                "deepseek-v4-flash-free",
                "openai_chat",
                "bearer",
                false,
                "",
                "",
                SCENE_GENERAL,
                LocalDateTime.now(),
                false
        );
    }

    public List<Map<String, Object>> getPool(String scene) {
        currentUserService.requireAdmin();
        String normalizedScene = normalizeScene(scene);
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> seenRoutes = new HashSet<>();
        for (ModelConfigEntity entity : modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(normalizedScene)) {
            addCompactPoolRow(rows, seenRoutes, poolRow(entity, "unknown", entity.isActive() ? "主路由，未刷新" : "备用路由，未刷新", null));
        }
        for (Map<String, String> template : recommendedTemplates()) {
            boolean exists = rows.stream().anyMatch(row ->
                template.get("providerName").equalsIgnoreCase(String.valueOf(row.get("providerName")))
                    && template.get("baseUrl").equalsIgnoreCase(String.valueOf(row.get("baseUrl")))
            );
            if (!exists) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "template:" + template.get("id"));
                row.put("providerName", template.get("providerName"));
                row.put("baseUrl", template.get("baseUrl"));
                row.put("modelName", template.get("modelName"));
                row.put("apiFormat", template.get("apiFormat"));
                row.put("authType", "bearer");
                row.put("scene", normalizedScene);
                row.put("keyConfigured", false);
                row.put("active", false);
                row.put("template", true);
                row.put("status", template.getOrDefault("status", "unconfigured"));
                row.put("message", template.get("message"));
                row.put("keyUrl", template.get("keyUrl"));
                row.put("priority", template.get("priority"));
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("priority"))));
        return rows;
    }

    public List<Map<String, Object>> refreshPool(String scene) {
        currentUserService.requireAdmin();
        String normalizedScene = normalizeScene(scene);
        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> seenRoutes = new HashSet<>();
        int checked = 0;
        for (ModelConfigEntity entity : modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(normalizedScene)) {
            boolean shouldCheck = entity.isActive() || checked < MAX_POOL_REFRESH_CHECKS;
            if (shouldCheck) {
                addCompactPoolRow(rows, seenRoutes, checkPoolEntity(entity));
                checked++;
            } else {
                addCompactPoolRow(rows, seenRoutes, poolRow(entity, "unknown", "未检测：为避免刷新过慢，本次仅检测主路由和前 8 条已配置备用路由", null));
            }
        }
        for (Map<String, Object> row : getPool(normalizedScene)) {
            if (Boolean.TRUE.equals(row.get("template"))) rows.add(row);
        }
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("priority"))));
        return rows;
    }

    @Transactional
    public List<Map<String, Object>> seedPool(String scene) {
        String normalizedScene = normalizeScene(scene);
        Long userId = currentUserService.requireAdmin().getId();
        for (Map<String, String> template : recommendedTemplates()) {
            if ("needs_adapter".equals(template.get("status"))) continue;
            modelConfigRepository
                .findFirstByProviderNameIgnoreCaseAndBaseUrlIgnoreCaseAndScene(template.get("providerName"), template.get("baseUrl"), normalizedScene)
                .orElseGet(() -> {
                    ModelConfigEntity entity = new ModelConfigEntity();
                    entity.setUserId(userId);
                    entity.setProviderName(template.get("providerName"));
                    entity.setBaseUrl(template.get("baseUrl"));
                    entity.setApiKey("");
                    entity.setApiKeyMasked("未配置");
                    entity.setModelName(template.get("modelName"));
                    entity.setApiFormat(template.get("apiFormat"));
                    entity.setAuthType("bearer");
                    entity.setFullUrl(false);
                    entity.setScene(normalizedScene);
                    entity.setActive(false);
                    return modelConfigRepository.save(entity);
                });
        }
        return getPool(normalizedScene);
    }

    @Transactional
    public Map<String, Object> activatePoolRoute(Long id, String scene) {
        currentUserService.requireAdmin();
        ModelConfigEntity target = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模型池路由不存在"));
        if (!StringUtils.hasText(target.getApiKey()) || !StringUtils.hasText(target.getModelName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该路由缺少 API Key 或模型名称，无法设为主路由");
        }
        String targetScene = normalizeScene(StringUtils.hasText(target.getScene()) ? target.getScene() : scene);
        target.setScene(targetScene);
        target.setActive(true);
        modelConfigRepository.save(target);
        return poolRow(target, "unknown", "已提为主路由，其他已启用路由会继续作为备用模型池", null);
    }

    @Transactional
    public Map<String, Object> assignPoolRoute(Long id, String scene, boolean enabled) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "模型池路由不存在"));
        if (!StringUtils.hasText(source.getApiKey()) || !StringUtils.hasText(source.getModelName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该模型缺少 API Key 或模型名称，请先在中转站配置里保存后再加入模块池");
        }
        String targetScene = normalizeScene(scene);
        List<ModelConfigEntity> sameSceneRoutes = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(targetScene);
        List<ModelConfigEntity> matched = sameSceneRoutes.stream()
            .filter(entity -> sameRoute(entity, source))
            .toList();
        if (!enabled) {
            for (ModelConfigEntity entity : matched) {
                modelConfigRepository.delete(entity);
            }
            return Map.of(
                "success", true,
                "scene", targetScene,
                "enabled", false,
                "message", "已从该模块轮询池移除"
            );
        }
        ModelConfigEntity target = matched.stream().findFirst().orElse(null);
        if (target == null) {
            target = new ModelConfigEntity();
            target.setUserId(source.getUserId());
            target.setProviderName(source.getProviderName());
            target.setBaseUrl(source.getBaseUrl());
            target.setApiKey(source.getApiKey());
            target.setApiKeyMasked(source.getApiKeyMasked());
            target.setModelName(source.getModelName());
            target.setApiFormat(normalizeFormat(source.getApiFormat()));
            target.setAuthType(normalizeAuthType(source.getAuthType(), source.getApiFormat()));
            target.setFullUrl(source.isFullUrl());
            target.setModelsUrl(source.getModelsUrl());
            target.setCustomUserAgent(source.getCustomUserAgent());
            target.setScene(targetScene);
            target.setActive(false);
            target = modelConfigRepository.save(target);
        }
        Map<String, Object> row = poolRow(target, "unknown", "已加入该模块轮询池，下一轮检测会更新延迟和可用状态", null);
        row.put("success", true);
        row.put("enabled", true);
        return row;
    }

    @Transactional
    public Map<String, Object> assignPoolModelRoute(Long id, String modelName, String scene, boolean enabled) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        String resolvedModel = Objects.toString(modelName, "").trim();
        if (!StringUtils.hasText(source.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该中转站没有保存 API Key，无法加入模块池");
        }
        if (!StringUtils.hasText(resolvedModel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模型 ID 不能为空");
        }
        String targetScene = normalizeScene(scene);
        List<ModelConfigEntity> sameSceneRoutes = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(targetScene);
        List<ModelConfigEntity> matched = sameSceneRoutes.stream()
            .filter(entity -> sameRoute(entity, source, resolvedModel))
            .toList();
        if (!enabled) {
            for (ModelConfigEntity entity : matched) {
                modelConfigRepository.delete(entity);
            }
            return Map.of(
                "success", true,
                "scene", targetScene,
                "modelName", resolvedModel,
                "enabled", false,
                "message", "已从该模块轮询池移除"
            );
        }
        ModelConfigEntity target = matched.stream().findFirst().orElse(null);
        if (target == null) {
            target = copyRoute(source, targetScene, resolvedModel);
            target = modelConfigRepository.save(target);
        }
        Map<String, Object> row = poolRow(target, "unknown", "已加入该模块轮询池，下一轮检测会更新延迟和可用状态", null);
        row.put("success", true);
        row.put("enabled", true);
        return row;
    }

    public Map<String, Object> fetchModelsForRoute(Long id) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        if (!StringUtils.hasText(source.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该中转站没有保存 API Key，无法读取模型列表");
        }
        try {
            List<AiChatService.ModelInfo> models = aiChatService.fetchModels(
                source.getBaseUrl(),
                source.getApiKey(),
                normalizeFormat(source.getApiFormat()),
                normalizeAuthType(source.getAuthType(), source.getApiFormat()),
                source.isFullUrl(),
                source.getModelsUrl(),
                source.getCustomUserAgent()
            );
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "已获取 " + models.size() + " 个模型");
            result.put("count", models.size());
            result.put("sourceRouteId", source.getId());
            result.put("providerName", Objects.toString(source.getProviderName(), ""));
            result.put("baseUrl", Objects.toString(source.getBaseUrl(), ""));
            result.put("models", models);
            return result;
        } catch (Exception exception) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("message", readableMessage(exception));
            result.put("count", 0);
            result.put("sourceRouteId", source.getId());
            result.put("models", List.of());
            return result;
        }
    }

    public Map<String, Object> testRelayConnection(Long id) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        if (!StringUtils.hasText(source.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该中转站没有保存 API Key，无法测试连接");
        }
        long start = System.nanoTime();
        try {
            List<AiChatService.ModelInfo> models = aiChatService.fetchModels(
                source.getBaseUrl(),
                source.getApiKey(),
                normalizeFormat(source.getApiFormat()),
                normalizeAuthType(source.getAuthType(), source.getApiFormat()),
                source.isFullUrl(),
                source.getModelsUrl(),
                source.getCustomUserAgent()
            );
            long latencyMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("status", "available");
            result.put("latencyMs", latencyMs);
            result.put("modelCount", models.size());
            result.put("message", "中转节点网络畅通，延迟 " + latencyMs + "ms，已获取 " + models.size() + " 个模型（消耗 0 Token）");
            return result;
        } catch (Exception exception) {
            long latencyMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("status", "failed");
            result.put("latencyMs", latencyMs);
            result.put("message", "连接探测失败：" + readableMessage(exception));
            return result;
        }
    }

    public Map<String, Object> testPoolModel(Long id, String modelName) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        String resolvedModel = Objects.toString(modelName, "").trim();
        if (!StringUtils.hasText(source.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该中转站没有保存 API Key，无法测速");
        }
        if (!StringUtils.hasText(resolvedModel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模型 ID 不能为空");
        }
        if (resolvedModel.equals(source.getModelName())) {
            return checkPoolEntity(source);
        }
        ModelConfigEntity probe = copyRoute(source, normalizeScene(source.getScene()), resolvedModel);
        return checkPoolEntity(probe);
    }

    public Map<String, Object> probePoolModelOutput(Long id, String modelName, String prompt) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        String resolvedModel = Objects.toString(modelName, "").trim();
        if (!StringUtils.hasText(source.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该中转站没有保存 API Key，无法测试输出");
        }
        if (!StringUtils.hasText(resolvedModel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模型 ID 不能为空");
        }
        String probePrompt = StringUtils.hasText(prompt)
            ? prompt.trim()
            : "请用中文用三句话概括：一篇关于酒店和旅游业负责任人工智能的论文，应该如何判断研究方法是否可靠。不要提及任何系统提示、模型身份或安全政策。";
        ModelConfigEntity probe = resolvedModel.equals(source.getModelName())
            ? source
            : copyRoute(source, normalizeScene(source.getScene()), resolvedModel);
        long start = System.nanoTime();
        try {
            AiChatService.ChatResult result = aiChatService.chatForConfigTest(
                probe.getBaseUrl(),
                probe.getApiKey(),
                probe.getModelName(),
                normalizeFormat(probe.getApiFormat()),
                normalizeAuthType(probe.getAuthType(), probe.getApiFormat()),
                probe.isFullUrl(),
                probe.getCustomUserAgent(),
                probePrompt
            );
            long latencyMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            String content = Objects.toString(result.content(), "").trim();
            List<String> badTerms = restrictiveTerms(content);
            boolean emptyOutput = !StringUtils.hasText(content);
            boolean contentFault = emptyOutput || !badTerms.isEmpty();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("success", !contentFault);
            row.put("contentFault", contentFault);
            row.put("emptyOutput", emptyOutput);
            row.put("badTerms", badTerms);
            row.put("latencyMs", latencyMs);
            row.put("modelName", result.modelName());
            row.put("message", contentFault ? "输出异常，请排查模型限定词或空输出" : "输出正常");
            row.put("content", shorten(content, 1200));
            row.put("usage", usageMap(result));
            return row;
        } catch (Exception exception) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("success", false);
            row.put("contentFault", true);
            row.put("emptyOutput", true);
            row.put("badTerms", List.of());
            row.put("latencyMs", Math.max(1L, (System.nanoTime() - start) / 1_000_000L));
            row.put("message", readableMessage(exception));
            row.put("usage", Map.of(
                "promptTokens", 0,
                "completionTokens", 0,
                "totalTokens", 0,
                "estimated", true
            ));
            return row;
        }
    }

    @Transactional
    public Map<String, Object> deleteRelay(Long id) {
        currentUserService.requireAdmin();
        ModelConfigEntity source = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "中转站配置不存在"));
        String provider = normalizeText(source.getProviderName());
        String baseUrl = normalizeText(source.getBaseUrl());
        List<ModelConfigEntity> matches = modelConfigRepository.findAllByOrderByActiveDescUpdatedAtDesc().stream()
            .filter(entity -> Objects.equals(normalizeText(entity.getProviderName()), provider))
            .filter(entity -> Objects.equals(normalizeText(entity.getBaseUrl()), baseUrl))
            .toList();
        for (ModelConfigEntity entity : matches) {
            modelConfigRepository.delete(entity);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("removed", matches.size());
        result.put("providerName", Objects.toString(source.getProviderName(), ""));
        result.put("baseUrl", Objects.toString(source.getBaseUrl(), ""));
        result.put("message", "已删除该中转站及其在各模块中的模型池记录");
        return result;
    }

    @Transactional
    public Map<String, Object> cleanupPool(String scene) {
        currentUserService.requireAdmin();
        String normalizedScene = normalizeScene(scene);
        List<ModelConfigEntity> candidates = modelConfigRepository.findBySceneAndActiveFalse(normalizedScene);
        List<Long> removedIds = new ArrayList<>();
        Map<String, Integer> reasons = new LinkedHashMap<>();
        Set<String> seenRoutes = new HashSet<>();
        for (ModelConfigEntity entity : candidates) {
            String reason = cleanupReason(entity, seenRoutes);
            if (reason.isBlank()) continue;
            removedIds.add(entity.getId());
            reasons.put(reason, reasons.getOrDefault(reason, 0) + 1);
            modelConfigRepository.delete(entity);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("removed", removedIds.size());
        result.put("removedIds", removedIds);
        result.put("reasons", reasons);
        result.put("pool", getPool(normalizedScene));
        return result;
    }

    @Transactional
    public Map<String, Object> deleteModelRoute(Long id) {
        currentUserService.requireAdmin();
        ModelConfigEntity entity = modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Model config not found"));
        modelConfigRepository.delete(entity);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        return result;
    }

    private ModelConfigVO toVO(ModelConfigEntity entity) {
        return new ModelConfigVO(
            entity.getProviderName(),
            entity.getBaseUrl(),
            entity.getModelName(),
            normalizeFormat(entity.getApiFormat()),
            normalizeAuthType(entity.getAuthType(), entity.getApiFormat()),
            entity.isFullUrl(),
            entity.getModelsUrl(),
            entity.getCustomUserAgent(),
            entity.getScene(),
            entity.getUpdatedAt(),
            entity.getApiKey() != null && !entity.getApiKey().isBlank()
        );
    }

    private Map<String, Object> checkPoolEntity(ModelConfigEntity entity) {
        if (!StringUtils.hasText(entity.getApiKey())) {
            return rememberPoolCheck(entity, "unconfigured", "缺少 API Key，暂不参与自动调用", null);
        }
        if (!StringUtils.hasText(entity.getModelName()) || "待填写".equals(entity.getModelName())) {
            return rememberPoolCheck(entity, "unconfigured", "缺少模型名称，请先填写或获取模型列表", null);
        }
        long start = System.nanoTime();
        try {
            AiChatService.ChatResult result = aiChatService.test(
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModelName(),
                normalizeFormat(entity.getApiFormat()),
                normalizeAuthType(entity.getAuthType(), entity.getApiFormat()),
                entity.isFullUrl(),
                entity.getCustomUserAgent()
            );
            long latencyMs = Math.max(1L, (System.nanoTime() - start) / 1_000_000L);
            return rememberPoolCheck(entity, "available", "可用，模型返回：" + result.modelName(), latencyMs);
        } catch (Exception error) {
            return rememberPoolCheck(entity, classifyPoolError(error), readableMessage(error), Math.max(1L, (System.nanoTime() - start) / 1_000_000L));
        }
    }

    private Map<String, Object> rememberPoolCheck(ModelConfigEntity entity, String status, String message, Long latencyMs) {
        if (entity.getId() != null) {
            entity.setLastStatus(status);
            entity.setLastMessage(shorten(message, 740));
            entity.setLastLatencyMs(latencyMs);
            entity.setLastTestedAt(LocalDateTime.now());
            modelConfigRepository.save(entity);
        }
        return poolRow(entity, status, message, latencyMs);
    }

    private Map<String, Object> poolRow(ModelConfigEntity entity, String status, String message, Long latencyMs) {
        String resolvedStatus = "unknown".equals(status) && StringUtils.hasText(entity.getLastStatus()) ? entity.getLastStatus() : status;
        String resolvedMessage = "unknown".equals(status) && StringUtils.hasText(entity.getLastMessage()) ? entity.getLastMessage() : message;
        Long resolvedLatencyMs = latencyMs != null ? latencyMs : entity.getLastLatencyMs();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("providerName", entity.getProviderName());
        row.put("baseUrl", entity.getBaseUrl());
        row.put("modelName", entity.getModelName());
        row.put("apiFormat", normalizeFormat(entity.getApiFormat()));
        row.put("authType", normalizeAuthType(entity.getAuthType(), entity.getApiFormat()));
        row.put("fullUrl", entity.isFullUrl());
        row.put("modelsUrl", entity.getModelsUrl());
        row.put("customUserAgent", entity.getCustomUserAgent());
        row.put("scene", normalizeScene(entity.getScene()));
        row.put("keyConfigured", StringUtils.hasText(entity.getApiKey()));
        row.put("active", entity.isActive());
        row.put("template", false);
        row.put("status", resolvedStatus);
        row.put("message", resolvedMessage);
        row.put("latencyMs", resolvedLatencyMs);
        row.put("lastTestedAt", entity.getLastTestedAt());
        row.put("updatedAt", entity.getUpdatedAt());
        row.put("keyUrl", inferKeyUrl(entity.getProviderName(), entity.getBaseUrl()));
        row.put("duplicateCount", 1);
        row.put("sortOrder", entity.getSortOrder() == null ? 0 : entity.getSortOrder());
        row.put("priority", poolPriority(entity, resolvedStatus, resolvedLatencyMs));
        return row;
    }

    private String poolPriority(ModelConfigEntity entity, String status, Long latencyMs) {
        int order = entity.getSortOrder() == null ? 0 : entity.getSortOrder();
        long latency = latencyMs == null ? 99_999L : latencyMs;
        return String.format("%05d", order) + "-" + statusPriority(status) + "-" + String.format("%08d", latency) + "-" + (entity.isActive() ? "0" : "1") + "-" + entity.getId();
    }

    public static String normalizeScene(String scene) {
        if ("paper_quiz".equalsIgnoreCase(scene) || "出题检测".equals(scene) || "形成考卷".equals(scene)) return "paper_quiz";
        if (!StringUtils.hasText(scene)) return SCENE_GENERAL;
        String value = scene.trim().toLowerCase();
        if (value.equals("paper_review") || value.equals("review") || value.equals("summary") || value.equals("综述")) {
            return SCENE_PAPER_REVIEW;
        }
        if (value.equals("topic") || value.equals("topics") || value.equals("topic_research") || value.equals("deep_research") || value.equals("deep-research") || value.equals("选题") || value.equals("选题调研")) {
            return SCENE_TOPIC_RESEARCH;
        }
        if (value.equals("paper_qa") || value.equals("qa") || value.equals("chat") || value.equals("问答")) {
            return SCENE_PAPER_QA;
        }
        if (value.equals("image_analysis") || value.equals("image") || value.equals("vision") || value.equals("图片分析") || value.equals("图像分析")) {
            return SCENE_IMAGE_ANALYSIS;
        }
        if (value.equals("forum") || value.equals("forum_moderation") || value.equals("moderation") || value.equals("发帖审核")) {
            return SCENE_FORUM_MODERATION;
        }
        if (value.equals("meeting_fusion") || value.equals("fusion") || value.equals("组会融合") || value.equals("一键融合")) {
            return SCENE_MEETING_FUSION;
        }
        if (value.equals("ppt") || value.equals("deck") || value.equals("meeting") || value.equals("meeting_report") || value.equals("meeting_deck")) {
            return SCENE_MEETING_DECK;
        }
        if (value.equals("backup") || value.equals("备用") || value.equals("备用号池") || value.equals("备用路由")) {
            return SCENE_BACKUP;
        }
        if (value.equals("free_pool") || value.equals("free") || value.equals("免费") || value.equals("免费号池")) {
            return SCENE_FREE_POOL;
        }
        if (value.equals("reading_notes") || value.equals("notes") || value.equals("阅读笔记")) {
            return SCENE_READING_NOTES;
        }
        return SCENE_GENERAL;
    }

    private void addCompactPoolRow(List<Map<String, Object>> rows, Set<String> seenRoutes, Map<String, Object> row) {
        String key = routeKey(row);
        if (seenRoutes.add(key)) {
            rows.add(row);
            return;
        }
        for (Map<String, Object> existing : rows) {
            if (!routeKey(existing).equals(key)) continue;
            int count = Number.class.isInstance(existing.get("duplicateCount"))
                ? ((Number) existing.get("duplicateCount")).intValue()
                : 1;
            existing.put("duplicateCount", count + 1);
            if (Boolean.TRUE.equals(row.get("active"))) {
                existing.put("active", true);
                existing.put("priority", "00");
            }
            return;
        }
    }

    private String routeKey(Map<String, Object> row) {
        return String.join("|",
            String.valueOf(row.get("providerName")).toLowerCase(),
            String.valueOf(row.get("baseUrl")).toLowerCase(),
            String.valueOf(row.get("modelName")).toLowerCase(),
            String.valueOf(row.get("apiFormat")).toLowerCase(),
            String.valueOf(row.get("authType")).toLowerCase()
        );
    }

    private boolean sameRoute(ModelConfigEntity a, ModelConfigEntity b) {
        return sameRoute(a, b, b.getModelName());
    }

    private boolean sameRoute(ModelConfigEntity a, ModelConfigEntity b, String modelName) {
        return Objects.equals(normalizeText(a.getProviderName()), normalizeText(b.getProviderName()))
            && Objects.equals(normalizeText(a.getBaseUrl()), normalizeText(b.getBaseUrl()))
            && Objects.equals(normalizeText(a.getModelName()), normalizeText(modelName))
            && Objects.equals(normalizeFormat(a.getApiFormat()), normalizeFormat(b.getApiFormat()))
            && Objects.equals(normalizeAuthType(a.getAuthType(), a.getApiFormat()), normalizeAuthType(b.getAuthType(), b.getApiFormat()));
    }

    private ModelConfigEntity copyRoute(ModelConfigEntity source, String scene, String modelName) {
        ModelConfigEntity target = new ModelConfigEntity();
        target.setUserId(source.getUserId());
        target.setProviderName(source.getProviderName());
        target.setBaseUrl(source.getBaseUrl());
        target.setApiKey(source.getApiKey());
        target.setApiKeyMasked(source.getApiKeyMasked());
        target.setModelName(modelName);
        target.setApiFormat(normalizeFormat(source.getApiFormat()));
        target.setAuthType(normalizeAuthType(source.getAuthType(), source.getApiFormat()));
        target.setFullUrl(source.isFullUrl());
        target.setModelsUrl(source.getModelsUrl());
        target.setCustomUserAgent(source.getCustomUserAgent());
        target.setScene(normalizeScene(scene));
        target.setActive(false);
        return target;
    }

    private String normalizeText(String value) {
        return Objects.toString(value, "").trim().toLowerCase();
    }

    private String shorten(String value, int max) {
        String text = Objects.toString(value, "").replaceAll("\\s+", " ").trim();
        return text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String cleanupReason(ModelConfigEntity entity, Set<String> seenRoutes) {
        String provider = Objects.toString(entity.getProviderName(), "").toLowerCase();
        String baseUrl = Objects.toString(entity.getBaseUrl(), "").toLowerCase();
        String model = Objects.toString(entity.getModelName(), "").trim();
        if (!StringUtils.hasText(entity.getApiKey())) return "未配置 Key";
        if (!StringUtils.hasText(model) || "待填写".equals(model)) return "模型名未填写";
        if ("gemini".equalsIgnoreCase(entity.getApiFormat())) return "当前自动池未适配 Gemini 原生协议";
        if (baseUrl.contains("{account_id}") || baseUrl.contains("models.inference.ai.azure.com")) return "需要额外平台配置";
        if (baseUrl.contains("abc-tunnel") || provider.contains("9router")) return "9Router 隧道不稳定";
        String routeKey = String.join("|",
            provider,
            baseUrl,
            model.toLowerCase(),
            normalizeFormat(entity.getApiFormat()),
            normalizeAuthType(entity.getAuthType(), entity.getApiFormat())
        );
        if (!seenRoutes.add(routeKey)) return "重复路由";
        try {
            aiChatService.test(
                entity.getBaseUrl(),
                entity.getApiKey(),
                entity.getModelName(),
                normalizeFormat(entity.getApiFormat()),
                normalizeAuthType(entity.getAuthType(), entity.getApiFormat()),
                entity.isFullUrl(),
                entity.getCustomUserAgent()
            );
        } catch (Exception error) {
            String status = classifyPoolError(error);
            if ("auth_error".equals(status) || "failed".equals(status)) {
                return "检测失败：" + readableMessage(error);
            }
        }
        return "";
    }

    private String inferKeyUrl(String providerName, String baseUrl) {
        String source = (Objects.toString(providerName, "") + " " + Objects.toString(baseUrl, "")).toLowerCase();
        if (source.contains("openrouter")) return "https://openrouter.ai/keys";
        if (source.contains("deepseek")) return "https://platform.deepseek.com/api_keys";
        if (source.contains("groq")) return "https://console.groq.com/keys";
        if (source.contains("cerebras")) return "https://cloud.cerebras.ai/platform";
        if (source.contains("huggingface") || source.contains("hugging face")) return "https://huggingface.co/settings/tokens";
        if (source.contains("cloudflare")) return "https://dash.cloudflare.com/profile/api-tokens";
        if (source.contains("github")) return "https://github.com/settings/tokens";
        if (source.contains("gemini") || source.contains("generativelanguage")) return "https://aistudio.google.com/apikey";
        return "";
    }

    private String classifyPoolError(Exception error) {
        String message = String.valueOf(error.getMessage()).toLowerCase();
        if (message.contains("429") || message.contains("rate limit") || message.contains("quota")) return "limited";
        if (message.contains("401") || message.contains("403") || message.contains("api key")) return "auth_error";
        if (message.contains("timeout") || message.contains("timed out")) return "timeout";
        return "failed";
    }

    private String statusPriority(String status) {
        return switch (status) {
            case "available" -> "10";
            case "limited" -> "20";
            case "unknown" -> "30";
            case "timeout" -> "40";
            case "auth_error" -> "50";
            case "failed" -> "60";
            default -> "70";
        };
    }

    private List<Map<String, String>> recommendedTemplates() {
        return List.of(
            Map.of(
                "id", "openrouter-relay",
                "providerName", "OpenRouter Relay",
                "baseUrl", "https://openrouter.ai/api/v1",
                "modelName", "deepseek/deepseek-chat-v3-0324:free",
                "apiFormat", "openai_chat",
                "status", "unconfigured",
                "message", "第三方 OpenAI-compatible 中转，适合低成本问答、审核和综述备用；PPT 入口请换强模型。",
                "keyUrl", "https://openrouter.ai/keys",
                "priority", "70-openrouter"
            ),
            Map.of(
                "id", "siliconflow-relay",
                "providerName", "SiliconFlow Relay",
                "baseUrl", "https://api.siliconflow.cn/v1",
                "modelName", "deepseek-ai/DeepSeek-V3",
                "apiFormat", "openai_chat",
                "status", "unconfigured",
                "message", "第三方 OpenAI-compatible 中转，国内访问友好，适合经济模型池和长文本任务备用。",
                "keyUrl", "https://cloud.siliconflow.cn/account/ak",
                "priority", "72-siliconflow"
            ),
            Map.of(
                "id", "aimlapi-relay",
                "providerName", "AIMLAPI Relay",
                "baseUrl", "https://api.aimlapi.com/v1",
                "modelName", "deepseek/deepseek-chat",
                "apiFormat", "openai_chat",
                "status", "unconfigured",
                "message", "第三方 OpenAI-compatible 中转，可作为备用号池候选；以实际账户价格和可用模型为准。",
                "keyUrl", "https://aimlapi.com/app/keys/",
                "priority", "74-aimlapi"
            ),
            Map.of(
                "id", "groq",
                "providerName", "GroqCloud",
                "baseUrl", "https://api.groq.com/openai/v1",
                "modelName", "llama-3.1-8b-instant",
                "apiFormat", "openai_chat",
                "status", "unconfigured",
                "message", "OpenAI-compatible，适合快速摘要和轻量 agent；填写 Groq API Key 后可刷新检测。",
                "keyUrl", "https://console.groq.com/keys",
                "priority", "80-groq"
            ),
            Map.of(
                "id", "cerebras",
                "providerName", "Cerebras Inference",
                "baseUrl", "https://api.cerebras.ai/v1",
                "modelName", "llama-3.1-8b",
                "apiFormat", "openai_chat",
                "status", "unconfigured",
                "message", "OpenAI-compatible，速度快；填写 Cerebras API Key 后可加入池。",
                "keyUrl", "https://cloud.cerebras.ai/platform",
                "priority", "81-cerebras"
            )
        );
    }

    public Map<String, Object> test(ModelConfigRequest request) {
        currentUserService.requireAdmin();
        try {
            AiChatService.ChatResult result = aiChatService.test(
                request.getBaseUrl(),
                request.getApiKey(),
                resolveModelName(request),
                resolveFormat(request, normalizeScene(request.getScene())),
                normalizeAuthType(request.getAuthType(), request.getApiFormat()),
                request.isFullUrl(),
                request.getCustomUserAgent()
            );
            return Map.of(
                "success", true,
                "message", "连接成功，模型已返回有效响应",
                "modelName", result.modelName(),
                "usage", usageMap(result)
            );
        } catch (Exception exception) {
            return Map.of(
                "success", false,
                "message", readableRequestMessage(request, exception)
            );
        }
    }

    @Transactional
    public Map<String, Object> stressTestPools(Integer samples, Integer concurrency, Boolean applySort) {
        currentUserService.requireAdmin();
        int safeSamples = Math.min(MAX_STRESS_SAMPLES, Math.max(2, samples == null ? 8 : samples));
        int safeConcurrency = Math.min(MAX_STRESS_CONCURRENCY, Math.max(4, concurrency == null ? 24 : concurrency));
        boolean shouldApplySort = applySort == null || applySort;
        List<String> scenes = List.of(
            SCENE_GENERAL,
            SCENE_PAPER_REVIEW,
            SCENE_PAPER_QA,
            SCENE_MEETING_FUSION,
            SCENE_FORUM_MODERATION,
            SCENE_TOPIC_RESEARCH,
            SCENE_READING_NOTES,
            SCENE_PAPER_QUIZ,
            SCENE_BACKUP
        );
        Map<String, Object> result = new LinkedHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(safeConcurrency);
        try {
            for (String scene : scenes) {
                List<ModelConfigEntity> rows = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(scene).stream()
                    .filter(row -> StringUtils.hasText(row.getApiKey()))
                    .filter(row -> StringUtils.hasText(row.getBaseUrl()))
                    .filter(row -> StringUtils.hasText(row.getModelName()))
                    .toList();
                List<ModelStressResult> tested = new ArrayList<>();
                for (ModelConfigEntity row : rows) {
                    tested.add(stressOneRoute(row, safeSamples, executor));
                }
                List<ModelStressResult> sorted = tested.stream()
                    .sorted(ModelStressResult::compareTo)
                    .toList();
                if (shouldApplySort) {
                    int order = 0;
                    for (ModelStressResult item : sorted) {
                        ModelConfigEntity row = item.route();
                        row.setSortOrder(order++);
                        row.setLastStatus(item.successRate() > 0 ? "available" : item.status());
                        row.setLastLatencyMs(item.p95LatencyMs() > 0 ? item.p95LatencyMs() : item.avgLatencyMs());
                        row.setLastMessage(item.message());
                        row.setLastTestedAt(LocalDateTime.now());
                        modelConfigRepository.save(row);
                    }
                }
                result.put(scene, sorted.stream().limit(10).map(ModelStressResult::toMap).toList());
            }
        } finally {
            executor.shutdownNow();
        }
        return Map.of(
            "success", true,
            "samples", safeSamples,
            "concurrency", safeConcurrency,
            "pptSkipped", true,
            "sorted", shouldApplySort,
            "scenes", result
        );
    }

    private ModelStressResult stressOneRoute(ModelConfigEntity row, int samples, ExecutorService executor) {
        List<CompletableFuture<ModelProbeResult>> tasks = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            tasks.add(CompletableFuture.supplyAsync(() -> probeRoute(row), executor));
        }
        List<ModelProbeResult> probes = tasks.stream().map(task -> {
            try {
                return task.get(75, TimeUnit.SECONDS);
            } catch (Exception error) {
                return new ModelProbeResult(false, 75_000L, "timeout", "并发自检超时");
            }
        }).toList();
        long success = probes.stream().filter(ModelProbeResult::success).count();
        List<Long> latencies = probes.stream()
            .filter(ModelProbeResult::success)
            .map(ModelProbeResult::latencyMs)
            .sorted()
            .toList();
        long avg = latencies.isEmpty() ? 0L : Math.round(latencies.stream().mapToLong(Long::longValue).average().orElse(0));
        long p95 = latencies.isEmpty() ? 0L : latencies.get(Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.95D) - 1));
        String status = success > 0 ? "available" : probes.stream().findFirst().map(ModelProbeResult::status).orElse("failed");
        String message = success + "/" + samples + " 成功，平均 " + avg + "ms，P95 " + p95 + "ms";
        if (success == 0) message = probes.stream().map(ModelProbeResult::message).filter(StringUtils::hasText).findFirst().orElse("全部失败");
        return new ModelStressResult(row, success, samples, avg, p95, status, shorten(message, 740));
    }

    private ModelProbeResult probeRoute(ModelConfigEntity row) {
        long start = System.nanoTime();
        try {
            aiChatService.test(
                row.getBaseUrl(),
                row.getApiKey(),
                row.getModelName(),
                normalizeFormat(row.getApiFormat()),
                normalizeAuthType(row.getAuthType(), row.getApiFormat()),
                row.isFullUrl(),
                row.getCustomUserAgent()
            );
            return new ModelProbeResult(true, Math.max(1L, (System.nanoTime() - start) / 1_000_000L), "available", "可用");
        } catch (Exception error) {
            return new ModelProbeResult(false, Math.max(1L, (System.nanoTime() - start) / 1_000_000L), classifyPoolError(error), readableMessage(error));
        }
    }

    public Map<String, Object> fetchModels(ModelConfigRequest request) {
        currentUserService.requireAdmin();
        try {
            List<AiChatService.ModelInfo> models = aiChatService.fetchModels(
                request.getBaseUrl(),
                request.getApiKey(),
                resolveFormat(request, normalizeScene(request.getScene())),
                normalizeAuthType(request.getAuthType(), request.getApiFormat()),
                request.isFullUrl(),
                request.getModelsUrl(),
                request.getCustomUserAgent()
            );
            return Map.of(
                "success", true,
                "message", "已获取 " + models.size() + " 个可用模型",
                "count", models.size(),
                "models", models
            );
        } catch (Exception exception) {
            return Map.of(
                "success", false,
                "message", readableRequestMessage(request, exception),
                "count", 0,
                "models", List.of()
            );
        }
    }

    public Map<String, Object> chat(ModelConfigRequest request, String prompt) {
        currentUserService.requireAdmin();
        if (!StringUtils.hasText(prompt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入测试问题");
        }
        try {
            AiChatService.ChatResult result = aiChatService.chatForConfigTest(
                request.getBaseUrl(),
                request.getApiKey(),
                resolveModelName(request),
                resolveFormat(request, normalizeScene(request.getScene())),
                normalizeAuthType(request.getAuthType(), request.getApiFormat()),
                request.isFullUrl(),
                request.getCustomUserAgent(),
                prompt.trim()
            );
            return Map.of(
                "success", true,
                "modelName", result.modelName(),
                "content", result.content(),
                "usage", usageMap(result)
            );
        } catch (Exception exception) {
            return Map.of(
                "success", false,
                "message", readableRequestMessage(request, exception)
            );
        }
    }

    private Map<String, Object> usageMap(AiChatService.ChatResult result) {
        return Map.of(
            "promptTokens", result.promptTokens(),
            "completionTokens", result.completionTokens(),
            "totalTokens", result.totalTokens(),
            "estimated", result.estimatedUsage()
        );
    }

    private List<String> restrictiveTerms(String content) {
        if (!StringUtils.hasText(content)) return List.of();
        String lower = content.toLowerCase();
        return RESTRICTIVE_OUTPUT_TERMS.stream()
            .filter(term -> lower.contains(term.toLowerCase()))
            .distinct()
            .toList();
    }

    private String normalizeFormat(String value) {
        if ("anthropic".equalsIgnoreCase(value)) return "anthropic";
        if ("openai_responses".equalsIgnoreCase(value)) return "openai_responses";
        return "openai_chat";
    }

    private String resolveFormat(ModelConfigRequest request) {
        return resolveFormat(request, normalizeScene(request.getScene()));
    }

    private String resolveFormat(ModelConfigRequest request, String scene) {
        if (SCENE_MEETING_DECK.equals(normalizeScene(scene))) {
            return "openai_responses";
        }
        String baseUrl = request.getBaseUrl() == null ? "" : request.getBaseUrl().toLowerCase();
        if (baseUrl.matches(".*/codex(?:/v\\d+)?/?$") || baseUrl.endsWith("/responses")) {
            return "openai_responses";
        }
        return normalizeFormat(request.getApiFormat());
    }

    private String resolveModelName(ModelConfigRequest request) {
        String model = Objects.toString(request.getModelName(), "").trim();
        String source = (Objects.toString(request.getProviderName(), "") + " " + Objects.toString(request.getBaseUrl(), "")).toLowerCase();
        if (source.contains("deepseek") && (!StringUtils.hasText(model) || model.toLowerCase().startsWith("gpt-"))) {
            return "deepseek-v4-flash";
        }
        return model;
    }

    private String normalizeAuthType(String value, String format) {
        if (StringUtils.hasText(value)) return value.trim().toLowerCase();
        return "anthropic".equalsIgnoreCase(format) ? "x-api-key" : "bearer";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskApiKey(String value) {
        if (!StringUtils.hasText(value)) return "未配置";
        String key = value.trim();
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private String readableMessage(Exception exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) return "连接失败，请检查地址、模型与 API Key";
        String lower = message.toLowerCase();
        if (message.contains("HTTP 530") || message.contains("error code: 1016")) {
            return "9Router 中转隧道当前不可用（HTTP 530 / 1016），不是模型选择错误。请稍后重试，或切换 OpenCode Zen / 自定义稳定中转站。";
        }
        if ((lower.contains("deepseek") || lower.contains("api.deepseek.com")) && (message.contains("HTTP 401") || lower.contains("invalid"))) {
            return "DeepSeek 官方 API 鉴权失败：请确认填写的是 DeepSeek Platform 的 API Key，不是网页登录账号/其他中转 Key；模型名请使用 deepseek-v4-flash 或 deepseek-v4-pro。";
        }
        if (message.contains("HTTP 502") || message.contains("HTTP 503") || message.contains("HTTP 504")) {
            return "中转站临时不可用或上游拥堵，系统已自动重试但仍失败。请稍后重试，或换用更稳定的中转地址。";
        }
        if (message.toLowerCase().contains("timed out") || message.toLowerCase().contains("timeout")) {
            return "模型响应超时。思考模型首次调用可能较慢，请稍后重试或先选择 Flash / Sonnet 等快速模型测试连接";
        }
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private String readableRequestMessage(ModelConfigRequest request, Exception exception) {
        String message = Objects.toString(exception.getMessage(), "");
        if (isDeepSeekRequest(request) && (message.contains("HTTP 401") || message.contains("HTTP 403") || message.toLowerCase().contains("invalid"))) {
            return "DeepSeek 官方 API 鉴权失败：请确认填写的是 DeepSeek Platform 的 API Key，不是网页登录账号/其他中转 Key；模型名请使用 deepseek-v4-flash 或 deepseek-v4-pro。";
        }
        if (isDeepSeekRequest(request) && message.contains("gpt-")) {
            return "DeepSeek 官方 API 不能使用 GPT 模型名；请改用 deepseek-v4-flash 或 deepseek-v4-pro。";
        }
        return readableMessage(exception);
    }

    private boolean isDeepSeekRequest(ModelConfigRequest request) {
        String source = (Objects.toString(request.getProviderName(), "") + " " + Objects.toString(request.getBaseUrl(), "")).toLowerCase();
        return source.contains("deepseek") || source.contains("api.deepseek.com");
    }

    @Transactional
    public Map<String, Object> sortPoolRoutes(List<Long> ids) {
        currentUserService.requireAdmin();
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可保存的模型节点");
        }
        Set<Long> seen = new HashSet<>();
        int saved = 0;
        for (Long id : ids) {
            if (id == null || id <= 0 || !seen.add(id)) continue;
            int order = saved;
            modelConfigRepository.findById(id).ifPresent(row -> {
                row.setSortOrder(order);
                modelConfigRepository.save(row);
            });
            saved++;
        }
        return Map.of("success", true, "message", "已保存排序", "saved", saved);
    }

    private record ModelProbeResult(boolean success, long latencyMs, String status, String message) {}

    private record ModelStressResult(
        ModelConfigEntity route,
        long successCount,
        int samples,
        long avgLatencyMs,
        long p95LatencyMs,
        String status,
        String message
    ) implements Comparable<ModelStressResult> {
        double successRate() {
            return samples <= 0 ? 0D : (double) successCount / (double) samples;
        }

        Map<String, Object> toMap() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", route.getId());
            row.put("providerName", route.getProviderName());
            row.put("baseUrl", route.getBaseUrl());
            row.put("modelName", route.getModelName());
            row.put("scene", route.getScene());
            row.put("successRate", successRate());
            row.put("successCount", successCount);
            row.put("samples", samples);
            row.put("avgLatencyMs", avgLatencyMs);
            row.put("p95LatencyMs", p95LatencyMs);
            row.put("status", status);
            row.put("message", message);
            return row;
        }

        @Override
        public int compareTo(ModelStressResult other) {
            int success = Double.compare(other.successRate(), successRate());
            if (success != 0) return success;
            int p95 = Long.compare(p95LatencyMs <= 0 ? Long.MAX_VALUE : p95LatencyMs, other.p95LatencyMs <= 0 ? Long.MAX_VALUE : other.p95LatencyMs);
            if (p95 != 0) return p95;
            return Long.compare(avgLatencyMs <= 0 ? Long.MAX_VALUE : avgLatencyMs, other.avgLatencyMs <= 0 ? Long.MAX_VALUE : other.avgLatencyMs);
        }
    }
}
