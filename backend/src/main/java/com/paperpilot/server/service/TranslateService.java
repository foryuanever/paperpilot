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
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class TranslateService {

    private static final int MAX_CHUNK_SIZE = 4500;
    private static final Map<String, String> PROVIDER_LABELS = new LinkedHashMap<>() {{
        put("google", "谷歌翻译");
        put("baidu", "百度翻译");
        put("youdao", "有道翻译");
        put("microsoft", "微软翻译");
        put("tencent", "腾讯翻译");
        put("deepl", "DeepL");
    }};

    private final TranslationRecordRepository translationRecordRepository;
    private final CurrentUserService currentUserService;

    public TranslateService(
        TranslationRecordRepository translationRecordRepository,
        CurrentUserService currentUserService
    ) {
        this.translationRecordRepository = translationRecordRepository;
        this.currentUserService = currentUserService;
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
                case "tencent" -> translateWithTencent(text, sourceLang, targetLang);
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
            throw new IllegalStateException(
                PROVIDER_LABELS.getOrDefault(provider, provider) + "失败: " + error.getMessage(),
                error
            );
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
            case "google" -> true;
            case "youdao" -> false;
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
        return "ai".equals(normalized) ? "google" : normalized;
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
