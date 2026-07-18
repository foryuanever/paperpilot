package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.dto.TranslateRequest;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.entity.TranslationRecordEntity;
import com.paperpilot.server.repository.ModelConfigRepository;
import com.paperpilot.server.repository.TranslationRecordRepository;
import com.paperpilot.server.service.CurrentUserService;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TranslateService {

    private static final int MAX_CHUNK_SIZE = 4500;
    private static final Map<String, String> PROVIDER_LABELS = Map.of(
        "google", "谷歌翻译",
        "youdao", "有道翻译",
        "deepl", "DeepL",
        "baidu", "百度翻译",
        "microsoft", "微软翻译",
        "ai", "AI 学术翻译"
    );

    private final ModelConfigRepository modelConfigRepository;
    private final TranslationRecordRepository translationRecordRepository;
    private final CurrentUserService currentUserService;
    private final AiUsageService aiUsageService;

    public TranslateService(
        ModelConfigRepository modelConfigRepository,
        TranslationRecordRepository translationRecordRepository,
        CurrentUserService currentUserService,
        AiUsageService aiUsageService
    ) {
        this.modelConfigRepository = modelConfigRepository;
        this.translationRecordRepository = translationRecordRepository;
        this.currentUserService = currentUserService;
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


    public TranslateResultVO translate(TranslateRequest request) {
        String provider = normalizeProvider(request.getProvider());
        String sourceLang = normalizeLang(request.getSourceLang(), "auto");
        String targetLang = normalizeLang(request.getTargetLang(), "zh-CN");
        String text = request.getText() == null ? "" : request.getText().trim();
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("待翻译文本不能为空");
        }

        long startTime = System.currentTimeMillis();
        boolean success = false;
        long charCount = text.length();

        try {
            String translated = switch (provider) {
                case "google" -> translateWithGoogle(text, sourceLang, targetLang);
                case "youdao" -> translateWithYoudao(text, sourceLang, targetLang);
                case "deepl" -> translateWithDeepL(text, sourceLang, targetLang);
                case "baidu" -> translateWithBaidu(text, sourceLang, targetLang);
                case "microsoft" -> translateWithMicrosoft(text, sourceLang, targetLang);
                case "ai" -> translateWithAi(text, sourceLang, targetLang);
                default -> throw new IllegalArgumentException("不支持的翻译引擎: " + provider);
            };

            success = true;
            long duration = System.currentTimeMillis() - startTime;
            saveRecord(provider, charCount, duration, true);

            TranslateResultVO result = new TranslateResultVO();
            result.setProvider(provider);
            result.setProviderLabel(PROVIDER_LABELS.getOrDefault(provider, provider));
            result.setSourceLang(sourceLang);
            result.setTargetLang(targetLang);
            result.setTranslatedText(translated);
            result.setFallback(false);
            return result;
        } catch (Exception error) {
            long duration = System.currentTimeMillis() - startTime;
            saveRecord(provider, charCount, duration, false);

            if ("google".equals(provider)) {
                throw new IllegalStateException("谷歌翻译失败: " + error.getMessage(), error);
            }
            try {
                long fallbackStart = System.currentTimeMillis();
                String fallbackText = translateWithGoogle(text, sourceLang, targetLang);
                long fallbackDuration = System.currentTimeMillis() - fallbackStart;
                saveRecord("google", charCount, fallbackDuration, true);

                TranslateResultVO result = new TranslateResultVO();
                result.setProvider(provider);
                result.setProviderLabel(PROVIDER_LABELS.getOrDefault(provider, provider));
                result.setSourceLang(sourceLang);
                result.setTargetLang(targetLang);
                result.setTranslatedText(fallbackText);
                result.setFallback(true);
                return result;
            } catch (Exception fallbackError) {
                long fallbackDuration = System.currentTimeMillis() - startTime;
                saveRecord("google", charCount, fallbackDuration, false);
                throw new IllegalStateException(
                    PROVIDER_LABELS.getOrDefault(provider, provider) + "失败，且谷歌回退也失败: "
                        + fallbackError.getMessage(),
                    fallbackError
                );
            }
        }
    }

    private void saveRecord(String provider, long charCount, long latencyMs, boolean success) {
        try {
            TranslationRecordEntity record = new TranslationRecordEntity();
            record.setUserId(currentUserService.getOrCreateDefaultUserId());
            record.setProvider(provider);
            record.setCharCount(charCount);
            record.setLatencyMs(latencyMs);
            record.setSuccess(success);
            translationRecordRepository.save(record);
        } catch (Exception e) {
            System.err.println("Failed to save translation record: " + e.getMessage());
        }
    }

    public List<Map<String, String>> listProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        for (Map.Entry<String, String> entry : PROVIDER_LABELS.entrySet()) {
            providers.add(Map.of(
                "id", entry.getKey(),
                "label", entry.getValue(),
                "configured", String.valueOf(isProviderConfigured(entry.getKey()))
            ));
        }
        return providers;
    }

    private boolean isProviderConfigured(String provider) {
        return switch (provider) {
            case "google", "youdao", "ai" -> true;
            case "deepl" -> StringUtils.hasText(deeplApiKey);
            case "baidu" -> StringUtils.hasText(baiduAppId) && StringUtils.hasText(baiduSecret);
            case "microsoft" -> StringUtils.hasText(microsoftKey);
            default -> false;
        };
    }

    private String translateWithAi(String text, String sourceLang, String targetLang) throws Exception {
        ModelConfigEntity modelConfig = modelConfigRepository
            .findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_GENERAL)
            .orElse(null);
        
        String baseUrl = "https://api.openai.com/v1";
        String apiKey = "";
        String modelName = "gpt-4o";
        
        if (modelConfig != null) {
            if (StringUtils.hasText(modelConfig.getBaseUrl())) {
                baseUrl = modelConfig.getBaseUrl().trim();
            }
            if (StringUtils.hasText(modelConfig.getApiKey())) {
                apiKey = modelConfig.getApiKey().trim();
            }
            if (StringUtils.hasText(modelConfig.getModelName())) {
                modelName = modelConfig.getModelName().trim();
            }
        }
        
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/chat/completions";
        
        String targetLangLabel = "简体中文";
        if ("zh-TW".equalsIgnoreCase(targetLang)) {
            targetLangLabel = "繁体中文";
        } else if ("ja".equalsIgnoreCase(targetLang)) {
            targetLangLabel = "日语";
        } else if ("ko".equalsIgnoreCase(targetLang)) {
            targetLangLabel = "韩语";
        } else if ("en".equalsIgnoreCase(targetLang)) {
            targetLangLabel = "英语";
        }
        
        String systemPrompt = "你是一个资深学术翻译家，精通英文学术论文翻译与润色。请将以下英文学术论文段落翻译成流畅、专业、符合" + targetLangLabel + "学术规范的学术译文。在翻译时，请遵循以下规范：\n"
            + "1. 保证学术术语的翻译专业、准确，符合该领域的" + targetLangLabel + "学术惯例。\n"
            + "2. 保持句子的通顺和学术语气，避免生硬的字面直译。\n"
            + "3. 保留所有的段落结构，如果输入中含有双换行符（\\n\\n），请在输出中也保留对应的双换行符，以便分段。\n"
            + "4. 只输出翻译后的纯文本内容，不要包含任何前言、尾言、解释或 Markdown 代码块包裹。\n"
            + "5. 如果输入文本中包含 `[X_SPLIT_X]` 分隔符，请不要对其进行翻译或修改，必须原封不动地在对应的输出段落之间保留该分隔符。";
            
        Map<String, Object> messageSystem = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> messageUser = Map.of("role", "user", "content", text);
        
        Map<String, Object> payloadMap = Map.of(
            "model", modelName,
            "messages", List.of(messageSystem, messageUser),
            "temperature", 0.3
        );
        
        String payload = objectMapper.writeValueAsString(payloadMap);

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(45))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload));

        if (StringUtils.hasText(apiKey)) {
            reqBuilder.header("Authorization", "Bearer " + apiKey);
        }

        long startedAt = System.nanoTime();
        try {
            HttpRequest request = reqBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("LLM 接口返回 HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("LLM 返回 choices 为空");
            }

            String resultText = choices.get(0).path("message").path("content").asText("").trim();
            if (resultText.isEmpty()) {
                throw new IllegalStateException("LLM 返回内容为空");
            }

            if (resultText.startsWith("```")) {
                resultText = resultText.replaceAll("^```[a-zA-Z]*\\n", "").replaceAll("\\n```$", "");
            }
            JsonNode usage = root.path("usage");
            long promptTokens = usage.path("prompt_tokens").asLong(estimateTokens(systemPrompt + "\n" + text));
            long completionTokens = usage.path("completion_tokens").asLong(estimateTokens(resultText));
            long totalTokens = usage.path("total_tokens").asLong(promptTokens + completionTokens);
            recordAiTranslateUsage(modelName, text, promptTokens, completionTokens, totalTokens, "", elapsedMs(startedAt), true);
            return resultText.trim();
        } catch (Exception error) {
            recordAiTranslateUsage(modelName, text, estimateTokens(systemPrompt + "\n" + text), 0L, estimateTokens(systemPrompt + "\n" + text), error.getMessage(), elapsedMs(startedAt), false);
            throw error;
        }
    }

    private void recordAiTranslateUsage(String modelName, String text, long promptTokens, long completionTokens, long totalTokens, String error, long latencyMs, boolean success) {
        try {
            AppUserEntity user = currentUserService.getOrCreateDefaultUser();
            if (success) {
                aiUsageService.recordAndCharge(user.getId(), modelName, "translate", "全文翻译", "当前论文", promptTokens, completionTokens, totalTokens, latencyMs);
            } else {
                aiUsageService.recordFailure(user.getId(), modelName, "translate", "全文翻译", "当前论文", promptTokens, error, latencyMs);
            }
        } catch (Exception ignored) {
            // Translation should not fail because accounting failed.
        }
    }

    private long elapsedMs(long startedAt) {
        return Math.max(0L, java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private long estimateTokens(String value) {
        if (!StringUtils.hasText(value)) return 0L;
        return Math.max(1L, (long) Math.ceil(value.replaceAll("\\s+", "").length() / 3.0));
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return "google";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeLang(String lang, String fallback) {
        if (!StringUtils.hasText(lang)) {
            return fallback;
        }
        return lang.trim();
    }

    private String translateWithGoogle(String text, String sourceLang, String targetLang) throws Exception {
        List<String> chunks = splitText(text);
        StringBuilder builder = new StringBuilder();
        for (String chunk : chunks) {
            String encoded = URLEncoder.encode(chunk, StandardCharsets.UTF_8);
            String sl = "auto".equalsIgnoreCase(sourceLang) ? "auto" : sourceLang;
            String tl = targetLang;
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                + sl + "&tl=" + tl + "&dt=t&q=" + encoded;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            String part = extractGoogleText(root);
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

    private String translateWithYoudao(String text, String sourceLang, String targetLang) throws Exception {
        String langType = mapYoudaoLangType(sourceLang, targetLang);
        String body = "q=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
            + "&langType=" + URLEncoder.encode(langType, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://aidemo.youdao.com/translate"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("errorCode") && !"0".equals(root.get("errorCode").asText())) {
            throw new IllegalStateException("有道返回错误码 " + root.get("errorCode").asText());
        }
        JsonNode translateResult = root.get("translateResult");
        if (translateResult == null || !translateResult.isArray()) {
            throw new IllegalStateException("有道返回格式异常");
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode line : translateResult) {
            if (!line.isArray()) {
                continue;
            }
            for (JsonNode segment : line) {
                if (segment.has("tgt")) {
                    if (!builder.isEmpty()) {
                        builder.append("\n");
                    }
                    builder.append(segment.get("tgt").asText(""));
                }
            }
        }
        if (builder.isEmpty()) {
            throw new IllegalStateException("未获取到有道译文");
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

    private String translateWithBaidu(String text, String sourceLang, String targetLang) throws Exception {
        if (!StringUtils.hasText(baiduAppId) || !StringUtils.hasText(baiduSecret)) {
            throw new IllegalStateException("未配置百度翻译 AppId / Secret");
        }
        throw new IllegalStateException("百度翻译签名暂未启用，请改用谷歌或有道");
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
        List<String> chunks = new ArrayList<>();
        if (text.length() <= MAX_CHUNK_SIZE) {
            chunks.add(text);
            return chunks;
        }
        String[] paragraphs = text.split("\\n{2,}");
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.length() > MAX_CHUNK_SIZE) {
                if (!current.isEmpty()) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                for (int index = 0; index < paragraph.length(); index += MAX_CHUNK_SIZE) {
                    chunks.add(paragraph.substring(index, Math.min(index + MAX_CHUNK_SIZE, paragraph.length())));
                }
                continue;
            }
            if (!current.isEmpty() && current.length() + paragraph.length() + 2 > MAX_CHUNK_SIZE) {
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
}
