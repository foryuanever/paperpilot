package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchFormulaService {
    private final AiChatService aiChatService;
    private final ObjectMapper objectMapper;

    public SearchFormulaService(AiChatService aiChatService, ObjectMapper objectMapper) {
        this.aiChatService = aiChatService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> generate(String idea) {
        String normalizedIdea = String.valueOf(idea == null ? "" : idea).trim();
        if (!StringUtils.hasText(normalizedIdea)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "研究想法不能为空");
        }
        if (normalizedIdea.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "研究想法不能超过 500 字");
        }

        String systemPrompt = """
            你是学术数据库检索式专家。请把用户的中文或英文研究想法转换成论文检索关键词。
            只输出 JSON，不要 Markdown，不要解释。
            JSON 必须包含：
            simple: 一个简短关键词串，适合 Google Scholar / 知网直接搜索；
            boolean: 一个专业布尔检索式，适合 Web of Science、PubMed、Scopus、Google Scholar；
            notes: 1-3 条中文使用建议。
            要求：
            1. 将中文核心概念翻译成常用英文学术表达；
            2. 同义词用 OR，同一研究维度之间用 AND；
            3. 多词短语必须使用英文双引号；
            4. 不要虚构具体论文、作者、DOI；
            5. boolean 不要超过 260 字符，AND/OR/NOT 总数最多 8 个，优先保留核心概念。
            """;
        String userPrompt = "研究想法：" + normalizedIdea;

        try {
            AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallbackUnmeteredForScene(
                systemPrompt,
                userPrompt,
                900,
                List.of(),
                ModelConfigService.SCENE_FREE_POOL
            );
            JsonNode root = objectMapper.readTree(extractJsonObject(result.content()));
            String simple = clean(root.path("simple").asText(""));
            String bool = clean(root.path("boolean").asText(""));
            if (!StringUtils.hasText(simple) || !StringUtils.hasText(bool)) {
                throw new IllegalStateException("模型返回缺少 simple 或 boolean");
            }
            bool = compactBooleanFormula(bool, simple);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("simple", limit(simple, 240));
            response.put("boolean", bool);
            response.put("notes", readNotes(root.path("notes")));
            response.put("scene", ModelConfigService.SCENE_FREE_POOL);
            return response;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "免费号池模型生成检索式失败，请稍后重试", exception);
        }
    }

    private List<String> readNotes(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        java.util.ArrayList<String> notes = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            String note = clean(item.asText(""));
            if (StringUtils.hasText(note)) notes.add(limit(note, 120));
            if (notes.size() >= 3) break;
        }
        return notes;
    }

    private String extractJsonObject(String raw) {
        String text = String.valueOf(raw == null ? "" : raw).trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("模型未返回 JSON 对象");
        }
        return text.substring(start, end + 1);
    }

    private String clean(String value) {
        return String.valueOf(value == null ? "" : value)
            .replace('\u201c', '"')
            .replace('\u201d', '"')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replaceAll("[ \\t\\u000B\\f\\r]+", " ")
            .trim();
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max).trim();
    }

    private String compactBooleanFormula(String value, String simple) {
        String normalized = clean(value)
            .replaceAll("\\s+", " ")
            .replaceAll("(?i)\\s*\\b(AND|OR|NOT)\\b\\s*", " $1 ")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.length() <= 260 && booleanConnectorCount(normalized) <= 8) {
            return normalized;
        }
        java.util.ArrayList<String> terms = new java.util.ArrayList<>();
        java.util.regex.Matcher quoted = java.util.regex.Pattern.compile("\"([^\"]{2,60})\"").matcher(normalized);
        while (quoted.find() && terms.size() < 8) {
            addUniqueTerm(terms, quoteTerm(quoted.group(1)));
        }
        java.util.regex.Matcher words = java.util.regex.Pattern.compile("\\b[A-Za-z][A-Za-z0-9-]{1,40}\\b").matcher(normalized);
        while (words.find() && terms.size() < 8) {
            String term = words.group();
            if (!term.matches("(?i)AND|OR|NOT")) addUniqueTerm(terms, quoteTerm(term));
        }
        if (terms.isEmpty() && StringUtils.hasText(simple)) {
            for (String part : clean(simple).split("[,，;；\\s]+")) {
                if (part.length() >= 2) addUniqueTerm(terms, quoteTerm(part));
                if (terms.size() >= 6) break;
            }
        }
        String compact = String.join(" OR ", terms.subList(0, Math.min(terms.size(), 8)));
        return StringUtils.hasText(compact) ? limit(compact, 260) : limit(normalized, 260);
    }

    private int booleanConnectorCount(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?i)\\b(AND|OR|NOT)\\b").matcher(value == null ? "" : value);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private void addUniqueTerm(java.util.List<String> terms, String term) {
        String value = clean(term);
        if (!StringUtils.hasText(value)) return;
        boolean exists = terms.stream().anyMatch(item -> item.equalsIgnoreCase(value));
        if (!exists) terms.add(value);
    }

    private String quoteTerm(String term) {
        String value = clean(term).replace("\"", "");
        if (!StringUtils.hasText(value)) return "";
        if (value.matches("[A-Z0-9-]{2,}")) return value;
        return value.contains(" ") ? "\"" + value + "\"" : value;
    }
}
