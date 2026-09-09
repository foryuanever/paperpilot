package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.dto.TranslateRequest;
import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.vo.TranslateResultVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TranslateService {

    private static final int MAX_CHUNK_SIZE = 4500;
    private static final Map<String, String> PROVIDER_LABELS = new LinkedHashMap<>() {{
        put("google-web", "谷歌翻译");
        put("google", "谷歌翻译");
        put("google-api", "Google(API)");
        put("bing", "微软翻译");
        put("cnki", "CNKI 翻译");
        put("deeplx", "DeepLX");
        put("baidu", "百度翻译");
        put("youdao", "有道翻译");
        put("huoshan-web", "火山翻译");
        put("tencent-transmart", "腾讯 TranSmart");
        put("libretranslate", "LibreTranslate");
        put("mtranserver", "MTranServer");
        put("microsoft", "微软翻译");
        put("tencent", "腾讯翻译");
        put("deepl", "DeepL");
        put("ai", "AI学术翻译（消耗1积分）");
    }};

    private final TranslationRecordRepository translationRecordRepository;
    private final CurrentUserService currentUserService;
    private final AiChatService aiChatService;
    private final AiUsageService aiUsageService;
    private final Semaphore baiduLimiter = new Semaphore(1, true);
    private final AtomicLong lastBaiduCallAt = new AtomicLong(0);
    private final Semaphore googleLimiter = new Semaphore(4, true);
    private final java.util.Random random = new java.util.Random();
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"
    };
    private static final String[] GOOGLE_HOSTS = {
        "translate.googleapis.com",
        "translate.google.com",
        "clients5.google.com",
        "translate.google.co.jp",
        "translate.google.co.uk",
        "translate.google.de",
        "translate.google.fr",
        "translate.google.com.hk",
        "translate.google.com.tw",
        "translate.google.co.kr",
        "translate.google.com.sg"
    };

    private static boolean googleOnline = true;
    private static long lastGoogleFailureTime = 0L;

    private static boolean isGoogleAvailable() {
        if (googleOnline) {
            return true;
        }
        if (System.currentTimeMillis() - lastGoogleFailureTime > 300000L) { // 5 minutes
            return true;
        }
        return false;
    }

    public TranslateService(
        TranslationRecordRepository translationRecordRepository,
        CurrentUserService currentUserService,
        AiChatService aiChatService,
        AiUsageService aiUsageService
    ) {
        this.translationRecordRepository = translationRecordRepository;
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
        this.aiUsageService = aiUsageService;
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paperpilot.translate.deepl-api-key:}")
    private String deeplApiKey;

    @Value("${paperpilot.translate.baidu-app-id:}")
    private String baiduAppId;

    @Value("${paperpilot.translate.baidu-secret:}")
    private String baiduSecret;

    @Value("${paperpilot.translate.microsoft-key:}")
    private String microsoftKey;

    @Value("${paperpilot.translate.microsoft-region:eastasia}")
    private String microsoftRegion;

    @Value("${paperpilot.translate.tencent-secret-id:}")
    private String tencentSecretId;

    @Value("${paperpilot.translate.tencent-secret-key:}")
    private String tencentSecretKey;

    @Value("${paperpilot.translate.tencent-region:ap-guangzhou}")
    private String tencentRegion;

    @Value("${paperpilot.translate.deeplx-endpoint:}")
    private String deeplxEndpoint;

    @Value("${paperpilot.translate.libretranslate-endpoint:}")
    private String libreTranslateEndpoint;

    @Value("${paperpilot.translate.mtranserver-endpoint:}")
    private String mtranServerEndpoint;

    @Value("${paperpilot.translate.google-api-key:}")
    private String googleApiKey;

    private final Map<String, String> translationCache = java.util.Collections.synchronizedMap(
        new java.util.LinkedHashMap<String, String>(1000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > 10000;
            }
        }
    );

    public TranslateResultVO translate(TranslateRequest request) {
        String provider = normalizeProvider(request.getProvider());
        String sourceLang = normalizeLang(request.getSourceLang(), "auto");
        String targetLang = normalizeLang(request.getTargetLang(), "zh-CN");
        String text = request.getText() == null ? "" : request.getText().trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("待翻译文本不能为空");
        }

        String cacheKey = provider + "|" + sourceLang + "|" + targetLang + "|" + text;
        boolean meteredSelectionAi = "ai".equals(provider) && "selection_ai_translate".equals(request.getUsageScene());
        if (!meteredSelectionAi && translationCache.containsKey(cacheKey)) {
            return result(provider, sourceLang, targetLang, translationCache.get(cacheKey), false);
        }

        long startTime = System.currentTimeMillis();
        long charCount = text.length();
        Exception firstError = null;

        try {
            String translated = translateWithProvider(provider, text, sourceLang, targetLang, request.getUsageScene());
            long duration = System.currentTimeMillis() - startTime;
            saveRecord(provider, sourceLang, targetLang, charCount, duration, true, null, request.getUsageScene(), request.getPaperTitle(), request.getTranslationMode());
            translationCache.put(cacheKey, translated);
            return result(provider, sourceLang, targetLang, translated, false);
        } catch (Exception error) {
            firstError = error;
            long duration = System.currentTimeMillis() - startTime;
            saveRecord(provider, sourceLang, targetLang, charCount, duration, false, describeFailure(error), request.getUsageScene(), request.getPaperTitle(), request.getTranslationMode());
        }

        for (String fallbackProvider : fallbackProviders(provider)) {
            long fallbackStart = System.currentTimeMillis();
            try {
                String translated = translateWithProvider(fallbackProvider, text, sourceLang, targetLang, request.getUsageScene());
                saveRecord(fallbackProvider, sourceLang, targetLang, charCount, System.currentTimeMillis() - fallbackStart, true, null, request.getUsageScene(), request.getPaperTitle(), request.getTranslationMode());
                translationCache.put(cacheKey, translated);
                return result(fallbackProvider, sourceLang, targetLang, translated, true);
            } catch (Exception error) {
                saveRecord(fallbackProvider, sourceLang, targetLang, charCount, System.currentTimeMillis() - fallbackStart, false, describeFailure(error), request.getUsageScene(), request.getPaperTitle(), request.getTranslationMode());
            }
        }

        throw new IllegalStateException(
            PROVIDER_LABELS.getOrDefault(provider, provider) + "失败: " + describeFailure(firstError),
            firstError
        );
    }

    private TranslateResultVO result(String provider, String sourceLang, String targetLang, String translated, boolean fallback) {
        TranslateResultVO result = new TranslateResultVO();
        result.setProvider(provider);
        result.setProviderLabel(PROVIDER_LABELS.getOrDefault(provider, provider));
        result.setSourceLang(sourceLang);
        result.setTargetLang(targetLang);
        result.setTranslatedText(translated);
        result.setFallback(fallback);
        return result;
    }

    private List<String> fallbackProviders(String provider) {
        if ("google".equals(provider) || "google-web".equals(provider) || "google-api".equals(provider)) {
            return List.of();
        }
        List<String> preferred = List.of("huoshan-web", "youdao", "google", "bing", "baidu");
        List<String> providers = new ArrayList<>();
        for (String item : preferred) {
            if (!item.equals(provider) && isProviderConfigured(item)) {
                providers.add(item);
            }
        }
        return providers;
    }

    private String translateWithProvider(String provider, String text, String sourceLang, String targetLang, String usageScene) throws Exception {
        return switch (provider) {
            case "google-web" -> translateWithGoogle(text, sourceLang, targetLang);
            case "google" -> translateWithGoogle(text, sourceLang, targetLang);
            case "google-api" -> translateWithGoogleApi(text, sourceLang, targetLang);
            case "youdao" -> translateWithYoudao(text, sourceLang, targetLang);
            case "bing" -> translateWithBing(text, sourceLang, targetLang);
            case "huoshanweb", "huoshan-web" -> translateWithHuoshanWeb(text, sourceLang, targetLang);
            case "deeplx" -> translateWithDeepLX(text, sourceLang, targetLang);
            case "libretranslate" -> translateWithLibreTranslate(text, sourceLang, targetLang);
            case "mtranserver" -> translateWithMTranServer(text, sourceLang, targetLang);
            case "deepl" -> translateWithDeepL(text, sourceLang, targetLang);
            case "baidu" -> translateWithBaiduRateLimited(text, sourceLang, targetLang);
            case "microsoft" -> translateWithMicrosoft(text, sourceLang, targetLang);
            case "tencent" -> translateWithTencent(text, sourceLang, targetLang);
            case "ai" -> translateWithAi(text, sourceLang, targetLang, usageScene);
            case "cnki", "tencent-transmart" ->
                throw new IllegalStateException("该网页引擎需要桌面端本机插件模式，当前后端暂不可用");
            default -> throw new IllegalArgumentException("不支持的翻译引擎: " + provider);
        };
    }

    private String translateWithAi(String text, String sourceLang, String targetLang, String usageScene) throws Exception {
        String systemPrompt = "你是一个专业的学术翻译助手。请将用户输入的学术论文片段精确翻译成简体中文。要求：保持学术词汇专业性，译文流顺，符合中文阅读习惯，不要输出任何多余的解释、引言或Markdown标记（如果原文有Markdown格式，保留原文格式即可）。";
        String userPrompt = "请翻译以下内容：\n\n" + text;
        Long userId = null;
        if ("selection_ai_translate".equals(usageScene)) {
            userId = currentUserService.getOrCreateDefaultUserId();
            aiUsageService.assertPointsAvailable(userId, 1, "选中 AI 翻译");
        }
        AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallbackForSceneAndModel(
            systemPrompt, userPrompt, 2000, "selection_translation", "", false, StringUtils::hasText
        );
        if ("selection_ai_translate".equals(usageScene)) {
            aiUsageService.recordAndChargeSpecial(
                userId, result.modelName(), "selection_translation", "选中 AI 翻译", "选中内容",
                result.promptTokens(), result.completionTokens(), result.totalTokens(), 1
            );
        }
        return result.content().trim();
    }

    private void saveRecord(String provider, String sourceLang, String targetLang, long charCount, long latencyMs, boolean success, String errorMessage, String usageScene, String paperTitle, String translationMode) {
        try {
            TranslationRecordEntity record = new TranslationRecordEntity();
            record.setUserId(currentUserService.getOrCreateDefaultUserId());
            record.setPaperTitle(paperTitle);
            record.setTranslationMode(translationMode == null || translationMode.isBlank()
                ? ("immersive".equalsIgnoreCase(usageScene) ? "沉浸式翻译" : "对照翻译") : translationMode);
            record.setProvider(provider);
            record.setRoute(routeForScene(usageScene, provider));
            record.setSourceLang(sourceLang);
            record.setTargetLang(targetLang);
            record.setClientType("backend");
            record.setCharCount(charCount);
            record.setLatencyMs(latencyMs);
            record.setSuccess(success);
            record.setErrorMessage(truncate(errorMessage, 1000));
            translationRecordRepository.save(record);
        } catch (Exception e) {
            System.err.println("Failed to save translation record: " + e.getMessage());
        }
    }

    private String routeForScene(String usageScene, String provider) {
        String normalizedProvider = provider == null || provider.isBlank() ? "unknown" : provider;
        return switch (usageScene == null ? "" : usageScene.trim()) {
            case "bilingual_translate" -> "bilingual-translate/" + normalizedProvider;
            case "immersive_translate" -> "immersive-translate/" + normalizedProvider;
            case "selection_ai_translate" -> "selection-ai-translate/" + normalizedProvider;
            default -> "text-translate/" + normalizedProvider;
        };
    }

    private String describeFailure(Throwable error) {
        if (error == null) return "未知翻译异常";
        StringBuilder detail = new StringBuilder(error.getClass().getSimpleName());
        Throwable current = error;
        int depth = 0;
        while (current != null && depth++ < 4) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                detail.append(": ").append(message.replaceAll("\\s+", " ").trim());
            }
            current = current.getCause();
            if (current != null) detail.append(" | cause=").append(current.getClass().getSimpleName());
        }
        return detail.toString();
    }

    public List<Map<String, String>> listProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        for (Map.Entry<String, String> entry : PROVIDER_LABELS.entrySet()) {
            if ("google-web".equals(entry.getKey())) {
                continue;
            }
            if (!isProviderConfigured(entry.getKey())) {
                continue;
            }
            providers.add(Map.of(
                "id", entry.getKey(),
                "label", entry.getValue(),
                "configured", "true"
            ));
        }
        return providers;
    }


    private boolean isProviderConfigured(String provider) {
        return switch (provider) {
            case "google-web" -> true;
            case "google" -> true;
            case "google-api" -> StringUtils.hasText(googleApiKey);
            case "youdao" -> true;
            case "bing" -> true;
            case "ai" -> true;
            case "huoshanweb", "huoshan-web" -> true;
            case "cnki" -> true;
            case "tencent-transmart" -> true;
            case "deeplx" -> StringUtils.hasText(deeplxEndpoint);
            case "libretranslate" -> StringUtils.hasText(libreTranslateEndpoint);
            case "mtranserver" -> StringUtils.hasText(mtranServerEndpoint);
            case "deepl" -> StringUtils.hasText(deeplApiKey);
            case "baidu" -> StringUtils.hasText(baiduAppId) && StringUtils.hasText(baiduSecret);
            case "microsoft" -> StringUtils.hasText(microsoftKey);
            case "tencent" -> StringUtils.hasText(tencentSecretId) && StringUtils.hasText(tencentSecretKey);
            default -> false;
        };
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "google";
        }
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if ("googleapi".equals(normalized)) return "google-api";
        if ("microsoft-edge".equals(normalized)) return "bing";
        if ("deeplcustom".equals(normalized) || "deeplx-api".equals(normalized)) return "deeplx";
        if ("huoshanweb".equals(normalized)) return "huoshan-web";
        if ("tencenttransmart".equals(normalized) || "transmart".equals(normalized)) return "tencent-transmart";
        return normalized;
    }

    private String normalizeLang(String lang, String fallback) {
        if (!StringUtils.hasText(lang)) {
            return fallback;
        }
        return lang.trim();
    }

    private String translateWithGoogle(String text, String sourceLang, String targetLang) throws Exception {
        List<String> chunks = splitText(text, 1200);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            String part = translateChunkWithGoogleAndRetry(chunk, sourceLang, targetLang);
            if (StringUtils.hasText(part)) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append(part.trim());
            }
        }
        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到译文");
        }
        return builder.toString();
    }

    private String translateChunkWithGoogleAndRetry(String chunk, String sourceLang, String targetLang) throws Exception {
        if (!isGoogleAvailable()) {
            throw new IllegalStateException("谷歌翻译目前不可用（已开启熔断保护，5分钟内自动跳过）");
        }
        googleLimiter.acquire();
        try {
            // Introduce a tiny staggered delay to prevent bursting
            Thread.sleep(20 + random.nextInt(30));

            Exception lastEx = null;
            for (int attempt = 0; attempt < 4; attempt++) {
                String host = GOOGLE_HOSTS[random.nextInt(GOOGLE_HOSTS.length)];
                String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];

                String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
                String sl = "auto".equalsIgnoreCase(sourceLang) ? "auto" : sourceLang;
                String tl = targetLang;
                String url = "https://" + host + "/translate_a/single?client=gtx&sl=" + sl + "&tl=" + tl + "&dt=t&q=" + encoded;

                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(4))
                        .header("User-Agent", userAgent)
                        .header("Accept", "*/*")
                        .header("Cache-Control", "no-cache")
                        .GET()
                        .build();
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        JsonNode root = objectMapper.readTree(response.body());
                        String part = extractGoogleText(root);
                        if (StringUtils.hasText(part)) {
                            return part;
                        }
                    } else {
                        throw new IllegalStateException("Google returns status " + response.statusCode());
                    }
                } catch (Exception ex) {
                    lastEx = ex;
                    // Exponential backoff + random jitter
                    long delay = (long) Math.pow(2, attempt) * 200L + random.nextInt(200);
                    Thread.sleep(delay);
                }
            }
            googleOnline = false;
            lastGoogleFailureTime = System.currentTimeMillis();
            throw new IllegalStateException("谷歌翻译请求重试耗尽失败: " + (lastEx != null ? lastEx.getMessage() : "未知错误"));
        } finally {
            googleLimiter.release();
        }
    }

    private String extractGoogleText(JsonNode root) {
        if (root == null || !root.isArray() || root.isEmpty()) {
            return "";
        }
        JsonNode segments = root.get(0);
        if (segments == null || !segments.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode segment : segments) {
            if (segment.isArray() && !segment.isEmpty()) {
                builder.append(segment.get(0).asText(""));
            }
        }
        return builder.toString();
    }

    private String translateWithGoogleApi(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(googleApiKey)) {
            throw new IllegalStateException("未配置 Google Translation API Key");
        }
        String endpoint = "https://translation.googleapis.com/language/translate/v2?key="
            + URLEncoder.encode(googleApiKey, StandardCharsets.UTF_8);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("q", text);
        payload.put("target", normalizeGoogleApiTarget(targetLang));
        payload.put("format", "text");
        if (!"auto".equalsIgnoreCase(sourceLang)) {
            payload.put("source", normalizeGoogleApiTarget(sourceLang));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode translations = objectMapper.readTree(response.body()).path("data").path("translations");
        if (!translations.isArray() || translations.isEmpty()) {
            throw new IllegalStateException("Google API 返回为空");
        }
        return translations.get(0).path("translatedText").asText("");
    }

    private String translateWithDeepLX(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(deeplxEndpoint)) {
            throw new IllegalStateException("未配置 DeepLX Endpoint");
        }
        String endpoint = deeplxEndpoint.replaceAll("/+$", "");
        if (!endpoint.endsWith("/translate")) {
            endpoint += "/translate";
        }
        String payload = objectMapper.writeValueAsString(Map.of(
            "text", text,
            "source_lang", "auto".equalsIgnoreCase(sourceLang) ? "AUTO" : mapDeepLSource(sourceLang),
            "target_lang", mapDeepLTarget(targetLang)
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String translated = firstJsonText(root, "data", "translation", "translatedText", "text", "result");
        if (!StringUtils.hasText(translated)) {
            throw new IllegalStateException("DeepLX 返回为空");
        }
        return translated;
    }

    private String translateWithLibreTranslate(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(libreTranslateEndpoint)) {
            throw new IllegalStateException("未配置 LibreTranslate Endpoint");
        }
        String endpoint = libreTranslateEndpoint.replaceAll("/+$", "");
        if (!endpoint.endsWith("/translate")) {
            endpoint += "/translate";
        }
        String payload = objectMapper.writeValueAsString(Map.of(
            "q", text,
            "source", "auto".equalsIgnoreCase(sourceLang) ? "auto" : mapLibreLang(sourceLang),
            "target", mapLibreLang(targetLang),
            "format", "text"
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String translated = firstJsonText(root, "translatedText", "translation", "text", "result");
        if (!StringUtils.hasText(translated)) {
            throw new IllegalStateException("LibreTranslate 返回为空");
        }
        return translated;
    }

    private String translateWithMTranServer(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(mtranServerEndpoint)) {
            throw new IllegalStateException("未配置 MTranServer Endpoint");
        }
        String endpoint = mtranServerEndpoint.replaceAll("/+$", "");
        if (!endpoint.endsWith("/translate")) {
            endpoint += "/translate";
        }
        String payload = objectMapper.writeValueAsString(Map.of(
            "text", text,
            "from", "auto".equalsIgnoreCase(sourceLang) ? "auto" : mapLibreLang(sourceLang),
            "to", mapLibreLang(targetLang)
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        String translated = firstJsonText(root, "translation", "translatedText", "text", "result", "data");
        if (!StringUtils.hasText(translated)) {
            throw new IllegalStateException("MTranServer 返回为空");
        }
        return translated;
    }

    private String translateWithYoudao(String text, String sourceLang, String targetLang) throws Exception {
        List<String> chunks = splitText(text);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            String translated = requestYoudaoDictionary(chunk);
            if (!StringUtils.hasText(translated)) {
                translated = requestYoudaoTranslate(chunk);
            }
            if (StringUtils.hasText(translated)) {
                if (!builder.isEmpty()) builder.append("\n\n");
                builder.append(translated);
            } else {
                throw new IllegalStateException("有道未返回译文");
            }
        }
        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到有道译文");
        }
        return builder.toString();
    }

    private String requestYoudaoDictionary(String text) {
        try {
            String url = "https://dict.youdao.com/jsonapi_s?doctype=json&jsonversion=4&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8)).header("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                .header("Accept", "application/json,text/plain,*/*").GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) return "";
            return objectMapper.readTree(response.body()).path("fanyi").path("tran").asText("").trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String requestYoudaoTranslate(String text) {
        try {
            String url = "https://fanyi.youdao.com/translate?doctype=json&type=AUTO&i=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8)).header("User-Agent", USER_AGENTS[random.nextInt(USER_AGENTS.length)])
                .header("Accept", "application/json,text/plain,*/*").GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) return "";
            JsonNode rows = objectMapper.readTree(response.body()).path("translateResult");
            return rows.isArray() && !rows.isEmpty() ? rows.get(0).path(0).path("tgt").asText("").trim() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String translateWithHuoshanWeb(String text, String sourceLang, String targetLang) throws Exception {
        List<String> chunks = splitText(text);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            String from = "auto".equalsIgnoreCase(sourceLang) ? "" : sourceLang.split("-")[0];
            String to = "auto".equalsIgnoreCase(targetLang) ? "zh" : targetLang.split("-")[0];
            String url = "https://translate.volcengine.com/crx/translate/v1/";

            Map<String, String> bodyMap = Map.of(
                "source_language", from,
                "target_language", to,
                "text", chunk
            );
            String bodyJson = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String translated = root.path("translation").asText("").trim();
            if (StringUtils.hasText(translated)) {
                if (!builder.isEmpty()) builder.append("\n\n");
                builder.append(translated);
            }
        }
        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到火山译文");
        }
        return builder.toString();
    }

    private String translateWithBing(String text, String sourceLang, String targetLang) throws Exception {
        String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
        HttpRequest pageRequest = HttpRequest.newBuilder(URI.create("https://www.bing.com/translator"))
            .timeout(Duration.ofSeconds(12))
            .header("User-Agent", UA)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .GET()
            .build();
        HttpResponse<String> pageResponse = httpClient.send(pageRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (pageResponse.statusCode() >= 400) {
            throw new IllegalStateException("微软翻译页面请求失败 " + pageResponse.statusCode());
        }
        String pageHtml = pageResponse.body();

        // Extract IG
        java.util.regex.Pattern igPattern = java.util.regex.Pattern.compile("IG:\"([A-F0-9]+)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher igMatcher = igPattern.matcher(pageHtml);
        String ig = igMatcher.find() ? igMatcher.group(1) : "";

        // Extract IID
        java.util.regex.Pattern iidPattern = java.util.regex.Pattern.compile("data-iid=\"([^\"]+)\"");
        java.util.regex.Matcher iidMatcher = iidPattern.matcher(pageHtml);
        String iid = iidMatcher.find() ? iidMatcher.group(1) : "translator.5024";

        // Extract key and token from params_AbusePreventionHelper = [key, "token", timeout];
        java.util.regex.Pattern helperPattern = java.util.regex.Pattern.compile("params_AbusePreventionHelper\\s*=\\s*\\[\\s*(-?\\d+)\\s*,\\s*\"([^\"]+)\"");
        java.util.regex.Matcher helperMatcher = helperPattern.matcher(pageHtml);
        String key = "";
        String token = "";
        if (helperMatcher.find()) {
            key = helperMatcher.group(1);
            token = helperMatcher.group(2);
        }

        List<String> chunks = splitText(text);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            String from = mapMicrosoftEdgeLang(sourceLang);
            String to = mapMicrosoftEdgeLang(targetLang);
            if ("auto".equalsIgnoreCase(from)) {
                from = "auto-detect";
            }
            if ("zh-CN".equalsIgnoreCase(to)) {
                to = "zh-Hans";
            }

            String url = "https://www.bing.com/ttranslatev3?isVertical=1"
                + (StringUtils.hasText(ig) ? "&IG=" + ig : "")
                + "&IID=" + URLEncoder.encode(iid, StandardCharsets.UTF_8);

            String bodyString = "fromLang=" + URLEncoder.encode(from, StandardCharsets.UTF_8)
                + "&to=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(chunk, StandardCharsets.UTF_8);
            if (StringUtils.hasText(key)) {
                bodyString += "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);
            }
            if (StringUtils.hasText(token)) {
                bodyString += "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bing.com/translator")
                .header("Origin", "https://www.bing.com")
                .POST(HttpRequest.BodyPublishers.ofString(bodyString))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String translated = "";
            if (root.isArray() && !root.isEmpty() && root.get(0).has("translations")) {
                JsonNode trans = root.get(0).path("translations");
                if (trans.isArray() && !trans.isEmpty()) {
                    translated = trans.get(0).path("text").asText("").trim();
                }
            } else {
                JsonNode trans = root.path("translations");
                if (trans.isArray() && !trans.isEmpty()) {
                    translated = trans.get(0).path("text").asText("").trim();
                } else {
                    translated = root.path("text").asText("").trim();
                }
            }

            if (StringUtils.hasText(translated)) {
                if (!builder.isEmpty()) builder.append("\n\n");
                builder.append(translated);
            }
        }

        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到微软译文");
        }
        return builder.toString();
    }

    private String translateWithDeepL(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(deeplApiKey)) {
            throw new IllegalStateException("未配置 DeepL API Key");
        }
        String endpoint = deeplApiKey.endsWith(":fx")
            ? "https://api-free.deepl.com/v2/translate"
            : "https://api.deepl.com/v2/translate";
        String body = "text=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
            + "&target_lang=" + URLEncoder.encode(mapDeepLTarget(targetLang), StandardCharsets.UTF_8);
        if (!"auto".equalsIgnoreCase(sourceLang)) {
            body += "&source_lang=" + URLEncoder.encode(mapDeepLSource(sourceLang), StandardCharsets.UTF_8);
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "DeepL-Auth-Key " + deeplApiKey)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode translations = root.path("translations");
        if (!translations.isArray() || translations.isEmpty()) {
            throw new IllegalStateException("DeepL 返回为空");
        }
        return translations.get(0).path("text").asText("");
    }

    private String translateWithBaiduRateLimited(String text, String sourceLang, String targetLang) throws Exception {
        baiduLimiter.acquire();
        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastBaiduCallAt.get();
            if (elapsed < 1_105) {
                Thread.sleep(1_105 - elapsed);
            }
            String translated = translateWithBaidu(text, sourceLang, targetLang);
            lastBaiduCallAt.set(System.currentTimeMillis());
            return translated;
        } finally {
            baiduLimiter.release();
        }
    }

    private String translateWithBaidu(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(baiduAppId) || !StringUtils.hasText(baiduSecret)) {
            throw new IllegalStateException("未配置百度翻译 AppId / Secret");
        }

        String salt = String.valueOf(System.currentTimeMillis());
        String signStr = baiduAppId + text + salt + baiduSecret;
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] array = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(Integer.toHexString((b & 0xFF) | 0x100), 1, 3);
        }
        String sign = sb.toString();

        String body = "q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
            + "&from=" + URLEncoder.encode(mapBaiduLang(sourceLang), StandardCharsets.UTF_8)
            + "&to=" + URLEncoder.encode(mapBaiduLang(targetLang), StandardCharsets.UTF_8)
            + "&appid=" + URLEncoder.encode(baiduAppId, StandardCharsets.UTF_8)
            + "&salt=" + URLEncoder.encode(salt, StandardCharsets.UTF_8)
            + "&sign=" + URLEncoder.encode(sign, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://fanyi-api.baidu.com/api/trans/vip/translate"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("error_code") && !"52000".equals(root.get("error_code").asText())) {
            throw new IllegalStateException("百度返回错误码 " + root.get("error_code").asText());
        }

        JsonNode transResult = root.get("trans_result");
        if (transResult == null || !transResult.isArray()) {
            throw new IllegalStateException("百度返回格式异常");
        }

        StringBuilder builder = new StringBuilder();
        for (JsonNode line : transResult) {
            if (line.has("dst")) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(line.get("dst").asText(""));
            }
        }

        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到百度译文");
        }
        return builder.toString();
    }

    private String translateWithMicrosoft(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(microsoftKey)) {
            throw new IllegalStateException("未配置微软翻译 Key");
        }
        String endpoint = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to="
            + URLEncoder.encode(targetLang, StandardCharsets.UTF_8);
        if (!"auto".equalsIgnoreCase(sourceLang)) {
            endpoint += "&from=" + URLEncoder.encode(sourceLang, StandardCharsets.UTF_8);
        }
        String payload = "[{\"Text\":\"" + escapeJson(text) + "\"}]";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
            .timeout(Duration.ofSeconds(20))
            .header("Ocp-Apim-Subscription-Key", microsoftKey)
            .header("Ocp-Apim-Subscription-Region", microsoftRegion)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray() || root.isEmpty()) {
            throw new IllegalStateException("微软翻译返回为空");
        }
        JsonNode translations = root.get(0).path("translations");
        if (!translations.isArray() || translations.isEmpty()) {
            throw new IllegalStateException("微软翻译结果为空");
        }
        return translations.get(0).path("text").asText("");
    }

    private String translateWithTencent(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(tencentSecretId) || !StringUtils.hasText(tencentSecretKey)) {
            throw new IllegalStateException("未配置腾讯翻译 SecretId / SecretKey");
        }

        long timestamp = System.currentTimeMillis() / 1000;
        String date = java.time.Instant.ofEpochSecond(timestamp)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .toString();
        String payload = objectMapper.writeValueAsString(Map.of(
            "SourceText", text,
            "Source", mapTencentLang(sourceLang),
            "Target", mapTencentLang(targetLang),
            "ProjectId", 0
        ));

        String service = "tmt";
        String host = "tmt.tencentcloudapi.com";
        String algorithm = "TC3-HMAC-SHA256";
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\nhost:" + host + "\n";
        String signedHeaders = "content-type;host";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n"
            + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;
        String credentialScope = date + "/" + service + "/tc3_request";
        String stringToSign = algorithm + "\n" + timestamp + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] secretDate = hmac256(("TC3" + tencentSecretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = bytesToHex(hmac256(secretSigning, stringToSign));
        String authorization = algorithm + " Credential=" + tencentSecretId + "/" + credentialScope
            + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + host))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", authorization)
            .header("Content-Type", "application/json; charset=utf-8")
            .header("Host", host)
            .header("X-TC-Action", "TextTranslate")
            .header("X-TC-Timestamp", String.valueOf(timestamp))
            .header("X-TC-Version", "2018-03-21")
            .header("X-TC-Region", tencentRegion)
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body()).path("Response");
        if (root.has("Error")) {
            JsonNode error = root.path("Error");
            throw new IllegalStateException(error.path("Code").asText("TencentError") + ": " + error.path("Message").asText(""));
        }
        String result = root.path("TargetText").asText("");
        if (!StringUtils.hasText(result)) {
            throw new IllegalStateException("腾讯翻译返回为空");
        }
        return result;
    }

    private String mapBaiduLang(String lang) {
        if ("auto".equalsIgnoreCase(lang)) return "auto";
        if ("zh-CN".equalsIgnoreCase(lang) || "zh".equalsIgnoreCase(lang)) return "zh";
        if ("zh-TW".equalsIgnoreCase(lang)) return "cht";
        if ("en".equalsIgnoreCase(lang)) return "en";
        if ("ja".equalsIgnoreCase(lang)) return "jp";
        if ("ko".equalsIgnoreCase(lang)) return "kor";
        if ("fr".equalsIgnoreCase(lang)) return "fra";
        if ("es".equalsIgnoreCase(lang)) return "spa";
        if ("ru".equalsIgnoreCase(lang)) return "ru";
        return "auto";
    }

    private String normalizeGoogleApiTarget(String lang) {
        if ("zh-CN".equalsIgnoreCase(lang) || "zh".equalsIgnoreCase(lang)) return "zh-CN";
        if ("zh-TW".equalsIgnoreCase(lang)) return "zh-TW";
        if ("ja".equalsIgnoreCase(lang) || "jp".equalsIgnoreCase(lang)) return "ja";
        if ("ko".equalsIgnoreCase(lang) || "kor".equalsIgnoreCase(lang)) return "ko";
        if ("en".equalsIgnoreCase(lang)) return "en";
        if ("fr".equalsIgnoreCase(lang)) return "fr";
        if ("de".equalsIgnoreCase(lang)) return "de";
        if ("es".equalsIgnoreCase(lang)) return "es";
        if ("ru".equalsIgnoreCase(lang)) return "ru";
        return lang;
    }

    private String mapLibreLang(String lang) {
        if ("auto".equalsIgnoreCase(lang)) return "auto";
        if ("zh-CN".equalsIgnoreCase(lang) || "zh".equalsIgnoreCase(lang)) return "zh";
        if ("zh-TW".equalsIgnoreCase(lang)) return "zt";
        if ("ja".equalsIgnoreCase(lang) || "jp".equalsIgnoreCase(lang)) return "ja";
        if ("ko".equalsIgnoreCase(lang) || "kor".equalsIgnoreCase(lang)) return "ko";
        if ("en".equalsIgnoreCase(lang)) return "en";
        if ("fr".equalsIgnoreCase(lang)) return "fr";
        if ("de".equalsIgnoreCase(lang)) return "de";
        if ("es".equalsIgnoreCase(lang)) return "es";
        if ("ru".equalsIgnoreCase(lang)) return "ru";
        return lang;
    }

    private String mapMicrosoftEdgeLang(String lang) {
        if ("auto".equalsIgnoreCase(lang)) return "auto";
        if ("zh-CN".equalsIgnoreCase(lang) || "zh".equalsIgnoreCase(lang)) return "zh-Hans";
        if ("zh-TW".equalsIgnoreCase(lang)) return "zh-Hant";
        if ("ja".equalsIgnoreCase(lang) || "jp".equalsIgnoreCase(lang)) return "ja";
        if ("ko".equalsIgnoreCase(lang) || "kor".equalsIgnoreCase(lang)) return "ko";
        if ("en".equalsIgnoreCase(lang)) return "en";
        if ("fr".equalsIgnoreCase(lang)) return "fr";
        if ("de".equalsIgnoreCase(lang)) return "de";
        if ("es".equalsIgnoreCase(lang)) return "es";
        if ("ru".equalsIgnoreCase(lang)) return "ru";
        return lang;
    }

    private String firstJsonText(JsonNode root, String... fieldNames) {
        if (root == null || root.isMissingNode() || root.isNull()) return "";
        for (String fieldName : fieldNames) {
            JsonNode node = root.path(fieldName);
            if (node.isTextual() && StringUtils.hasText(node.asText())) {
                return node.asText();
            }
            if (node.isObject() || (node.isArray() && !node.isEmpty())) {
                String nested = firstJsonText(node.isArray() ? node.get(0) : node, fieldNames);
                if (StringUtils.hasText(nested)) return nested;
            }
        }
        if (root.isTextual()) return root.asText();
        if (root.isArray() && !root.isEmpty()) return firstJsonText(root.get(0), fieldNames);
        return "";
    }

    private String mapYoudaoLangType(String sourceLang, String targetLang) {
        if ("auto".equalsIgnoreCase(sourceLang)) {
            if (targetLang.toLowerCase(Locale.ROOT).startsWith("zh")) {
                return "auto";
            }
            return "auto2" + targetLang;
        }
        if (targetLang.toLowerCase(Locale.ROOT).startsWith("zh")) {
            return sourceLang + "-zh-CHS";
        }
        if ("en".equalsIgnoreCase(targetLang)) {
            return "zh-CHS2en";
        }
        return "auto";
    }

    private String mapTencentLang(String lang) {
        if ("auto".equalsIgnoreCase(lang)) return "auto";
        if ("zh-CN".equalsIgnoreCase(lang) || "zh".equalsIgnoreCase(lang)) return "zh";
        if ("zh-TW".equalsIgnoreCase(lang)) return "zh-TW";
        if ("en".equalsIgnoreCase(lang)) return "en";
        if ("ja".equalsIgnoreCase(lang)) return "ja";
        if ("ko".equalsIgnoreCase(lang)) return "ko";
        if ("fr".equalsIgnoreCase(lang)) return "fr";
        if ("es".equalsIgnoreCase(lang)) return "es";
        if ("ru".equalsIgnoreCase(lang)) return "ru";
        return lang;
    }

    private String mapDeepLTarget(String targetLang) {
        if ("zh-CN".equalsIgnoreCase(targetLang) || "zh".equalsIgnoreCase(targetLang)) {
            return "ZH";
        }
        if ("en".equalsIgnoreCase(targetLang)) {
            return "EN";
        }
        return targetLang.toUpperCase(Locale.ROOT);
    }

    private String mapDeepLSource(String sourceLang) {
        if ("zh-CN".equalsIgnoreCase(sourceLang) || "zh".equalsIgnoreCase(sourceLang)) {
            return "ZH";
        }
        if ("en".equalsIgnoreCase(sourceLang)) {
            return "EN";
        }
        return sourceLang.toUpperCase(Locale.ROOT);
    }

    private List<String> splitText(String text) {
        return splitText(text, MAX_CHUNK_SIZE);
    }

    private List<String> splitText(String text, int limit) {
        List<String> chunks = new ArrayList<>();
        if (text.length() <= limit) {
            chunks.add(text);
            return chunks;
        }
        String[] paragraphs = text.split("\\n{2,}");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > limit) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                for (int index = 0; index < paragraph.length(); index += limit) {
                    chunks.add(paragraph.substring(index, Math.min(index + limit, paragraph.length())));
                }
                continue;
            }
            if (!current.isEmpty() && current.length() + paragraph.length() + 2 > limit) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String sha256Hex(String value) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac256(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value & 0xff));
        }
        return builder.toString();
    }
}
