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
    private final ObjectMapper objectMapper;

    public TopicResearchService(
        TopicResearchRepository topicResearchRepository,
        PaperRepository paperRepository,
        CurrentUserService currentUserService,
        AiChatService aiChatService,
        ExternalSearchService externalSearchService,
        ObjectMapper objectMapper
    ) {
        this.topicResearchRepository = topicResearchRepository;
        this.paperRepository = paperRepository;
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
        this.externalSearchService = externalSearchService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<Map<String, Object>> list(String keyword, String discipline, String stage, String goal, String sort, boolean savedOnly) {
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        seedIfEmpty(user.getId());
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
        AppUserEntity user = currentUserService.getOrCreateDefaultUser();
        String direction = text(body.get("direction"));
        if (!StringUtils.hasText(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写研究方向大类");
        }
        String discipline = defaultText(body, "discipline", direction);
        String stage = defaultText(body, "stage", "硕士");
        String goal = defaultText(body, "goal", "开题");
        String resource = defaultText(body, "resource", "无实验，仅公开数据");
        String note = text(body.get("note"));
        List<Map<String, Object>> evidencePapers = searchAcademicEvidence(direction, discipline, goal);

        List<TopicResearchEntity> entities;
        try {
            AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallbackUnmetered(
                "你是 deep-research 选题调研 agent。只输出 JSON，不要 Markdown。你必须基于 academic_search_results 中的真实检索候选做选题，不允许编造论文题名、DOI、作者或年份。任务：用户给的是研究方向大类，你需要生成 4-6 张可供选择的选题卡。JSON 字段：topics(array)。每个 topic 包含 title, summary, discipline, stage, goal, tags(array), themeClusters(array), researchQuestion, researchGap, methodRoute, riskNote, feasibilityScore(number), innovationScore(number), difficultyScore(number), subtopics(array of {name, analysis, papers(array of paper title from academic_search_results)}), representativePapers(array of {title, source, year, reason})。每张卡必须推荐 3-5 个小类，每个小类要具体分析并关联真实候选论文。代表论文只能从 academic_search_results 里选择；如果候选不足，就明确写“候选文献不足，需要继续检索”。",
                objectMapper.writeValueAsString(Map.of(
                    "researchCategory", direction,
                    "discipline", discipline,
                    "stage", stage,
                    "goal", goal,
                    "resource", resource,
                    "note", note,
                    "academic_search_results", evidencePapers
                )),
                2600,
                TOPIC_FALLBACK_MODELS
            );
            entities = fromAiJsonMany(result.content(), user.getId(), result.modelName(), discipline, stage, goal, evidencePapers);
            for (TopicResearchEntity entity : entities) {
                if (!evidencePapers.isEmpty()) {
                    entity.setRepresentativePapersJson(objectMapper.writeValueAsString(mergeEvidencePapers(parsePapers(entity.getRepresentativePapersJson()), evidencePapers)));
                    entity.setSource("deep-research + academic-search");
                }
            }
        } catch (Exception error) {
            entities = deterministicTopicSet(user.getId(), direction, discipline, stage, goal, resource, note, "deterministic-fallback", evidencePapers);
            for (TopicResearchEntity entity : entities) {
                if (!evidencePapers.isEmpty()) {
                    entity.setRepresentativePapersJson(writePapers(evidencePapers));
                    entity.setSource("academic-search");
                }
            }
        }
        String userId = String.valueOf(user.getId());
        List<TopicResearchEntity> saved = new ArrayList<>();
        for (TopicResearchEntity entity : entities.stream().limit(6).toList()) {
            entity.setSavedByUserIds(userId);
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
            entity.setSubtopicsJson(writeSubtopics(normalizeSubtopics(entity, evidencePapers)));
            entities.add(entity);
        }
        if (entities.size() < 6) {
            List<TopicResearchEntity> fallback = deterministicTopicSet(userId, fallbackDiscipline, fallbackDiscipline, fallbackStage, fallbackGoal, "无实验，仅公开数据", "", modelName, evidencePapers);
            for (TopicResearchEntity item : fallback) {
                if (entities.size() >= 6) break;
                boolean duplicate = entities.stream().anyMatch(existing -> normalizeTitle(existing.getTitle()).equals(normalizeTitle(item.getTitle())));
                if (!duplicate) entities.add(item);
            }
        }
        return entities.isEmpty() ? deterministicTopicSet(userId, fallbackDiscipline, fallbackDiscipline, fallbackStage, fallbackGoal, "无实验，仅公开数据", "", modelName, evidencePapers) : entities;
    }

    private TopicResearchEntity fromAiTopicNode(JsonNode root, Long userId, String modelName, String fallbackDiscipline, String fallbackStage, String fallbackGoal) throws Exception {
        TopicResearchEntity entity = new TopicResearchEntity();
        entity.setUserId(userId);
        entity.setTitle(defaultNode(root, "title", "待完善选题"));
        entity.setSummary(defaultNode(root, "summary", "AI 已生成选题调研卡，请继续补充方向约束。"));
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
        return entity;
    }

    private List<TopicResearchEntity> deterministicTopicSet(Long userId, String direction, String discipline, String stage, String goal, String resource, String note, String modelName, List<Map<String, Object>> evidencePapers) {
        List<String> lanes = List.of("数据可得性", "方法复现", "场景迁移", "评价指标", "轻量部署", "争议问题");
        List<TopicResearchEntity> topics = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String lane = lanes.get(i % lanes.size());
            TopicResearchEntity entity = deterministicTopic(userId, direction + "：" + lane + "切入", discipline, stage, goal, resource, note, modelName);
            entity.setTitle("面向" + direction + "的" + lane + "选题拆解");
            entity.setSummary("围绕“" + direction + "”的大方向，从“" + lane + "”收窄成可执行选题，优先保证真实文献、数据条件和方法路线能继续推进。");
            entity.setThemeClusters(String.join(",", List.of(lane, "代表论文", "研究空白", "可做方向")));
            entity.setSubtopicsJson(writeSubtopics(buildSubtopics(entity, evidencePapers)));
            topics.add(entity);
        }
        return topics;
    }

    private TopicResearchEntity deterministicTopic(Long userId, String direction, String discipline, String stage, String goal, String resource, String note, String modelName) {
        TopicResearchEntity entity = new TopicResearchEntity();
        entity.setUserId(userId);
        entity.setTitle(direction + "的可行选题与研究路线");
        entity.setSummary("围绕“" + direction + "”拆出一个可执行选题，适合" + stage + goal + "；当前资源条件为：" + resource + "。");
        entity.setDiscipline(discipline);
        entity.setStage(stage);
        entity.setGoal(goal);
        entity.setSource("AI生成");
        entity.setTags(String.join(",", List.of(goal, discipline, "deep-research", resource.replace("，", " ").split(" ")[0])));
        entity.setThemeClusters(String.join(",", List.of(direction, "代表论文矩阵", "研究空白", "可复现实验")));
        entity.setResearchQuestion("在" + direction + "中，哪些关键变量、数据条件或应用场景尚未被充分解释？");
        entity.setResearchGap("已有研究往往偏重方法效果展示，缺少对数据边界、可复现对照和真实场景迁移的连续分析。");
        entity.setMethodRoute("先做系统检索和主题聚类，再建立代表论文矩阵；随后选择公开数据、问卷访谈或案例材料完成小规模验证，最后把结论转为开题问题链。");
        entity.setRiskNote(StringUtils.hasText(note) ? "注意和补充说明保持一致：" + note : "需要尽快确认数据来源、评价指标、伦理边界和导师认可的研究范围。");
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
        List<String> frontiers = List.of(
            "多模态医学影像基础模型",
            "小样本学习与公开数据复现",
            "液态神经网络动态时序建模",
            "具身智能评测与仿真数据",
            "AI 药物发现中的分子表征",
            "教育大模型的学习过程诊断",
            "低碳材料筛选与机器学习",
            "公共卫生风险预测与可解释模型"
        );
        for (String frontier : frontiers) {
            List<Map<String, Object>> evidence = searchAcademicEvidence(frontier, inferDiscipline(frontier), "前沿追踪");
            TopicResearchEntity entity = deterministicTopic(user.getId(), frontier, inferDiscipline(frontier), "硕士", "开题", "无实验，仅公开数据", "每日前沿自动更新", "daily-frontier");
            entity.setSource("daily-frontier + academic-search");
            if (!evidence.isEmpty()) entity.setRepresentativePapersJson(writePapers(evidence));
            entity.setSubtopicsJson(writeSubtopics(buildSubtopics(entity, evidence.isEmpty() ? parsePapers(entity.getRepresentativePapersJson()) : evidence)));
            topicResearchRepository.save(entity);
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
        map.put("subtopics", normalizeSubtopics(topic, papers));
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
            List<Map<String, Object>> linked = papers.stream().skip(Math.min(i, Math.max(0, papers.size() - 1))).limit(2).toList();
            if (linked.isEmpty() && !papers.isEmpty()) linked = papers.stream().limit(2).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("analysis", concreteSubtopicAnalysis(topic, name, linked));
            item.put("papers", linked.stream().map(this::publicPaper).toList());
            subtopics.add(item);
        }
        return subtopics;
    }

    private List<Map<String, Object>> normalizeSubtopics(TopicResearchEntity topic, List<Map<String, Object>> papers) {
        List<Map<String, Object>> normalized = new ArrayList<>();
        int index = 0;
        for (Map<String, Object> item : parseSubtopics(topic.getSubtopicsJson())) {
            if (!StringUtils.hasText(text(item.get("name")))) continue;
            Map<String, Object> copy = new LinkedHashMap<>(item);
            List<Map<String, Object>> linked = resolveSubtopicPapers(copy.get("papers"), papers, index);
            copy.put("name", concreteSubtopicName(topic, text(copy.get("name")), linked, index));
            copy.put("papers", linked);
            if (isGenericAnalysis(text(copy.get("analysis")))) copy.put("analysis", concreteSubtopicAnalysis(topic, text(copy.get("name")), linked));
            normalized.add(copy);
            index++;
        }
        Set<String> names = new LinkedHashSet<>();
        normalized.forEach(item -> names.add(text(item.get("name"))));
        for (Map<String, Object> item : buildSubtopics(topic, papers)) {
            if (normalized.size() >= 5) break;
            String name = text(item.get("name"));
            if (names.add(name)) normalized.add(item);
        }
        while (normalized.size() < 3) {
            int fallbackIndex = normalized.size() + 1;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", "可做小方向 " + fallbackIndex);
            List<Map<String, Object>> linked = papers.stream().limit(2).map(this::publicPaper).toList();
            item.put("analysis", concreteSubtopicAnalysis(topic, text(item.get("name")), linked));
            item.put("papers", linked);
            normalized.add(item);
        }
        return normalized.stream().limit(5).toList();
    }

    private List<Map<String, Object>> resolveSubtopicPapers(Object value, List<Map<String, Object>> allPapers, int index) {
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
        if (linked.isEmpty() && !allPapers.isEmpty()) {
            int start = Math.min(index, Math.max(0, allPapers.size() - 1));
            linked.addAll(allPapers.stream().skip(start).limit(2).map(this::publicPaper).toList());
        }
        return linked.stream()
            .filter(paper -> StringUtils.hasText(text(paper.get("title"))))
            .limit(3)
            .toList();
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
        if (linked.isEmpty()) {
            return "切口定义：把“" + name + "”限制在一个研究对象、一个数据来源和一组评价指标内。当前代表论文不足，先补充英文关键词与公开数据来源，再判断是否适合开题。";
        }
        String source = paperCitation(linked.get(0));
        return "切口定义：围绕“" + name + "”限定研究对象、数据条件和评价指标。可做任务：整理代表论文的实验场景、数据来源和指标设置，做一组小规模复现或对比。交付结果：问题边界、数据表、方法对照和 2-3 个可解释指标。参考来源：" + source + "。";
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
        String title = firstNonBlank(text(topic.getTitle()), linked.isEmpty() ? "" : text(linked.get(0).get("title")));
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
        return value.contains("可把大方向收窄到可检索")
            || value.contains("围绕该小类比较代表论文")
            || value.contains("继续从真实候选论文中抽取")
            || value.contains("先读")
            || value.length() < 24;
    }

    private String inferDiscipline(String text) {
        String value = text(text);
        if (value.contains("医学") || value.contains("药物") || value.contains("公共卫生")) return "医学";
        if (value.contains("教育")) return "教育";
        if (value.contains("材料")) return "材料";
        return "计算机";
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split("[,，]"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
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
