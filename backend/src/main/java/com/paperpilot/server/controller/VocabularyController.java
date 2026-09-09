package com.paperpilot.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paperpilot.server.entity.VocabularyEntity;
import com.paperpilot.server.repository.ModelConfigRepository;
import com.paperpilot.server.repository.VocabularyRepository;
import com.paperpilot.server.service.AiChatService;
import com.paperpilot.server.service.AiUsageService;
import com.paperpilot.server.service.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/vocabulary")
public class VocabularyController {

    private static final Logger log = LoggerFactory.getLogger(VocabularyController.class);

    private final VocabularyRepository vocabRepo;
    private final CurrentUserService currentUserService;
    private final ModelConfigRepository modelRepo;
    private final AiChatService aiChatService;
    private final AiUsageService usageService;
    private final ObjectMapper objectMapper;

    public VocabularyController(VocabularyRepository vocabRepo,
                                CurrentUserService currentUserService,
                                ModelConfigRepository modelRepo,
                                AiChatService aiChatService,
                                AiUsageService usageService,
                                ObjectMapper objectMapper) {
        this.vocabRepo = vocabRepo;
        this.currentUserService = currentUserService;
        this.modelRepo = modelRepo;
        this.aiChatService = aiChatService;
        this.usageService = usageService;
        this.objectMapper = objectMapper;
    }

    private Long getUserId() {
        return currentUserService.getOrCreateDefaultUserId();
    }

    @GetMapping
    public List<VocabularyEntity> list(
            @RequestParam(required = false) String paperId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer masteryLevel) {
        Long userId = getUserId();
        List<VocabularyEntity> list;
        if (keyword != null && !keyword.isBlank()) {
            list = vocabRepo.searchByKeyword(userId, keyword.trim());
        } else if (paperId != null && !paperId.isBlank()) {
            list = vocabRepo.findAllByUserIdAndPaperIdOrderByCreatedAtDesc(userId, paperId);
        } else {
            list = vocabRepo.findAllByUserIdOrderByCreatedAtDesc(userId);
        }

        if (masteryLevel != null) {
            list = list.stream().filter(v -> Objects.equals(v.masteryLevel, masteryLevel)).toList();
        }
        return list;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Long userId = getUserId();
        List<VocabularyEntity> all = vocabRepo.findAllByUserIdOrderByCreatedAtDesc(userId);
        long total = all.size();
        long newWords = all.stream().filter(v -> v.masteryLevel == null || v.masteryLevel == 0).count();
        long familiarWords = all.stream().filter(v -> v.masteryLevel != null && v.masteryLevel == 1).count();
        long masteredWords = all.stream().filter(v -> v.masteryLevel != null && v.masteryLevel >= 2).count();

        LocalDate today = LocalDate.now();
        long todayAdded = all.stream().filter(v -> v.createdAt != null && v.createdAt.toLocalDate().equals(today)).count();
        long needsReview = all.stream().filter(v -> {
            if (v.masteryLevel != null && v.masteryLevel >= 2) return false;
            if (v.lastReviewedAt == null) return true;
            return v.lastReviewedAt.isBefore(LocalDateTime.now().minusDays(1));
        }).count();

        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("newWords", newWords);
        res.put("familiarWords", familiarWords);
        res.put("masteredWords", masteredWords);
        res.put("todayAdded", todayAdded);
        res.put("needsReview", needsReview);
        res.put("masteryRate", total > 0 ? Math.round((double) masteredWords / total * 100) : 0);
        return res;
    }

    @PostMapping
    @Transactional
    public VocabularyEntity save(@RequestBody JsonNode body) {
        Long userId = getUserId();
        String word = body.path("word").asText("").trim();
        if (word.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "单词内容不能为空");
        }

        String id = body.path("id").asText("");
        VocabularyEntity entity = null;
        if (!id.isEmpty()) {
            entity = vocabRepo.findById(id).orElse(null);
        }
        if (entity == null) {
            entity = vocabRepo.findFirstByUserIdAndWordIgnoreCase(userId, word).orElse(null);
        }

        boolean isNew = (entity == null);
        if (isNew) {
            entity = new VocabularyEntity();
            entity.id = id.isEmpty() ? "vocab_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) : id;
            entity.userId = userId;
            entity.word = word;
            entity.createdAt = LocalDateTime.now();
        }

