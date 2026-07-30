package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.repository.AppUserRepository;
import com.paperpilot.server.repository.ModelConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiChatService {
    private static final List<String> COMPAT_SUFFIXES = List.of(
        "/api/claudecode", "/api/anthropic", "/apps/anthropic", "/api/coding",
        "/claudecode", "/anthropic", "/step_plan", "/coding", "/claude"
    );
    private static final List<String> OPENROUTER_FREE_FALLBACKS = List.of(
        "cohere/north-mini-code:free",
        "qwen/qwen3-next-80b-a3b-instruct:free",
        "openai/gpt-oss-20b:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemma-4-26b-a4b-it:free"
    );
    private static final int MAX_POOL_FALLBACK_ROUTES = 8;
    private static final Semaphore MODEL_TEST_LIMITER = new Semaphore(6);
    private static final Map<String, String> OPEN_CODE_FREE_MODEL_ALIASES = Map.ofEntries(
        Map.entry("DeepSeek V4 Flash Free", "oc/deepseek-v4-flash-free"),
        Map.entry("deepseek v4 flash free", "oc/deepseek-v4-flash-free"),
        Map.entry("MiniMax M2.5 Free", "oc/deepseek-v4-flash-free"),
        Map.entry("minimax m2.5 free", "oc/deepseek-v4-flash-free"),
        Map.entry("oc/minimax-m2.5-free", "oc/deepseek-v4-flash-free"),
        Map.entry("Qwen 3.6 Plus Free", "oc/deepseek-v4-flash-free"),
        Map.entry("qwen 3.6 plus free", "oc/deepseek-v4-flash-free"),
        Map.entry("oc/qwen3.6-plus-free", "oc/deepseek-v4-flash-free"),
        Map.entry("Nemotron 3 Super Free", "oc/deepseek-v4-flash-free"),
        Map.entry("nemotron 3 super free", "oc/deepseek-v4-flash-free"),
        Map.entry("oc/nemotron-3-super-free", "oc/deepseek-v4-flash-free"),
        Map.entry("MiMo V2.5 Free", "oc/mimo-v2.5-free"),
        Map.entry("mimo v2.5 free", "oc/mimo-v2.5-free"),
        Map.entry("MiniMax M3 Free", "oc/deepseek-v4-flash-free"),
        Map.entry("minimax m3 free", "oc/deepseek-v4-flash-free"),
        Map.entry("oc/minimax-m3-free", "oc/deepseek-v4-flash-free"),
        Map.entry("Nemotron 3 Ultra Free", "oc/nemotron-3-ultra-free"),
        Map.entry("nemotron 3 ultra free", "oc/nemotron-3-ultra-free"),
        Map.entry("North Mini Code Free", "oc/north-mini-code-free"),
        Map.entry("north mini code free", "oc/north-mini-code-free")
    );

    private final ModelConfigRepository modelConfigRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    public AiChatService(
        ModelConfigRepository modelConfigRepository,
        AppUserRepository appUserRepository,
        CurrentUserService currentUserService,
        AiUsageService aiUsageService,
        ObjectMapper objectMapper
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.appUserRepository = appUserRepository;
        this.currentUserService = currentUserService;
        this.aiUsageService = aiUsageService;
        this.objectMapper = objectMapper;
    }

    public ChatResult chatJson(String systemPrompt, String userPrompt) throws Exception {
        return chatJson(systemPrompt, userPrompt, 2400);
    }

    public ChatResult chatJson(String systemPrompt, String userPrompt, int maxOutputTokens) throws Exception {
        String scene = inferModelConfigScene(systemPrompt, userPrompt);
        ModelConfigEntity config = activeSceneConfig(scene);
        if (config == null && shouldUseConfiguredPoolOnly(scene)) {
            throw new IllegalStateException("当前入口未配置可用模型，请在管理员 AI 路由中为 " + scene + " 配置第三方 OpenAI 兼容中转。");
        }
        return send(
            config == null ? "https://api.openai.com/v1" : config.getBaseUrl(),
            config == null ? "" : config.getApiKey(),
            config == null ? "gpt-4.1-mini" : config.getModelName(),
            config == null ? "openai_chat" : config.getApiFormat(),
            config == null ? "bearer" : config.getAuthType(),
            config != null && config.isFullUrl(),
            config == null ? "" : config.getCustomUserAgent(),
            systemPrompt,
            userPrompt,
            maxOutputTokens
        );
    }

    public ChatResult chatJsonWithModelFallback(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> fallbackModels
    ) throws Exception {
        return chatJsonWithModelFallback(systemPrompt, userPrompt, maxOutputTokens, fallbackModels, true, Set.of());
    }

    public ChatResult chatJsonWithModelFallbackSkipping(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> fallbackModels,
        Set<String> skippedModels
    ) throws Exception {
        return chatJsonWithModelFallback(systemPrompt, userPrompt, maxOutputTokens, fallbackModels, true, skippedModels);
    }

    public ChatResult chatJsonWithModelFallbackUnmetered(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> fallbackModels
    ) throws Exception {
        return chatJsonWithModelFallback(systemPrompt, userPrompt, maxOutputTokens, fallbackModels, false, Set.of());
    }

    private ChatResult chatJsonWithModelFallback(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> fallbackModels,
        boolean accountUsage,
        Set<String> skippedModels
    ) throws Exception {
        String scene = inferModelConfigScene(systemPrompt, userPrompt);
        ModelConfigEntity config = activeSceneConfig(scene);
        List<ModelRoute> routes = new ArrayList<>();
        List<ModelConfigEntity> pool = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(scene).stream()
            .filter(row -> StringUtils.hasText(row.getApiKey()))
            .filter(row -> StringUtils.hasText(row.getModelName()))
            .filter(row -> StringUtils.hasText(row.getBaseUrl()))
            .sorted(this::comparePoolRoute)
            .toList();
        if (pool.isEmpty() && config != null && config.isActive()) {
            pool = List.of(config);
        }
        int routeCount = 0;
        for (ModelConfigEntity row : pool) {
            if (!StringUtils.hasText(row.getApiKey()) || !StringUtils.hasText(row.getModelName()) || !StringUtils.hasText(row.getBaseUrl())) continue;
            if (routeCount >= MAX_POOL_FALLBACK_ROUTES) break;
            routeCount++;
            Iterable<String> expansion = shouldUseConfiguredPoolOnly(scene) ? List.of(row.getModelName()) : expandedModels(row.getModelName(), row.getBaseUrl(), row.getProviderName(), fallbackModels);
            for (String model : expansion) {
                routes.add(new ModelRoute(
                    row.getBaseUrl(),
                    row.getApiKey(),
                    model,
                    row.getApiFormat(),
                    row.getAuthType(),
                    row.isFullUrl(),
                    row.getCustomUserAgent()
                ));
            }
        }
        // --- BACKUP POOL ---
        if (!"backup".equals(scene)) {
            List<ModelConfigEntity> backupPool = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc("backup").stream()
                .filter(row -> StringUtils.hasText(row.getApiKey()) && StringUtils.hasText(row.getModelName()) && StringUtils.hasText(row.getBaseUrl()))
                .sorted(this::comparePoolRoute)
                .toList();
            for (ModelConfigEntity row : backupPool) {
                if (routeCount >= MAX_POOL_FALLBACK_ROUTES + 4) break;
                routeCount++;
                routes.add(new ModelRoute(
                    row.getBaseUrl(),
                    row.getApiKey(),
                    row.getModelName(),
                    row.getApiFormat(),
                    row.getAuthType(),
                    row.isFullUrl(),
                    row.getCustomUserAgent()
                ));
            }
        }
        if (routes.isEmpty() && config == null && !shouldUseConfiguredPoolOnly(scene)) {
            routes.add(new ModelRoute("https://api.openai.com/v1", "", "gpt-4.1-mini", "openai_chat", "bearer", false, ""));
        }
        if (routes.isEmpty()) {
            throw new IllegalStateException("当前入口未配置可用模型，请在管理员 AI 路由中为 " + scene + " 配置第三方 OpenAI 兼容中转。");
        }
        String lastError = "没有可用模型";
        LinkedHashSet<String> attempted = new LinkedHashSet<>();
        boolean hadSkippedRoutes = false;
        for (int pass = 0; pass < 2; pass++) {
            for (ModelRoute route : routes) {
                if (!StringUtils.hasText(route.model())) continue;
                String normalizedModel = normalizeOpenCodeFreeModel(route.model());
                if (pass == 0 && skippedModels != null && skippedModels.contains(normalizedModel)) {
                    hadSkippedRoutes = true;
                    continue;
                }
                String attemptKey = route.baseUrl() + " " + route.model();
                if (!attempted.add(attemptKey)) continue;
                try {
                    return send(route.baseUrl(), route.apiKey(), route.model(), route.apiFormat(), route.authType(), route.fullUrl(), route.customUserAgent(), systemPrompt, userPrompt, maxOutputTokens, accountUsage);
                } catch (Exception error) {
                    lastError = route.model() + "：" + error.getMessage();
                }
            }
            if (!attempted.isEmpty() || !hadSkippedRoutes) break;
            lastError = "未被质量门跳过的模型已耗尽，正在放开跳过列表重试完整模型池";
        }
        throw new IllegalStateException("模型池全部尝试失败，最后错误：" + lastError);
    }

    private boolean shouldUseConfiguredPoolOnly(String scene) {
        return ModelConfigService.SCENE_PAPER_REVIEW.equals(scene)
            || ModelConfigService.SCENE_PAPER_QA.equals(scene)
            || ModelConfigService.SCENE_TOPIC_RESEARCH.equals(scene);
    }

    private int comparePoolRoute(ModelConfigEntity a, ModelConfigEntity b) {
        int orderCompare = Integer.compare(
            a.getSortOrder() == null ? 0 : a.getSortOrder(),
            b.getSortOrder() == null ? 0 : b.getSortOrder()
        );
        if (orderCompare != 0) return orderCompare;
        int status = Integer.compare(poolStatusRank(a), poolStatusRank(b));
        if (status != 0) return status;
        int latency = Long.compare(poolLatency(a), poolLatency(b));
        if (latency != 0) return latency;
        int active = Boolean.compare(!a.isActive(), !b.isActive());
        if (active != 0) return active;
        return nullSafeUpdatedAt(b).compareTo(nullSafeUpdatedAt(a));
    }

    private int poolStatusRank(ModelConfigEntity row) {
        String status = Objects.toString(row.getLastStatus(), "").trim().toLowerCase(Locale.ROOT);
        if ("available".equals(status)) return 0;
        if ("unknown".equals(status) || status.isBlank()) return 1;
        if ("limited".equals(status) || "timeout".equals(status) || "needs_adapter".equals(status)) return 2;
        return 3;
    }

    private long poolLatency(ModelConfigEntity row) {
        Long value = row.getLastLatencyMs();
        return value == null || value <= 0 ? 99_999L : value;
    }

    private java.time.LocalDateTime nullSafeUpdatedAt(ModelConfigEntity row) {
        return row.getUpdatedAt() == null ? java.time.LocalDateTime.MIN : row.getUpdatedAt();
    }

    public ChatResult chatJsonForDeckAgent(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> preferredModels,
        List<String> fallbackModels
    ) throws Exception {
        ModelConfigEntity active = modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK).orElse(null);
        List<ModelConfigEntity> configs = new ArrayList<>(modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK).stream()
            .filter(ModelConfigEntity::isActive)
            .toList());
        configs.sort(Comparator
            .comparing((ModelConfigEntity row) -> active != null && Objects.equals(row.getId(), active.getId()) ? 0 : 1)
            .thenComparing(row -> strongModelScore(row.getProviderName(), row.getModelName(), row.getBaseUrl())));
        List<ModelRoute> routes = new ArrayList<>();
        for (ModelConfigEntity row : configs) {
            if (!StringUtils.hasText(row.getApiKey()) || !StringUtils.hasText(row.getBaseUrl())) continue;
            LinkedHashSet<String> models = new LinkedHashSet<>();
            if (routeCanTryPreferredModels(row.getBaseUrl(), row.getProviderName())) {
                preferredModels.forEach(models::add);
            }
            models.add(row.getModelName());
            models.addAll(expandedModels(row.getModelName(), row.getBaseUrl(), row.getProviderName(), fallbackModels));
            for (String model : models) {
                routes.add(new ModelRoute(
                    row.getBaseUrl(),
                    row.getApiKey(),
                    model,
                    row.getApiFormat(),
                    row.getAuthType(),
                    row.isFullUrl(),
                    row.getCustomUserAgent()
                ));
            }
        }
        // --- BACKUP POOL ---
        List<ModelConfigEntity> backupConfigs = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc("backup").stream()
            .filter(ModelConfigEntity::isActive)
            .filter(row -> StringUtils.hasText(row.getApiKey()) && StringUtils.hasText(row.getModelName()) && StringUtils.hasText(row.getBaseUrl()))
            .sorted(this::comparePoolRoute)
            .toList();
        for (ModelConfigEntity row : backupConfigs) {
            routes.add(new ModelRoute(
                row.getBaseUrl(),
                row.getApiKey(),
                row.getModelName(),
                row.getApiFormat(),
                row.getAuthType(),
                row.isFullUrl(),
                row.getCustomUserAgent()
            ));
        }
        if (routes.isEmpty()) {
            throw new IllegalStateException("PPT 生成未配置专用模型池。请到管理员模型池切换到“组会汇报 / PPT生成”，配置强模型 Key 后再生成。");
        }
        String lastError = "没有可用强模型";
        LinkedHashSet<String> attempted = new LinkedHashSet<>();
        for (ModelRoute route : routes) {
            if (!StringUtils.hasText(route.model())) continue;
            String attemptKey = route.baseUrl() + " " + route.model();
            if (!attempted.add(attemptKey)) continue;
            try {
                return send(route.baseUrl(), route.apiKey(), route.model(), route.apiFormat(), route.authType(), route.fullUrl(), route.customUserAgent(), systemPrompt, userPrompt, maxOutputTokens);
            } catch (Exception error) {
                lastError = route.model() + "：" + error.getMessage();
            }
        }
        throw new IllegalStateException(lastError);
    }

    public ChatResult chatJsonForDeckAgentStrict(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> preferredModels
    ) throws Exception {
        return chatJsonForDeckAgentStrict(systemPrompt, userPrompt, maxOutputTokens, preferredModels, true);
    }

    public ChatResult chatJsonForDeckAgentStrictUnmetered(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> preferredModels
    ) throws Exception {
        return chatJsonForDeckAgentStrict(systemPrompt, userPrompt, maxOutputTokens, preferredModels, false);
    }

    private ChatResult chatJsonForDeckAgentStrict(
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        List<String> preferredModels,
        boolean accountUsage
    ) throws Exception {
        ModelConfigEntity active = modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK).orElse(null);
        List<ModelConfigEntity> configs = new ArrayList<>(modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK).stream()
            .filter(ModelConfigEntity::isActive)
            .toList());
        if (active != null
            && StringUtils.hasText(active.getApiKey())
            && StringUtils.hasText(active.getBaseUrl())
            && strongModelScore(active.getProviderName(), active.getModelName(), active.getBaseUrl()) <= 3) {
            configs = List.of(active);
        }
        configs.sort(Comparator.comparing(row -> strongModelScore(row.getProviderName(), row.getModelName(), row.getBaseUrl())));
        List<ModelRoute> routes = new ArrayList<>();
        for (ModelConfigEntity row : configs) {
            if (!StringUtils.hasText(row.getApiKey()) || !StringUtils.hasText(row.getBaseUrl())) continue;
            boolean strongConfiguredModel = strongModelScore(row.getProviderName(), row.getModelName(), row.getBaseUrl()) <= 3;
            boolean canTryPreferred = routeCanTryPreferredModels(row.getBaseUrl(), row.getProviderName());
            if (!strongConfiguredModel && !canTryPreferred) continue;
            LinkedHashSet<String> models = new LinkedHashSet<>();
            if (canTryPreferred) preferredModels.forEach(models::add);
            if (strongConfiguredModel) models.add(row.getModelName());
            for (String model : models) {
                routes.add(new ModelRoute(
                    row.getBaseUrl(),
                    row.getApiKey(),
                    model,
                    row.getApiFormat(),
                    row.getAuthType(),
                    row.isFullUrl(),
                    row.getCustomUserAgent()
                ));
            }
        }
        // --- BACKUP POOL ---
        List<ModelConfigEntity> backupConfigs = modelConfigRepository.findAllBySceneOrderByActiveDescUpdatedAtDesc("backup").stream()
            .filter(ModelConfigEntity::isActive)
            .filter(row -> StringUtils.hasText(row.getApiKey()) && StringUtils.hasText(row.getModelName()) && StringUtils.hasText(row.getBaseUrl()))
            .sorted(this::comparePoolRoute)
            .toList();
        for (ModelConfigEntity row : backupConfigs) {
            routes.add(new ModelRoute(
                row.getBaseUrl(),
                row.getApiKey(),
                row.getModelName(),
                row.getApiFormat(),
                row.getAuthType(),
                row.isFullUrl(),
                row.getCustomUserAgent()
            ));
        }
        if (routes.isEmpty()) {
            throw new IllegalStateException("PPT 生成专用模型池未检测到强模型 Key。请在管理员模型池切换到“组会汇报 / PPT生成”，配置 GPT-5/GPT-4.1/o3/Claude Opus/Sonnet/Gemini Pro/DeepSeek R1/Qwen 235B 等强模型后再生成。");
        }
        String lastError = "强模型不可用";
        LinkedHashSet<String> attempted = new LinkedHashSet<>();
        for (ModelRoute route : routes) {
            if (!StringUtils.hasText(route.model())) continue;
            String attemptKey = route.baseUrl() + " " + route.model();
            if (!attempted.add(attemptKey)) continue;
            try {
                return send(route.baseUrl(), route.apiKey(), route.model(), route.apiFormat(), route.authType(), route.fullUrl(), route.customUserAgent(), systemPrompt, userPrompt, maxOutputTokens, accountUsage);
            } catch (Exception error) {
                lastError = route.model() + "：" + error.getMessage();
            }
        }
        throw new IllegalStateException("严格 Agent 模式强模型全部不可用，已停止生成，避免降级产出劣质 PPT。最后错误：" + lastError);
    }

    private boolean routeCanTryPreferredModels(String baseUrl, String providerName) {
        String source = (Objects.toString(baseUrl, "") + " " + Objects.toString(providerName, "")).toLowerCase();
        return source.contains("openrouter.ai")
            || source.contains("api.openai.com")
            || source.contains("anthropic")
            || source.contains("generativelanguage")
            || source.contains("google")
            || source.contains("deepseek")
            || source.contains("dashscope")
            || source.contains("siliconflow")
            || source.contains("volces")
            || source.contains("moonshot");
    }

    private int strongModelScore(String providerName, String modelName, String baseUrl) {
        String source = (Objects.toString(providerName, "") + " " + Objects.toString(modelName, "") + " " + Objects.toString(baseUrl, "")).toLowerCase();
        if (source.contains("gpt-5") || source.contains("opus") || source.contains("o3")) return 0;
        if (source.contains("sonnet") || source.contains("gemini") && source.contains("pro")) return 1;
        if (source.contains("deepseek") && source.contains("r1")) return 2;
        if (source.contains("qwen") && (source.contains("235b") || source.contains("max") || source.contains("thinking"))) return 3;
        if (source.contains("free") || source.contains("flash") || source.contains("mini")) return 8;
        return 5;
    }

    private LinkedHashSet<String> expandedModels(String modelName, String baseUrl, String providerName, List<String> fallbackModels) {
        String activeModel = normalizeOpenCodeFreeModel(modelName);
        boolean openCodeRoute = activeModel != null && activeModel.startsWith("oc/")
            || (providerName != null && providerName.toLowerCase().contains("9router"))
            || (baseUrl != null && baseUrl.toLowerCase().contains("abc-tunnel"));
        boolean openRouterRoute = baseUrl != null && baseUrl.toLowerCase().contains("openrouter.ai");
        LinkedHashSet<String> models = new LinkedHashSet<>();
        models.add(activeModel);
        if (openRouterRoute) {
            models.addAll(OPENROUTER_FREE_FALLBACKS);
        } else if (openCodeRoute) {
            for (String fallbackModel : fallbackModels) {
                models.add(normalizeOpenCodeFreeModel(fallbackModel));
            }
        }
        return models;
    }

    public ChatResult test(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent
    ) throws Exception {
        String resolvedKey = resolveKey(apiKey);
        String resolvedModel = normalizeOpenCodeFreeModel(model);
        if (!StringUtils.hasText(resolvedModel)) {
            List<ModelInfo> models = fetchModels(baseUrl, resolvedKey, apiFormat, authType, fullUrl, null, customUserAgent);
            if (models.isEmpty()) throw new IllegalStateException("模型列表为空，请手动填写模型名称");
            resolvedModel = models.get(0).id();
        }
        if (!MODEL_TEST_LIMITER.tryAcquire(1, TimeUnit.SECONDS)) {
            throw new IllegalStateException("测速队列繁忙，请稍后重试");
        }
        try {
            return send(
                baseUrl, resolvedKey, resolvedModel, apiFormat, authType, fullUrl, customUserAgent,
                "You are a connection tester. Reply with only OK.",
                "OK",
                24,
                false
            );
        } finally {
            MODEL_TEST_LIMITER.release();
        }
    }

    public ChatResult chat(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent,
        String userPrompt
    ) throws Exception {
        return send(
            baseUrl, resolveKey(apiKey), normalizeOpenCodeFreeModel(model), apiFormat, authType, fullUrl, customUserAgent,
            "You are a helpful assistant. Answer the user's question directly.",
            userPrompt,
            1024
        );
    }

    public ChatResult chatForConfigTest(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent,
        String userPrompt
    ) throws Exception {
        return send(
            baseUrl, resolveKey(apiKey), normalizeOpenCodeFreeModel(model), apiFormat, authType, fullUrl, customUserAgent,
            "You are a connection tester. Answer briefly.",
            userPrompt,
            1024,
            false
        );
    }

    public List<ModelInfo> fetchModels(
        String baseUrl,
        String apiKey,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String modelsUrl,
        String customUserAgent
    ) throws Exception {
        String resolvedKey = resolveKey(apiKey);
        List<String> candidates = buildModelCandidates(baseUrl, fullUrl, modelsUrl);
        String lastError = "没有可尝试的模型端点";
        for (String url : candidates) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET();
                applyHeaders(builder, resolvedKey, authType, apiFormat, customUserAgent);
                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    List<ModelInfo> models = parseModels(response.body());
                    models.sort(Comparator.comparing(ModelInfo::id));
                    return models;
                }
                lastError = responseError(response);
                if (response.statusCode() != 404 && response.statusCode() != 405) break;
            } catch (Exception exception) {
                lastError = exception.getMessage();
                if (shouldTryCurlModelFallback(exception)) {
                    List<ModelInfo> models = fetchModelsWithCurl(url, resolvedKey, authType, apiFormat, customUserAgent);
                    models.sort(Comparator.comparing(ModelInfo::id));
                    return models;
                }
            }
        }
        throw new IllegalStateException("获取模型列表失败：" + lastError);
    }

    private boolean shouldTryCurlModelFallback(Exception exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) return false;
        String normalized = message.toLowerCase();
        return normalized.contains("subject alternative")
            || normalized.contains("no subject alternative dns name")
            || normalized.contains("sslhandshake")
            || normalized.contains("certificate");
    }

    private List<ModelInfo> fetchModelsWithCurl(
        String url,
        String apiKey,
        String authType,
        String apiFormat,
        String customUserAgent
    ) throws Exception {
        List<String> command = new ArrayList<>(List.of(
            "curl",
            "-sS",
            "--connect-timeout", "8",
            "--max-time", "20"
        ));
        addCurlHeaders(command, apiKey, authType, apiFormat, customUserAgent);
        command.add(url);

        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes();
        boolean finished = process.waitFor(22, TimeUnit.SECONDS);
        String body = new String(output, StandardCharsets.UTF_8);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("curl 模型列表请求超时");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(body.isBlank() ? "curl 模型列表请求失败" : body.trim());
        }
        List<ModelInfo> models = parseModels(body);
        if (models.isEmpty() && body.stripLeading().startsWith("<")) {
            throw new IllegalStateException("模型列表地址返回了网页 HTML，请填写正确的 /v1/models 地址");
        }
        return models;
    }

    private void addCurlHeaders(
        List<String> command,
        String apiKey,
        String authType,
        String apiFormat,
        String customUserAgent
    ) {
        String resolvedAuth = StringUtils.hasText(authType)
            ? authType : "anthropic".equals(apiFormat) ? "x-api-key" : "bearer";
        if (StringUtils.hasText(apiKey) && !"none".equals(resolvedAuth)) {
            String headerName = "bearer".equals(resolvedAuth) ? "Authorization" : resolvedAuth;
            String headerValue = "bearer".equals(resolvedAuth) ? "Bearer " + apiKey.trim() : apiKey.trim();
            command.add("-H");
            command.add(headerName + ": " + headerValue);
        }
        if ("anthropic".equals(apiFormat)) {
            command.add("-H");
            command.add("anthropic-version: 2023-06-01");
        }
        if (StringUtils.hasText(customUserAgent)) {
            command.add("-A");
            command.add(customUserAgent.trim());
        }
    }
    private ChatResult send(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent,
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens
    ) throws Exception {
        return send(baseUrl, apiKey, model, apiFormat, authType, fullUrl, customUserAgent, systemPrompt, userPrompt, maxOutputTokens, true);
    }

    private ChatResult send(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent,
        String systemPrompt,
        String userPrompt,
        int maxOutputTokens,
        boolean accountUsage
    ) throws Exception {
        long startedAt = System.nanoTime();
        try {
            if (!StringUtils.hasText(baseUrl)) throw new IllegalArgumentException("Base URL 不能为空");
            if (!StringUtils.hasText(model)) throw new IllegalArgumentException("模型名称不能为空");
            model = normalizeOpenCodeFreeModel(model);
            if (isCodexBaseUrl(baseUrl, apiFormat)) {
                ChatResult result = sendViaCodexCli(baseUrl, apiKey, model, systemPrompt, userPrompt);
                if (accountUsage) recordUsage(result.modelName(), estimateUsage(systemPrompt, userPrompt, result.content()), systemPrompt, userPrompt, elapsedMs(startedAt));
                return result;
            }
            List<String> endpoints = buildRequestCandidates(baseUrl, normalizeFormat(apiFormat), fullUrl);
            String lastError = "没有可尝试的请求端点";
            for (String endpoint : endpoints) {
                HttpResponse<String> response = null;
                for (int tokenBudget : tokenBudgetCandidates(maxOutputTokens)) {
                    for (int attempt = 0; attempt < 3; attempt++) {
                        response = sendToEndpoint(
                            endpoint, apiKey, model, normalizeFormat(apiFormat), authType, customUserAgent, systemPrompt, userPrompt, tokenBudget
                        );
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            ChatResult parsed = parseChatResult(model, normalizeFormat(apiFormat), response.body());
                            if (accountUsage) {
                                UsageEstimate usage = parsed.totalTokens() > 0
                                    ? new UsageEstimate(parsed.promptTokens(), parsed.completionTokens(), parsed.totalTokens())
                                    : estimateUsage(systemPrompt, userPrompt, parsed.content());
                                recordUsage(parsed.modelName(), usage, systemPrompt, userPrompt, elapsedMs(startedAt));
                            }
                            return parsed;
                        }
                        lastError = responseError(response);
                        if (isTokenBudgetError(response, tokenBudget)) break;
                        if (!isRetryableStatus(response.statusCode()) || attempt == 2) break;
                        Thread.sleep(700L * (attempt + 1));
                    }
                    if (response == null || !isTokenBudgetError(response, tokenBudget)) break;
                }
                if (response == null || (response.statusCode() != 404 && response.statusCode() != 405)) break;
            }
            throw new IllegalStateException(lastError);
        } catch (Exception error) {
            if (accountUsage) recordFailure(model, systemPrompt, userPrompt, error, elapsedMs(startedAt));
            throw error;
        }
    }

    private List<Integer> tokenBudgetCandidates(int requested) {
        LinkedHashSet<Integer> budgets = new LinkedHashSet<>();
        budgets.add(Math.max(64, requested));
        if (requested > 3600) budgets.add(3200);
        if (requested > 2200) budgets.add(1800);
        if (requested > 1200) budgets.add(900);
        return new ArrayList<>(budgets);
    }

    private boolean isTokenBudgetError(HttpResponse<String> response, int tokenBudget) {
        if (response == null || response.statusCode() != 402 || tokenBudget <= 900) return false;
        String body = Objects.toString(response.body(), "").toLowerCase();
        return body.contains("fewer max") || body.contains("more credits") || body.contains("max tokens") || body.contains("credits");
    }

    private ChatResult sendViaCodexCli(
        String baseUrl,
        String apiKey,
        String model,
        String systemPrompt,
        String userPrompt
    ) throws Exception {
        if (!StringUtils.hasText(apiKey)) throw new IllegalArgumentException("中转站 Key 不能为空");
        String executable = Files.isExecutable(Path.of("/Applications/Codex.app/Contents/Resources/codex"))
            ? "/Applications/Codex.app/Contents/Resources/codex" : "codex";
        String providerBaseUrl = trimTrailingSlash(baseUrl.trim())
            .replaceFirst("/(?:v1/)?responses$", "");
        String prompt = systemPrompt + "\n\n用户问题：\n" + userPrompt;
        Path outputFile = Files.createTempFile("paperpilot-codex-output-", ".txt");
        Path logFile = Files.createTempFile("paperpilot-codex-log-", ".txt");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                executable,
                "exec",
                "--ignore-user-config",
                "--ephemeral",
                "--skip-git-repo-check",
                "--sandbox", "read-only",
                "--color", "never",
                "-m", model,
                "-c", "model_provider=\"paperpilot_relay\"",
                "-c", "model_providers.paperpilot_relay.name=\"PaperPilot Relay\"",
                "-c", "model_providers.paperpilot_relay.base_url=\"" + providerBaseUrl + "\"",
                "-c", "model_providers.paperpilot_relay.env_key=\"OPENAI_API_KEY\"",
                "-c", "model_providers.paperpilot_relay.wire_api=\"responses\"",
                "-c", "model_providers.paperpilot_relay.requires_openai_auth=true",
                "-o", outputFile.toString(),
                prompt
            );
            processBuilder.environment().put("OPENAI_API_KEY", apiKey.trim());
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(logFile.toFile());
            Process process = processBuilder.start();
            process.getOutputStream().close();
            if (!process.waitFor(120, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Codex 模型调用超时");
            }
            String content = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || !StringUtils.hasText(content)) {
                String log = Files.readString(logFile, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ").trim();
                if (log.length() > 300) log = log.substring(log.length() - 300);
                throw new IllegalStateException(
                    StringUtils.hasText(log) ? "Codex 调用失败：" + log : "Codex 模型返回为空"
                );
            }
            return new ChatResult(model, content, 0L, 0L, 0L, false);
        } finally {
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(logFile);
        }
    }

    private HttpResponse<String> sendToEndpoint(
        String endpoint,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        String customUserAgent,
        String systemPrompt,
        String userPrompt,
        int requestedMaxTokens
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        boolean codexEndpoint = isCodexEndpoint(endpoint, apiFormat);
        boolean connectionTest = systemPrompt.contains("connection tester");
        int maxTokens = connectionTest ? 24 : Math.max(64, requestedMaxTokens);
        if ("anthropic".equals(apiFormat)) {
            payload.put("model", model);
            payload.put("max_tokens", maxTokens);
            payload.put("system", systemPrompt);
            payload.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));
        } else if ("openai_responses".equals(apiFormat)) {
            payload.put("model", model);
            payload.put("instructions", systemPrompt);
            payload.put("store", false);
            if (codexEndpoint) {
                payload.put("input", List.of(Map.of(
                    "role", "user",
                    "content", List.of(Map.of("type", "input_text", "text", userPrompt))
                )));
                payload.put("tools", List.of());
                payload.put("tool_choice", "auto");
                payload.put("parallel_tool_calls", false);
                payload.put("stream", true);
                payload.put("include", List.of("reasoning.encrypted_content"));
            } else {
                payload.put("input", userPrompt);
                payload.put("max_output_tokens", maxTokens);
            }
        } else {
            payload.put("model", model);
            payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ));
            payload.put("temperature", connectionTest ? 0 : 0.25);
            payload.put("max_tokens", maxTokens);
        }
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(requestTimeoutSeconds(systemPrompt, userPrompt, connectionTest)))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        applyHeaders(builder, apiKey, authType, apiFormat, customUserAgent, codexEndpoint);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private int requestTimeoutSeconds(String systemPrompt, String userPrompt, boolean connectionTest) {
        if (connectionTest) return 7;
        String scene = inferModelConfigScene(systemPrompt, userPrompt);
        if (ModelConfigService.SCENE_TOPIC_RESEARCH.equals(scene)) return 90;
        if (ModelConfigService.SCENE_PAPER_QA.equals(scene)) return 90;
        if (ModelConfigService.SCENE_FORUM_MODERATION.equals(scene)) return 20;
        return 65;
    }

    private void applyHeaders(
        HttpRequest.Builder builder,
        String apiKey,
        String authType,
        String apiFormat,
        String customUserAgent
    ) {
        applyHeaders(builder, apiKey, authType, apiFormat, customUserAgent, false);
    }

    private void applyHeaders(
        HttpRequest.Builder builder,
        String apiKey,
        String authType,
        String apiFormat,
        String customUserAgent,
        boolean codexEndpoint
    ) {
        String resolvedAuth = StringUtils.hasText(authType)
            ? authType : "anthropic".equals(apiFormat) ? "x-api-key" : "bearer";
        if (StringUtils.hasText(apiKey) && !"none".equals(resolvedAuth)) {
            if ("x-api-key".equals(resolvedAuth)) builder.header("x-api-key", apiKey.trim());
            else if ("api-key".equals(resolvedAuth)) builder.header("api-key", apiKey.trim());
            else builder.header("Authorization", "Bearer " + apiKey.trim());
        }
        if ("anthropic".equals(apiFormat)) builder.header("anthropic-version", "2023-06-01");
        if (codexEndpoint) {
            String requestId = UUID.randomUUID().toString();
            builder.header("Accept", "text/event-stream");
            builder.header("User-Agent", StringUtils.hasText(customUserAgent)
                ? customUserAgent.trim()
                : "codex_cli_rs/0.77.0 (Mac OS 15.5.0; arm64) Apple_Terminal");
            builder.header("originator", "codex_cli_rs");
            builder.header("x-client-request-id", requestId);
            builder.header("session-id", requestId);
            builder.header("thread-id", requestId);
            builder.header("x-codex-installation-id", UUID.randomUUID().toString());
        } else if (StringUtils.hasText(customUserAgent)) {
            builder.header("User-Agent", customUserAgent.trim());
        }
    }

    private ChatResult parseChatResult(String model, String apiFormat, String body) throws Exception {
        String leadingBody = body.stripLeading();
        if ("openai_responses".equals(apiFormat)
            && (leadingBody.startsWith("event:") || leadingBody.startsWith("data:"))) {
            return parseResponsesStreamResult(model, body);
        }
        if ("openai_chat".equals(apiFormat)
            && (leadingBody.startsWith("event:") || leadingBody.startsWith("data:"))) {
            return parseChatStreamResult(model, body);
        }
        JsonNode root = objectMapper.readTree(stripMixedStreamTail(body));
        String content;
        if ("anthropic".equals(apiFormat)) {
            content = root.path("content").path(0).path("text").asText("");
        } else if ("openai_responses".equals(apiFormat)) {
            content = root.path("output_text").asText("");
            if (!StringUtils.hasText(content)) {
                StringBuilder parts = new StringBuilder();
                for (JsonNode output : root.path("output")) {
                    for (JsonNode item : output.path("content")) {
                        String text = item.path("text").asText("");
                        if (StringUtils.hasText(text)) parts.append(text);
                    }
                }
                content = parts.toString();
            }
        } else {
            JsonNode messageContent = root.path("choices").path(0).path("message").path("content");
            content = messageContent.asText("");
            if (!StringUtils.hasText(content) && messageContent.isArray()) {
                StringBuilder parts = new StringBuilder();
                for (JsonNode item : messageContent) {
                    String text = item.path("text").asText(item.path("content").asText(""));
                    if (StringUtils.hasText(text)) parts.append(text);
                }
                content = parts.toString();
            }
            if (!StringUtils.hasText(content)) {
                content = root.path("choices").path(0).path("text").asText("");
            }
            if (!StringUtils.hasText(content)) {
                content = root.path("choices").path(0).path("message").path("reasoning_content").asText("");
            }
            if (!StringUtils.hasText(content)) {
                content = root.path("choices").path(0).path("delta").path("content").asText("");
            }
            if (!StringUtils.hasText(content)) {
                content = root.path("choices").path(0).path("delta").path("reasoning_content").asText("");
            }
            if (!StringUtils.hasText(content)) {
                content = root.path("output_text").asText("");
            }
        }
        content = content.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        if (!StringUtils.hasText(content)) throw new IllegalStateException("模型返回为空");
        UsageEstimate usage = parseResponseUsage(root);
        return new ChatResult(model, content, usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.totalTokens() <= 0);
    }

    private UsageEstimate parseResponseUsage(JsonNode root) {
        JsonNode usage = root.path("usage");
        long promptTokens = firstLong(
            usage.path("prompt_tokens"),
            usage.path("input_tokens"),
            usage.path("promptTokens"),
            usage.path("inputTokens")
        );
        long completionTokens = firstLong(
            usage.path("completion_tokens"),
            usage.path("output_tokens"),
            usage.path("completionTokens"),
            usage.path("outputTokens")
        );
        JsonNode completionDetails = usage.path("completion_tokens_details");
        completionTokens += firstLong(
            completionDetails.path("reasoning_tokens"),
            completionDetails.path("accepted_prediction_tokens")
        );
        long totalTokens = firstLong(
            usage.path("total_tokens"),
            usage.path("totalTokens")
        );
        if (totalTokens <= 0 && (promptTokens > 0 || completionTokens > 0)) {
            totalTokens = promptTokens + completionTokens;
        }
        if (totalTokens > 0 && completionTokens <= 0 && promptTokens > 0) {
            completionTokens = Math.max(0L, totalTokens - promptTokens);
        }
        if (totalTokens > 0 && promptTokens <= 0 && completionTokens > 0) {
            promptTokens = Math.max(0L, totalTokens - completionTokens);
        }
        return new UsageEstimate(promptTokens, completionTokens, totalTokens);
    }

    private long firstLong(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (node == null || node.isMissingNode() || node.isNull()) continue;
            if (node.isNumber()) return Math.max(0L, node.asLong());
            String text = node.asText("");
            if (StringUtils.hasText(text)) {
                try {
                    return Math.max(0L, Long.parseLong(text.replace(",", "").trim()));
                } catch (Exception ignored) {
                    // Try the next node.
                }
            }
        }
        return 0L;
    }

    private String stripMixedStreamTail(String body) {
        if (body == null) return "";
        String trimmed = body.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) return trimmed;
        int lfMarker = trimmed.indexOf("\ndata:");
        int crlfMarker = trimmed.indexOf("\r\ndata:");
        int marker = lfMarker >= 0 ? lfMarker : crlfMarker;
        return marker >= 0 ? trimmed.substring(0, marker).trim() : trimmed;
    }

    private ChatResult parseResponsesStreamResult(String model, String body) throws Exception {
        StringBuilder content = new StringBuilder();
        String completedText = "";
        UsageEstimate usage = new UsageEstimate(0L, 0L, 0L);
        for (String line : body.split("\\R")) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.isBlank() || "[DONE]".equals(data)) continue;
            JsonNode event = objectMapper.readTree(data);
            UsageEstimate eventUsage = parseResponseUsage(event.path("response"));
            if (eventUsage.totalTokens() <= 0) eventUsage = parseResponseUsage(event);
            if (eventUsage.totalTokens() > 0) usage = eventUsage;
            String type = event.path("type").asText("");
            if ("response.output_text.delta".equals(type)) {
                content.append(event.path("delta").asText(""));
            } else if ("response.completed".equals(type)) {
                completedText = extractResponsesText(event.path("response"));
            } else if ("response.failed".equals(type) || "error".equals(type)) {
                String message = event.path("error").path("message").asText(
                    event.path("message").asText("模型流式响应失败")
                );
                throw new IllegalStateException(message);
            }
        }
        String result = content.length() > 0 ? content.toString() : completedText;
        result = result.trim();
        if (!StringUtils.hasText(result)) throw new IllegalStateException("模型返回为空");
        return new ChatResult(model, result, usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.totalTokens() <= 0);
    }

    private ChatResult parseChatStreamResult(String model, String body) throws Exception {
        StringBuilder content = new StringBuilder();
        UsageEstimate usage = new UsageEstimate(0L, 0L, 0L);
        for (String line : body.split("\\R")) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.isBlank() || "[DONE]".equals(data)) continue;
            JsonNode event = objectMapper.readTree(data);
            UsageEstimate eventUsage = parseResponseUsage(event);
            if (eventUsage.totalTokens() > 0) usage = eventUsage;
            JsonNode delta = event.path("choices").path(0).path("delta");
            String text = delta.path("content").asText("");
            if (StringUtils.hasText(text)) content.append(text);
            String reasoningText = delta.path("reasoning_content").asText("");
            if (!StringUtils.hasText(text) && StringUtils.hasText(reasoningText)) content.append(reasoningText);
            String messageText = event.path("choices").path(0).path("message").path("content").asText("");
            if (StringUtils.hasText(messageText)) content.append(messageText);
            String messageReasoning = event.path("choices").path(0).path("message").path("reasoning_content").asText("");
            if (!StringUtils.hasText(messageText) && StringUtils.hasText(messageReasoning)) content.append(messageReasoning);
        }
        String result = content.toString().trim();
        if (!StringUtils.hasText(result)) throw new IllegalStateException("模型返回为空");
        return new ChatResult(model, result, usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage.totalTokens() <= 0);
    }

    private String normalizeOpenCodeFreeModel(String model) {
        if (!StringUtils.hasText(model)) return model;
        String trimmed = model.trim();
        if (trimmed.startsWith("oc/")) return trimmed;
        String mapped = OPEN_CODE_FREE_MODEL_ALIASES.get(trimmed);
        if (StringUtils.hasText(mapped)) return mapped;
        mapped = OPEN_CODE_FREE_MODEL_ALIASES.get(trimmed.toLowerCase());
        return StringUtils.hasText(mapped) ? mapped : trimmed;
    }

    private UsageEstimate estimateUsage(String systemPrompt, String userPrompt, String responseText) {
        long promptTokens = estimateTokens((systemPrompt == null ? "" : systemPrompt) + "\n" + (userPrompt == null ? "" : userPrompt));
        long completionTokens = estimateTokens(responseText);
        return new UsageEstimate(promptTokens, completionTokens, promptTokens + completionTokens);
    }

    private long estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0L;
        long cjk = 0L;
        long compactChars = 0L;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) continue;
            if (isCjk(codePoint)) cjk++;
            else compactChars++;
        }
        long latinTokens = (long) Math.ceil(compactChars / 4.0);
        return Math.max(1L, cjk + latinTokens);
    }

    private boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
            || script == Character.UnicodeScript.HIRAGANA
            || script == Character.UnicodeScript.KATAKANA
            || script == Character.UnicodeScript.HANGUL;
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private void recordUsage(String modelName, UsageEstimate usage, String systemPrompt, String userPrompt, long latencyMs) {
        long totalTokens = usage.totalTokens();
        if (totalTokens <= 0) return;
        try {
            AppUserEntity user = currentUserService.getOrCreateDefaultUser();
            aiUsageService.recordAndCharge(
                user.getId(),
                modelName,
                inferModelConfigScene(systemPrompt, userPrompt),
                inferAction(systemPrompt, userPrompt),
                inferPaperTitle(systemPrompt, userPrompt),
                usage.promptTokens(),
                usage.completionTokens(),
                totalTokens,
                latencyMs
            );
        } catch (Exception ignored) {
            // Token accounting should never make a successful AI response fail.
        }
    }

    private void recordFailure(String modelName, String systemPrompt, String userPrompt, Exception error, long latencyMs) {
        try {
            AppUserEntity user = currentUserService.getOrCreateDefaultUser();
            long promptTokens = estimateUsage(systemPrompt, userPrompt, "").promptTokens();
            aiUsageService.recordFailure(
                user.getId(),
                StringUtils.hasText(modelName) ? normalizeOpenCodeFreeModel(modelName) : "unknown-model",
                inferModelConfigScene(systemPrompt, userPrompt),
                inferAction(systemPrompt, userPrompt),
                inferPaperTitle(systemPrompt, userPrompt),
                promptTokens,
                readableError(error),
                latencyMs
            );
        } catch (Exception ignored) {
            // Failure accounting should never change the original model error.
        }
    }

    private String readableError(Throwable error) {
        String message = error == null ? "" : Objects.toString(error.getMessage(), "");
        if (!StringUtils.hasText(message) && error != null) message = error.getClass().getSimpleName();
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 760 ? message.substring(0, 759) + "…" : message;
    }

    private String inferScene(String systemPrompt, String userPrompt) {
        String combined = ((systemPrompt == null ? "" : systemPrompt) + "\n" + (userPrompt == null ? "" : userPrompt)).toLowerCase();
        if (combined.contains("deep-research") || combined.contains("选题调研") || combined.contains("选题广场") || combined.contains("可执行选题") || combined.contains("topic research")) return ModelConfigService.SCENE_TOPIC_RESEARCH;
        if (combined.contains("translate") || combined.contains("翻译")) return "translate";
        if (combined.contains("meeting report") || combined.contains("组会论文综述生成") || combined.contains("deck agent")) return "report";
        if (combined.contains("学术研读助手") || combined.contains("论文研究助手") || combined.contains("用户当前选中") || combined.contains("用户问题") || combined.contains("question") || combined.contains("问答") || combined.contains("qa")) return "qa";
        if (combined.contains("文献综述生成") || combined.contains("生成文献综述") || combined.contains("生成综述")) return "summary";
        return "qa";
    }

    private String inferModelConfigScene(String systemPrompt, String userPrompt) {
        String combined = ((systemPrompt == null ? "" : systemPrompt) + "\n" + (userPrompt == null ? "" : userPrompt)).toLowerCase();
        if (combined.contains("内容审核") || combined.contains("审核员") || combined.contains("risklevel") || combined.contains("approved(boolean)")) {
            return ModelConfigService.SCENE_FORUM_MODERATION;
        }
        if (combined.contains("deep-research") || combined.contains("选题调研") || combined.contains("选题广场") || combined.contains("可执行选题") || combined.contains("topic research")) {
            return ModelConfigService.SCENE_TOPIC_RESEARCH;
        }
        if (combined.contains("meeting report") || combined.contains("组会论文综述生成") || combined.contains("ppt agent") || combined.contains("deck agent")) {
            return ModelConfigService.SCENE_MEETING_DECK;
        }
        if (combined.contains("学术研读助手") || combined.contains("论文研究助手") || combined.contains("用户当前选中") || combined.contains("用户问题") || combined.contains("question") || combined.contains("问答") || combined.contains("qa") || combined.contains("回答用户")) {
            return ModelConfigService.SCENE_PAPER_QA;
        }
        if (combined.contains("文献综述生成") || combined.contains("生成文献综述") || combined.contains("生成综述")) {
            return ModelConfigService.SCENE_PAPER_REVIEW;
        }
        return ModelConfigService.SCENE_PAPER_QA;
    }

    private String inferAction(String systemPrompt, String userPrompt) {
        String scene = inferScene(systemPrompt, userPrompt);
        String combined = ((systemPrompt == null ? "" : systemPrompt) + "\n" + (userPrompt == null ? "" : userPrompt));
        return switch (scene) {
            case ModelConfigService.SCENE_TOPIC_RESEARCH -> {
                yield combined.contains("质检返工") || combined.contains("quality_error") ? "选题调研返工" : "选题调研";
            }
            case "translate" -> "PDF双栏翻译";
            case "report" -> "组会论文综述生成";
            case "summary" -> "文献综述生成";
            case "qa" -> combined.contains("用户当前选中内容") || combined.contains("选区") ? "论文选区解读与问答" : "AI研读对话";
            default -> "AI研读对话";
        };
    }

    private String inferPaperTitle(String systemPrompt, String userPrompt) {
        String combined = (userPrompt == null ? "" : userPrompt) + "\n" + (systemPrompt == null ? "" : systemPrompt);
        Matcher quoted = Pattern.compile("《([^》]{2,180})》").matcher(combined);
        if (quoted.find()) return quoted.group(1).trim();
        Matcher titled = Pattern.compile("题目[:：]\\s*([^\\n]{2,180})").matcher(combined);
        if (titled.find()) return titled.group(1).trim();
        return "当前论文";
    }

    private String extractResponsesText(JsonNode root) {
        String direct = root.path("output_text").asText("");
        if (StringUtils.hasText(direct)) return direct;
        StringBuilder parts = new StringBuilder();
        for (JsonNode output : root.path("output")) {
            for (JsonNode item : output.path("content")) {
                String text = item.path("text").asText("");
                if (StringUtils.hasText(text)) parts.append(text);
            }
        }
        return parts.toString();
    }

    private boolean isCodexEndpoint(String endpoint, String apiFormat) {
        if (!"openai_responses".equals(apiFormat)) return false;
        String normalized = endpoint.toLowerCase();
        return normalized.contains("/codex/") || normalized.endsWith("/codex/responses");
    }

    private boolean isCodexBaseUrl(String baseUrl, String apiFormat) {
        if (!"openai_responses".equals(normalizeFormat(apiFormat))) return false;
        String normalized = trimTrailingSlash(baseUrl.trim().toLowerCase());
        return normalized.matches(".*/codex(?:/(?:v1/)?responses)?$");
    }

    private List<ModelInfo> parseModels(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode data = root.isArray() ? root : root.path("data");
        if (!data.isArray()) data = root.path("models");
        List<ModelInfo> models = new ArrayList<>();
        if (data.isArray()) {
            for (JsonNode item : data) {
                String id = item.isTextual() ? item.asText() : item.path("id").asText("");
                if (!StringUtils.hasText(id)) id = item.path("name").asText("");
                if (id.startsWith("models/")) id = id.substring("models/".length());
                if (StringUtils.hasText(id)) {
                    models.add(new ModelInfo(id, item.path("owned_by").asText(item.path("displayName").asText(""))));
                }
            }
        }
        return models;
    }

    private List<String> buildRequestCandidates(String rawBaseUrl, String apiFormat, boolean fullUrl) {
        String base = trimTrailingSlash(rawBaseUrl.trim());
        if (fullUrl || isKnownEndpoint(base)) return List.of(base);
        String endpoint = "anthropic".equals(apiFormat) ? "messages"
            : "openai_responses".equals(apiFormat) ? "responses" : "chat/completions";
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (base.matches(".*/v\\d+$")) {
            urls.add(base + "/" + endpoint);
        } else if (URI.create(base).getPath() == null || URI.create(base).getPath().isBlank() || "/".equals(URI.create(base).getPath())) {
            urls.add(base + "/v1/" + endpoint);
            urls.add(base + "/" + endpoint);
        } else {
            urls.add(base + "/" + endpoint);
            urls.add(base + "/v1/" + endpoint);
        }
        return List.copyOf(urls);
    }

    private List<String> buildModelCandidates(String rawBaseUrl, boolean fullUrl, String override) {
        if (StringUtils.hasText(override)) return List.of(override.trim());
        String base = trimTrailingSlash(rawBaseUrl.trim());
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (fullUrl || isKnownEndpoint(base)) {
            String root = base.replaceFirst("/v1/(chat/completions|responses|messages)$", "");
            urls.add(root + "/v1/models");
            urls.add(root + "/models");
            return List.copyOf(urls);
        }
        if (base.matches(".*/v\\d+$")) {
            urls.add(base + "/models");
            if (!base.endsWith("/v1")) urls.add(base + "/v1/models");
        } else {
            urls.add(base + "/v1/models");
            urls.add(base + "/models");
        }
        for (String suffix : COMPAT_SUFFIXES) {
            if (base.endsWith(suffix)) {
                String root = base.substring(0, base.length() - suffix.length());
                urls.add(root + "/v1/models");
                urls.add(root + "/models");
                break;
            }
        }
        return List.copyOf(urls);
    }

    private boolean isKnownEndpoint(String value) {
        return value.endsWith("/chat/completions") || value.endsWith("/responses") || value.endsWith("/messages");
    }

    private String resolveKey(String apiKey) {
        if (StringUtils.hasText(apiKey)) return apiKey.trim();
        ModelConfigEntity config = activeGeneralConfig();
        return config == null ? "" : config.getApiKey();
    }

    private ModelConfigEntity activeGeneralConfig() {
        return modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_GENERAL)
            .orElse(null);
    }

    private ModelConfigEntity activeSceneConfig(String scene) {
        ModelConfigEntity config = modelConfigRepository.findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(scene).orElse(null);
        if (shouldUseConfiguredPoolOnly(scene)) return config;
        return config == null ? activeGeneralConfig() : config;
    }

    private String normalizeFormat(String value) {
        if ("anthropic".equalsIgnoreCase(value)) return "anthropic";
        if ("openai_responses".equalsIgnoreCase(value)) return "openai_responses";
        return "openai_chat";
    }

    private String responseError(HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body().replaceAll("\\s+", " ").trim();
        if (body.length() > 300) body = body.substring(0, 300) + "…";
        return "接口返回 HTTP " + response.statusCode() + (body.isBlank() ? "" : "：" + body);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 408
            || statusCode == 429
            || statusCode == 500
            || statusCode == 502
            || statusCode == 503
            || statusCode == 504
            || statusCode == 520
            || statusCode == 522
            || statusCode == 523
            || statusCode == 524
            || statusCode == 530;
    }

    private String trimTrailingSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    public record ChatResult(
        String modelName,
        String content,
        long promptTokens,
        long completionTokens,
        long totalTokens,
        boolean estimatedUsage
    ) {
        public ChatResult(String modelName, String content) {
            this(modelName, content, 0L, 0L, 0L, true);
        }
    }
    private record UsageEstimate(long promptTokens, long completionTokens, long totalTokens) {}
    public record ModelInfo(String id, String ownedBy) {}
    private record ModelRoute(
        String baseUrl,
        String apiKey,
        String model,
        String apiFormat,
        String authType,
        boolean fullUrl,
        String customUserAgent
    ) {}
}
