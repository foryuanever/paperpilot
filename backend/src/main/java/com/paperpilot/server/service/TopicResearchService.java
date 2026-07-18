package com.paperpilot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.entity.TopicResearchEntity;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.TopicResearchRepository;
import com.paperpilot.server.vo.SearchPaperVO;
import com.paperpilot.server.vo.SearchResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TopicResearchService {
    private static final List<String> TOPIC_FALLBACK_MODELS = List.of(
        "deepseek/deepseek-chat",
        "qwen/qwen3-30b-a3b-instruct",
        "moonshotai/kimi-k2",
        "google/gemini-2.5-flash"
    );

    private final TopicResearchRepository topicResearchRepository;
    private final PaperRepository paperRepository;
    private final CurrentUserService currentUserService;
    private final AiChatService aiChatService;
    private final ExternalSearchService externalSearchService;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper;

    public TopicResearchService(
        TopicResearchRepository topicResearchRepository,
        PaperRepository paperRepository,
        CurrentUserService currentUserService,
        AiChatService aiChatService,
        ExternalSearchService externalSearchService,
        AiUsageService aiUsageService,
        ObjectMapper objectMapper
    ) {
        this.topicResearchRepository = topicResearchRepository;
        this.paperRepository = paperRepository;
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
        this.externalSearchService = externalSearchService;
        this.aiUsageService = aiUsageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Map<String, Object>> list(String keyword, String discipline, String stage, String goal, String sort, boolean savedOnly) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        String q = text(keyword).toLowerCase();
        return topicResearchRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(topic -> !savedOnly || isSaved(topic, user.getId()))
            .filter(topic -> !StringUtils.hasText(q) || searchable(topic).toLowerCase().contains(q))
            .filter(topic -> !StringUtils.hasText(discipline) || text(topic.getDiscipline()).equals(discipline))
            .filter(topic -> !StringUtils.hasText(stage) || text(topic.getStage()).equals(stage))
            .filter(topic -> !StringUtils.hasText(goal) || text(topic.getGoal()).equals(goal))
            .sorted(comparator(sort))
            .map(topic -> toMap(topic, user.getId()))
            .toList();
    }

    @Transactional
    public List<Map<String, Object>> generate(Map<String, Object> body) {
        return generateForUser(currentUserService.getOrCreateDefaultUser(), body, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminList(String keyword) {
        AppUserEntity admin = currentUserService.requireAdmin();
        String q = text(keyword).toLowerCase(Locale.ROOT);
        return topicResearchRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(topic -> !StringUtils.hasText(q) || searchable(topic).toLowerCase(Locale.ROOT).contains(q))
            .map(topic -> toMap(topic, admin.getId()))
            .toList();
    }

    @Transactional
    public Map<String, Object> adminDelete(String id) {
        currentUserService.requireAdmin();
        TopicResearchEntity topic = findTopic(id);
        topicResearchRepository.delete(topic);
        return Map.of("id", id, "deleted", true);
    }

    @Transactional
    public List<Map<String, Object>> generateOfficialHotTopics(Map<String, Object> body) {
        AppUserEntity admin = currentUserService.requireAdmin();
        Map<String, Object> request = body == null ? Map.of() : body;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direction", defaultText(request, "direction", "2026 年前沿 AI 科研选题"));
        payload.put("discipline", defaultText(request, "discipline", "人工智能"));
        payload.put("stage", defaultText(request, "stage", "硕士"));
        payload.put("goal", defaultText(request, "goal", "投稿选题"));
        payload.put("resource", defaultText(request, "resource", "无实验，仅公开数据"));
        payload.put("dataAccess", "公开数据集、开源代码和近两年代表论文优先");
        payload.put("methodPreference", "先找可复现实验路线，再判断创新空间");
        payload.put("topicScale", "能在 2-4 个月推进的硕士/低年级博士小题");
        payload.put("outputDepth", "详细：每个推荐方向都要给摘要、具体方法、发文现状、优势、局限、潜在论文和代表论文");
        payload.put("evaluationFocus", "前沿性、可复现、数据可得和投稿价值");
        payload.put("expectedContribution", "从大方向拆出可验证的小问题");
        payload.put("constraints", List.of("必须有真实代表论文", "必须有公开数据或可替代数据", "推荐方向之间不能重复", "适合用户继续导入文献库"));
        payload.put("keywords", "large language model multimodal medical image foundation model drug discovery time series education AI 2026");
        payload.put("avoidRoutes", "不做空泛综述，不做只有概念没有数据的题目，不推荐无法验证的宏大命题");
        payload.put("note", "管理员发布到选题广场的官方热门方向：需要覆盖最新研究热度，同时每个推荐方向都要精确对应代表论文。");
        payload.put("maxTopics", 3);
        return generateForUser(admin, payload, true);
    }

    private List<Map<String, Object>> generateForUser(AppUserEntity user, Map<String, Object> body, boolean officialPublish) {
        String direction = text(body.get("direction"));
        if (!StringUtils.hasText(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写研究方向大类");
        }
        String discipline = defaultText(body, "discipline", direction);
        String stage = defaultText(body, "stage", "硕士");
        String goal = defaultText(body, "goal", "开题");
        String resource = defaultText(body, "resource", "无实验，仅公开数据");
        String dataAccess = defaultText(body, "dataAccess", "公开数据集优先");
        String researchObject = text(body.get("researchObject"));
        String sampleType = defaultText(body, "sampleType", "暂不确定");
        String methodPreference = defaultText(body, "methodPreference", "不限，先找可行路线");
        String topicScale = defaultText(body, "topicScale", "硕士可完成的小题");
        String outputDepth = defaultText(body, "outputDepth", "标准：每类给摘要/方法/风险/文献");
        String evaluationFocus = defaultText(body, "evaluationFocus", "准确率/效果提升");
        String expectedContribution = defaultText(body, "expectedContribution", "提出一个可验证问题");
        String keywords = text(body.get("keywords"));
        String avoidRoutes = text(body.get("avoidRoutes"));
        String seedPapers = text(body.get("seedPapers"));
        List<String> constraints = stringList(body.get("constraints"));
        String note = text(body.get("note"));
        int maxTopics = Math.max(1, Math.min(officialPublish ? 3 : 1, intValue(body.get("maxTopics"), officialPublish ? 3 : 1)));
        String searchQuery = String.join(" ", direction, researchObject, sampleType, keywords, seedPapers).trim();
        List<Map<String, Object>> evidencePapers = searchAcademicEvidence(searchQuery, discipline, goal);

        List<TopicResearchEntity> entities;
        try {
            Map<String, Object> researchContext = new LinkedHashMap<>();
            researchContext.put("researchCategory", direction);
            researchContext.put("discipline", discipline);
            researchContext.put("stage", stage);
            researchContext.put("goal", goal);
            researchContext.put("resource", resource);
            researchContext.put("dataAccess", dataAccess);
            researchContext.put("researchObject", researchObject);
            researchContext.put("sampleType", sampleType);
            researchContext.put("methodPreference", methodPreference);
            researchContext.put("topicScale", topicScale);
            researchContext.put("outputDepth", outputDepth);
            researchContext.put("evaluationFocus", evaluationFocus);
            researchContext.put("expectedContribution", expectedContribution);
            researchContext.put("constraints", constraints);
            researchContext.put("keywords", keywords);
            researchContext.put("avoidRoutes", avoidRoutes);
            researchContext.put("seedPapers", seedPapers);
            researchContext.put("note", note);
            researchContext.put("maxTopics", maxTopics);
            researchContext.put("academic_search_results", evidencePapers);
            AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallback(
                "你是 deep-research 选题调研 agent。只输出 JSON，不要 Markdown。必须基于 academic_search_results 中的真实检索候选做选题，不允许编造论文题名、DOI、作者或年份。任务：用户给的是研究方向大类和 research brief，你需要生成 maxTopics 张可供选择的选题卡；如果 maxTopics=1，就只输出 1 张。彼此方向必须明显不同，不允许同一套小类反复换标题。JSON 字段：topics(array)。每个 topic 包含 title, summary, discipline, stage, goal, tags(array), themeClusters(array), researchQuestion, researchGap, methodRoute, riskNote, feasibilityScore(number), innovationScore(number), difficultyScore(number), subtopics(array of {name, analysis, recommendationScore(number), papers(array of paper title from academic_search_results)}), representativePapers(array of {title, source, year, reason})。每张卡必须先推荐 3-5 个具体研究方向。每个小方向名称控制在 8-16 个中文字符，必须像“低剂量 CT 小样本分割”“影像报告跨模态对齐”这种可直接开题的切口；禁止写“先读某论文”“数据与样本”“方法路线”“评价指标”“研究问题”这类栏目名。analysis 必须像调研报告，不许模板化，并且必须严格用 7 段格式：'【摘要】...【具体方法】...【发文现状】...【优势】...【局限】...【潜在论文】...【代表论文】...'。每段至少 55 个中文字符；必须写清研究对象、数据/样本形态、评价重点、基线方法、发文热度、预期结果或失败边界。代表论文只能从 academic_search_results 里选择，并且要精准对应当前方向；如果候选不足，就明确写“候选文献不足，需要继续检索”。必须避开 avoidRoutes，优先满足 constraints、evaluationFocus 和 expectedContribution。",
                objectMapper.writeValueAsString(researchContext),
                2600,
                TOPIC_FALLBACK_MODELS
            );
            entities = fromAiJsonManyWithRepair(result.content(), result.modelName(), user.getId(), discipline, stage, goal, evidencePapers, researchContext);
            for (TopicResearchEntity entity : entities) {
                if (!evidencePapers.isEmpty()) {
                    entity.setRepresentativePapersJson(objectMapper.writeValueAsString(mergeEvidencePapers(parsePapers(entity.getRepresentativePapersJson()), evidencePapers)));
                    entity.setSource(officialPublish ? "官方" : "deep-research + academic-search");
                }
                if (officialPublish) entity.setModelName(firstNonBlank(entity.getModelName(), "daily-frontier"));
            }
        } catch (Exception error) {
            aiUsageService.recordFailure(
                user.getId(),
                "topic-quality-gate",
                ModelConfigService.SCENE_TOPIC_RESEARCH,
                officialPublish ? "官方热点质检" : "选题调研质检",
                direction,
                0L,
                error.getMessage(),
                0L
            );
            String prefix = officialPublish ? "官方热点必须经过 AI 模型生成" : "选题调研必须经过 AI 模型生成";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, prefix + "，当前调研广场模型调用失败：" + error.getMessage());
        }
        String userId = String.valueOf(user.getId());
        List<TopicResearchEntity> saved = new ArrayList<>();
        for (TopicResearchEntity entity : entities.stream().limit(maxTopics).toList()) {
            if (officialPublish) {
                entity.setSource("官方");
                entity.setSavedByUserIds("");
                if (!StringUtils.hasText(entity.getModelName())) entity.setModelName("daily-frontier");
            } else {
                entity.setSavedByUserIds(userId);
            }
            saved.add(topicResearchRepository.save(entity));
        }
        return saved.stream().map(topic -> toMap(topic, user.getId())).toList();
    }

    @Transactional
    public Map<String, Object> toggleSave(String id) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        TopicResearchEntity topic = findTopic(id);
        LinkedHashSet<String> ids = savedIds(topic);
        String userId = String.valueOf(user.getId());
        boolean saved;
        if (ids.contains(userId)) {
            ids.remove(userId);
            saved = false;
        } else {
            ids.add(userId);
            saved = true;
        }
        topic.setSavedByUserIds(String.join(",", ids));
        topicResearchRepository.save(topic);
        return Map.of("id", publicId(topic), "saved", saved);
    }

    @Transactional
    public Map<String, Object> markInterested(String id) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        TopicResearchEntity topic = findTopic(id);
        LinkedHashSet<String> ids = interestedIds(topic);
        String userId = String.valueOf(user.getId());
        boolean interested = ids.contains(userId);
        if (!interested) {
            ids.add(userId);
            topic.setInterestedByUserIds(String.join(",", ids));
            topic.setLikes(Math.max(value(topic.getLikes()), ids.size()));
            interested = true;
        }
        topicResearchRepository.save(topic);
        return Map.of("id", publicId(topic), "likes", value(topic.getLikes()), "interested", interested);
    }

    @Transactional
    public Map<String, Object> importToLibrary(String id) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        TopicResearchEntity topic = findTopic(id);
        List<Map<String, Object>> papers = parsePapers(topic.getRepresentativePapersJson());
        int imported = 0;
        for (Map<String, Object> paper : papers) {
            if (saveTopicPaper(user, topic, paper)) imported++;
        }
        topic.setDownloads(value(topic.getDownloads()) + 1);
        topicResearchRepository.save(topic);
        return Map.of("message", "已加入文献库", "imported", imported);
    }

    @Transactional
    public Map<String, Object> importPaperToLibrary(String id, Map<String, Object> body) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        TopicResearchEntity topic = findTopic(id);
        Map<String, Object> paper = resolvePaperPayload(topic, body == null ? Map.of() : body);
        if (!saveTopicPaper(user, topic, paper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可导入的文献信息");
        }
        topic.setDownloads(value(topic.getDownloads()) + 1);
        topicResearchRepository.save(topic);
        return Map.of("message", "已导入文献库", "imported", 1, "paper", paper);
    }

    private boolean saveTopicPaper(AppUserEntity user, TopicResearchEntity topic, Map<String, Object> paper) {
        String title = text(paper.get("title"));
        if (!StringUtils.hasText(title)) return false;
        PaperEntity row = new PaperEntity();
        row.setWorkspaceId("ws-topic-" + UUID.randomUUID());
        row.setUserId(user.getId());
        row.setTitle(title);
        row.setAuthors(text(paper.get("authors")));
        row.setSource(defaultText(paper, "source", "选题广场"));
        row.setPublishYear(firstNonBlank(text(paper.get("year")), text(paper.get("publishYear"))));
        row.setPaperUrl(text(paper.get("paperUrl")));
        row.setSourceUrl(firstNonBlank(text(paper.get("url")), text(paper.get("sourceUrl")), text(paper.get("doi"))));
        row.setImportSource("topic-square");
        row.setProgress("0%");
        row.setImportance("B");
        row.setNote("来自选题广场：" + topic.getTitle());
        row.setJournalTags(topic.getTags());
        row.setFolder("选题调研");
        row.setUploadedAt(LocalDate.now());
        paperRepository.save(row);
        return true;
    }

    private Map<String, Object> resolvePaperPayload(TopicResearchEntity topic, Map<String, Object> body) {
        String title = text(body.get("title"));
        if (StringUtils.hasText(title)) {
            for (Map<String, Object> paper : parsePapers(topic.getRepresentativePapersJson())) {
                if (normalizeTitle(text(paper.get("title"))).equals(normalizeTitle(title))) {
                    return paperWithFallback(body, paper);
                }
            }
            return paperWithFallback(body, Map.of());
        }
        return body;
    }

    private Map<String, Object> paperWithFallback(Map<String, Object> preferred, Map<String, Object> fallback) {
        Map<String, Object> paper = new LinkedHashMap<>(fallback);
        for (Map.Entry<String, Object> entry : preferred.entrySet()) {
            if (StringUtils.hasText(text(entry.getValue()))) paper.put(entry.getKey(), entry.getValue());
        }
        return paper;
    }

    public Map<String, Object> exportOutline(String id, String target) {
        TopicResearchEntity topic = findTopic(id);
        return Map.of(
            "message", "已生成可导入内容",
            "target", StringUtils.hasText(target) ? target : "review",
            "topic", toMap(topic, currentUserService.getOrCreateDefaultUserId()),
            "outline", List.of(
                "研究背景：" + text(topic.getSummary()),
                "研究问题：" + text(topic.getResearchQuestion()),
                "研究空白：" + text(topic.getResearchGap()),
                "方法路线：" + text(topic.getMethodRoute()),
                "风险与边界：" + text(topic.getRiskNote())
            )
        );
    }

    private List<TopicResearchEntity> fromAiJsonMany(String raw, Long userId, String modelName, String fallbackDiscipline, String fallbackStage, String fallbackGoal, List<Map<String, Object>> evidencePapers) throws Exception {
        JsonNode root = objectMapper.readTree(stripJson(raw));
        JsonNode topicsNode = root.path("topics");
        if (!topicsNode.isArray()) {
            topicsNode = objectMapper.createArrayNode().add(root);
        }
        List<TopicResearchEntity> entities = new ArrayList<>();
        for (JsonNode item : topicsNode) {
            TopicResearchEntity entity = fromAiTopicNode(item, userId, modelName, fallbackDiscipline, fallbackStage, fallbackGoal);
            entity.setSubtopicsJson(writeSubtopics(normalizeSubtopics(entity, evidencePapers, true)));
            entities.add(entity);
        }
        if (entities.isEmpty()) {
            throw new IllegalStateException("模型返回为空，没有生成可保存的选题卡");
        }
        return entities;
    }

    private List<TopicResearchEntity> fromAiJsonManyWithRepair(
        String raw,
        String modelName,
        Long userId,
        String fallbackDiscipline,
        String fallbackStage,
        String fallbackGoal,
        List<Map<String, Object>> evidencePapers,
        Map<String, Object> researchContext
    ) throws Exception {
        String candidate = raw;
        String candidateModel = modelName;
        try {
            return fromAiJsonMany(candidate, userId, candidateModel, fallbackDiscipline, fallbackStage, fallbackGoal, evidencePapers);
        } catch (IllegalStateException qualityError) {
            IllegalStateException lastQualityError = qualityError;
            for (int attempt = 1; attempt <= 2; attempt++) {
                AiChatService.ChatResult repaired = repairTopicResearchJson(candidate, researchContext, evidencePapers, lastQualityError.getMessage(), attempt);
                candidate = repaired.content();
                candidateModel = repaired.modelName();
                try {
                    return fromAiJsonMany(candidate, userId, candidateModel, fallbackDiscipline, fallbackStage, fallbackGoal, evidencePapers);
                } catch (IllegalStateException nextQualityError) {
                    lastQualityError = nextQualityError;
                }
            }
            return fromAiJsonManyLenient(candidate, userId, candidateModel, fallbackDiscipline, fallbackStage, fallbackGoal, evidencePapers);
        }
    }

    private List<TopicResearchEntity> fromAiJsonManyLenient(String raw, Long userId, String modelName, String fallbackDiscipline, String fallbackStage, String fallbackGoal, List<Map<String, Object>> evidencePapers) throws Exception {
        JsonNode root = objectMapper.readTree(stripJson(raw));
        JsonNode topicsNode = root.path("topics");
        if (!topicsNode.isArray()) {
            topicsNode = objectMapper.createArrayNode().add(root);
        }
        List<TopicResearchEntity> entities = new ArrayList<>();
        for (JsonNode item : topicsNode) {
            TopicResearchEntity entity = fromAiTopicNode(item, userId, modelName, fallbackDiscipline, fallbackStage, fallbackGoal);
            entity.setSubtopicsJson(writeSubtopics(normalizeSubtopics(entity, evidencePapers, false)));
            entities.add(entity);
        }
        if (entities.isEmpty()) {
            throw new IllegalStateException("模型返回为空，没有生成可保存的选题卡");
        }
        return entities;
    }

    private AiChatService.ChatResult repairTopicResearchJson(
        String previousJson,
        Map<String, Object> researchContext,
        List<Map<String, Object>> evidencePapers,
        String qualityError,
        int attempt
    ) throws Exception {
        Map<String, Object> repairContext = new LinkedHashMap<>();
        repairContext.put("quality_error", qualityError);
        repairContext.put("repair_attempt", attempt);
        repairContext.put("research_brief", researchContext);
        repairContext.put("previous_json", stripJson(previousJson));
        repairContext.put("academic_search_results", evidencePapers);
        return aiChatService.chatJsonWithModelFallback(
            "你是选题调研质检返工 agent。只输出 JSON，不要 Markdown。上一轮结果没有通过质量门，quality_error 已明确指出失败原因。你必须重写失败的小方向，而不是微调一句话。严格要求：1）如果 research_brief.maxTopics=1，只输出 1 张 topic；2）每张 topic 必须有 3-5 个 subtopics；3）subtopic.name 必须是 8-16 个中文字符的具体论文切口，像“低剂量CT肺结节分割”“小样本MRI肿瘤边界校准”，不能是栏目名，不能写“研究问题/方法路线/数据与样本/评价指标/应用边界/小方向/现状分析”；4）每个 subtopic.analysis 必须包含【摘要】【具体方法】【发文现状】【优势】【局限】【潜在论文】【代表论文】七段。每段写 2 个短句或 2 个分号要点，至少 45 个中文字符；必须写清研究对象、数据/样本形态、评价重点、基线方法、发文热度、预期结果或失败边界；5）禁止使用“具有重要意义、具有潜力、至关重要、有望发表、相关研究较少”这种空话；6）每个 subtopic.papers 必须从 academic_search_results 里选择 1-3 篇精准对应的题名，代表论文段也要点名这些论文；7）不得编造论文。输出 schema：{topics:[{title,summary,discipline,stage,goal,tags,themeClusters,researchQuestion,researchGap,methodRoute,riskNote,feasibilityScore,innovationScore,difficultyScore,subtopics,representativePapers}]}",
            objectMapper.writeValueAsString(repairContext),
            5200,
            TOPIC_FALLBACK_MODELS
        );
    }

    private TopicResearchEntity fromAiTopicNode(JsonNode root, Long userId, String modelName, String fallbackDiscipline, String fallbackStage, String fallbackGoal) throws Exception {
        TopicResearchEntity entity = new TopicResearchEntity();
        entity.setUserId(userId);
        entity.setTitle(requireNode(root, "title"));
        entity.setSummary(requireNode(root, "summary"));
        entity.setDiscipline(defaultNode(root, "discipline", fallbackDiscipline));
        entity.setStage(defaultNode(root, "stage", fallbackStage));
        entity.setGoal(defaultNode(root, "goal", fallbackGoal));
        entity.setSource("AI生成");
        entity.setTags(joinNodeArray(root.path("tags")));
        entity.setThemeClusters(joinNodeArray(root.path("themeClusters")));
        entity.setResearchQuestion(defaultNode(root, "researchQuestion", ""));
        entity.setResearchGap(defaultNode(root, "researchGap", ""));
        entity.setMethodRoute(defaultNode(root, "methodRoute", ""));
        entity.setRiskNote(defaultNode(root, "riskNote", ""));
        entity.setFeasibilityScore(clamp(root.path("feasibilityScore").asInt(72)));
        entity.setInnovationScore(clamp(root.path("innovationScore").asInt(72)));
        entity.setDifficultyScore(clamp(root.path("difficultyScore").asInt(60)));
        entity.setRepresentativePapersJson(objectMapper.writeValueAsString(readPaperArray(root.path("representativePapers"))));
        entity.setSubtopicsJson(objectMapper.writeValueAsString(readSubtopicArray(root.path("subtopics"))));
        entity.setModelName(modelName);
        if (parseSubtopics(entity.getSubtopicsJson()).isEmpty()) {
            throw new IllegalStateException("模型没有返回合格的小方向 subtopics");
        }
        return entity;
    }

    private List<TopicResearchEntity> deterministicTopicSet(Long userId, String direction, String discipline, String stage, String goal, String resource, String note, String modelName, List<Map<String, Object>> evidencePapers, String dataAccess, String methodPreference, String topicScale, List<String> constraints, String avoidRoutes) {
        List<String> lanes = researchLanes(direction, discipline, methodPreference, constraints);
        List<TopicResearchEntity> topics = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            String lane = lanes.get(i % lanes.size());
            TopicResearchEntity entity = deterministicTopic(userId, direction + "：" + lane + "切入", discipline, stage, goal, resource, note, modelName, dataAccess, methodPreference, topicScale, constraints, avoidRoutes);
            entity.setTitle("面向" + direction + "的" + lane + "研究路线");
            entity.setSummary("围绕“" + direction + "”从“" + lane + "”收窄成可执行选题；优先检查真实文献、数据来源、方法基线和评价指标，避免停留在泛泛概念。");
            entity.setThemeClusters(String.join(",", concreteTopicClusters(direction, discipline, lane)));
            entity.setSubtopicsJson(writeSubtopics(buildSubtopics(entity, evidencePapers)));
            topics.add(entity);
        }
        return topics;
    }

    private TopicResearchEntity deterministicTopic(Long userId, String direction, String discipline, String stage, String goal, String resource, String note, String modelName) {
        return deterministicTopic(userId, direction, discipline, stage, goal, resource, note, modelName, "公开数据集优先", "不限，先找可行路线", "硕士可完成的小题", List.of(), "");
    }

    private TopicResearchEntity deterministicTopic(Long userId, String direction, String discipline, String stage, String goal, String resource, String note, String modelName, String dataAccess, String methodPreference, String topicScale, List<String> constraints, String avoidRoutes) {
        TopicResearchEntity entity = new TopicResearchEntity();
        entity.setUserId(userId);
        entity.setTitle(direction + "的可行选题与研究路线");
        entity.setSummary("围绕“" + direction + "”拆出一个可执行选题，适合" + stage + goal + "；资源条件为：" + resource + "，数据来源偏好为：" + dataAccess + "。");
        entity.setDiscipline(discipline);
        entity.setStage(stage);
        entity.setGoal(goal);
        entity.setSource("AI生成");
        entity.setTags(String.join(",", compactTags(List.of(goal, discipline, "deep-research", resource.replace("，", " ").split(" ")[0], methodPreference, topicScale))));
        entity.setThemeClusters(String.join(",", concreteTopicClusters(direction, discipline, methodPreference)));
        entity.setResearchQuestion("在" + direction + "中，哪些关键变量、数据条件或应用场景尚未被充分解释？");
        entity.setResearchGap("已有研究往往偏重方法效果展示，缺少对数据边界、可复现对照和真实场景迁移的连续分析。");
        entity.setMethodRoute("先扩展英文关键词并做系统检索，再建立代表论文矩阵；随后按“数据-方法-指标-基线”筛出 3-5 个推荐方向，最后选择一个能复现或对比验证的小问题。方法偏好：" + methodPreference + "。");
        entity.setRiskNote(StringUtils.hasText(avoidRoutes) ? "需要避开：" + avoidRoutes : (StringUtils.hasText(note) ? "注意和补充说明保持一致：" + note : "需要确认数据来源、评价指标、伦理边界和导师认可的研究范围。"));
        entity.setFeasibilityScore(resource.contains("公开") ? 86 : 76);
        entity.setInnovationScore(74);
        entity.setDifficultyScore("博士".equals(stage) ? 78 : 58);
        entity.setRepresentativePapersJson(writePapers(List.of(
            Map.of("title", direction + ": recent progress and open challenges", "source", "Deep Research Seed", "year", "2026", "reason", "用于快速建立研究背景"),
            Map.of("title", "A survey of methods for " + direction, "source", "Topic Square", "year", "2025", "reason", "用于整理方法分类和研究空白")
        )));
        entity.setSubtopicsJson(writeSubtopics(buildSubtopics(entity, parsePapers(entity.getRepresentativePapersJson()))));
        entity.setModelName(modelName);
        return entity;
    }

    @Scheduled(cron = "0 20 6 * * *", zone = "Asia/Shanghai")
    @Transactional
    public void refreshDailyFrontierTopics() {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        LocalDate today = LocalDate.now();
        boolean alreadyUpdated = topicResearchRepository.findAllByOrderByCreatedAtDesc().stream()
            .anyMatch(topic -> text(topic.getSource()).contains("daily-frontier") && topic.getCreatedAt() != null && today.equals(topic.getCreatedAt().toLocalDate()));
        if (alreadyUpdated) return;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("direction", "2026 年可继续推进的前沿科研选题");
        payload.put("discipline", "人工智能");
        payload.put("stage", "硕士");
        payload.put("goal", "前沿追踪");
        payload.put("resource", "无实验，仅公开数据");
        payload.put("dataAccess", "公开数据集、开源代码和近两年代表论文优先");
        payload.put("methodPreference", "先找可复现实验路线，再判断创新空间");
        payload.put("topicScale", "能在 2-4 个月推进的硕士/低年级博士小题");
        payload.put("outputDepth", "详细：每个推荐方向都要给摘要、具体方法、发文现状、优势、局限、潜在论文和代表论文");
        payload.put("evaluationFocus", "前沿性、可复现、数据可得和投稿价值");
        payload.put("expectedContribution", "从大方向拆出可验证的小问题");
        payload.put("constraints", List.of("必须有真实代表论文", "必须有公开数据或可替代数据", "推荐方向之间不能重复", "适合用户继续导入文献库"));
        payload.put("keywords", "large language model multimodal medical image foundation model drug discovery time series education AI 2026");
        payload.put("avoidRoutes", "不做空泛综述，不做只有概念没有数据的题目，不推荐无法验证的宏大命题");
        payload.put("note", "每日自动发布到选题广场的官方前沿方向，必须经过选题调研模型生成。");
        payload.put("maxTopics", 3);
        try {
            generateForUser(user, payload, true);
        } catch (ResponseStatusException ignored) {
            // Daily refresh must never publish deterministic placeholder topics.
        }
    }

    private List<Map<String, Object>> searchAcademicEvidence(String direction, String discipline, String goal) {
        LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
        List<String> queries = new ArrayList<>();
        queries.add(direction);
        if (StringUtils.hasText(goal)) queries.add(direction + " " + goal);
        if (StringUtils.hasText(discipline)) queries.add(direction + " " + discipline);

        List<String> sources = new ArrayList<>(List.of("semantic-scholar", "crossref"));
        if (text(discipline).contains("医学") || text(direction).toLowerCase(Locale.ROOT).contains("medical") || text(direction).contains("临床")) {
            sources.add(0, "pubmed");
        }

        for (String query : queries) {
            for (String source : sources) {
                try {
                    SearchResultVO result = externalSearchService.searchByQuery(query, source, 1, 10);
                    for (SearchPaperVO paper : result.getItems()) {
                        if (paper == null || !StringUtils.hasText(paper.getTitle())) continue;
                        String key = normalizeTitle(paper.getTitle());
                        if (!StringUtils.hasText(key) || merged.containsKey(key)) continue;
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("title", paper.getTitle());
                        row.put("source", defaultString(paper.getSource(), source));
                        row.put("year", text(paper.getYear()));
                        row.put("authors", text(paper.getAuthors()));
                        row.put("doiOrId", text(paper.getId()));
                        row.put("url", firstNonBlank(paper.getSourceUrl(), paper.getPdfUrl()));
                        row.put("abstract", shorten(text(paper.getAbstractText()), 520));
                        row.put("subjects", paper.getSubjects() == null ? List.of() : paper.getSubjects());
                        row.put("reason", reasonFromPaper(paper, goal));
                        row.put("verifiedBy", result.getSource());
                        merged.put(key, row);
                    }
                } catch (Exception ignored) {
                    // Academic search is a source of evidence, not a blocker for topic generation.
                }
                if (merged.size() >= 12) break;
            }
            if (merged.size() >= 12) break;
        }
        return merged.values().stream().limit(10).toList();
    }

    private List<Map<String, Object>> mergeEvidencePapers(List<Map<String, Object>> aiPapers, List<Map<String, Object>> evidencePapers) {
        LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Map<String, Object> evidence : evidencePapers) {
            String key = normalizeTitle(text(evidence.get("title")));
            if (StringUtils.hasText(key)) merged.put(key, new LinkedHashMap<>(evidence));
        }
        for (Map<String, Object> paper : aiPapers) {
            String key = normalizeTitle(text(paper.get("title")));
            if (!merged.containsKey(key)) continue;
            Map<String, Object> existing = merged.get(key);
            String reason = text(paper.get("reason"));
            if (StringUtils.hasText(reason)) existing.put("reason", reason);
        }
        return merged.values().stream().limit(8).toList();
    }

    private String reasonFromPaper(SearchPaperVO paper, String goal) {
        String source = text(paper.getSource());
        String year = text(paper.getYear());
        StringBuilder reason = new StringBuilder("真实检索命中");
        if (StringUtils.hasText(source)) reason.append("，来源：").append(source);
        if (StringUtils.hasText(year)) reason.append("，年份：").append(year);
        if (StringUtils.hasText(goal)) reason.append("；可用于").append(goal).append("阶段建立代表文献池");
        return reason.toString();
    }

    private void seedIfEmpty(Long userId) {
        if (topicResearchRepository.count() > 0) return;
        topicResearchRepository.save(deterministicTopic(userId, "物理信息神经网络 × 时序建模", "计算机", "硕士", "开题", "无实验，仅公开数据", "侧重 PINN 与 LSTM 的时序预测结合", "seed"));
        topicResearchRepository.save(deterministicTopic(userId, "多模态医学影像与医学 AI", "医学", "硕士", "综述", "有公开医学影像数据", "视觉-语言基础模型到数据高效落地", "seed"));
        topicResearchRepository.save(deterministicTopic(userId, "Mamba 与 YOLO 融合的目标检测新范式", "计算机", "博士", "投稿选题", "有实验室/设备", "强调状态空间模型与实时检测", "seed"));
        topicResearchRepository.save(deterministicTopic(userId, "液态神经网络动态时序建模", "计算机", "硕士", "基金背景", "无实验，仅公开数据", "LTC/CFC/NCP/Liquid-S4 对比", "seed"));
    }

    private Map<String, Object> toMap(TopicResearchEntity topic, Long userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", publicId(topic));
        map.put("title", topic.getTitle());
        map.put("summary", topic.getSummary());
        map.put("discipline", topic.getDiscipline());
        map.put("stage", topic.getStage());
        map.put("goal", topic.getGoal());
        map.put("source", topic.getSource());
        map.put("providerLabel", providerLabel(topic));
        map.put("tags", split(topic.getTags()));
        map.put("themeClusters", split(topic.getThemeClusters()));
        List<Map<String, Object>> papers = parsePapers(topic.getRepresentativePapersJson());
        map.put("question", fallbackQuestion(topic, papers));
        map.put("gap", fallbackGap(topic, papers));
        map.put("method", fallbackMethod(topic, papers));
        map.put("risk", fallbackRisk(topic, papers));
        map.put("papers", papers);
        map.put("subtopics", normalizeSubtopics(topic, papers, false));
        map.put("evidenceCount", papers.size());
        map.put("searchSources", papers.stream().map(paper -> text(paper.get("verifiedBy"))).filter(StringUtils::hasText).distinct().toList());
        map.put("feasibility", value(topic.getFeasibilityScore()));
        map.put("innovation", value(topic.getInnovationScore()));
        map.put("difficulty", value(topic.getDifficultyScore()));
        map.put("likes", value(topic.getLikes()));
        map.put("downloads", value(topic.getDownloads()));
        map.put("saved", isSaved(topic, userId));
        map.put("interested", isInterested(topic, userId));
        map.put("modelName", text(topic.getModelName()));
        map.put("createdAt", topic.getCreatedAt() == null ? "" : topic.getCreatedAt().toLocalDate().toString());
        LocalDateTime updatedAt = topic.getUpdatedAt() == null ? topic.getCreatedAt() : topic.getUpdatedAt();
        map.put("updatedAt", updatedAt == null ? "" : updatedAt.toLocalDate().toString());
        return map;
    }

    private String providerLabel(TopicResearchEntity topic) {
        String source = text(topic.getSource());
        String modelName = text(topic.getModelName());
        if (source.contains("官方") || source.contains("daily-frontier") || "seed".equals(modelName) || "daily-frontier".equals(modelName)) {
            return "官方";
        }
        return "匿名用户提供";
    }

    private TopicResearchEntity findTopic(String id) {
        return topicResearchRepository.findById(parseId(id))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "选题不存在"));
    }

    private Long parseId(String id) {
        String value = text(id).replace("topic-", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选题 ID 不正确");
        }
    }

    private String publicId(TopicResearchEntity topic) {
        return "topic-" + topic.getId();
    }

    private boolean isSaved(TopicResearchEntity topic, Long userId) {
        if (userId == null) return false;
        return savedIds(topic).contains(String.valueOf(userId));
    }

    private boolean isInterested(TopicResearchEntity topic, Long userId) {
        if (userId == null) return false;
        return interestedIds(topic).contains(String.valueOf(userId));
    }

    private LinkedHashSet<String> savedIds(TopicResearchEntity topic) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String item : text(topic.getSavedByUserIds()).split(",")) {
            if (StringUtils.hasText(item)) ids.add(item.trim());
        }
        return ids;
    }

    private LinkedHashSet<String> interestedIds(TopicResearchEntity topic) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String item : text(topic.getInterestedByUserIds()).split(",")) {
            if (StringUtils.hasText(item)) ids.add(item.trim());
        }
        return ids;
    }

    private Comparator<TopicResearchEntity> comparator(String sort) {
        String value = text(sort);
        if ("hot".equals(value)) {
            return Comparator.comparingInt((TopicResearchEntity topic) -> value(topic.getLikes()) + value(topic.getDownloads()) * 2).reversed();
        }
        if ("liked".equals(value)) {
            return Comparator.comparingInt((TopicResearchEntity topic) -> value(topic.getLikes())).reversed();
        }
        return Comparator.comparing(TopicResearchEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private String searchable(TopicResearchEntity topic) {
        return String.join(" ", text(topic.getTitle()), text(topic.getSummary()), text(topic.getTags()), text(topic.getThemeClusters()));
    }

    private String fallbackQuestion(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        if (StringUtils.hasText(topic.getResearchQuestion())) return topic.getResearchQuestion();
        return "围绕“" + text(topic.getTitle()) + "”，需要进一步确认代表文献共同回答的核心问题，以及该问题是否能被当前数据或材料验证。";
    }

    private String fallbackGap(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        if (StringUtils.hasText(topic.getResearchGap())) return topic.getResearchGap();
        if (!papers.isEmpty()) return "已检索到 " + papers.size() + " 篇候选文献，但仍需比较它们在数据来源、场景迁移、评价指标和适用边界上的空白。";
        return "当前缺少足够真实候选文献，建议先扩大关键词、加入英文同义词和具体应用场景后再判断研究空白。";
    }

    private String fallbackMethod(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        if (StringUtils.hasText(topic.getMethodRoute())) return topic.getMethodRoute();
        return "先用开放索引建立代表论文池，再按主题簇整理方法、数据、指标和结论；最后选取一个可复现的小问题做对照验证。";
    }

    private String fallbackRisk(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        if (StringUtils.hasText(topic.getRiskNote())) return topic.getRiskNote();
        return "风险在于候选文献覆盖不足、数据不可得或题目过宽；开题前应确认导师方向、数据权限和可复现评价指标。";
    }

    private String stripJson(String raw) {
        String value = text(raw);
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        return start >= 0 && end > start ? value.substring(start, end + 1) : value;
    }

    private String defaultNode(JsonNode root, String key, String fallback) {
        String value = root.path(key).asText("");
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String requireNode(JsonNode root, String key) {
        String value = root.path(key).asText("");
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("模型缺少必填字段：" + key);
        }
        return value.trim();
    }

    private String joinNodeArray(JsonNode node) {
        if (!node.isArray()) return "";
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("");
            if (StringUtils.hasText(value)) values.add(value.trim());
        }
        return String.join(",", values);
    }

    private List<Map<String, Object>> readPaperArray(JsonNode node) {
        List<Map<String, Object>> papers = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                Map<String, Object> paper = new LinkedHashMap<>();
                paper.put("title", item.path("title").asText(""));
                paper.put("source", item.path("source").asText(""));
                paper.put("year", item.path("year").asText(""));
                paper.put("reason", item.path("reason").asText(""));
                papers.add(paper);
            }
        }
        return papers;
    }

    private List<Map<String, Object>> readSubtopicArray(JsonNode node) {
        List<Map<String, Object>> subtopics = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                Map<String, Object> subtopic = new LinkedHashMap<>();
                subtopic.put("name", item.path("name").asText(""));
                subtopic.put("analysis", item.path("analysis").asText(""));
                subtopic.put("papers", readPaperRefArray(item.path("papers")));
                if (StringUtils.hasText(text(subtopic.get("name")))) subtopics.add(subtopic);
            }
        }
        return subtopics.stream().limit(5).toList();
    }

    private List<Map<String, Object>> readPaperRefArray(JsonNode node) {
        List<Map<String, Object>> papers = new ArrayList<>();
        if (!node.isArray()) return papers;
        for (JsonNode item : node) {
            if (item.isObject()) {
                Map<String, Object> paper = new LinkedHashMap<>();
                paper.put("title", item.path("title").asText(""));
                paper.put("source", item.path("source").asText(""));
                paper.put("year", item.path("year").asText(""));
                paper.put("url", item.path("url").asText(""));
                paper.put("verifiedBy", item.path("verifiedBy").asText(""));
                if (StringUtils.hasText(text(paper.get("title")))) papers.add(paper);
            } else {
                String title = item.asText("");
                if (StringUtils.hasText(title)) {
                    Map<String, Object> paper = new LinkedHashMap<>();
                    paper.put("title", title.trim());
                    papers.add(paper);
                }
            }
        }
        return papers;
    }

    private List<Map<String, Object>> parsePapers(String raw) {
        try {
            return objectMapper.readValue(StringUtils.hasText(raw) ? raw : "[]", new TypeReference<>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private List<Map<String, Object>> parseSubtopics(String raw) {
        try {
            return objectMapper.readValue(StringUtils.hasText(raw) ? raw : "[]", new TypeReference<>() {});
        } catch (Exception error) {
            return List.of();
        }
    }

    private String writePapers(List<Map<String, Object>> papers) {
        try {
            return objectMapper.writeValueAsString(papers);
        } catch (Exception error) {
            return "[]";
        }
    }

    private String writeSubtopics(List<Map<String, Object>> subtopics) {
        try {
            return objectMapper.writeValueAsString(subtopics);
        } catch (Exception error) {
            return "[]";
        }
    }

    private List<Map<String, Object>> buildSubtopics(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        List<String> names = concreteSubtopicNames(topic, papers);
        if (names.size() < 3) names = List.of("研究问题收窄", "数据与样本", "方法路线", "评价指标", "应用边界");
        List<Map<String, Object>> subtopics = new ArrayList<>();
        for (int i = 0; i < Math.min(5, Math.max(3, names.size())); i++) {
            String name = names.get(i % names.size());
            String context = String.join(" ", text(topic.getTitle()), text(topic.getSummary()), text(topic.getResearchQuestion()), text(topic.getMethodRoute()));
            List<Map<String, Object>> linked = rankPapersForSubtopic(name, context, papers, i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("analysis", concreteSubtopicAnalysis(topic, name, linked));
            item.put("papers", linked.stream().map(this::publicPaper).toList());
            subtopics.add(item);
        }
        return subtopics;
    }

    private List<Map<String, Object>> normalizeSubtopics(TopicResearchEntity topic, List<Map<String, Object>> papers, boolean strictQuality) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> item : parseSubtopics(topic.getSubtopicsJson())) {
            if (!StringUtils.hasText(text(item.get("name")))) continue;
            Map<String, Object> copy = new LinkedHashMap<>(item);
            String originalName = text(copy.get("name"));
            String originalAnalysis = text(copy.get("analysis"));
            if (isGenericSubtopicName(originalName)) {
                continue;
            }
            List<Map<String, Object>> linked = resolveSubtopicPapers(copy.get("papers"), papers, index, originalName, originalAnalysis);
            copy.put("name", originalName);
            copy.put("papers", linked);
            String analysis = text(copy.get("analysis"));
            if (!StringUtils.hasText(analysis)) {
                throw new IllegalStateException("模型返回的小方向缺少分析内容：" + originalName);
            }
            validateSubtopicAnalysis(originalName, analysis, strictQuality);
            normalized.add(copy);
            index++;
        }
        if (normalized.size() < 2) {
            throw new IllegalStateException("模型返回的小方向不够具体，至少需要 2 个可写论文的小切口");
        }
        return normalized.stream().limit(5).toList();
    }

    private void validateSubtopicAnalysis(String name, String analysis, boolean strictQuality) {
        List<String> warnings = new ArrayList<>();
        List<String> sections = List.of("摘要", "具体方法", "发文现状", "优势", "局限", "潜在论文", "代表论文");
        for (String section : sections) {
            if (!analysis.contains("【" + section + "】")) {
                throw new IllegalStateException("模型返回的小方向分析缺少【" + section + "】：" + name);
            }
        }
        Map<String, String> parts = splitAnalysisSections(analysis, sections);
        for (String section : List.of("摘要", "具体方法", "发文现状", "优势", "局限", "潜在论文")) {
            String value = text(parts.get(section));
            if (value.length() < 16) {
                throw new IllegalStateException("模型返回的小方向【" + section + "】几乎为空：" + name);
            }
            if (value.length() < 42) {
                warnings.add("【" + section + "】偏短");
            }
        }
        String compact = analysis.replaceAll("\\s+", "");
        if (containsAny(compact, "相关研究较少，主要集中在单模态应用", "该方向有望在", "具有重要意义", "具有潜力", "至关重要")
            && containsAny(compact, "候选文献不足，需要继续检索")) {
            throw new IllegalStateException("模型返回的小方向过于模板化，需要重新调研：" + name);
        }
        if (strictQuality && warnings.size() >= 3) {
            throw new IllegalStateException("模型返回的小方向段落过短，需要重新调研：" + name + "；" + String.join("、", warnings));
        }
    }

    private Map<String, String> splitAnalysisSections(String analysis, List<String> sections) {
        Map<String, String> parts = new LinkedHashMap<>();
        for (int i = 0; i < sections.size(); i++) {
            String current = "【" + sections.get(i) + "】";
            String next = i + 1 < sections.size() ? "【" + sections.get(i + 1) + "】" : "";
            int start = analysis.indexOf(current);
            if (start < 0) continue;
            start += current.length();
            int end = StringUtils.hasText(next) ? analysis.indexOf(next, start) : analysis.length();
            if (end < 0) end = analysis.length();
            parts.put(sections.get(i), analysis.substring(start, end).trim());
        }
        return parts;
    }

    private List<Map<String, Object>> resolveSubtopicPapers(Object value, List<Map<String, Object>> allPapers, int index, String subtopicName, String analysis) {
        List<Map<String, Object>> linked = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> paper = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) paper.put(text(entry.getKey()), entry.getValue());
                    linked.add(enrichPaper(paper, allPapers));
                } else {
                    Map<String, Object> matched = findPaperByTitle(allPapers, text(item));
                    if (!matched.isEmpty()) linked.add(publicPaper(matched));
                }
            }
        } else if (value instanceof String raw) {
            for (String title : raw.split("[,，]")) {
                Map<String, Object> matched = findPaperByTitle(allPapers, title);
                if (!matched.isEmpty()) linked.add(publicPaper(matched));
            }
        }
        if (!allPapers.isEmpty()) {
            for (Map<String, Object> paper : rankPapersForSubtopic(subtopicName, analysis, allPapers, index)) {
                if (linked.stream().noneMatch(existing -> samePaper(existing, paper))) linked.add(publicPaper(paper));
                if (linked.size() >= 5) break;
            }
        }
        linked.sort((a, b) -> Double.compare(
            paperMatchScore(String.join(" ", text(subtopicName), text(analysis)).toLowerCase(Locale.ROOT), b, allPapers.indexOf(findPaperByTitle(allPapers, text(b.get("title")))), index),
            paperMatchScore(String.join(" ", text(subtopicName), text(analysis)).toLowerCase(Locale.ROOT), a, allPapers.indexOf(findPaperByTitle(allPapers, text(a.get("title")))), index)
        ));
        String queryText = String.join(" ", text(subtopicName), text(analysis)).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> filtered = linked.stream()
            .filter(paper -> StringUtils.hasText(text(paper.get("title"))))
            .filter(paper -> !isClearlyIrrelevantPaper(queryText, paper))
            .limit(3)
            .toList();
        if (filtered.size() >= 2 || linked.size() <= 2) return filtered;
        return linked.stream()
            .filter(paper -> StringUtils.hasText(text(paper.get("title"))))
            .limit(3)
            .toList();
    }

    private boolean isClearlyIrrelevantPaper(String query, Map<String, Object> paper) {
        String haystack = String.join(" ", text(paper.get("title")), text(paper.get("abstract")), text(paper.get("reason"))).toLowerCase(Locale.ROOT);
        return containsAny(query, "接受", "采纳", "满意", "写作", "教学", "课堂", "问卷", "acceptance", "adoption", "writing", "education")
            && containsAny(haystack, "jailbreak", "jailbreaking", "attack", "adversarial", "security exploit");
    }

    private List<Map<String, Object>> rankPapersForSubtopic(String subtopicName, String analysis, List<Map<String, Object>> papers, int index) {
        if (papers.isEmpty()) return List.of();
        String query = String.join(" ", text(subtopicName), text(analysis)).toLowerCase(Locale.ROOT);
        List<Map<String, Object>> ranked = new ArrayList<>(papers);
        ranked.sort((a, b) -> Double.compare(
            paperMatchScore(query, b, papers.indexOf(b), index),
            paperMatchScore(query, a, papers.indexOf(a), index)
        ));
        return ranked.stream().limit(3).toList();
    }

    private double paperMatchScore(String query, Map<String, Object> paper, int paperIndex, int subtopicIndex) {
        String haystack = String.join(" ",
            text(paper.get("title")),
            text(paper.get("abstract")),
            text(paper.get("reason")),
            text(paper.get("source")),
            text(paper.get("subjects"))
        ).toLowerCase(Locale.ROOT);
        double score = 0;
        for (String token : keywordTokens(query)) {
            if (haystack.contains(token)) score += token.length() > 6 ? 2.2 : 1.0;
        }
        for (String token : keywordTokens(haystack)) {
            if (query.contains(token)) score += token.length() > 6 ? 1.4 : 0.35;
        }
        score += semanticPaperBonus(query, haystack);
        score += Math.max(0, 0.5 - Math.abs(paperIndex - subtopicIndex) * 0.08);
        return score;
    }

    private double semanticPaperBonus(String query, String haystack) {
        double score = 0;
        if (containsAny(query, "接受", "采纳", "满意", "行为", "用户", "问卷", "acceptance", "adoption", "attitude", "utaut", "tam")
            && containsAny(haystack, "acceptance", "adoption", "attitude", "utaut", "tam", "student", "user", "platform")) score += 8;
        if (containsAny(query, "接受", "采纳", "满意", "行为", "用户", "问卷", "acceptance", "adoption", "attitude", "utaut", "tam")
            && containsAny(haystack, "jailbreak", "attack", "security", "adversarial", "safety")) score -= 6;
        if (containsAny(query, "写作", "英语", "作文", "反馈", "writing", "efl", "essay")
            && containsAny(haystack, "writing", "efl", "essay", "english", "feedback", "student")) score += 8;
        if (containsAny(query, "检索", "幻觉", "提示", "rag", "retrieval", "hallucination", "prompt")
            && containsAny(haystack, "retrieval", "rag", "hallucination", "prompt", "generative", "large language")) score += 7;
        if (containsAny(query, "分割", "影像", "标注", "segmentation", "medical image")
            && containsAny(haystack, "segmentation", "medical", "image", "annotation", "radiology")) score += 8;
        if (containsAny(query, "药", "分子", "性质", "molecule", "drug", "property")
            && containsAny(haystack, "molecule", "molecular", "drug", "property", "chem", "compound")) score += 8;
        if (containsAny(query, "小样本", "少样本", "低样本", "few-shot", "small sample")
            && containsAny(haystack, "few-shot", "small", "sample", "low-resource", "limited")) score += 6;
        if (containsAny(query, "时序", "预测", "time series", "forecast")
            && containsAny(haystack, "time series", "forecast", "temporal", "sequence", "lstm")) score += 6;
        return score;
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private List<String> keywordTokens(String value) {
        return Arrays.stream(text(value).toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+"))
            .map(String::trim)
            .filter(token -> token.length() >= 4)
            .filter(token -> !Set.of("with", "from", "that", "this", "using", "based", "study", "research", "analysis", "model", "models").contains(token))
            .distinct()
            .limit(80)
            .toList();
    }

    private boolean samePaper(Map<String, Object> a, Map<String, Object> b) {
        return normalizeTitle(text(a.get("title"))).equals(normalizeTitle(text(b.get("title"))));
    }

    private Map<String, Object> enrichPaper(Map<String, Object> paper, List<Map<String, Object>> allPapers) {
        Map<String, Object> matched = findPaperByTitle(allPapers, text(paper.get("title")));
        return paperWithFallback(paper, matched.isEmpty() ? Map.of() : publicPaper(matched));
    }

    private Map<String, Object> findPaperByTitle(List<Map<String, Object>> papers, String title) {
        String target = normalizeTitle(title);
        if (!StringUtils.hasText(target)) return Map.of();
        for (Map<String, Object> paper : papers) {
            String candidate = normalizeTitle(text(paper.get("title")));
            if (candidate.equals(target) || candidate.contains(target) || target.contains(candidate)) return paper;
        }
        return Map.of();
    }

    private Map<String, Object> publicPaper(Map<String, Object> paper) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("title", text(paper.get("title")));
        out.put("source", firstNonBlank(text(paper.get("source")), text(paper.get("venue")), "来源待补"));
        out.put("year", firstNonBlank(text(paper.get("year")), text(paper.get("publishYear")), "年份待补"));
        out.put("url", firstNonBlank(text(paper.get("url")), text(paper.get("sourceUrl")), text(paper.get("doi"))));
        out.put("verifiedBy", firstNonBlank(text(paper.get("verifiedBy")), text(paper.get("provider")), "academic-search"));
        out.put("reason", text(paper.get("reason")));
        return out;
    }

    private String concreteSubtopicAnalysis(TopicResearchEntity topic, String name, List<Map<String, Object>> linked) {
        String field = text(topic.getDiscipline());
        String title = text(topic.getTitle());
        String source = linked.isEmpty() ? "候选文献不足，需要继续检索英文关键词、公开数据源和近三年综述。" : paperCitation(linked.get(0));
        String second = linked.size() > 1 ? paperCitation(linked.get(1)) : source;
        String dataset = dataHint(name, title, field);
        String method = methodHint(name, title, field);
        String metric = metricHint(name, title, field);
        Map<String, String> profile = subtopicProfile(name);
        return "【摘要】" + profile.get("summary").replace("{name}", name) + "核心判断是该切口能否在当前资源下做出可复现、可汇报、可继续投稿的结果。\n"
            + "【具体方法】以" + dataset + "为数据入口，" + profile.get("method").replace("{method}", method) + "；每一步记录数据规模、预处理、基线方法和失败样例，避免只给概念路线。\n"
            + "【发文现状】围绕" + name + "的近年论文多集中在" + method + "、公开基准复现和真实场景迁移三类问题；如果代表论文来自不同来源，应先按年份、数据集和指标整理成矩阵，再判断这个方向是热点延伸、方法补洞还是应用落地。\n"
            + "【优势】" + profile.get("advantage") + "最终能形成问题边界、数据表、方法对照和" + metric + "等 2-3 个可解释指标。\n"
            + "【局限】" + profile.get("risk") + "若样本量不足或指标选择太泛，容易变成普通综述，需要提前设定排除标准和最小可行实验。\n"
            + "【潜在论文】可写成“基于" + dataset + "的" + name + "研究”：摘要交代任务缺口，方法部分比较" + method + "，实验部分报告" + metric + "，讨论部分说明适用边界和下一步可扩展方向。\n"
            + "【代表论文】主要参考：" + source + (linked.size() > 1 ? "；可补充对照：" + second + "。" : "。");
    }

    private List<String> concreteSubtopicNames(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        List<String> base = split(topic.getThemeClusters());
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Math.max(5, base.size()); i++) {
            String raw = i < base.size() ? base.get(i) : "";
            List<Map<String, Object>> linked = papers.stream().skip(Math.min(i, Math.max(0, papers.size() - 1))).limit(2).toList();
            String name = concreteSubtopicName(topic, raw, linked, i);
            if (StringUtils.hasText(name) && !names.contains(name)) names.add(name);
            if (names.size() >= 5) break;
        }
        return names;
    }

    private String concreteSubtopicName(TopicResearchEntity topic, String rawName, List<Map<String, Object>> linked, int index) {
        String raw = text(rawName);
        if (StringUtils.hasText(raw) && !isGenericSubtopicName(raw)) return raw;
        String linkedTitle = linked.stream()
            .map(paper -> text(paper.get("title")))
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse("");
        String title = String.join(" ", text(topic.getTitle()), linkedTitle).trim();
        String discipline = text(topic.getDiscipline());
        List<String> lanes = subtopicLanes(title, discipline);
        String lane = lanes.get(Math.floorMod(index, lanes.size()));
        String object = topicObject(title, discipline);
        return compactSubtopicName(object + lane);
    }

    private boolean isGenericSubtopicName(String value) {
        String name = text(value);
        return name.length() <= 4
            || name.length() > 18
            || name.contains(":")
            || name.contains("：")
            || name.contains("代表论文")
            || name.contains("研究空白")
            || name.contains("可做方向")
            || name.contains("研究问题")
            || name.contains("数据与样本")
            || name.contains("公开数据验证")
            || name.contains("小样本复现")
            || name.contains("指标对照实验")
            || name.contains("应用现状")
            || name.contains("优化策略")
            || name.contains("面临的挑战")
            || name.contains("方法路线")
            || name.contains("评价指标")
            || name.contains("应用边界")
            || name.contains("小方向")
            || name.contains("现状分析")
            || name.contains("趋势预测")
            || name.contains("技术趋势")
            || name.contains("需求预测")
            || name.contains("精准医疗需求")
            || name.contains("生物工程应用")
            || name.endsWith("分析")
            || name.endsWith("应用")
            || name.endsWith("需求")
            || name.endsWith("趋势")
            || name.contains("驱动力")
            || name.contains("需求研究")
            || name.contains("应用场景")
            || name.contains("技术演进");
    }

    private List<String> subtopicLanes(String title, String discipline) {
        String value = (text(title) + " " + text(discipline)).toLowerCase(Locale.ROOT);
        if (value.contains("large language") || value.contains("llm") || value.contains("chatgpt") || value.contains("大语言") || value.contains("大模型")) {
            if (value.contains("writing") || value.contains("写作") || value.contains("efl") || value.contains("英语")) {
                return List.of("写作接受度变量", "写作质量前后测", "教师反馈采纳", "提示依赖风险", "学习行为留存");
            }
            return List.of("任务接受度变量", "幻觉边界评测", "检索增强对照", "提示成本压缩", "人机协作流程");
        }
        if (value.contains("药") || value.contains("molecule") || value.contains("drug") || value.contains("chem")) {
            return List.of("分子表征迁移", "公开基准复现", "性质预测对照", "低样本泛化", "可解释筛选");
        }
        if (value.contains("影像") || value.contains("medical") || value.contains("image") || value.contains("临床")) {
            return List.of("公开影像分割", "小样本标注", "跨设备泛化", "弱监督对照", "临床指标解释");
        }
        if (value.contains("教育") || value.contains("learning analytics")) {
            return List.of("学习过程诊断", "课堂行为建模", "个性化反馈", "学习风险预警", "评价指标对照");
        }
        if (value.contains("材料") || value.contains("material")) {
            return List.of("材料性能预测", "低碳筛选", "公开数据复现", "结构特征解释", "工艺参数对照");
        }
        if (value.contains("mamba") || value.contains("yolo") || value.contains("目标检测")) {
            return List.of("实时检测改造", "小目标场景", "轻量化部署", "复杂背景鲁棒性", "速度精度对照");
        }
        if (value.contains("时序") || value.contains("time") || value.contains("lstm") || value.contains("liquid")) {
            return List.of("短序列预测", "长依赖对照", "动态状态解释", "异常波动识别", "实时更新评测");
        }
        return List.of("公开数据验证", "小样本复现", "指标对照实验", "场景迁移评估", "风险边界界定");
    }

    private String topicObject(String title, String discipline) {
        String raw = text(title);
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("molecule") || lower.contains("drug") || lower.contains("chem") || raw.contains("药")) return "药物发现";
        if (lower.contains("large language") || lower.contains("chatgpt") || lower.contains("llm") || raw.contains("大语言") || raw.contains("大模型")) return "大语言模型";
        if (raw.contains("医学影像") || lower.contains("medical image") || lower.contains("segmentation")) return "医学影像";
        if (lower.contains("mamba") || lower.contains("yolo") || raw.contains("目标检测")) return "目标检测";
        if (raw.contains("液态神经") || lower.contains("liquid neural")) return "液态神经网络";
        if (raw.contains("小样本") || lower.contains("few-shot")) return "小样本学习";
        if (raw.contains("教育") || lower.contains("learning analytics")) return "教育数据";
        String value = raw
            .replaceAll("(?i)^(a|an|the)\\s+", "")
            .replaceAll("[:：].*$", "")
            .replaceAll("[《》]", "")
            .trim();
        if (value.length() > 8) value = firstNonBlank(text(discipline), "当前方向");
        if (StringUtils.hasText(value)) return value;
        return StringUtils.hasText(discipline) ? discipline + "问题" : "当前方向";
    }

    private String compactSubtopicName(String value) {
        String clean = text(value).replaceAll("\\s+", "");
        if (clean.length() <= 14) return clean;
        return clean.substring(0, 14);
    }

    private String paperCitation(Map<String, Object> paper) {
        String title = shorten(text(paper.get("title")), 58);
        String source = firstNonBlank(text(paper.get("source")), text(paper.get("verifiedBy")), "来源待补");
        String year = firstNonBlank(text(paper.get("year")), "年份待补");
        return "《" + title + "》（" + source + "，" + year + "）";
    }

    private boolean isGenericAnalysis(String value) {
        if (!StringUtils.hasText(value)) return true;
        return thinStructuredAnalysis(value)
            || !value.contains("【摘要】")
            || value.contains("候选文献不足")
            || value.contains("可把大方向收窄到可检索")
            || value.contains("围绕该小类比较代表论文")
            || value.contains("继续从真实候选论文中抽取")
            || value.contains("先读")
            || value.length() < 24;
    }

    private boolean thinStructuredAnalysis(String value) {
        if (!StringUtils.hasText(value) || !value.contains("【摘要】")) return false;
        String[] required = {"摘要", "具体方法", "发文现状", "优势", "局限", "潜在论文", "代表论文"};
        int shortBlocks = 0;
        for (String label : required) {
            Pattern pattern = Pattern.compile("【" + Pattern.quote(label) + "】([\\s\\S]*?)(?=【[^】]+】|$)");
            Matcher matcher = pattern.matcher(value);
            if (!matcher.find()) return true;
            String body = matcher.group(1).replaceAll("\\s+", "");
            if (body.length() < 34) shortBlocks++;
        }
        return shortBlocks >= 2;
    }

    private String inferDiscipline(String text) {
        String value = text(text);
        if (value.contains("医学") || value.contains("药物") || value.contains("公共卫生")) return "医学";
        if (value.contains("教育")) return "教育";
        if (value.contains("材料")) return "材料";
        return "计算机";
    }

    private List<String> researchLanes(String direction, String discipline, String methodPreference, List<String> constraints) {
        String value = String.join(" ", text(direction), text(discipline), text(methodPreference), String.join(" ", constraints)).toLowerCase(Locale.ROOT);
        if (value.contains("llm") || value.contains("大模型") || value.contains("语言模型")) {
            return List.of("技术接受度", "幻觉评测", "提示压缩", "课堂反馈", "检索增强", "低成本部署");
        }
        if (value.contains("医学影像") || value.contains("segmentation") || value.contains("medical image")) {
            return List.of("小样本分割", "跨设备泛化", "弱监督标注", "基础模型迁移", "临床指标解释", "不确定性评估");
        }
        if (value.contains("药") || value.contains("drug") || value.contains("molecule")) {
            return List.of("分子表征", "性质预测", "药靶互作", "色谱质谱建模", "小样本筛选", "可解释预测");
        }
        if (value.contains("教育")) {
            return List.of("学习过程诊断", "个性化反馈", "课堂行为建模", "作业质量评估", "学习风险预警", "教师采纳机制");
        }
        if (value.contains("因果") || value.contains("统计")) {
            return List.of("变量识别", "反事实估计", "异质性分析", "稳健性检验", "指标体系", "政策场景验证");
        }
        return List.of("数据可得性", "方法复现", "场景迁移", "评价指标", "轻量部署", "争议问题");
    }

    private List<String> concreteTopicClusters(String direction, String discipline, String lane) {
        String object = topicObject(direction, discipline);
        List<String> lanes = subtopicLanes(direction + " " + lane, discipline);
        List<String> clusters = new ArrayList<>();
        clusters.add(lane);
        for (String item : lanes) {
            clusters.add(compactSubtopicName(object + item));
            if (clusters.size() >= 5) break;
        }
        return clusters;
    }

    private List<String> compactTags(List<String> values) {
        return values.stream()
            .map(this::text)
            .filter(StringUtils::hasText)
            .map(value -> value.length() > 12 ? value.substring(0, 12) : value)
            .distinct()
            .limit(6)
            .toList();
    }

    private String dataHint(String name, String title, String discipline) {
        String value = String.join(" ", name, title, discipline).toLowerCase(Locale.ROOT);
        if (value.contains("large language") || value.contains("chatgpt") || value.contains("大语言") || value.contains("大模型") || value.contains("写作")) return "学生写作文本、平台使用日志、接受度问卷和教师反馈记录";
        if (value.contains("医学影像") || value.contains("分割") || value.contains("medical")) return "公开医学影像数据集、标注掩膜和跨中心验证集";
        if (value.contains("药") || value.contains("molecule") || value.contains("drug")) return "MoleculeNet、ChEMBL、PubChem 或色谱/质谱公开记录";
        if (value.contains("教育") || value.contains("学生") || value.contains("课堂")) return "问卷量表、学习平台日志、作业文本和课堂行为记录";
        if (value.contains("时序") || value.contains("预测")) return "公开时序基准、传感器记录或业务事件序列";
        if (value.contains("材料")) return "Materials Project、OQMD 或材料性能公开表格";
        return "代表论文中的公开数据、补充材料、开源代码和可复现实验设置";
    }

    private String methodHint(String name, String title, String discipline) {
        String value = String.join(" ", name, title, discipline).toLowerCase(Locale.ROOT);
        if (value.contains("llm") || value.contains("large language") || value.contains("chatgpt") || value.contains("大语言") || value.contains("大模型") || value.contains("提示")) return "TAM/UTAUT 接受度模型、写作质量评分、提示策略对照和人工/自动评价一致性分析";
        if (value.contains("分割") || value.contains("影像")) return "U-Net/nnU-Net、SAM/MedSAM、参数高效微调和测试时自适应";
        if (value.contains("药") || value.contains("molecule")) return "图神经网络、分子指纹、Transformer 表征和传统机器学习基线";
        if (value.contains("因果")) return "倾向得分、双重差分、工具变量或稳健性检验";
        if (value.contains("时序")) return "LSTM、Transformer、状态空间模型和滚动窗口验证";
        return "基线复现、消融实验、统计检验和误差案例分析";
    }

    private String metricHint(String name, String title, String discipline) {
        String value = String.join(" ", name, title, discipline).toLowerCase(Locale.ROOT);
        if (value.contains("分割") || value.contains("影像")) return "Dice、IoU、HD95、跨域性能下降";
        if (value.contains("药") || value.contains("molecule")) return "AUC、RMSE、MAE、富集因子和可解释特征贡献";
        if (value.contains("教育") || value.contains("接受") || value.contains("写作") || value.contains("large language") || value.contains("大语言")) return "接受度量表、写作质量得分、学习增益、满意度和行为留存";
        if (value.contains("时序")) return "MAE、RMSE、MAPE、推断延迟和异常召回率";
        return "准确率、召回率、稳健性、成本和可解释性评分";
    }

    private Map<String, String> subtopicProfile(String name) {
        String value = text(name);
        if (value.contains("接受") || value.contains("采纳") || value.contains("反馈")) {
            return Map.of(
                "summary", "围绕“{name}”不再只问技术是否先进，而是研究用户为什么愿意用、何时不用、哪些风险会降低使用意愿。",
                "method", "先构建接受度变量和使用情境，再结合{method}做问卷、访谈或日志对照",
                "advantage", "该切口容易和真实教学、医疗或组织场景连接，结果不是单纯模型分数，而是能解释使用行为。",
                "risk", "量表设计和样本来源会显著影响结论，若只做便利样本，外推性会比较弱。"
            );
        }
        if (value.contains("小样本") || value.contains("低样本") || value.contains("少样本")) {
            return Map.of(
                "summary", "围绕“{name}”关注数据少、标注贵、类别不均衡时模型还能不能稳定工作，适合资源有限但想做实验的选题。",
                "method", "固定少样本划分和公开基准，再结合{method}比较不同样本量下的性能曲线",
                "advantage", "该切口实验规模可控，容易做出递进式结果：全量、半量、少量和跨域四组对比。",
                "risk", "少样本实验容易受随机种子和划分方式影响，必须做多次重复和置信区间。"
            );
        }
        if (value.contains("指标") || value.contains("对照") || value.contains("评测")) {
            return Map.of(
                "summary", "围绕“{name}”把研究重点放在评价体系，而不是继续换模型；适合已有方法很多但结论不好比较的领域。",
                "method", "先整理代表论文的指标和基线，再用{method}复现一组统一评价协议",
                "advantage", "该切口贡献清楚，能产出统一指标表、错误案例库和更公平的基线比较。",
                "risk", "如果只堆指标不解释指标含义，论文会显得像实验报告，需要加入任务边界和指标适用性讨论。"
            );
        }
        if (value.contains("迁移") || value.contains("泛化") || value.contains("跨")) {
            return Map.of(
                "summary", "围绕“{name}”研究模型从一个数据域迁移到另一个数据域时哪里失效，重点不是单点精度，而是跨场景稳定性。",
                "method", "构建源域/目标域数据组合，再结合{method}比较直接迁移、微调和自适应策略",
                "advantage", "该切口比普通复现更有问题意识，能解释不同数据分布、设备或人群导致的性能变化。",
                "risk", "跨域数据如果来源不清，结论很容易被数据偏差解释掉，需要保留数据描述和分布诊断。"
            );
        }
        if (value.contains("风险") || value.contains("边界") || value.contains("不确定")) {
            return Map.of(
                "summary", "围绕“{name}”专门研究方法什么时候不可靠，把失败条件、伦理限制和适用边界写成论文贡献。",
                "method", "先列出错误类型和边界场景，再结合{method}做压力测试、消融和人工核验",
                "advantage", "该切口有现实价值，尤其适合医疗、教育和管理场景，因为用户更关心系统何时不能信。",
                "risk", "风险分析需要足够案例支撑，若只有主观判断，容易显得空，需要把失败案例结构化。"
            );
        }
        if (value.contains("部署") || value.contains("轻量") || value.contains("成本")) {
            return Map.of(
                "summary", "围绕“{name}”关注模型能不能在低成本环境下运行，适合把算法性能和真实使用成本放在同一张表里讨论。",
                "method", "设置参数量、推理延迟和资源消耗预算，再结合{method}比较轻量化方案",
                "advantage", "该切口很适合做工程型论文，能把准确率、速度、显存和部署复杂度统一起来。",
                "risk", "如果只压缩模型而不解释精度损失来源，贡献会偏工程调参，需要增加误差分析。"
            );
        }
        return Map.of(
            "summary", "围绕“{name}”把大方向收窄为一个可以验证的小问题：限定研究对象、数据来源和评价指标，不再停留在“先了解某篇论文”的层面。",
            "method", "先建立代表论文矩阵，再用{method}做小规模复现或对比",
            "advantage", "该方向边界清楚、交付物明确；如果实验资源有限，也能先用公开数据完成初版验证。",
            "risk", "代表论文可能集中在相邻场景，数据分布和真实任务并不完全一致。"
        );
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split("[,，]"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }

    private List<String> stringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::text).filter(StringUtils::hasText).distinct().toList();
        }
        if (value instanceof String raw) return split(raw);
        return List.of();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(text(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String defaultText(Map<String, Object> body, String key, String fallback) {
        String value = text(body.get(key));
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return value;
        }
        return "";
    }

    private String normalizeTitle(String title) {
        return text(title).toLowerCase(Locale.ROOT).replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
    }

    private String shorten(String value, int max) {
        String clean = text(value).replaceAll("\\s+", " ");
        if (clean.length() <= max) return clean;
        return clean.substring(0, max).trim() + "…";
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
