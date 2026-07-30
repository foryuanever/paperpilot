package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ModelRelayResearchService {
    private static final String MODELOC_RELAYS = "https://modeloc.com/relays/list?sort=rating&limit=100&offset=0";
    private static final String MODELOC_SITE = "https://modeloc.com/domain/";
    private static final Pattern RMB_TO_USD_SHORT = Pattern.compile("1\\s*[rR元](?:MB)?\\s*[:：=兑换]+\\s*(\\d+(?:\\.\\d+)?)\\s*[uU美刀美元]");
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    public ModelRelayResearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> topRelays() {
        List<Map<String, Object>> rows = fetchTopRows();
        rows.sort(Comparator
            .comparing((Map<String, Object> row) -> score(row, "rating")).reversed()
            .thenComparing(Comparator.comparing((Map<String, Object> row) -> score(row, "score")).reversed())
            .thenComparing(row -> String.valueOf(row.get("domain"))));
        if (rows.size() > 20) rows = new ArrayList<>(rows.subList(0, 20));

        List<CompletableFuture<Map<String, Object>>> futures = rows.stream().map(this::enrich).toList();
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<Map<String, Object>> enriched = futures.stream().map(CompletableFuture::join).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "MODELOC + 中转站公开价格/状态 API + 官网公开价格页");
        result.put("sourceUrl", "https://modeloc.com/relays");
        result.put("fetchedAt", OffsetDateTime.now().toString());
        result.put("pricingNote", "自动并发刷新前 20：MODELOC 负责评分和实测模型，各站公开接口负责充值单价与分组倍率。人民币成本按公开充值价、模型倍率和分组倍率估算；登录后才可见的数据会明确标为需登录，不参与首轮采购。");
        result.put("recommendation", recommendation());
        result.put("purchasePlan", purchasePlan(enriched));
        result.put("economyModelPlan", economyModelPlan(enriched));
        result.put("sceneRoutingPlan", sceneRoutingPlan(enriched));
        result.put("membershipPlan", membershipPlan());
        result.put("items", enriched);
        return result;
    }

    private List<Map<String, Object>> fetchTopRows() {
        JsonNode root = fetchJsonNow(MODELOC_RELAYS, 20);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (root != null) {
            for (JsonNode item : root.path("items")) rows.add(baseRow(item));
        }
        return rows;
    }

    private CompletableFuture<Map<String, Object>> enrich(Map<String, Object> row) {
        String domain = String.valueOf(row.get("domain"));
        CompletableFuture<JsonNode> statusFuture = fetchJson("https://" + domain + "/api/status");
        CompletableFuture<JsonNode> pricingFuture = fetchJson("https://" + domain + "/api/pricing");
        CompletableFuture<JsonNode> detailFuture = fetchJson(MODELOC_SITE + domain);
        CompletableFuture<JsonNode> sub2SettingsFuture = fetchJson("https://" + domain + "/api/v1/settings/public");

        return CompletableFuture.allOf(statusFuture, pricingFuture, detailFuture, sub2SettingsFuture)
            .thenApply(ignored -> {
                JsonNode status = statusFuture.getNow(null);
                JsonNode pricing = pricingFuture.getNow(null);
                JsonNode detail = detailFuture.getNow(null);
                JsonNode sub2Settings = sub2SettingsFuture.getNow(null);
                applyDetail(row, detail);
                Procurement procurement = procurement(domain, status, sub2Settings, detail);
                PricingSnapshot snapshot = pricing(domain, status, pricing, procurement);
                applySnapshot(row, procurement, snapshot);
                return row;
            });
    }

    private Map<String, Object> baseRow(JsonNode item) {
        String domain = item.path("domain").asText("");
        double rating = item.path("rating").isNumber() ? item.path("rating").asDouble() : 0D;
        int score = item.path("score").isNumber() ? item.path("score").asInt() : 0;
        int models = item.path("models").asInt(0);
        int runs = item.path("runs").asInt(0);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("domain", domain);
        row.put("name", item.path("display_name").asText("").isBlank() ? domain : item.path("display_name").asText());
        row.put("website", item.path("website").isNull() ? "" : item.path("website").asText(""));
        row.put("score", score);
        row.put("rating", rating == 0D ? null : rating);
        row.put("reviews", item.path("reviews").asInt(0));
        row.put("models", models);
        row.put("runs", runs);
        row.put("lastTested", item.path("last_tested").asText(""));
        row.put("featured", item.path("featured").asBoolean(false));
        row.put("valueScore", valueScore(score, rating, models, runs));
        return row;
    }

    private void applyDetail(Map<String, Object> row, JsonNode detail) {
        if (detail == null) return;
        JsonNode site = detail.path("site");
        String website = site.path("website").asText("");
        if (!website.isBlank()) row.put("website", website);
        String name = site.path("display_name").asText("");
        if (!name.isBlank()) row.put("name", name);
        row.put("description", site.path("description").asText(""));
        List<String> detectedModels = new ArrayList<>();
        boolean detectedGpt54 = false;
        boolean detectedDeepSeek = false;
        int bestGpt54Score = 0;
        for (JsonNode model : detail.path("models")) {
            String modelName = model.path("model").asText("");
            if (!modelName.isBlank() && !detectedModels.contains(modelName)) detectedModels.add(modelName);
            if (isStrongGpt54(modelName)) {
                detectedGpt54 = true;
                bestGpt54Score = Math.max(bestGpt54Score, model.path("latest_score").asInt(0));
            }
            if (isDeepSeek(modelName)) detectedDeepSeek = true;
        }
        row.put("detectedModels", detectedModels);
        row.put("detectedGpt54", detectedGpt54);
        row.put("detectedDeepSeek", detectedDeepSeek);
        row.put("gpt54ModelocScore", bestGpt54Score == 0 ? null : bestGpt54Score);
    }

    private Procurement procurement(String domain, JsonNode status, JsonNode sub2Settings, JsonNode detail) {
        String description = detail == null ? "" : detail.path("site").path("description").asText("");
        String announcements = announcementText(status);
        String contact = sub2Settings == null ? "" : sub2Settings.path("data").path("contact_info").asText("");
        String combined = (description + " " + announcements + " " + contact).toLowerCase(Locale.ROOT);

        boolean registerEnabled = true;
        boolean paymentEnabled = true;
        if (status != null && status.path("data").has("register_enabled") && status.path("data").path("register_enabled").isBoolean()) {
            registerEnabled = status.path("data").path("register_enabled").asBoolean();
        }
        if (sub2Settings != null && sub2Settings.path("data").isObject()) {
            JsonNode settings = sub2Settings.path("data");
            if (settings.path("registration_enabled").isBoolean()) registerEnabled = settings.path("registration_enabled").asBoolean();
            if (settings.path("payment_enabled").isBoolean()) paymentEnabled = settings.path("payment_enabled").asBoolean();
        }
        if (combined.contains("不开注册") || combined.contains("关闭注册") || combined.contains("暂未开放注册")) registerEnabled = false;
        if (combined.contains("关闭充值") || combined.contains("不对外开放") || combined.contains("仅对老用户")) paymentEnabled = false;

        String statusText;
        if (!registerEnabled) statusText = "关闭注册";
        else if (!paymentEnabled) statusText = "不可公开采购";
        else statusText = "可注册采购";
        return new Procurement(registerEnabled, paymentEnabled, statusText, combined);
    }

    private PricingSnapshot pricing(String domain, JsonNode status, JsonNode pricing, Procurement procurement) {
        Double pricePerUsd = publicPricePerUsd(status);
        String priceSource = pricePerUsd == null ? "未公开" : "公开状态 API";
        RoutePrice gpt54 = null;
        RoutePrice deepSeek = null;
        List<RoutePrice> economyModels = new ArrayList<>();

        if (pricing != null && pricing.path("data").isArray()) {
            Map<String, Double> groups = numberMap(pricing.path("group_ratio"));
            for (JsonNode model : pricing.path("data")) {
                String modelName = model.path("model_name").asText("");
                RoutePrice candidate = routeFromNewApi(model, groups, pricePerUsd);
                if (isStrongGpt54(modelName)) gpt54 = cheaper(gpt54, candidate);
                if (isProductionDeepSeek(modelName)) deepSeek = betterDeepSeek(deepSeek, candidate);
                if (isEconomyTextModel(modelName)) economyModels.add(candidate);
            }
        } else if (pricing != null && pricing.path("data").path("model_group").isObject()) {
            JsonNode completionRatios = pricing.path("data").path("model_completion_ratio");
            var fields = pricing.path("data").path("model_group").fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String group = entry.getKey();
                JsonNode groupNode = entry.getValue();
                double groupRatio = groupNode.path("GroupRatio").asDouble(1D);
                var models = groupNode.path("ModelPrice").fields();
                while (models.hasNext()) {
                    var modelEntry = models.next();
                    String modelName = modelEntry.getKey();
                    JsonNode modelNode = modelEntry.getValue();
                    double modelRatio = modelNode.path("price").asDouble(0D);
                    double completionRatio = completionRatios.path(modelName).asDouble(1D);
                    RoutePrice candidate = routePrice(modelName, group, groupRatio, modelRatio, completionRatio, pricePerUsd);
                    if (isStrongGpt54(modelName)) gpt54 = cheaper(gpt54, candidate);
                    if (isProductionDeepSeek(modelName)) deepSeek = betterDeepSeek(deepSeek, candidate);
                    if (isEconomyTextModel(modelName)) economyModels.add(candidate);
                }
            }
        }

        if ("api.beiji.fun".equals(domain)) {
            pricePerUsd = 1D;
            priceSource = "官网公开价格页";
            gpt54 = new RoutePrice("gpt-5.4", "OpenAI 特价", 0.3D, 0.75D, 4.50D);
            deepSeek = new RoutePrice("deepseek-v4-flash", "国内模型", 0.7D, 0.70D, 1.40D);
            economyModels.add(deepSeek);
        } else if ("api-cdn.apishop.org".equals(domain)) {
            pricePerUsd = 1D;
            priceSource = "官网公开价格页（页面口径存在冲突）";
        } else if ("chatgpt.kfcmv.vip".equals(domain)) {
            priceSource = "公开卡密商店";
        }
        return new PricingSnapshot(pricePerUsd, priceSource, gpt54, deepSeek, cheapestEconomyByFamily(economyModels), procurement);
    }

    private RoutePrice routeFromNewApi(JsonNode model, Map<String, Double> groups, Double pricePerUsd) {
        String modelName = model.path("model_name").asText("");
        double modelRatio = model.path("model_ratio").asDouble(0D);
        double completionRatio = model.path("completion_ratio").asDouble(1D);
        String bestGroup = "default";
        double bestGroupRatio = Double.MAX_VALUE;
        for (JsonNode enabledGroup : model.path("enable_groups")) {
            String group = enabledGroup.asText("");
            Double ratio = groups.get(group);
            if (ratio != null && ratio > 0D && ratio < bestGroupRatio) {
                bestGroup = group;
                bestGroupRatio = ratio;
            }
        }
        if (bestGroupRatio == Double.MAX_VALUE) bestGroupRatio = groups.getOrDefault("default", 1D);
        return routePrice(modelName, bestGroup, bestGroupRatio, modelRatio, completionRatio, pricePerUsd);
    }

    private RoutePrice routePrice(String model, String group, double groupRatio, double modelRatio, double completionRatio, Double pricePerUsd) {
        Double input = pricePerUsd == null || modelRatio <= 0 ? null : pricePerUsd * groupRatio * modelRatio * 2D;
        Double output = input == null ? null : input * completionRatio;
        return new RoutePrice(model, group, groupRatio, input, output);
    }

    private RoutePrice cheaper(RoutePrice current, RoutePrice candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        if (candidate.inputCny() == null) return current;
        if (current.inputCny() == null || candidate.inputCny() < current.inputCny()) return candidate;
        return current;
    }

    private RoutePrice betterDeepSeek(RoutePrice current, RoutePrice candidate) {
        if (candidate == null) return current;
        if (current == null) return candidate;
        int currentRank = deepSeekRank(current.model());
        int candidateRank = deepSeekRank(candidate.model());
        if (candidateRank < currentRank) return candidate;
        if (candidateRank > currentRank) return current;
        return cheaper(current, candidate);
    }

    private int deepSeekRank(String model) {
        String value = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (value.contains("v4-flash")) return 0;
        if (value.contains("v3.2") || value.contains("v3-2")) return 1;
        if (value.contains("deepseek-chat")) return 2;
        if (value.contains("v3.1") || value.contains("v3-1") || value.endsWith("deepseek-v3")) return 3;
        if (value.contains("v4-pro")) return 4;
        if (value.contains("r1") || value.contains("reasoner")) return 5;
        return 6;
    }

    private void applySnapshot(Map<String, Object> row, Procurement procurement, PricingSnapshot snapshot) {
        String domain = String.valueOf(row.get("domain"));
        RoutePrice gpt54 = snapshot.gpt54();
        RoutePrice deepSeek = snapshot.deepSeek();
        boolean supportsGpt54 = gpt54 != null || Boolean.TRUE.equals(row.get("detectedGpt54"));
        boolean supportsDeepSeek = deepSeek != null || Boolean.TRUE.equals(row.get("detectedDeepSeek"));

        row.put("procurementStatus", procurement.status());
        row.put("publicPrice", publicPriceLabel(domain, snapshot));
        row.put("pricingSource", snapshot.source());
        row.put("buyExample", buyExample(domain, snapshot));
        row.put("supportsGpt54", supportsGpt54);
        row.put("supportsDeepSeek", supportsDeepSeek);
        row.put("gpt54Model", gpt54 == null ? "" : gpt54.model());
        row.put("deepSeekModel", deepSeek == null ? "" : deepSeek.model());
        row.put("gpt54Cost", costLabel(gpt54));
        row.put("deepSeekCost", costLabel(deepSeek));
        row.put("gpt54InputCny", gpt54 == null ? null : gpt54.inputCny());
        row.put("gpt54OutputCny", gpt54 == null ? null : gpt54.outputCny());
        row.put("deepSeekInputCny", deepSeek == null ? null : deepSeek.inputCny());
        row.put("deepSeekOutputCny", deepSeek == null ? null : deepSeek.outputCny());
        row.put("economyModels", economyModelsForRow(snapshot.economyModels(), row));
        row.put("multiplier", multiplierLabel(gpt54, deepSeek));
        row.put("suggestion", purchaseSuggestion(domain, procurement, gpt54, deepSeek, row));
        row.put("recommendedScenes", recommendedScenes(procurement, gpt54, deepSeek));
    }

    private String publicPriceLabel(String domain, PricingSnapshot snapshot) {
        if ("chatgpt.kfcmv.vip".equals(domain)) return "¥1/1块额度；¥5/5块额度";
        if (snapshot.pricePerUsd() == null) return "需登录后核价";
        return "¥" + decimal(snapshot.pricePerUsd()) + " = $1 额度";
    }

    private String buyExample(String domain, PricingSnapshot snapshot) {
        if ("chatgpt.kfcmv.vip".equals(domain)) return "¥36 国产月卡；¥88 GPT-5.5月卡";
        if (snapshot.pricePerUsd() == null) return "公开页面未给出";
        return "¥100 ≈ $" + decimal(100D / snapshot.pricePerUsd()) + " 额度";
    }

    private String costLabel(RoutePrice route) {
        if (route == null) return "未发现公开报价";
        if (route.inputCny() == null) return route.model() + " · " + decimal(route.groupRatio()) + "x";
        return "入 ¥" + decimal(route.inputCny()) + " / 出 ¥" + decimal(route.outputCny()) + " 每1M";
    }

    private String multiplierLabel(RoutePrice gpt54, RoutePrice deepSeek) {
        List<String> labels = new ArrayList<>();
        if (gpt54 != null) labels.add("GPT " + decimal(gpt54.groupRatio()) + "x");
        if (deepSeek != null) labels.add("DS " + decimal(deepSeek.groupRatio()) + "x");
        return labels.isEmpty() ? "需登录核价" : String.join(" · ", labels);
    }

    private String purchaseSuggestion(String domain, Procurement procurement, RoutePrice gpt54, RoutePrice deepSeek, Map<String, Object> row) {
        if (!procurement.registerEnabled() || !procurement.paymentEnabled()) return "不买：" + procurement.status();
        if ("api-cdn.apishop.org".equals(domain)) return "先不买：充值口径冲突且未公开 GPT-5.4";
        if ("api.beiji.fun".equals(domain)) return "买 ¥20：PPT 透明价备用，先实测 GPT-5.4";
        int gptScore = number(row.get("gpt54ModelocScore"), 0).intValue();
        if (gpt54 != null && gptScore >= 90) return "买 ¥30：GPT-5.4 已有 MODELOC 实测，可做 PPT 主路由候选";
        if (gpt54 != null && gpt54.inputCny() != null && gpt54.inputCny() <= 0.25D) return "仅充 ¥10：价格极低但缺少 GPT-5.4 实测，先压测";
        if (gpt54 != null && gpt54.inputCny() != null && gpt54.inputCny() <= 1D) return "买 ¥20：PPT 备用候选，先验证模型真实性";
        if (deepSeek != null && deepSeek.inputCny() != null && deepSeek.inputCny() <= 0.6D) return "买 ¥30：DeepSeek 主路由候选";
        if (deepSeek != null && deepSeek.inputCny() != null && deepSeek.inputCny() <= 1D) return "买 ¥20：DeepSeek 备用候选";
        if (Boolean.TRUE.equals(row.get("detectedGpt54"))) return "有 GPT-5.4 实测记录，但需登录核价";
        return "先不买：价格或目标模型信息不足";
    }

    private List<String> recommendedScenes(Procurement procurement, RoutePrice gpt54, RoutePrice deepSeek) {
        if (!procurement.registerEnabled() || !procurement.paymentEnabled()) return List.of();
        List<String> scenes = new ArrayList<>();
        if (deepSeek != null) {
            scenes.add("论文综述");
            scenes.add("AI论文问答");
            scenes.add("发帖审核");
        }
        if (gpt54 != null) scenes.add("PPT生成");
        return scenes;
    }

    private List<RoutePrice> cheapestEconomyByFamily(List<RoutePrice> routes) {
        Map<String, RoutePrice> best = new LinkedHashMap<>();
        for (RoutePrice route : routes) {
            if (route == null || route.model() == null || route.model().isBlank()) continue;
            String family = modelFamily(route.model());
            RoutePrice current = best.get(family);
            if (current == null || routeCost(route) < routeCost(current)) best.put(family, route);
        }
        return best.values().stream()
            .sorted(Comparator.comparingDouble(this::routeCost))
            .limit(8)
            .toList();
    }

    private List<Map<String, Object>> economyModelsForRow(List<RoutePrice> routes, Map<String, Object> station) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (RoutePrice route : routes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("family", modelFamily(route.model()));
            item.put("model", route.model());
            item.put("group", route.group());
            item.put("groupRatio", route.groupRatio());
            item.put("inputCny", route.inputCny());
            item.put("outputCny", route.outputCny());
            item.put("cost", costLabel(route));
            item.put("scenes", economyScenes(route.model()));
            item.put("risk", economyRisk(route, station));
            result.add(item);
        }
        return result;
    }

    private String economyRisk(RoutePrice route, Map<String, Object> station) {
        int stationScore = number(station.get("score"), 0).intValue();
        if (route.inputCny() != null && route.inputCny() < 0.05D) return "超低价，先验证模型真实性";
        if (stationScore < 90) return "站点评分偏低，仅进入压测池";
        return "小额实测后进入经济模型池";
    }

    private double routeCost(RoutePrice route) {
        if (route == null || route.inputCny() == null || route.outputCny() == null) return Double.MAX_VALUE;
        return route.inputCny() + route.outputCny() * 0.35D;
    }

    private boolean isEconomyTextModel(String model) {
        if (model == null || model.isBlank()) return false;
        String value = model.toLowerCase(Locale.ROOT);
        if (value.matches(".*(embedding|rerank|image|video|audio|voice|realtime|speech|tts|whisper|sora|veo|banana|vision|\\bvl\\b|coder|codex|hailuo|distill|ocr).*")) return false;
        return value.contains("deepseek")
            || value.contains("qwen")
            || value.contains("glm")
            || value.contains("kimi")
            || value.contains("minimax")
            || value.contains("mimo")
            || (value.contains("gemini") && (value.contains("flash") || value.contains("lite")))
            || (value.contains("grok") && value.contains("fast"))
            || (value.contains("gpt") && (value.contains("mini") || value.contains("nano") || value.contains("luna")))
            || (value.contains("claude") && value.contains("haiku"))
            || value.contains("llama");
    }

    private String modelFamily(String model) {
        String value = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (value.contains("deepseek")) return "DeepSeek";
        if (value.contains("qwen")) return "Qwen";
        if (value.contains("glm")) return "GLM";
        if (value.contains("kimi")) return "Kimi";
        if (value.contains("minimax")) return "MiniMax";
        if (value.contains("mimo")) return "MiMo";
        if (value.contains("gemini")) return "Gemini Flash";
        if (value.contains("grok")) return "Grok Fast";
        if (value.contains("gpt")) return "GPT 轻量";
        if (value.contains("claude")) return "Claude Haiku";
        if (value.contains("llama")) return "Llama";
        return "其他文本模型";
    }

    private List<String> economyScenes(String model) {
        String family = modelFamily(model);
        List<String> scenes = new ArrayList<>();
        String value = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (!List.of("Grok Fast", "Llama").contains(family) && !value.contains("nano") && !value.contains("luna")) scenes.add("论文综述");
        scenes.add("AI论文问答");
        scenes.add("AI发帖审核");
        return scenes;
    }

    private Double publicPricePerUsd(JsonNode status) {
        if (status == null || !status.path("data").isObject()) return null;
        String announcements = announcementText(status);
        Matcher matcher = RMB_TO_USD_SHORT.matcher(announcements);
        if (matcher.find()) {
            double usd = Double.parseDouble(matcher.group(1));
            if (usd > 0) return 1D / usd;
        }
        JsonNode price = status.path("data").path("price");
        return price.isNumber() && price.asDouble() > 0 ? price.asDouble() : null;
    }

    private String announcementText(JsonNode status) {
        if (status == null) return "";
        StringBuilder text = new StringBuilder();
        for (JsonNode announcement : status.path("data").path("announcements")) {
            text.append(announcement.path("content").asText("")).append(' ');
        }
        return text.toString();
    }

    private Map<String, Double> numberMap(JsonNode node) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (node == null || !node.isObject()) return values;
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) values.put(entry.getKey(), entry.getValue().asDouble());
        });
        return values;
    }

    private CompletableFuture<JsonNode> fetchJson(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(9))
            .header("Accept", "application/json")
            .header("User-Agent", "PaperPilot relay research/2.0")
            .GET()
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .orTimeout(10, TimeUnit.SECONDS)
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                try {
                    return objectMapper.readTree(response.body());
                } catch (Exception ignored) {
                    return null;
                }
            })
            .exceptionally(ignored -> null);
    }

    private JsonNode fetchJsonNow(String url, int seconds) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(seconds))
                .header("Accept", "application/json")
                .header("User-Agent", "PaperPilot relay research/2.0")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) return objectMapper.readTree(response.body());
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isStrongGpt54(String model) {
        String normalized = model == null ? "" : model.toLowerCase(Locale.ROOT);
        if (!normalized.contains("gpt-5.4")) return false;
        return !normalized.contains("mini") && !normalized.contains("nano") && !normalized.contains("compact");
    }

    private boolean isDeepSeek(String model) {
        return model != null && model.toLowerCase(Locale.ROOT).contains("deepseek");
    }

    private boolean isProductionDeepSeek(String model) {
        if (!isDeepSeek(model)) return false;
        String value = model.toLowerCase(Locale.ROOT);
        return !value.contains("distill") && !value.contains("ocr");
    }

    private int valueScore(int score, double rating, int models, int runs) {
        double ratingPart = rating <= 0 ? 70 : rating * 20D;
        double coverage = Math.min(12, models * 2.5D);
        double confidence = Math.min(10, runs);
        return (int) Math.round(score * 0.48D + ratingPart * 0.26D + coverage + confidence);
    }

    private Double score(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private String decimal(double value) {
        return BigDecimal.valueOf(value).setScale(value < 1 ? 3 : 2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private Map<String, Object> recommendation() {
        return Map.of(
            "paperReview", "论文综述：DeepSeek、Qwen、Kimi、GLM、MiniMax 按长文本质量轮换，复杂整合再升级；目标成本不超过 ¥0.80/篇。",
            "paperQa", "AI 论文问答：Qwen/GLM/Gemini Flash/GPT 轻量等经济模型主跑，难题才升级；普通轮成本控制在 ¥0.03 内。",
            "meetingDeck", "PPT 生成：只在大纲、结构和终审使用完整 gpt-5.4；每次刷新按 MODELOC 实测分和人民币成本动态选择主备。",
            "forumModeration", "AI 发帖审核：规则引擎先筛，GPT nano、Qwen Turbo、GLM Flash、Grok Fast 等只审边界内容，目标低于 ¥0.005/帖。"
        );
    }

    private List<Map<String, Object>> purchasePlan(List<Map<String, Object>> rows) {
        List<Map<String, Object>> gptCandidates = rows.stream()
            .filter(this::isPurchasable)
            .filter(row -> number(row.get("gpt54InputCny"), null) != null)
            .sorted(Comparator
                .comparing((Map<String, Object> row) -> number(row.get("gpt54ModelocScore"), 0).intValue() > 0 ? 0 : 1)
                .thenComparing(row -> number(row.get("gpt54InputCny"), Double.MAX_VALUE).doubleValue())
                .thenComparing(Comparator.comparing((Map<String, Object> row) -> score(row, "valueScore")).reversed()))
            .toList();
        List<Map<String, Object>> deepSeekCandidates = rows.stream()
            .filter(this::isPurchasable)
            .filter(row -> number(row.get("deepSeekInputCny"), null) != null)
            .sorted(Comparator
                .comparing((Map<String, Object> row) -> number(row.get("deepSeekInputCny"), Double.MAX_VALUE).doubleValue())
                .thenComparing(Comparator.comparing((Map<String, Object> row) -> score(row, "valueScore")).reversed()))
            .toList();
        List<Map<String, Object>> unverifiedCheapGpt = gptCandidates.stream()
            .filter(row -> number(row.get("gpt54ModelocScore"), 0).intValue() == 0)
            .sorted(Comparator.comparing(row -> number(row.get("gpt54InputCny"), Double.MAX_VALUE).doubleValue()))
            .toList();

        Map<String, Object> pptPrimary = first(gptCandidates);
        Map<String, Object> pptBackup = gptCandidates.size() > 1 ? gptCandidates.get(1) : null;
        Map<String, Object> deepPrimary = first(deepSeekCandidates);
        Map<String, Object> deepBackup = deepSeekCandidates.size() > 1 ? deepSeekCandidates.get(1) : null;
        Map<String, Object> cheapTest = first(unverifiedCheapGpt);
        return List.of(
            plan("PPT 生成", pptPrimary, pptBackup, "首轮主站 ¥30 / 备用 ¥20", "优先选择 MODELOC 已实测完整 GPT-5.4 的站点，再比较真实人民币输入成本。", "gpt54Cost"),
            plan("综述 / 问答 / 审核", deepPrimary, deepBackup, "首轮主站 ¥30 / 备用 ¥20", "三类轻任务共用 DeepSeek 低价池，但额度和调用统计仍按入口拆分。", "deepSeekCost"),
            plan("超低价压力测试", cheapTest, null, "只充 ¥10", "未有 GPT-5.4 实测记录的低价站只跑固定测试集，不直接承接用户流量。", "gpt54Cost")
        );
    }

    private List<Map<String, Object>> economyModelPlan(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> bestByFamily = new LinkedHashMap<>();
        for (Map<String, Object> station : rows) {
            if (!isPurchasable(station)) continue;
            Object rawModels = station.get("economyModels");
            if (!(rawModels instanceof List<?> models)) continue;
            for (Object rawModel : models) {
                if (!(rawModel instanceof Map<?, ?> model)) continue;
                Number input = number(model.get("inputCny"), null);
                Number output = number(model.get("outputCny"), null);
                if (input == null || output == null) continue;
                String family = String.valueOf(model.get("family"));
                Map<String, Object> candidate = new LinkedHashMap<>();
                model.forEach((key, value) -> candidate.put(String.valueOf(key), value));
                candidate.put("station", displayName(station));
                candidate.put("domain", station.get("domain"));
                candidate.put("stationScore", station.get("score"));
                candidate.put("rating", station.get("rating"));
                Map<String, Object> current = bestByFamily.get(family);
                if (current == null || economyCost(candidate) < economyCost(current)) bestByFamily.put(family, candidate);
            }
        }
        return bestByFamily.values().stream()
            .sorted(Comparator.comparingDouble(this::economyCost))
            .limit(12)
            .toList();
    }

    private double economyCost(Map<String, Object> model) {
        double input = number(model.get("inputCny"), Double.MAX_VALUE).doubleValue();
        double output = number(model.get("outputCny"), Double.MAX_VALUE).doubleValue();
        return input + output * 0.35D;
    }

    private List<Map<String, Object>> sceneRoutingPlan(List<Map<String, Object>> rows) {
        List<Map<String, Object>> economy = economyModelPlan(rows);
        return List.of(
            economyScenePlan("论文综述", economy, List.of("DeepSeek", "Qwen", "Kimi", "GLM", "MiniMax", "Gemini Flash", "Claude Haiku"), "长文本分段处理，主模型整合；超长论文先检索再送模型。"),
            economyScenePlan("AI论文问答", economy, List.of("Qwen", "DeepSeek", "GLM", "GPT 轻量", "Gemini Flash", "Kimi", "MiMo"), "普通问题走经济模型，复杂推理或多文献交叉问题再升级。"),
            economyScenePlan("AI发帖审核", economy, List.of("GPT 轻量", "Qwen", "GLM", "Gemini Flash", "Grok Fast", "DeepSeek", "Llama"), "规则引擎先筛；模型只处理边界内容，并强制 JSON 输出。")
        );
    }

    private Map<String, Object> economyScenePlan(String scene, List<Map<String, Object>> models, List<String> preferredFamilies, String strategy) {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (String family : preferredFamilies) {
            models.stream()
                .filter(model -> family.equals(model.get("family")))
                .filter(model -> modelScenes(model).contains(scene))
                .findFirst()
                .ifPresent(selected::add);
            if (selected.size() == 3) break;
        }
        for (Map<String, Object> model : models) {
            if (selected.size() == 3) break;
            if (!selected.contains(model) && modelScenes(model).contains(scene)) selected.add(model);
        }
        return Map.of(
            "scene", scene,
            "primary", selected.isEmpty() ? "暂无公开报价候选" : economyModelLabel(selected.get(0)),
            "backup", selected.size() < 2 ? "暂不配置" : economyModelLabel(selected.get(1)),
            "fallback", selected.size() < 3 ? "暂不配置" : economyModelLabel(selected.get(2)),
            "strategy", strategy
        );
    }

    private List<?> modelScenes(Map<String, Object> model) {
        Object scenes = model.get("scenes");
        return scenes instanceof List<?> list ? list : List.of();
    }

    private String economyModelLabel(Map<String, Object> model) {
        return model.get("model") + " @ " + model.get("station") + " · " + model.get("cost");
    }

    private Map<String, Object> plan(String scene, Map<String, Object> primary, Map<String, Object> backup, String budget, String reason, String costKey) {
        String primaryText = primary == null ? "暂无合格候选" : displayName(primary) + " · " + primary.get(costKey);
        String backupText = backup == null ? "暂不配置" : displayName(backup) + " · " + backup.get(costKey);
        return Map.of("scene", scene, "primary", primaryText, "backup", backupText, "budget", budget, "reason", reason);
    }

    private boolean isPurchasable(Map<String, Object> row) {
        return "可注册采购".equals(row.get("procurementStatus"));
    }

    private Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String displayName(Map<String, Object> row) {
        String name = String.valueOf(row.get("name"));
        return name.isBlank() ? String.valueOf(row.get("domain")) : name;
    }

    private Number number(Object value, Number fallback) {
        return value instanceof Number number ? number : fallback;
    }

    private List<Map<String, Object>> membershipPlan() {
        return List.of(
            Map.of("name", "个人 Lite", "price", "¥9.9/月", "review", 15, "qa", 30, "ppt", 2, "positioning", "一杯瑞幸咖啡价，适合文献阅读与基础综述。"),
            Map.of("name", "个人 Plus", "price", "¥19.9/月", "review", 30, "qa", 60, "ppt", 4, "positioning", "课程论文与周会准备，高频论文问答。"),
            Map.of("name", "个人 Pro", "price", "¥29.9/月", "review", 60, "qa", 120, "ppt", 6, "positioning", "高压课题推进，享受发帖置顶与高峰优先。"),
            Map.of("name", "课题组团队 Plus", "price", "¥17.91/人/月", "review", 30, "qa", 60, "ppt", 4, "positioning", "导师按需购买统一分配，全队 9 折与共享席位。"),
            Map.of("name", "课题组团队 Pro", "price", "¥26.91/人/月", "review", 60, "qa", 120, "ppt", 6, "positioning", "实验室全员极速旗舰，顶配额度与全特权。")
        );
    }

    private record Procurement(boolean registerEnabled, boolean paymentEnabled, String status, String evidence) {
    }

    private record RoutePrice(String model, String group, double groupRatio, Double inputCny, Double outputCny) {
    }

    private record PricingSnapshot(Double pricePerUsd, String source, RoutePrice gpt54, RoutePrice deepSeek, List<RoutePrice> economyModels, Procurement procurement) {
    }
}