        if (body.has("phonetic")) entity.phonetic = body.path("phonetic").asText("");
        if (body.has("audioUrl")) entity.audioUrl = body.path("audioUrl").asText("");
        if (body.has("partOfSpeech")) entity.partOfSpeech = body.path("partOfSpeech").asText("");
        if (body.has("meaningCn")) entity.meaningCn = body.path("meaningCn").asText("");
        if (body.has("meaningEn")) entity.meaningEn = body.path("meaningEn").asText("");
        if (body.has("oxfordLevel")) entity.oxfordLevel = body.path("oxfordLevel").asText("");
        if (body.has("contextSentence")) entity.contextSentence = body.path("contextSentence").asText("");
        if (body.has("contextTranslation")) entity.contextTranslation = body.path("contextTranslation").asText("");
        if (body.has("paperId")) entity.paperId = body.path("paperId").asText("");
        if (body.has("paperTitle")) entity.paperTitle = body.path("paperTitle").asText("");
        if (body.has("sectionName")) entity.sectionName = body.path("sectionName").asText("");
        if (body.has("collocationsJson")) entity.collocationsJson = body.path("collocationsJson").asText("");
        if (body.has("academicExamplesJson")) entity.academicExamplesJson = body.path("academicExamplesJson").asText("");
        if (body.has("synonymsJson")) entity.synonymsJson = body.path("synonymsJson").asText("");
        if (body.has("antonymsJson")) entity.antonymsJson = body.path("antonymsJson").asText("");
        if (body.has("etymology")) entity.etymology = body.path("etymology").asText("");
        if (body.has("masteryLevel")) entity.masteryLevel = body.path("masteryLevel").asInt(0);
        if (body.has("tagsJson")) entity.tagsJson = body.path("tagsJson").asText("");

        entity.updatedAt = LocalDateTime.now();
        return vocabRepo.save(entity);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable String id) {
        Long userId = getUserId();
        VocabularyEntity entity = vocabRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "词汇条目不存在"));
        if (!entity.userId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权删除此词汇条目");
        }
        vocabRepo.delete(entity);
        return Map.of("success", true, "id", id);
    }

    @PatchMapping("/{id}/mastery")
    @Transactional
    public VocabularyEntity updateMastery(@PathVariable String id, @RequestBody JsonNode body) {
        Long userId = getUserId();
        VocabularyEntity entity = vocabRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "词汇条目不存在"));
        if (!entity.userId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改此词汇条目");
        }
        if (body.has("masteryLevel")) {
            entity.masteryLevel = Math.max(0, Math.min(2, body.path("masteryLevel").asInt()));
        }
        entity.updatedAt = LocalDateTime.now();
        return vocabRepo.save(entity);
    }

    @PostMapping("/{id}/review")
    @Transactional
    public VocabularyEntity recordReview(@PathVariable String id, @RequestBody JsonNode body) {
        Long userId = getUserId();
        VocabularyEntity entity = vocabRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "词汇条目不存在"));
        if (!entity.userId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权修改此词汇条目");
        }
        entity.reviewCount = (entity.reviewCount == null ? 0 : entity.reviewCount) + 1;
        entity.lastReviewedAt = LocalDateTime.now();
        if (body.has("masteryLevel")) {
            entity.masteryLevel = Math.max(0, Math.min(2, body.path("masteryLevel").asInt()));
        }
        entity.updatedAt = LocalDateTime.now();
        return vocabRepo.save(entity);
    }

    @PostMapping("/enrich")
    public JsonNode enrich(@RequestBody JsonNode body) {
        String word = body.path("word").asText("").trim();
        String contextSentence = body.path("contextSentence").asText("").trim();
        if (word.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提供需要解析的单词");
        }

        try {
            String systemPrompt = "你是一位权威的牛津/柯林斯高级双解学术英语词典专家与学术写作指导导师。\n" +
                    "请针对用户提供的学术英语单词（以及可选的文献语境例句），输出专业、规范、严谨的结构化 JSON 解析。\n" +
                    "JSON 必须严格遵守以下格式，且只返回合法 JSON，不带任何 Markdown 代码块标签或其他无关文字：\n" +
                    "{\n" +
                    "  \"word\": \"" + word + "\",\n" +
                    "  \"phonetic\": \"[音标，如 /prɪˈpɒndərəns/]\",\n" +
                    "  \"partOfSpeech\": \"n. / v. / adj. / adv.\",\n" +
                    "  \"oxfordLevel\": \"C1 / C2 / Academic / GRE\",\n" +
                    "  \"meaningCn\": \"精准中文释义（若有多个用分号隔开）\",\n" +
                    "  \"meaningEn\": \"牛津/柯林斯简明英文释义\",\n" +
                    "  \"contextTranslation\": \"提供语境例句的地道学术中文翻译（若未提供语境则给出该词最地道的语境例句翻译）\",\n" +
                    "  \"collocations\": [\n" +
                    "    {\"en\": \"常用学术搭配1\", \"cn\": \"中文释义\"},\n" +
                    "    {\"en\": \"常用学术搭配2\", \"cn\": \"中文释义\"},\n" +
                    "    {\"en\": \"常用学术搭配3\", \"cn\": \"中文释义\"}\n" +
                    "  ],\n" +
                    "  \"academicExamples\": [\n" +
                    "    {\"en\": \"顶级期刊经典学术例句1\", \"cn\": \"中文对照翻译\", \"source\": \"Nature / IEEE / ACL\"},\n" +
                    "    {\"en\": \"顶级期刊经典学术例句2\", \"cn\": \"中文对照翻译\", \"source\": \"Science / Cell\"}\n" +
                    "  ],\n" +
                    "  \"synonyms\": [\"近义词1\", \"近义词2\", \"近义词3\"],\n" +
                    "  \"antonyms\": [\"反义词1\", \"反义词2\"],\n" +
                    "  \"etymology\": \"词根词缀结构拆解与联想记忆口诀（如 pre-[前] + ponder[称重/沉思] -> 重量在前面 -> 占优势）\"\n" +
                    "}";

            String userPrompt = "待解析学术单词：" + word + "\n" +
                    (contextSentence.isEmpty() ? "" : "原文献语境句子：" + contextSentence);

            var result = aiChatService.chatJsonWithModelFallback(systemPrompt, userPrompt, 2500, List.of());
            if (result != null && result.content() != null && !result.content().isBlank()) {
                String cleaned = result.content().trim();
                if (cleaned.contains("```json")) {
                    int start = cleaned.indexOf("```json") + 7;
                    int end = cleaned.lastIndexOf("```");
                    if (end > start) cleaned = cleaned.substring(start, end).trim();
                } else if (cleaned.contains("```")) {
                    int start = cleaned.indexOf("```") + 3;
                    int end = cleaned.lastIndexOf("```");
                    if (end > start) cleaned = cleaned.substring(start, end).trim();
                }
                if (cleaned.startsWith("<think>")) {
                    int endThink = cleaned.indexOf("</think>");
                    if (endThink != -1) cleaned = cleaned.substring(endThink + 8).trim();
                }
                return objectMapper.readTree(cleaned);
            }
        } catch (Exception e) {
            log.warn("AI enrichment failed for word: {}, generating local fallback: {}", word, e.getMessage());
        }

        // Fallback structured template
        ObjectNode fallback = objectMapper.createObjectNode();
        fallback.put("word", word);
        fallback.put("phonetic", "/" + word.toLowerCase() + "/");
        fallback.put("partOfSpeech", "adj./n.");
        fallback.put("oxfordLevel", "Academic");
        fallback.put("meaningCn", "学术专用词汇 / 专有名词");
        fallback.put("meaningEn", "Academic term extracted from literature");
        fallback.put("contextTranslation", contextSentence.isEmpty() ? "" : "（语境例句已收录）");
        fallback.set("collocations", objectMapper.createArrayNode());
        fallback.set("academicExamples", objectMapper.createArrayNode());
        fallback.set("synonyms", objectMapper.createArrayNode());
        fallback.set("antonyms", objectMapper.createArrayNode());
        fallback.put("etymology", "词源解析可通过点击「AI 深度解析」实时生成");
        return fallback;
    }
}
