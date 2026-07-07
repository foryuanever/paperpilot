package com.paperpilot.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.entity.MeetingReportEntity;
import com.paperpilot.server.entity.ModelConfigEntity;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.repository.MeetingReportRepository;
import com.paperpilot.server.repository.ModelConfigRepository;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.vo.SearchPaperVO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MeetingReportService {
    private static final List<String> SECTION_KEYS = List.of(
        "synthesis", "basicInfo", "overview", "background", "method", "results", "conclusion", "datasets"
    );
    private static final List<String> MEETING_MODEL_FALLBACKS = List.of(
        "oc/deepseek-v4-flash-free",
        "oc/north-mini-code-free",
        "oc/mimo-v2.5-free"
    );
    private static final List<String> DECK_AGENT_STRONG_MODELS = List.of(
        "gpt-5.5",
        "openai/gpt-5.5",
        "gpt-5",
        "openai/gpt-5",
        "gpt-4.1",
        "openai/gpt-4.1",
        "o3",
        "openai/o3",
        "anthropic/claude-opus-4.1",
        "anthropic/claude-sonnet-4",
        "google/gemini-2.5-pro",
        "deepseek/deepseek-r1",
        "qwen/qwen3-235b-a22b-thinking-2507"
    );
    private static final int SECTION_AI_TIMEOUT_SECONDS = 130;
    private static final int PPTXGEN_TIMEOUT_SECONDS = 120;
    private static final long STALE_JOB_MILLIS = Duration.ofMinutes(3).toMillis();
    private static final Map<String, List<String>> SECTION_BLOCKS = Map.of(
        "synthesis", List.of("研究背景", "研究问题", "研究方法与数据", "实验与结论", "创新点与启示", "局限性"),
        "basicInfo", List.of("论文定位", "发表信息", "汇报价值"),
        "overview", List.of("核心要点", "研究问题", "主要贡献"),
        "background", List.of("核心要点", "关键问题", "本文思想", "关键贡献"),
        "method", List.of("整体框架", "关键模块", "实现流程"),
        "results", List.of("主要发现", "对比结果", "实验结论"),
        "conclusion", List.of("研究结论", "现有不足", "未来展望"),
        "datasets", List.of("数据来源", "数据设置", "评测指标")
    );
    private final PaperRepository paperRepository;
    private final MeetingReportRepository reportRepository;
    private final ModelConfigRepository modelConfigRepository;
    private final CurrentUserService currentUserService;
    private final AiChatService aiChatService;
    private final AiUsageService aiUsageService;
    private final NotificationService notificationService;
    private final ExternalSearchService externalSearchService;
    private final ObjectMapper objectMapper;
    private final Map<String, ReportJob> jobs = new ConcurrentHashMap<>();
    private final Map<String, DeckJob> deckJobs = new ConcurrentHashMap<>();
    private final ExecutorService reportExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService deckExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService sectionAiExecutor = Executors.newCachedThreadPool();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(12))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build();

    @Value("${paperpilot.ppt-master.skill-dir:/Users/yuan/.codex/skills/ppt-master}")
    private String pptMasterSkillDir;

    @Value("${paperpilot.ppt-master.python:}")
    private String pptMasterPython;

    @Value("${paperpilot.ppt-master.codex:/Applications/Codex.app/Contents/Resources/codex}")
    private String pptMasterCodex;

    @Value("${paperpilot.ppt-master.agent-timeout-minutes:120}")
    private int pptMasterAgentTimeoutMinutes;

    public MeetingReportService(
        PaperRepository paperRepository,
        MeetingReportRepository reportRepository,
        ModelConfigRepository modelConfigRepository,
        CurrentUserService currentUserService,
        AiChatService aiChatService,
        AiUsageService aiUsageService,
        NotificationService notificationService,
        ExternalSearchService externalSearchService,
        ObjectMapper objectMapper
    ) {
        this.paperRepository = paperRepository;
        this.reportRepository = reportRepository;
        this.modelConfigRepository = modelConfigRepository;
        this.currentUserService = currentUserService;
        this.aiChatService = aiChatService;
        this.aiUsageService = aiUsageService;
        this.notificationService = notificationService;
        this.externalSearchService = externalSearchService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> get(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        PaperEntity paper = requirePaper(workspaceId, userId);
        return reportRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .map(report -> response(paper, report))
            .orElseGet(() -> response(paper, null));
    }

    public Map<String, Object> generate(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        return generateForUser(workspaceId, userId);
    }

    public Map<String, Object> startGenerate(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        requirePaper(workspaceId, userId);
        String key = jobKey(userId, workspaceId);
        ReportJob existing = jobs.get(key);
        if (existing != null && "running".equals(existing.status())) {
            markStaleJob(existing);
            return jobResponse(existing);
        }
        ReportJob job = new ReportJob(workspaceId, userId);
        jobs.put(key, job);
        CompletableFuture.runAsync(() -> {
            try {
                job.message("正在读取 PDF 正文与论文元数据");
                generateForUser(workspaceId, userId, job);
                job.complete();
                notificationService.create(userId, null, "meeting_report", null,
                    "组会汇报生成完成", "论文《" + job.paperTitle() + "》的 AI 分析已保存。");
            } catch (Exception error) {
                job.fail(readableError(error));
                notificationService.create(userId, null, "meeting_report", null,
                    "组会汇报生成失败", "论文《" + job.paperTitle() + "》分析失败：" + readableError(error));
            }
        }, reportExecutor);
        return jobResponse(job);
    }

    public Map<String, Object> generateStatus(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        ReportJob job = jobs.get(jobKey(userId, workspaceId));
        if (job == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "idle");
            response.put("progress", 0);
            response.put("message", "暂无后台生成任务");
            response.put("done", false);
            return response;
        }
        markStaleJob(job);
        return jobResponse(job);
    }

    private Map<String, Object> generateForUser(String workspaceId, Long userId) {
        return generateForUser(workspaceId, userId, null);
    }

    private Map<String, Object> generateForUser(String workspaceId, Long userId, ReportJob job) {
        PaperEntity paper = requirePaper(workspaceId, userId);
        if (job != null) job.paperTitle(paper.getTitle());
        String modelName = "";
        long promptTokens = 0L;
        long completionTokens = 0L;
        long totalTokens = 0L;
        int aiSuccessCount = 0;
        List<String> failedSections = new ArrayList<>();
        MeetingReportEntity report = reportRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseGet(MeetingReportEntity::new);
        Map<String, String> sections = report.getContent() == null ? emptySections() : readSections(report.getContent());
        SECTION_KEYS.forEach(key -> {
            if (isGeneratedFallback(sections.get(key))) sections.put(key, "");
        });
        String paperText = extractPaperText(paper);
        boolean fullTextAvailable = paperText.length() > 1200;
        if (job != null) job.progress(10, "已读取论文，开始逐章分析");

        AtomicInteger index = new AtomicInteger(0);
        for (String key : SECTION_KEYS) {
            int current = index.getAndIncrement();
            if (job != null) job.progress(12 + current * 11, "正在生成：" + sectionName(key));
            try {
                AiChatService.ChatResult result = callSectionModel(
                    sectionSystemPrompt(key),
                    sectionPrompt(paper, key, paperText, fullTextAvailable)
                );
                sections.put(key, normalizeSection(key, result.content(), paper));
                modelName = result.modelName();
                promptTokens += result.promptTokens();
                completionTokens += result.completionTokens();
                totalTokens += result.totalTokens();
                aiSuccessCount++;
                if (job != null) job.progress(20 + current * 11, "已完成：" + sectionName(key));
            } catch (Exception error) {
                failedSections.add(sectionName(key) + "：" + readableError(error));
                if (isGeneratedFallback(sections.get(key))) sections.put(key, "");
            }
        }

        if (aiSuccessCount == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "本次免费模型全部调用失败，未写入不准确模板内容。建议稍后重试或切换到更稳定的模型。最后错误：" +
                    (failedSections.isEmpty() ? "模型无响应" : failedSections.get(failedSections.size() - 1))
            );
        }

        report.setUserId(userId);
        report.setWorkspaceId(workspaceId);
        report.setModelName(modelName);
        try {
            report.setContent(objectMapper.writeValueAsString(sections));
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 分析结果保存失败");
        }
        reportRepository.save(report);
        if (job != null && totalTokens > 0) {
            aiUsageService.recordAndCharge(
                userId,
                modelName,
                "report",
                "组会论文综述生成",
                paper.getTitle(),
                promptTokens,
                completionTokens,
                totalTokens
            );
        }
        if (job != null) job.progress(100, "组会汇报已保存");
        Map<String, Object> result = response(paper, report);
        Map<String, Object> usage = Map.of(
            "promptTokens", promptTokens,
            "completionTokens", completionTokens,
            "totalTokens", totalTokens,
            "estimated", true
        );
        result.put("usage", usage);
        result.put("aiGenerated", aiSuccessCount > 0);
        result.put("partial", !failedSections.isEmpty());
        result.put("failedSections", failedSections);
        result.put("fullTextAvailable", fullTextAvailable);
        return result;
    }

    private AiChatService.ChatResult callSectionModel(String systemPrompt, String userPrompt) throws Exception {
        CompletableFuture<AiChatService.ChatResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return aiChatService.chatJsonWithModelFallback(
                    systemPrompt,
                    userPrompt,
                    4200,
                    MEETING_MODEL_FALLBACKS
                );
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
        }, sectionAiExecutor);
        try {
            return future.get(SECTION_AI_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException error) {
            future.cancel(true);
            throw new TimeoutException("本章节模型调用超过 " + SECTION_AI_TIMEOUT_SECONDS + " 秒");
        } catch (java.util.concurrent.ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtime && runtime.getCause() instanceof Exception nested) {
                throw nested;
            }
            if (cause instanceof Exception exception) throw exception;
            throw new IllegalStateException(cause == null ? error.getMessage() : cause.getMessage());
        }
    }

    public Map<String, Object> save(String workspaceId, Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        PaperEntity paper = requirePaper(workspaceId, userId);
        Object sections = body.get("sections");
        if (!(sections instanceof Map<?, ?>)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分析内容格式错误");
        }
        MeetingReportEntity report = reportRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseGet(MeetingReportEntity::new);
        report.setUserId(userId);
        report.setWorkspaceId(workspaceId);
        report.setModelName((String) body.getOrDefault("modelName", "人工编辑"));
        try {
            report.setContent(objectMapper.writeValueAsString(sections));
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分析内容无法保存");
        }
        reportRepository.save(report);
        return response(paper, report);
    }

    public Map<String, Object> askSelection(String workspaceId, Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        PaperEntity paper = requirePaper(workspaceId, userId);
        String selection = Objects.toString(body.get("selection"), "").trim();
        String paragraph = Objects.toString(body.get("paragraph"), "").trim();
        String question = Objects.toString(body.get("question"), "").trim();
        if (question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写与当前论文有关的问题");
        }
        if (selection.length() > 4000 || paragraph.length() > 8000 || question.length() > 800) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "选中内容或问题过长，请缩小范围后重试");
        }
        String paperContext = extractPaperText(paper);
        if (paperContext.length() > 30000) paperContext = paperContext.substring(0, 30000);
        String systemPrompt = """
            你是 PaperSolver 学术阅读助手，当前主要服务于用户正在阅读的论文。
            允许回答三类内容：
            第一，正常礼貌问候，例如“你好”“你是谁”，应友好简短回应并提示可以询问学术问题。
            第二，与当前论文的内容、方法、数据、实验、结论、图表和术语有关的问题。
            第三，其他明确的学术研究问题，例如研究方法、论文写作、统计分析、学术概念和相关领域知识。
            对生活、娱乐、购物、情感、游戏、八卦等非学术问题，必须只回答：“抱歉，我只能协助论文阅读和学术相关问题。”
            不得编造当前论文中没有的信息。问题涉及当前论文时优先以提供的论文原文为依据。
            回答必须使用纯文本，不得使用星号、短横线项目符号、井号标题、Markdown 表格或其他 Markdown 标记。
            如需分点，只能使用“1.”“2.”“3.”格式。回答清晰具体，控制在 500 字以内。
            """;
        String userPrompt = """
            论文题目：%s

            论文正文与元数据：
            %s

            用户当前选中内容（可能为空）：
            %s

            选中内容所在段落（可能为空）：
            %s

            用户问题：%s
            """.formatted(
                paper.getTitle(),
                paperContext,
                selection.isBlank() ? "无" : selection,
                paragraph.isBlank() ? "无" : paragraph,
                question
            );
        try {
            AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallback(
                systemPrompt,
                userPrompt,
                1000,
                MEETING_MODEL_FALLBACKS
            );
            if (result.totalTokens() > 0) {
                aiUsageService.recordAndCharge(
                    userId,
                    result.modelName(),
                    "qa",
                    "论文选区提问",
                    paper.getTitle(),
                    result.promptTokens(),
                    result.completionTokens(),
                    result.totalTokens()
                );
            }
            return Map.of(
                "answer", cleanAcademicAnswer(result.content()),
                "modelName", result.modelName()
            );
        } catch (Exception error) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "PaperSolver 暂时无法回答：" + readableError(error)
            );
        }
    }

    private String cleanAcademicAnswer(String value) {
        return Optional.ofNullable(value).orElse("")
            .replace("**", "")
            .replaceAll("(?m)^\\s*[-*]\\s+", "")
            .replaceAll("(?m)^\\s*#{1,6}\\s+", "")
            .replaceAll("(?m)^\\s*\\|.*\\|\\s*$", "")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    public byte[] createPptx(String workspaceId) {
        Map<String, Object> data = get(workspaceId);
        @SuppressWarnings("unchecked")
        Map<String, String> sections = (Map<String, String>) data.get("sections");
        @SuppressWarnings("unchecked")
        Map<String, Object> paper = (Map<String, Object>) data.get("paper");
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(1280, 720));
            addTitleSlide(ppt, String.valueOf(paper.get("title")), String.valueOf(paper.get("authors")));
            String[] titles = {"一、基本信息", "二、文章概述", "三、研究背景", "四、研究思路",
                "五、研究结果", "六、研究结论、不足与展望", "七、数据集"};
            for (int i = 0; i < SECTION_KEYS.size(); i++) {
                addContentSlide(ppt, titles[i], sections.getOrDefault(SECTION_KEYS.get(i), "暂无内容"), i + 1);
            }
            ppt.write(out);
            return out.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("PPT 生成失败", error);
        }
    }

    public Map<String, Object> prepareDeckGeneration(Map<String, Object> body) {
        return prepareDeckGeneration(body, null);
    }

    public Map<String, Object> prepareDeckGeneration(String payload, MultipartFile reportPaper) {
        try {
            Map<String, Object> body = objectMapper.readValue(payload, new TypeReference<>() {});
            return prepareDeckGeneration(body, reportPaper);
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PPT 参数格式错误");
        }
    }

    private Map<String, Object> prepareDeckGeneration(Map<String, Object> body, MultipartFile reportPaper) {
        String jobId = "meeting-deck-" + UUID.randomUUID();
        DeckJob job = new DeckJob(jobId);
        job.userId(currentUserService.getOrCreateDefaultUserId());
        job.paperTitle(Objects.toString(body.getOrDefault("reportPaperTitle", body.getOrDefault("title", "组会汇报PPT")), "组会汇报PPT"));
        deckJobs.put(jobId, job);
        byte[] reportPaperBytes = null;
        String reportPaperName = "";
        try {
            if (reportPaper != null && !reportPaper.isEmpty()) {
                reportPaperBytes = reportPaper.getBytes();
                reportPaperName = Optional.ofNullable(reportPaper.getOriginalFilename()).orElse("report-paper.pdf");
            } else {
                String reportWorkspaceId = Objects.toString(body.get("reportWorkspaceId"), "").trim();
                if (!reportWorkspaceId.isBlank()) {
                    PaperEntity paper = requirePaper(reportWorkspaceId, currentUserService.getOrCreateDefaultUserId());
                    Optional<byte[]> storedPdf = loadPdfBytes(paper);
                    if (storedPdf.isPresent()) {
                        reportPaperBytes = storedPdf.get();
                        reportPaperName = safeDeckPaperFilename(paper);
                        job.paperTitle(paper.getTitle());
                    }
                }
            }
        } catch (Exception error) {
            job.fail("上传汇报论文读取失败：" + readableError(error));
            return deckJobResponse(job);
        }
        byte[] finalReportPaperBytes = reportPaperBytes;
        String finalReportPaperName = reportPaperName;
        deckExecutor.submit(() -> runDeckGenerationJob(job, body, finalReportPaperName, finalReportPaperBytes));
        return deckJobResponse(job);
    }

    private String safeDeckPaperFilename(PaperEntity paper) {
        String title = Optional.ofNullable(paper.getTitle()).orElse("paper")
            .replaceAll("[\\\\/:*?\"<>|]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (title.isBlank()) title = "paper";
        if (title.length() > 80) title = title.substring(0, 80).trim();
        return title + ".pdf";
    }

    private void runDeckGenerationJob(DeckJob job, Map<String, Object> body, String reportPaperName, byte[] reportPaperBytes) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        job.progress(4, "任务已创建，正在校验论文材料");
        if (reportPaperBytes == null || reportPaperBytes.length == 0) {
            job.fail("请先上传一篇需要汇报的 PDF 论文");
            return;
        }
        Object rawPaperIds = body.get("paperIds");
        List<String> workspaceIds = rawPaperIds instanceof List<?> paperIds
            ? paperIds.stream()
                .map(item -> Objects.toString(item, "").trim())
                .filter(id -> !id.isBlank())
                .distinct()
                .toList()
            : List.of();

        List<PaperEntity> papers = new ArrayList<>();
        if (!workspaceIds.isEmpty()) {
            try {
                papers = workspaceIds.stream()
                    .map(workspaceId -> requireDeckPaper(workspaceId, userId))
                    .toList();
            } catch (ResponseStatusException error) {
                job.fail(Optional.ofNullable(error.getReason()).orElse("论文校验失败"));
                return;
            }
        }
        job.progress(12, "PDF 校验完成，正在读取 PPT Master 默认生成参数");
        String templateName = "PPT Master Skill";
        Object template = body.get("template");
        if (template instanceof Map<?, ?> templateMap) {
            templateName = Objects.toString(templateMap.get("name"), templateName);
        }
        String slideCount = Objects.toString(body.getOrDefault("slideCount", "10-12"), "10-12");
        String audience = Objects.toString(body.getOrDefault("audience", "导师与课题组"), "导师与课题组");
        String focus = Objects.toString(body.getOrDefault("focus", ""), "");
        Map<String, Object> pptMasterSettings = readPptMasterSettings(body);
        List<String> dimensions = readDeckDimensionLabels(body.get("dimensions"));

        String jobId = job.jobId();
        Path outputDir = Path.of(System.getProperty("user.dir"), "ppt-master-jobs", jobId);
        Path materialPath = outputDir.resolve("meeting-report-input.md");
        Path pptxPath = outputDir.resolve("meeting-report.pptx");
        Path reportPaperPath = null;
        try {
            Files.createDirectories(outputDir);
            if (reportPaperBytes != null && reportPaperBytes.length > 0) {
                String filename = Optional.ofNullable(reportPaperName).orElse("report-paper.pdf")
                    .replaceAll("[\\\\/:*?\"<>|]+", "_");
                reportPaperPath = outputDir.resolve("report-paper-" + filename);
                Files.write(reportPaperPath, reportPaperBytes);
            }
            job.progress(20, "正在整理主论文材料");
            Files.writeString(
                materialPath,
                buildDeckMaterial(papers, dimensions, templateName, slideCount, audience, focus, reportPaperPath)
            );
            Map<String, Object> confirmedSettings = runPptMasterConfirmUi(
                job,
                outputDir,
                materialPath,
                reportPaperPath,
                slideCount,
                audience
            );
            pptMasterSettings.put("confirmUi", confirmedSettings);
            if (StringUtils.hasText(Objects.toString(confirmedSettings.get("page_count"), ""))) {
                slideCount = Objects.toString(confirmedSettings.get("page_count"), slideCount);
                pptMasterSettings.put("slideCount", slideCount);
            }
            if (StringUtils.hasText(Objects.toString(confirmedSettings.get("audience"), ""))) {
                audience = Objects.toString(confirmedSettings.get("audience"), audience);
                pptMasterSettings.put("audience", audience);
            }
        } catch (ResponseStatusException error) {
            job.fail(Optional.ofNullable(error.getReason()).orElse("PPT Master 参数确认失败"));
            return;
        } catch (Exception error) {
            job.fail("PPT 生成材料写入失败：" + readableError(error));
            return;
        }

        try {
            Map<String, Object> handoff = createPptMasterAgentHandoff(
                jobId,
                outputDir,
                materialPath,
                reportPaperPath,
                slideCount,
                audience,
                pptMasterSettings
            );
            executePptMasterAgent(job, outputDir, materialPath, reportPaperPath, pptxPath, handoff);
        } catch (Exception error) {
            job.fail("PPT Master Agent 执行失败：" + readableError(error));
        }
    }

    public GeneratedDeck readGeneratedDeck(String jobId) {
        String cleanJobId = Objects.toString(jobId, "").trim();
        if (!cleanJobId.matches("meeting-deck-[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PPT 任务编号无效");
        }
        DeckJob job = deckJobs.get(cleanJobId);
        if (job != null) ensurePptUsageRecorded(job);
        Path root = Path.of(System.getProperty("user.dir"), "ppt-master-jobs").toAbsolutePath().normalize();
        Path pptx = root.resolve(cleanJobId).resolve("meeting-report.pptx").normalize();
        if (!pptx.startsWith(root) || !Files.isRegularFile(pptx)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PPT 文件不存在或尚未生成");
        }
        try {
            return new GeneratedDeck(Files.readAllBytes(pptx), "组会汇报-" + cleanJobId + ".pptx");
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PPT 文件读取失败");
        }
    }

    public Map<String, Object> deckGenerationStatus(String jobId) {
        String cleanJobId = Objects.toString(jobId, "").trim();
        if (!cleanJobId.matches("meeting-deck-[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PPT 任务编号无效");
        }
        DeckJob job = deckJobs.get(cleanJobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PPT 任务不存在或已过期");
        }
        return deckJobResponse(job);
    }

    public Map<String, Object> analyzeDeckComparison(Map<String, Object> body) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        List<String> workspaceIds = readDeckWorkspaceIds(body.get("paperIds"));
        if (workspaceIds.size() < 3 || workspaceIds.size() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 对比需要选择 3-5 篇论文");
        }
        List<PaperEntity> papers = workspaceIds.stream()
            .map(workspaceId -> requireDeckPaper(workspaceId, userId))
            .toList();
        List<String> dimensionKeys = readDeckDimensionKeys(body.get("dimensions"));
        List<Map<String, Object>> paperContexts = new ArrayList<>();
        for (PaperEntity paper : papers) {
            String text = extractPaperText(paper);
            if (text.length() > 9000) text = text.substring(0, 9000);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", paper.getWorkspaceId());
            row.put("title", paper.getTitle());
            row.put("authors", Optional.ofNullable(paper.getAuthors()).orElse(""));
            row.put("source", Optional.ofNullable(paper.getSource()).orElse(""));
            row.put("year", Optional.ofNullable(paper.getPublishYear()).orElse(""));
            row.put("abstract", Optional.ofNullable(paper.getAbstractText()).orElse(""));
            row.put("note", Optional.ofNullable(paper.getNote()).orElse(""));
            row.put("text", text);
            paperContexts.add(row);
        }
        String systemPrompt = """
            你是严谨的组会论文对比助手。必须基于给定论文正文、摘要、笔记和元数据做横向分析。
            不得编造实验数值、数据集名称或论文结论。信息不足时返回空字符串 ""，不要重复占位句。
            只返回 JSON，不要 Markdown。
            JSON 格式：
            {"matrix":{"workspaceId":{"researchProblem":"...","method":"...","dataExperiment":"...","results":"...","contribution":"...","limitation":"...","discussion":"..."}}}
            每个单元格用 1-3 句中文，适合直接放进组会对比表。
            """;
        String userPrompt;
        try {
            userPrompt = objectMapper.writeValueAsString(Map.of(
                "dimensions", dimensionKeys,
                "papers", paperContexts
            ));
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "论文对比材料整理失败");
        }
        try {
            AiChatService.ChatResult result = aiChatService.chatJsonWithModelFallback(
                systemPrompt,
                userPrompt,
                5200,
                MEETING_MODEL_FALLBACKS
            );
            Map<String, Object> parsed = objectMapper.readValue(extractJson(result.content()), new TypeReference<>() {});
            Object matrix = parsed.get("matrix");
            if (!(matrix instanceof Map<?, ?>)) {
                throw new IllegalStateException("AI 返回内容缺少 matrix");
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "done");
            response.put("message", "AI 对比已生成");
            response.put("modelName", result.modelName());
            response.put("matrix", matrix);
            response.put("dimensions", dimensionKeys);
            return response;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 对比生成失败：" + readableError(error));
        }
    }

    private Map<String, Object> buildStructuredDeckPayload(
        List<PaperEntity> papers,
        Map<String, Object> body,
        List<String> dimensionLabels,
        String templateName,
        String slideCount,
        String audience,
        String focus,
        Map<String, Object> pptMasterSettings,
        Path reportPaperPath,
        DeckJob job
    ) {
        List<Map<String, Object>> dimensions = readDeckDimensions(body.get("dimensions"));
        if (dimensions.isEmpty()) {
            dimensions = dimensionLabels.stream()
                .map(label -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", label);
                    row.put("label", label);
                    return row;
                })
                .toList();
        }
        Map<String, Object> matrix = normalizeMatrix(body.get("analysisMatrix"));
        List<Map<String, Object>> paperCards = new ArrayList<>();
        StringBuilder paperContext = new StringBuilder();
        for (PaperEntity paper : papers) {
            String text = extractBestStructuredText(paper);
            if (text.length() > 6500) text = text.substring(0, 6500);
            Map<String, Object> card = new LinkedHashMap<>();
            card.put("id", paper.getWorkspaceId());
            card.put("title", Optional.ofNullable(paper.getTitle()).orElse("未命名论文"));
            card.put("shortTitle", shortTitle(paper.getTitle()));
            card.put("authors", Optional.ofNullable(paper.getAuthors()).orElse(""));
            card.put("source", Optional.ofNullable(paper.getSource()).orElse(""));
            card.put("year", Optional.ofNullable(paper.getPublishYear()).orElse(""));
            card.put("abstract", Optional.ofNullable(paper.getAbstractText()).orElse(""));
            card.put("note", Optional.ofNullable(paper.getNote()).orElse(""));
            paperCards.add(card);
            paperContext.append("\n\n## ").append(paper.getTitle()).append("\n")
                .append("作者：").append(Optional.ofNullable(paper.getAuthors()).orElse("")).append("\n")
                .append("来源：").append(Optional.ofNullable(paper.getSource()).orElse("")).append(" ")
                .append(Optional.ofNullable(paper.getPublishYear()).orElse("")).append("\n")
                .append("摘要：").append(Optional.ofNullable(paper.getAbstractText()).orElse("")).append("\n")
                .append("笔记：").append(Optional.ofNullable(paper.getNote()).orElse("")).append("\n")
                .append("结构化正文摘录：").append(text);
        }
        String reportPaperText = extractUploadedReportPaperText(reportPaperPath);
        Map<String, Object> primaryReportPaper = buildPrimaryReportPaper(reportPaperPath, reportPaperText);
        boolean includeComparisonAppendix = includeComparisonAppendix(pptMasterSettings);
        String systemPrompt = """
            你是资深博士后级别的学术 PPT agent，不是普通 PPT 大纲助手。你的任务是按 PPT Master skill 的范式：先读懂上传主论文的学术精髓，再组织为 Background、Methodology、Experiment/Results、Conclusion、Outlook 五段式学术汇报。
            参考 PPT Master skill 的工作方式：保留论文中的公式、图、表、方法流程和实验结论；先做研究理解和叙事策略，再做逐页内容规划；不要输出机械栏目填空。
            用户上传的 reportPaperText 是“汇报主论文”，上方选择的 papers 是“对比文献库”，二者必须分层处理。
            PPT 主线必须只围绕汇报主论文展开：研究背景、研究问题、核心方法、实验与证据、主要结论、贡献局限、组会问题。
            默认不要生成“对比文献”“横向对比”“对比矩阵”“多论文比较”等独立章节。只有 includeComparisonAppendix 为 true 时，才允许在最后追加一个对比附录。
            必须基于 reportPaperText 中能看到的证据写；不要编造论文没有的实验数值、数据集、结论或作者意图。信息不足时写“待核对：……”并说明缺什么。
            不要输出 LaTeX、Markdown、$ 符号、\\rightarrow、\\leftarrow 或公式转义。
            只返回 JSON，不要 Markdown。
            JSON 格式：
            {
              "title":"...",
              "subtitle":"...",
              "researchEssence":{
                "oneSentence":"用一句话讲清本文到底解决什么问题",
                "centralQuestion":"本文最核心研究问题",
                "coreClaim":"作者真正想证明的核心判断",
                "argumentChain":["问题如何出现","作者如何处理","证据如何支撑","结论意味着什么"],
                "methodKernel":"方法/框架/理论机制的核心",
                "evidenceKernel":"最关键证据与证据强度",
                "contributionKernel":"本文相对已有工作的真实增量",
                "weaknessKernel":"最值得追问的局限或风险",
                "formulaCandidates":["论文中值得保留到 PPT 的公式或数学表达，若无则空数组"],
                "figureCandidates":["论文中值得截图/重画的图，说明图号、含义和应该放在哪一页"],
                "tableCandidates":["论文中值得保留的表格，说明表号、含义和应该放在哪一页"]
              },
              "takeaways":["..."],
              "agenda":["..."],
              "slides":[
                {
                  "eyebrow":"...",
                  "title":"...",
                  "subtitle":"...",
                  "section":"Background|Methodology|Experiment|Results|Conclusion|Outlook",
                  "visualType":"academic_background|method_pipeline|formula_focus|figure_explain|table_result|result_comparison|conclusion_takeaway|future_outlook|discussion",
                  "bullets":["每条必须是具体论文判断，不要写空泛栏目名"],
                  "evidence":["来自正文的短证据或待核对项"],
                  "assetCue":"这一页应保留/重画的公式、图、表或论文截图；没有则写空字符串",
                  "keyMessage":"这一页讲给导师听的一句话",
                  "speakerNotes":"90-140 字中文讲稿，解释这一页为什么重要"
                }
              ],
              "discussionQuestions":["..."]
            }
            slides 数量必须贴近用户页数要求。必须覆盖 Background、Methodology、Experiment/Results、Conclusion、Outlook；每页只讲一个论证动作：为什么研究、问题是什么、作者怎么做、实验/结果说明什么、贡献在哪里、未来怎么做。
            禁止输出“本次汇报以上传论文为唯一主线”“待补充”这类模板句，除非材料确实缺失且必须写成“待核对：……”。
            """;
        Map<String, Object> promptData = new LinkedHashMap<>();
        promptData.put("template", templateName);
        promptData.put("slideCount", slideCount);
        promptData.put("pptMasterSettings", pptMasterSettings);
        promptData.put("audience", audience);
        promptData.put("focus", focus);
        promptData.put("dimensions", dimensions);
        promptData.put("primaryReportPaper", primaryReportPaper);
        promptData.put("comparisonPapers", includeComparisonAppendix ? paperCards : List.of());
        promptData.put("includeComparisonAppendix", includeComparisonAppendix);
        promptData.put("comparisonMatrix", includeComparisonAppendix ? matrix : Map.of());
        promptData.put("reportPaperText", reportPaperText.length() > 10000 ? reportPaperText.substring(0, 10000) : reportPaperText);
        promptData.put("paperContext", includeComparisonAppendix
            ? (paperContext.length() > 18000 ? paperContext.substring(0, 18000) : paperContext.toString())
            : "");

        Map<String, Object> payload = fallbackDeckPayload(primaryReportPaper, paperCards, dimensions, matrix, templateName, slideCount, audience, focus, pptMasterSettings);
        try {
            Map<String, Object> agentPayload = buildDeckWithMultiRoundAgent(promptData, payload, job);
            mergeIfPresent(payload, agentPayload, "title");
            mergeIfPresent(payload, agentPayload, "subtitle");
            mergeIfPresent(payload, agentPayload, "takeaways");
            mergeIfPresent(payload, agentPayload, "agenda");
            mergeIfPresent(payload, agentPayload, "researchEssence");
            mergeIfPresent(payload, agentPayload, "slides");
            mergeIfPresent(payload, agentPayload, "discussionQuestions");
            mergeIfPresent(payload, agentPayload, "agentRounds");
            payload.put("modelName", Objects.toString(agentPayload.getOrDefault("modelName", ""), ""));
            payload.put("contentEngine", "deck-agent-multiround");
        } catch (Exception error) {
            Map<String, Object> extractedPayload = buildExtractedPdfDeckPayload(
                primaryReportPaper,
                reportPaperText,
                templateName,
                slideCount,
                audience,
                focus,
                pptMasterSettings,
                readableError(error)
            );
            mergeIfPresent(payload, extractedPayload, "title");
            mergeIfPresent(payload, extractedPayload, "subtitle");
            mergeIfPresent(payload, extractedPayload, "takeaways");
            mergeIfPresent(payload, extractedPayload, "agenda");
            mergeIfPresent(payload, extractedPayload, "researchEssence");
            mergeIfPresent(payload, extractedPayload, "slides");
            mergeIfPresent(payload, extractedPayload, "discussionQuestions");
            payload.put("contentEngine", "pdf-extracted-fallback");
            payload.put("modelWarning", "强模型结构化 JSON 失败，已改用 PDF 正文提取生成：" + readableError(error));
            job.progress(48, "强模型结构化失败，已切换为 PDF 正文提取生成");
        }
        payload.put("papers", paperCards);
        payload.put("primaryReportPaper", primaryReportPaper);
        payload.put("dimensions", dimensions);
        payload.put("matrix", matrix);
        payload.put("template", templateName);
        payload.put("audience", audience);
        payload.put("slideCount", slideCount);
        payload.put("pptMasterSettings", pptMasterSettings);
        payload.put("includeComparisonAppendix", includeComparisonAppendix);
        payload.put("renderEngine", "ppt-master-skill");
        payload.put("generatedAt", java.time.LocalDateTime.now().toString().replace('T', ' '));
        return sanitizeDeckPayload(payload, includeComparisonAppendix);
    }

    private Map<String, Object> buildDeckWithMultiRoundAgent(
        Map<String, Object> promptData,
        Map<String, Object> basePayload,
        DeckJob job
    ) throws Exception {
        List<Map<String, String>> rounds = new ArrayList<>();
        Map<String, Object> result = new LinkedHashMap<>();

        if (job != null) job.progress(32, "强模型 Agent 第 1/3 轮：精读主论文并提炼论文精髓");
        Map<String, Object> essenceRound = runDeckAgentRound(
            "paper_understanding",
            """
                你是 PPT Master skill 的论文理解 agent。只做第一步：从主论文材料中提炼学术精髓和可视化资产。不要设计 PPT，不要写目录。
                必须基于 reportPaperText，不得把 comparisonPapers 当成主论文。
                返回 JSON：
                {"researchEssence":{
                  "oneSentence":"...",
                  "centralQuestion":"...",
                  "coreClaim":"...",
                  "argumentChain":["...","...","...","..."],
                  "methodKernel":"...",
                  "evidenceKernel":"...",
                  "contributionKernel":"...",
                  "weaknessKernel":"...",
                  "formulaCandidates":["..."],
                  "figureCandidates":["..."],
                  "tableCandidates":["..."]
                }}
                每个字段必须具体到论文内容；材料缺失时写“待核对：缺少……”，不要写模板话。
                """,
            agentPayload(
                "primaryReportPaper", promptData.get("primaryReportPaper"),
                "reportPaperText", promptData.get("reportPaperText"),
                "audience", promptData.get("audience"),
                "focus", promptData.get("focus")
            ),
            1800
        );
        rounds.add(agentRoundMeta("paper_understanding", essenceRound));
        Object researchEssence = essenceRound.getOrDefault("researchEssence", Map.of());
        result.put("researchEssence", researchEssence);

        if (job != null) job.progress(40, "强模型 Agent 第 2/3 轮：规划组会叙事与页序");
        Map<String, Object> planRound;
        try {
            planRound = runDeckAgentRound(
                "narrative_strategy",
                """
                    你是 PPT Master skill 的学术叙事 agent。只做第二步：根据 researchEssence 规划五段式学术 PPT 页序。
                    必须覆盖 Background、Methodology、Experiment/Results、Conclusion、Outlook。不要写泛泛栏目名；每一页都必须对应一个论证动作，并标明是否需要保留公式/图/表/截图。
                    返回 JSON：
                    {"title":"...","subtitle":"...","takeaways":["..."],"agenda":["..."],
                     "slidePlan":[{"section":"Background|Methodology|Experiment|Results|Conclusion|Outlook","eyebrow":"...","title":"...","subtitle":"...","visualType":"academic_background|method_pipeline|formula_focus|figure_explain|table_result|result_comparison|conclusion_takeaway|future_outlook|discussion","assetCue":"公式/图/表/截图线索","purpose":"这一页在论证链中的作用"}]}
                    页数贴近 slideCount；默认不要生成对比文献章节。
                    """,
                agentPayload(
                    "researchEssence", researchEssence,
                    "slideCount", promptData.get("slideCount"),
                    "audience", promptData.get("audience"),
                    "pptMasterSettings", promptData.get("pptMasterSettings"),
                    "includeComparisonAppendix", promptData.get("includeComparisonAppendix")
                ),
                1600
            );
        } catch (Exception planError) {
            planRound = deterministicAcademicPlan(researchEssence, promptData);
            planRound.put("_roundName", "narrative_strategy");
            planRound.put("_modelName", "deterministic-academic-plan-after-strong-essence");
            planRound.put("_fallbackReason", readableError(planError));
        }
        rounds.add(agentRoundMeta("narrative_strategy", planRound));
        mergeIfPresent(result, planRound, "title");
        mergeIfPresent(result, planRound, "subtitle");
        mergeIfPresent(result, planRound, "takeaways");
        mergeIfPresent(result, planRound, "agenda");

        if (job != null) job.progress(50, "强模型 Agent 第 3/3 轮：逐页设计内容、证据与讲稿");
        Map<String, Object> slideRound;
        try {
            slideRound = runDeckAgentRound(
                "slide_designer",
                """
                    你是 PPT Master skill 的逐页设计 agent。只做第三步：把 slidePlan 写成可渲染的逐页内容。
                    每页必须包含：section、visualType、具体论文判断 bullets、正文证据 evidence、assetCue、keyMessage、speakerNotes。
                    bullets 不要超过 4 条；evidence 用短句，必须来自主论文材料或写“待核对：……”。speakerNotes 90-140 字。
                    返回 JSON：
                    {"slides":[{"section":"...","eyebrow":"...","title":"...","subtitle":"...","visualType":"...","bullets":["..."],"evidence":["..."],"assetCue":"...","keyMessage":"...","speakerNotes":"..."}],
                     "discussionQuestions":["..."]}
                    不要输出 Markdown，不要写空泛占位句。
                    """,
                agentPayload(
                    "researchEssence", researchEssence,
                    "slidePlan", planRound.getOrDefault("slidePlan", basePayload.getOrDefault("slides", List.of())),
                    "primaryReportPaper", promptData.get("primaryReportPaper"),
                    "reportPaperText", promptData.get("reportPaperText"),
                    "audience", promptData.get("audience")
                ),
                2400
            );
        } catch (Exception slideError) {
            slideRound = deterministicAcademicSlides(researchEssence, planRound);
            slideRound.put("_roundName", "slide_designer");
            slideRound.put("_modelName", "deterministic-academic-slides-after-strong-essence");
            slideRound.put("_fallbackReason", readableError(slideError));
        }
        rounds.add(agentRoundMeta("slide_designer", slideRound));
        mergeIfPresent(result, slideRound, "slides");
        mergeIfPresent(result, slideRound, "discussionQuestions");
        result.put("agentRounds", rounds);
        result.put("modelName", rounds.stream()
            .map(row -> row.getOrDefault("modelName", ""))
            .filter(text -> text != null && !text.isBlank())
            .collect(Collectors.joining(" → ")));
        return result;
    }

    private Map<String, Object> agentPayload(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            map.put(Objects.toString(values[i], ""), values[i + 1]);
        }
        return map;
    }

    private Map<String, Object> deterministicAcademicPlan(Object researchEssence, Map<String, Object> promptData) {
        Map<?, ?> essence = researchEssence instanceof Map<?, ?> map ? map : Map.of();
        String title = shortTitle(mapText(essence, "centralQuestion", Objects.toString(promptData.getOrDefault("template", "学术论文汇报"), "学术论文汇报")));
        List<String> agenda = List.of(
            "Background：问题背景与研究动机",
            "Methodology：方法框架与核心机制",
            "Experiment：实验设置与评价依据",
            "Results：关键结果与证据强度",
            "Conclusion：贡献、局限与展望"
        );
        List<Map<String, Object>> slidePlan = new ArrayList<>();
        slidePlan.add(academicPlanItem("Background", "BACKGROUND", "研究背景与核心问题", Objects.toString(essence.get("centralQuestion"), "待核对：核心研究问题"), "academic_background", "", "交代研究为什么重要"));
        slidePlan.add(academicPlanItem("Methodology", "METHODOLOGY", "方法框架与技术路线", Objects.toString(essence.get("methodKernel"), "待核对：方法核心"), "method_pipeline", firstAsset(essence, "formulaCandidates", "figureCandidates"), "解释作者如何解决问题"));
        slidePlan.add(academicPlanItem("Experiment", "EXPERIMENT", "实验设置与证据来源", Objects.toString(essence.get("evidenceKernel"), "待核对：实验与证据"), "table_result", firstAsset(essence, "tableCandidates", "figureCandidates"), "说明证据从哪里来"));
        slidePlan.add(academicPlanItem("Results", "RESULTS", "关键结果与结论解释", Objects.toString(essence.get("coreClaim"), "待核对：核心结论"), "result_comparison", firstAsset(essence, "figureCandidates", "tableCandidates"), "解释结果如何支撑结论"));
        slidePlan.add(academicPlanItem("Conclusion", "CONCLUSION", "贡献与局限", Objects.toString(essence.get("contributionKernel"), "待核对：贡献"), "conclusion_takeaway", "", "收束论文价值和边界"));
        slidePlan.add(academicPlanItem("Outlook", "OUTLOOK", "未来工作与组会讨论", Objects.toString(essence.get("weaknessKernel"), "待核对：局限与展望"), "future_outlook", "", "提出可讨论的问题"));
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("title", title);
        plan.put("subtitle", mapText(essence, "oneSentence", "Academic paper presentation"));
        plan.put("takeaways", safeStringList(essence.get("argumentChain"), 4));
        plan.put("agenda", agenda);
        plan.put("slidePlan", slidePlan);
        return plan;
    }

    private Map<String, Object> academicPlanItem(String section, String eyebrow, String title, String subtitle, String visualType, String assetCue, String purpose) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("section", section);
        row.put("eyebrow", eyebrow);
        row.put("title", title);
        row.put("subtitle", subtitle);
        row.put("visualType", visualType);
        row.put("assetCue", Objects.toString(assetCue, ""));
        row.put("purpose", purpose);
        return row;
    }

    private Map<String, Object> deterministicAcademicSlides(Object researchEssence, Map<String, Object> planRound) {
        Map<?, ?> essence = researchEssence instanceof Map<?, ?> map ? map : Map.of();
        List<?> plan = planRound.get("slidePlan") instanceof List<?> list ? list : List.of();
        List<Map<String, Object>> slides = new ArrayList<>();
        for (Object item : plan) {
            if (!(item instanceof Map<?, ?> row)) continue;
            String section = mapText(row, "section", "Academic");
            String visualType = mapText(row, "visualType", "academic_background");
            String title = mapText(row, "title", section);
            String subtitle = mapText(row, "subtitle", "");
            String assetCue = mapText(row, "assetCue", "");
            List<String> bullets = academicBulletsForSection(section, essence);
            Map<String, Object> slide = new LinkedHashMap<>();
            slide.put("section", section);
            slide.put("eyebrow", mapText(row, "eyebrow", section.toUpperCase(Locale.ROOT)));
            slide.put("title", title);
            slide.put("subtitle", subtitle);
            slide.put("visualType", visualType);
            slide.put("bullets", bullets);
            slide.put("evidence", academicEvidenceForSection(section, essence));
            slide.put("assetCue", assetCue);
            slide.put("keyMessage", academicKeyMessage(section, essence));
            slide.put("speakerNotes", title + "。这一页围绕主论文的" + section + "展开，重点说明：" + String.join("；", bullets).replaceAll("\\s+", " ") + "。");
            slides.add(slide);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("slides", slides);
        result.put("discussionQuestions", List.of(
            "这篇论文的核心假设是否被充分验证？",
            "方法框架中哪一部分最值得复现或替换？",
            "实验/结果是否足以支撑作者的核心判断？",
            "局限性对我们自己的课题有什么启发？"
        ));
        return result;
    }

    private List<String> academicBulletsForSection(String section, Map<?, ?> essence) {
        String lower = section.toLowerCase(Locale.ROOT);
        if (lower.contains("background")) {
            return List.of(
                mapText(essence, "centralQuestion", "待核对：研究问题"),
                mapText(essence, "coreClaim", "待核对：核心判断")
            );
        }
        if (lower.contains("method")) {
            return List.of(mapText(essence, "methodKernel", "待核对：方法框架"));
        }
        if (lower.contains("experiment") || lower.contains("result")) {
            return List.of(mapText(essence, "evidenceKernel", "待核对：实验与结果"));
        }
        if (lower.contains("conclusion")) {
            return List.of(mapText(essence, "contributionKernel", "待核对：贡献"));
        }
        return List.of(mapText(essence, "weaknessKernel", "待核对：局限与展望"));
    }

    private List<String> academicEvidenceForSection(String section, Map<?, ?> essence) {
        String lower = section.toLowerCase(Locale.ROOT);
        if (lower.contains("method")) return safeStringList(essence.get("formulaCandidates"), 2);
        if (lower.contains("experiment") || lower.contains("result")) {
            List<String> tables = safeStringList(essence.get("tableCandidates"), 2);
            return tables.isEmpty() ? safeStringList(essence.get("figureCandidates"), 2) : tables;
        }
        return safeStringList(essence.get("argumentChain"), 2);
    }

    private String academicKeyMessage(String section, Map<?, ?> essence) {
        String lower = section.toLowerCase(Locale.ROOT);
        if (lower.contains("method")) return mapText(essence, "methodKernel", "方法是本文论证链的核心。");
        if (lower.contains("experiment") || lower.contains("result")) return mapText(essence, "evidenceKernel", "结果证据决定结论可信度。");
        if (lower.contains("conclusion")) return mapText(essence, "contributionKernel", "贡献需要与局限一起理解。");
        if (lower.contains("outlook")) return mapText(essence, "weaknessKernel", "局限决定后续可讨论空间。");
        return mapText(essence, "oneSentence", "先抓住论文真正解决的问题。");
    }

    private String mapText(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        String text = Objects.toString(value, "").trim();
        return text.isBlank() ? fallback : text;
    }

    private List<String> safeStringList(Object value, int limit) {
        if (!(value instanceof Collection<?> collection)) return List.of();
        return collection.stream()
            .map(item -> Objects.toString(item, "").trim())
            .filter(text -> !text.isBlank())
            .limit(limit)
            .toList();
    }

    private String firstAsset(Map<?, ?> essence, String firstKey, String secondKey) {
        List<String> first = safeStringList(essence.get(firstKey), 1);
        if (!first.isEmpty()) return first.get(0);
        List<String> second = safeStringList(essence.get(secondKey), 1);
        return second.isEmpty() ? "" : second.get(0);
    }

    private Map<String, Object> runDeckAgentRound(
        String roundName,
        String systemPrompt,
        Object promptPayload,
        int maxOutputTokens
    ) throws Exception {
        String userPrompt = objectMapper.writeValueAsString(promptPayload);
        AiChatService.ChatResult result = aiChatService.chatJsonForDeckAgentStrict(
            systemPrompt,
            userPrompt,
            maxOutputTokens,
            DECK_AGENT_STRONG_MODELS
        );
        Map<String, Object> parsed = parseDeckAgentJson(roundName, result.content(), systemPrompt);
        parsed.put("_roundName", roundName);
        parsed.put("_modelName", result.modelName());
        return parsed;
    }

    private Map<String, Object> parseDeckAgentJson(String roundName, String raw, String originalSystemPrompt) throws Exception {
        try {
            return objectMapper.readValue(extractJson(raw), new TypeReference<>() {});
        } catch (Exception parseError) {
            String repairPrompt = """
                你是 JSON 修复器。用户会给你一段模型输出，它本应是 JSON 但可能被截断、包含多余文字或缺少引号/括号。
                请只返回一个合法 JSON 对象，不要解释，不要 Markdown。
                如果字段缺失，请尽量保留可恢复字段；无法恢复的数组用 []，对象用 {}，字符串用 ""。
                本轮名称：%s
                原始任务要求：%s
                """.formatted(roundName, originalSystemPrompt);
            String repairInput = Optional.ofNullable(raw).orElse("");
            if (repairInput.length() > 12000) {
                repairInput = repairInput.substring(0, 12000);
            }
            AiChatService.ChatResult fixed = aiChatService.chatJsonForDeckAgentStrict(
                repairPrompt,
                repairInput,
                1600,
                DECK_AGENT_STRONG_MODELS
            );
            try {
                Map<String, Object> parsed = objectMapper.readValue(extractJson(fixed.content()), new TypeReference<>() {});
                parsed.put("_jsonRepaired", true);
                parsed.put("_repairModelName", fixed.modelName());
                return parsed;
            } catch (Exception repairError) {
                throw new IllegalStateException(roundName + " 返回 JSON 不完整，自动修复也失败：" + repairError.getMessage());
            }
        }
    }

    private Map<String, String> agentRoundMeta(String roundName, Map<String, Object> round) {
        return Map.of(
            "round", roundName,
            "modelName", Objects.toString(round.getOrDefault("_modelName", ""), "")
        );
    }

    private void mergeIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) return;
        if (value instanceof String text && text.isBlank()) return;
        if (value instanceof Collection<?> collection && collection.isEmpty()) return;
        target.put(key, value);
    }

    private Map<String, Object> fallbackDeckPayload(
        Map<String, Object> primaryReportPaper,
        List<Map<String, Object>> papers,
        List<Map<String, Object>> dimensions,
        Map<String, Object> matrix,
        String templateName,
        String slideCount,
        String audience,
        String focus,
        Map<String, Object> pptMasterSettings
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        String primaryTitle = Objects.toString(primaryReportPaper.getOrDefault("title", "汇报主论文"), "汇报主论文");
        payload.put("title", "组会汇报：" + shortTitle(primaryTitle));
        payload.put("subtitle", "围绕上传主论文生成的组会汇报");
        payload.put("takeaways", List.of(
            "本次 PPT 只围绕上传主论文展开，不默认生成对比模块。",
            "汇报主线覆盖研究背景、核心问题、方法框架、证据链、贡献和局限。",
            focus == null || focus.isBlank() ? "结尾聚焦可讨论问题和后续研究切入点。" : focus
        ));
        payload.put("agenda", List.of("Background：研究背景与问题", "Methodology：方法与模型", "Experiment：实验设计", "Results：结果解释", "Conclusion：结论贡献", "Outlook：局限与展望"));
        List<Map<String, Object>> slides = new ArrayList<>();
        slides.add(Map.of(
            "eyebrow", "PRIMARY PAPER",
            "title", "研究背景与问题定位",
            "section", "Background",
            "visualType", "academic_background",
            "subtitle", Objects.toString(primaryReportPaper.getOrDefault("fileName", "上传论文"), ""),
            "bullets", List.of(
                "本次汇报以上传论文为唯一主线。",
                "先说明论文试图解决的具体问题，以及这个问题为什么值得讨论。",
                "材料不足处保留“待补充”，避免把外部文献内容误写进主论文。"
            ),
            "keyMessage", "把听众带进主论文的问题现场，而不是先展开文献对比。",
            "speakerNotes", "开场先说明本次汇报聚焦一篇主论文：它研究什么问题，为什么这个问题值得课题组讨论。"
        ));
        slides.add(Map.of(
            "eyebrow", "METHODOLOGY",
            "title", "方法框架与论证路径",
            "section", "Methodology",
            "visualType", "method_pipeline",
            "assetCue", "待核对：从主论文中保留方法流程图、核心公式或模型结构图。",
            "subtitle", "把全文拆成可以口头汇报的逻辑链。",
            "bullets", List.of(
                "研究问题：主论文试图解决什么具体问题，为什么重要。",
                "方法路径：作者如何从理论、模型、实验或案例推进论证。",
                "证据链：哪些数据、实验、案例或推理支撑主要结论。",
                "可讨论点：哪些假设、边界或复现问题值得组会追问。"
            ),
            "keyMessage", "先建立主论文的“问题-方法-证据-结论”链条。",
            "speakerNotes", "这一页建议用 1 分钟说明主论文的阅读框架，后续每一页都围绕这个框架展开。"
        ));
        slides.add(Map.of(
            "eyebrow", "EXPERIMENT / RESULTS",
            "title", "实验、案例与核心证据",
            "section", "Results",
            "visualType", "result_comparison",
            "assetCue", "待核对：从主论文中保留最关键实验表格、结果图或消融图。",
            "subtitle", "只讲主论文中能支撑结论的材料。",
            "bullets", List.of("数据、案例或实验设置：待补充。", "主要结果与证据强度：待补充。", "作者如何从证据推出结论：待补充。"),
            "keyMessage", "汇报时要把结论和证据绑定，而不是只复述摘要。",
            "speakerNotes", "这一页只讲主论文自己的证据。没有从正文提取到的信息必须标注待补充。"
        ));
        slides.add(Map.of(
            "eyebrow", "CONCLUSION / OUTLOOK",
            "title", "贡献、局限与未来方向",
            "section", "Outlook",
            "visualType", "future_outlook",
            "assetCue", "",
            "subtitle", "把论文价值收束到可讨论的问题。",
            "bullets", List.of("主要贡献：待补充。", "关键假设和边界条件：待补充。", "值得课题组讨论的问题：待补充。"),
            "keyMessage", "最后要留下可以讨论、可以复现、可以延伸的问题。",
            "speakerNotes", "结尾不要再引入新的对比模块，直接回到这篇主论文的贡献、局限和下一步问题。"
        ));
        payload.put("slides", slides);
        payload.put("discussionQuestions", List.of(
            "这篇论文最关键的研究假设是什么？",
            "作者给出的证据是否足以支撑主要结论？",
            "哪些实验、案例或指标最需要复现或补充？",
            "这篇论文对我们的课题有什么可迁移的启发？"
        ));
        return payload;
    }

    private Map<String, Object> buildExtractedPdfDeckPayload(
        Map<String, Object> primaryReportPaper,
        String reportPaperText,
        String templateName,
        String slideCount,
        String audience,
        String focus,
        Map<String, Object> pptMasterSettings,
        String warning
    ) {
        String title = Objects.toString(primaryReportPaper.getOrDefault("title", "汇报主论文"), "汇报主论文");
        Map<String, Object> payload = new LinkedHashMap<>();
        String abstractText = extractSectionSnippet(reportPaperText, "(?i)^\\s*abstract\\b|^\\s*摘要\\b", "(?i)^\\s*(keywords|index terms|introduction|1\\.?\\s+introduction|关键词)\\b", 720);
        String introText = extractSectionSnippet(reportPaperText, "(?i)^\\s*(1\\.?\\s+)?introduction\\b|^\\s*引言\\b|^\\s*简介\\b", "(?i)^\\s*(2\\.?\\s+|background|related work|method|methods|methodology)\\b", 900);
        String methodText = extractSectionSnippet(reportPaperText, "(?i)^\\s*(method|methods|methodology|approach|framework|model)\\b|^\\s*方法\\b", "(?i)^\\s*(experiment|experiments|evaluation|result|results|discussion|case study)\\b", 950);
        String resultText = extractSectionSnippet(reportPaperText, "(?i)^\\s*(experiment|experiments|evaluation|result|results|findings|case study)\\b|^\\s*(实验|结果|评估)\\b", "(?i)^\\s*(discussion|conclusion|limitations|future work)\\b", 950);
        String conclusionText = extractSectionSnippet(reportPaperText, "(?i)^\\s*(discussion|conclusion|limitations|future work)\\b|^\\s*(讨论|结论|局限|展望)\\b", "(?i)^\\s*(references|acknowledg)\\b|^\\s*参考文献\\b", 800);
        if (abstractText.isBlank()) {
            abstractText = firstUsefulParagraph(reportPaperText, 620);
        }
        List<String> abstractBullets = bulletsFromText(abstractText, 3, "摘要段落未能稳定提取，请回到 PDF 核对研究目标、对象和主要结论。");
        List<String> introBullets = bulletsFromText(introText, 4, "引言信息提取不足，请核对论文的问题背景、研究动机和缺口。");
        List<String> methodBullets = bulletsFromText(methodText, 4, "方法段落提取不足，请核对模型、框架、变量或流程。");
        List<String> resultBullets = bulletsFromText(resultText, 4, "结果段落提取不足，请核对实验设置、指标、数据和主要发现。");
        List<String> conclusionBullets = bulletsFromText(conclusionText, 4, "结论段落提取不足，请核对贡献、边界条件和未来方向。");

        payload.put("title", "组会汇报：" + shortTitle(title));
        payload.put("subtitle", "基于 PDF 正文提取与 PPT Master 参数确认生成");
        Map<String, Object> essence = new LinkedHashMap<>();
        essence.put("oneSentence", firstSentence(abstractText, "本文核心问题需要结合摘要与引言核对。"));
        essence.put("centralQuestion", firstSentence(introText, "研究问题需从引言部分进一步核对。"));
        essence.put("coreClaim", firstSentence(conclusionText, firstSentence(abstractText, "核心结论需从全文核对。")));
        essence.put("argumentChain", List.of("从摘要定位研究主题", "从引言提取问题背景", "从方法段落提取技术路径", "从结果/结论段落提取证据与贡献"));
        essence.put("methodKernel", firstSentence(methodText, "方法核心需从正文方法章节核对。"));
        essence.put("evidenceKernel", firstSentence(resultText, "关键证据需从实验或结果章节核对。"));
        essence.put("contributionKernel", firstSentence(conclusionText, "贡献需从结论和讨论章节核对。"));
        essence.put("weaknessKernel", focus == null || focus.isBlank() ? "建议在组会中追问数据、指标、适用边界与可复现性。" : focus);
        essence.put("formulaCandidates", findPaperAssetCues(reportPaperText, "Equation|公式|\\(\\d+\\)", 3));
        essence.put("figureCandidates", findPaperAssetCues(reportPaperText, "Fig\\.?|Figure|图\\s*\\d+", 5));
        essence.put("tableCandidates", findPaperAssetCues(reportPaperText, "Table|表\\s*\\d+", 4));
        payload.put("researchEssence", essence);
        payload.put("takeaways", List.of(
            firstSentence(abstractText, "本文主题和核心问题已从 PDF 摘要/正文中提取。"),
            firstSentence(methodText, "方法路线需要围绕论文正文中的框架、模型或流程展开。"),
            firstSentence(resultText, "结果页优先保留论文自己的图、表、指标和结论证据。")
        ));
        payload.put("agenda", List.of(
            "Background：研究背景与问题缺口",
            "Research Question：论文试图回答的问题",
            "Methodology：方法框架与实现路径",
            "Evidence：实验、案例或结果证据",
            "Contribution：主要贡献、局限与讨论"
        ));
        payload.put("slides", List.of(
            extractedSlide("RESEARCH BACKGROUND", "研究背景与问题缺口", "为什么这篇论文值得在组会讨论", "Background", "academic_background", introBullets, "从引言中提取研究动机与问题背景。"),
            extractedSlide("CORE QUESTION", "核心研究问题", "把摘要和引言压缩成可讲的一句话", "Background", "formula_focus", abstractBullets, "先让听众明白论文到底要解决什么。"),
            extractedSlide("METHODOLOGY", "方法框架与技术路径", "从正文方法章节提取模型、流程和关键机制", "Methodology", "method_pipeline", methodBullets, "这一页解释作者如何推进论证。"),
            extractedSlide("EVIDENCE", "实验、案例与结果证据", "用论文自己的结果支撑结论", "Results", "table_result", resultBullets, "这一页避免泛泛复述摘要，绑定证据和判断。"),
            extractedSlide("TAKEAWAYS", "贡献、局限与组会讨论", "收束到可追问、可复现、可迁移的问题", "Outlook", "future_outlook", conclusionBullets, "结尾明确论文价值和组会讨论入口。")
        ));
        payload.put("discussionQuestions", List.of(
            "论文的核心问题是否被方法和证据充分支撑？",
            "哪些图、表、公式最值得在组会中逐页讲解？",
            "实验设置、数据来源或评价指标有没有明显边界？",
            "这篇论文的方法或结论能否迁移到我们的课题？",
            "强模型结构化阶段失败原因：" + warning
        ));
        payload.put("template", templateName);
        payload.put("slideCount", slideCount);
        payload.put("audience", audience);
        payload.put("pptMasterSettings", pptMasterSettings);
        return payload;
    }

    private Map<String, Object> extractedSlide(
        String eyebrow,
        String title,
        String subtitle,
        String section,
        String visualType,
        List<String> bullets,
        String speakerNotes
    ) {
        return Map.of(
            "eyebrow", eyebrow,
            "title", title,
            "subtitle", subtitle,
            "section", section,
            "visualType", visualType,
            "bullets", bullets,
            "evidence", bullets,
            "assetCue", "",
            "keyMessage", bullets.isEmpty() ? subtitle : bullets.get(0),
            "speakerNotes", speakerNotes
        );
    }

    private String extractSectionSnippet(String text, String startRegex, String endRegex, int maxLength) {
        String source = Optional.ofNullable(text).orElse("");
        if (source.isBlank()) return "";
        java.util.regex.Pattern start = java.util.regex.Pattern.compile(startRegex, java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher matcher = start.matcher(source);
        if (!matcher.find()) return "";
        int begin = matcher.end();
        int end = Math.min(source.length(), begin + Math.max(maxLength * 4, maxLength));
        java.util.regex.Pattern stop = java.util.regex.Pattern.compile(endRegex, java.util.regex.Pattern.MULTILINE);
        java.util.regex.Matcher stopMatcher = stop.matcher(source.substring(begin, end));
        if (stopMatcher.find() && stopMatcher.start() > 120) {
            end = begin + stopMatcher.start();
        }
        return compactAcademicText(source.substring(begin, Math.min(source.length(), end)), maxLength);
    }

    private String firstUsefulParagraph(String text, int maxLength) {
        String source = Optional.ofNullable(text).orElse("");
        for (String paragraph : source.split("\\R{2,}")) {
            String compact = compactAcademicText(paragraph, maxLength);
            if (compact.length() > 80 && !compact.toLowerCase(Locale.ROOT).contains("downloaded from")) return compact;
        }
        return compactAcademicText(source, maxLength);
    }

    private List<String> bulletsFromText(String text, int limit, String fallback) {
        String compact = compactAcademicText(text, 1200);
        if (compact.isBlank()) return List.of(fallback);
        List<String> sentences = Arrays.stream(compact.split("(?<=[。！？.!?])\\s+|；|;"))
            .map(item -> compactAcademicText(item, 150))
            .filter(item -> item.length() >= 18)
            .distinct()
            .limit(limit)
            .collect(Collectors.toCollection(ArrayList::new));
        if (sentences.isEmpty()) sentences.add(compactAcademicText(compact, 150));
        return sentences;
    }

    private String firstSentence(String text, String fallback) {
        List<String> bullets = bulletsFromText(text, 1, fallback);
        return bullets.isEmpty() ? fallback : bullets.get(0);
    }

    private List<String> findPaperAssetCues(String text, String regex, int limit) {
        String source = Optional.ofNullable(text).orElse("");
        if (source.isBlank()) return List.of();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(source);
        List<String> cues = new ArrayList<>();
        while (matcher.find() && cues.size() < limit) {
            int begin = Math.max(0, matcher.start() - 80);
            int end = Math.min(source.length(), matcher.end() + 180);
            String cue = compactAcademicText(source.substring(begin, end), 180);
            if (!cue.isBlank() && cues.stream().noneMatch(existing -> existing.equalsIgnoreCase(cue))) cues.add(cue);
        }
        return cues;
    }

    private String compactAcademicText(String text, int maxLength) {
        String compact = Optional.ofNullable(text).orElse("")
            .replaceAll("https?://\\S+", "")
            .replaceAll("(?i)Downloaded from .*", "")
            .replaceAll("\\[[0-9,\\s-]+]", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (compact.length() <= maxLength) return compact;
        return compact.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }

    private Map<String, Object> readPptMasterSettings(Map<String, Object> body) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("generationMode", "paper_reading");
        defaults.put("aspectRatio", "16:9");
        defaults.put("slideCount", Objects.toString(body.getOrDefault("slideCount", "10-12"), "10-12"));
        defaults.put("duration", Objects.toString(body.getOrDefault("duration", "10 分钟"), "10 分钟"));
        defaults.put("audience", Objects.toString(body.getOrDefault("audience", "导师与课题组"), "导师与课题组"));
        defaults.put("languageTone", "学术但口语化");
        defaults.put("visualStyle", "academic_blue");
        defaults.put("density", "中等密度");
        defaults.put("imageMode", "paper_figures");
        defaults.put("notesMode", "speaker_notes");
        defaults.put("animation", "none");
        defaults.put("outputFormat", "editable_pptx");
        defaults.put("editable", true);
        defaults.put("includeComparisonAppendix", false);
        Object raw = body.get("pptSettings");
        if (raw instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                defaults.put(Objects.toString(entry.getKey(), ""), entry.getValue());
            }
        }
        defaults.put("slideCount", Objects.toString(defaults.getOrDefault("slideCount", body.getOrDefault("slideCount", "10-12")), "10-12"));
        defaults.put("audience", Objects.toString(defaults.getOrDefault("audience", body.getOrDefault("audience", "导师与课题组")), "导师与课题组"));
        return defaults;
    }

    private boolean includeComparisonAppendix(Map<String, Object> pptMasterSettings) {
        Object explicit = pptMasterSettings.get("includeComparisonAppendix");
        if (explicit instanceof Boolean value) return value;
        if (explicit != null) return Boolean.parseBoolean(Objects.toString(explicit, "false"));
        Object sections = pptMasterSettings.get("sections");
        if (sections instanceof Collection<?> collection) {
            return collection.stream().map(Objects::toString).anyMatch(text -> text.contains("对比文献附录"));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeDeckPayload(Map<String, Object> payload, boolean includeComparisonAppendix) {
        Map<String, Object> sanitized = (Map<String, Object>) sanitizeDeckValue(payload);
        if (!includeComparisonAppendix && sanitized.get("slides") instanceof List<?> slides) {
            List<Object> filtered = slides.stream()
                .filter(item -> !(item instanceof Map<?, ?> map) || !isComparisonDeckSlide(map))
                .collect(Collectors.toList());
            sanitized.put("slides", filtered);
            filterComparisonTextList(sanitized, "agenda");
            filterComparisonTextList(sanitized, "takeaways");
            sanitized.put("subtitle", "围绕上传主论文生成的组会汇报");
        }
        return sanitized;
    }

    private void filterComparisonTextList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof Collection<?> collection)) return;
        List<String> filtered = collection.stream()
            .map(item -> Objects.toString(item, ""))
            .filter(text -> !isComparisonText(text))
            .collect(Collectors.toList());
        payload.put(key, filtered);
    }

    private Object sanitizeDeckValue(Object value) {
        if (value instanceof String text) return cleanDeckText(text);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> cleaned = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                cleaned.put(Objects.toString(entry.getKey(), ""), sanitizeDeckValue(entry.getValue()));
            }
            return cleaned;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(this::sanitizeDeckValue).toList();
        }
        return value;
    }

    private boolean isComparisonDeckSlide(Map<?, ?> slide) {
        String text = Stream.of("eyebrow", "title", "subtitle")
            .map(key -> Objects.toString(slide.get(key), ""))
            .collect(Collectors.joining(" "))
            .toLowerCase(Locale.ROOT);
        return isComparisonText(text);
    }

    private boolean isComparisonText(String text) {
        String compact = Objects.toString(text, "").toLowerCase(Locale.ROOT);
        return compact.contains("对比")
            || compact.contains("矩阵")
            || compact.contains("横向")
            || compact.contains("comparison")
            || compact.contains("reference")
            || compact.contains("matrix")
            || compact.contains("synthesis");
    }

    private String cleanDeckText(String text) {
        if (text == null) return "";
        return cleanMarkdown(text)
            .replace("\\\\rightarrow", "→")
            .replace("\\rightarrow", "→")
            .replace("\\\\leftarrow", "←")
            .replace("\\leftarrow", "←")
            .replace("\\\\Rightarrow", "⇒")
            .replace("\\Rightarrow", "⇒")
            .replaceAll("(?i)\\brightarrow\\b", "→")
            .replaceAll("(?i)\\bleftarrow\\b", "←")
            .replace("\\\\geq", "≥")
            .replace("\\geq", "≥")
            .replace("\\\\leq", "≤")
            .replace("\\leq", "≤")
            .replaceAll("\\$+", "")
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
            .replaceAll("`([^`]+)`", "$1")
            .replaceAll("(?m)^\\s*[-*]\\s+", "")
            .replaceAll("\\s{2,}", " ")
            .trim();
    }

    private String matrixValue(Map<String, Object> matrix, Object paperId, String key, String fallback) {
        Object row = matrix.get(Objects.toString(paperId, ""));
        if (row instanceof Map<?, ?> map) {
            String value = Objects.toString(map.get(key), "").trim();
            if (!value.isBlank()) return value;
        }
        return fallback;
    }

    private Map<String, Object> executePptMasterSkill(Path structurePath, Path pptxPath, Path outputDir) {
        Map<String, Object> response = new LinkedHashMap<>();
        Optional<String> nodePath = resolveCommand("node");
        if (nodePath.isEmpty()) {
            response.put("status", "missing_runtime");
            response.put("generated", false);
            response.put("message", "未检测到 Node.js，无法执行 PPT Master SVG 渲染器。");
            return response;
        }
        Path skillDir = Path.of(Objects.toString(pptMasterSkillDir, "")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(skillDir.resolve("scripts/svg_to_pptx.py"))) {
            response.put("status", "missing_ppt_master_skill");
            response.put("generated", false);
            response.put("message", "未找到已安装的 PPT Master skill，请检查 paperpilot.ppt-master.skill-dir：" + skillDir);
            return response;
        }
        Path renderer = Path.of(System.getProperty("user.dir"), "pptx-renderer", "render-meeting-deck.mjs");
        if (!Files.isRegularFile(renderer)) {
            response.put("status", "missing_renderer");
            response.put("generated", false);
            response.put("message", "未找到 PPT Master SVG 渲染脚本。");
            return response;
        }
        List<String> command = List.of(
            nodePath.get(),
            renderer.toAbsolutePath().toString(),
            structurePath.toAbsolutePath().toString(),
            pptxPath.toAbsolutePath().toString()
        );
        response.put("command", command);
        response.put("commandText", shellJoin(command));
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(outputDir.toFile())
                .redirectErrorStream(true);
            processBuilder.environment().put("PPT_MASTER_SKILL_DIR", skillDir.toString());
            if (StringUtils.hasText(pptMasterPython)) {
                processBuilder.environment().put("PPT_MASTER_PYTHON", pptMasterPython.trim());
            }
            Process process = processBuilder.start();
            boolean finished = process.waitFor(PPTXGEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                response.put("status", "timeout");
                response.put("generated", false);
                response.put("message", "PPT Master skill 渲染超过 2 分钟，已保留结构化 JSON。");
                return response;
            }
            String output = new String(process.getInputStream().readAllBytes());
            response.put("rendererLog", output.length() > 2400 ? output.substring(output.length() - 2400) : output);
            if (process.exitValue() != 0) {
                response.put("status", "failed");
                response.put("generated", false);
                response.put("message", "PPT Master skill 渲染失败：" + compactLog(output));
                return response;
            }
            if (!Files.isRegularFile(pptxPath)) {
                response.put("status", "missing_output");
                response.put("generated", false);
                response.put("message", "PPT Master skill 已执行，但没有找到生成的 PPTX 文件。");
                return response;
            }
            mergeRendererMetadata(response, output);
            response.put("status", "generated");
            response.put("generated", true);
            response.put("renderer", "ppt-master-skill");
            response.put("pptMasterSkillDir", skillDir.toString());
            response.put("pptxPath", pptxPath.toAbsolutePath().toString());
            return response;
        } catch (Exception error) {
            response.put("status", "failed");
            response.put("generated", false);
            response.put("message", "PPT Master skill 渲染失败：" + readableError(error));
            return response;
        }
    }

    private Map<String, Object> createPptMasterAgentHandoff(
        String jobId,
        Path projectDir,
        Path materialPath,
        Path reportPaperPath,
        String slideCount,
        String audience,
        Map<String, Object> pptMasterSettings
    ) throws Exception {
        Path handoffPath = projectDir.resolve("PPT_MASTER_AGENT_HANDOFF.md");
        Path confirmedPath = projectDir.resolve("confirm_ui").resolve("result.json");
        String confirmedJson = Files.isRegularFile(confirmedPath)
            ? Files.readString(confirmedPath)
            : "{}";
        String instructions = """
            # PPT Master Agent Handoff

            这个目录已经完成网页侧准备：

            - 主论文 PDF：`%s`
            - 材料摘要：`%s`
            - 官方参数确认结果：`%s`
            - 目标页数：`%s`
            - 汇报对象：`%s`

            ## 必须走真正 PPT Master skill

            不要再调用 `backend/pptx-renderer/render-meeting-deck.mjs`。
            根据 `/Users/yuan/.codex/skills/ppt-master/SKILL.md`，真正流程必须由 Codex/PPT Master agent 串行执行：

            1. `source_to_md.py` 转换主论文。
            2. `project_manager.py init/import-sources` 创建并导入项目。
            3. 使用本目录 `confirm_ui/result.json` 作为已确认参数。
            4. Strategist 写 `design_spec.md` 和 `spec_lock.md`。
            5. Executor 按页手写 SVG，逐页读取 `spec_lock.md`，不能脚本批量生成。
            6. 启动 live preview，跑 `svg_quality_checker.py`。
            7. 依次执行 `total_md_split.py`、`finalize_svg.py`、`svg_to_pptx.py` 导出 PPTX。

            ## 已确认参数

            ```json
            %s
            ```
            """.formatted(
            reportPaperPath == null ? "" : reportPaperPath.toAbsolutePath(),
            materialPath.toAbsolutePath(),
            confirmedPath.toAbsolutePath(),
            slideCount,
            audience,
            confirmedJson
        );
        Files.writeString(handoffPath, instructions, StandardCharsets.UTF_8);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", jobId);
        response.put("engine", "ppt-master-skill");
        response.put("status", "awaiting_agent");
        response.put("generated", false);
        response.put("paperCount", 0);
        response.put("slideCount", slideCount);
        response.put("audience", audience);
        response.put("projectPath", projectDir.toAbsolutePath().toString());
        response.put("materialPath", materialPath.toAbsolutePath().toString());
        response.put("reportPaperPath", reportPaperPath == null ? "" : reportPaperPath.toAbsolutePath().toString());
        response.put("confirmResultPath", confirmedPath.toAbsolutePath().toString());
        response.put("handoffPath", handoffPath.toAbsolutePath().toString());
        response.put("pptMasterSettings", pptMasterSettings);
        response.put("message", "官方参数已确认，已停止网页老渲染器；请由 Codex/PPT Master agent 接管逐页设计与导出。");
        return response;
    }

    private void executePptMasterAgent(
        DeckJob job,
        Path outputDir,
        Path materialPath,
        Path reportPaperPath,
        Path pptxPath,
        Map<String, Object> handoff
    ) throws Exception {
        ModelConfigEntity modelConfig = modelConfigRepository
            .findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK)
            .orElseThrow(() -> new IllegalStateException("管理员模型池未配置“组会汇报/PPT生成”的活动模型"));
        if (!StringUtils.hasText(modelConfig.getApiKey())) {
            throw new IllegalStateException("管理员模型池的“组会汇报/PPT生成”模型未配置 Key");
        }
        if (!StringUtils.hasText(modelConfig.getBaseUrl())) {
            throw new IllegalStateException("管理员模型池的“组会汇报/PPT生成”模型未配置中转地址");
        }
        String codexExecutable = resolveCodexExecutable();
        Path skillDir = Path.of(Objects.toString(pptMasterSkillDir, "")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(skillDir.resolve("SKILL.md"))) {
            throw new IllegalStateException("未找到 PPT Master skill：" + skillDir.resolve("SKILL.md"));
        }
        String pythonPath = resolvePptMasterPython()
            .orElseThrow(() -> new IllegalStateException("未检测到 Python，无法运行 PPT Master 官方脚本"));

        Path agentProjectDir = outputDir.resolve("ppt-master-agent-project");
        Path promptPath = outputDir.resolve("ppt-master-agent-prompt.md");
        Path logPath = outputDir.resolve("ppt-master-agent.log");
        Path lastMessagePath = outputDir.resolve("ppt-master-agent-final.txt");
        Files.createDirectories(agentProjectDir);
        String agentPrompt = buildPptMasterAgentPrompt(
            outputDir,
            agentProjectDir,
            skillDir,
            materialPath,
            reportPaperPath,
            pptxPath,
            pythonPath,
            handoff
        );
        Files.writeString(promptPath, agentPrompt, StandardCharsets.UTF_8);

        job.result().put("engine", "ppt-master-skill-agent");
        job.result().put("agentProjectPath", agentProjectDir.toAbsolutePath().toString());
        job.result().put("agentPromptPath", promptPath.toAbsolutePath().toString());
        job.result().put("agentLogPath", logPath.toAbsolutePath().toString());
        job.result().put("modelProvider", modelConfig.getProviderName());
        job.result().put("modelName", modelConfig.getModelName());
        job.progress(36, "正在启动 PPT Master 多轮 Agent，使用管理员组会汇报模型：" + modelConfig.getModelName());

        validateCodexResponsesModel(modelConfig);
        String providerBaseUrl = cleanCodexProviderBaseUrl(modelConfig.getBaseUrl());
        List<String> command = new ArrayList<>(List.of(
            codexExecutable,
            "exec",
            "--ignore-user-config",
            "--ephemeral",
            "--skip-git-repo-check",
            "--dangerously-bypass-approvals-and-sandbox",
            "--color", "never",
            "-C", agentProjectDir.toAbsolutePath().toString(),
            "--add-dir", outputDir.toAbsolutePath().toString(),
            "--add-dir", skillDir.toAbsolutePath().toString(),
            "-m", modelConfig.getModelName(),
            "-c", "model_provider=\"paperpilot_relay\"",
            "-c", "model_providers.paperpilot_relay.name=\"PaperPilot Relay\"",
            "-c", "model_providers.paperpilot_relay.base_url=" + tomlString(providerBaseUrl),
            "-c", "model_providers.paperpilot_relay.env_key=\"OPENAI_API_KEY\"",
            "-c", "model_providers.paperpilot_relay.wire_api=\"responses\"",
            "-c", "model_providers.paperpilot_relay.requires_openai_auth=true",
            "-o", lastMessagePath.toAbsolutePath().toString(),
            agentPrompt
        ));
        ProcessBuilder processBuilder = new ProcessBuilder(command)
            .directory(agentProjectDir.toFile())
            .redirectErrorStream(true)
            .redirectOutput(logPath.toFile());
        processBuilder.environment().put("OPENAI_API_KEY", modelConfig.getApiKey().trim());
        processBuilder.environment().put("CODEX_HOME", "/Users/yuan/.codex");
        processBuilder.environment().put("PPT_MASTER_PYTHON", pythonPath);
        Process process = processBuilder.start();
        process.getOutputStream().close();

        long startedAt = System.currentTimeMillis();
        long timeoutMillis = TimeUnit.MINUTES.toMillis(Math.max(30, pptMasterAgentTimeoutMinutes));
        int[] progressPoints = {40, 48, 56, 64, 72, 80, 88, 92};
        String[] messages = {
            "Agent 正在精读 PDF 并提取论文主线",
            "Agent 正在生成叙事策略与页面设计规范",
            "Agent 正在逐页设计 SVG 页面",
            "Agent 正在补充图表、机制图和视觉层级",
            "Agent 正在执行页面预览与质量检查",
            "Agent 正在修复质检问题并整理导出文件",
            "Agent 正在导出 PPTX",
            "正在校验生成结果"
        };
        int progressIndex = 0;
        while (true) {
            if (Files.isRegularFile(pptxPath) && Files.size(pptxPath) > 0) break;
            if (process.waitFor(8, TimeUnit.SECONDS)) break;
            long elapsed = System.currentTimeMillis() - startedAt;
            int expectedIndex = (int) Math.min(progressPoints.length - 1, elapsed / Math.max(1, timeoutMillis / progressPoints.length));
            while (progressIndex <= expectedIndex && progressIndex < progressPoints.length) {
                job.progress(progressPoints[progressIndex], messages[progressIndex]);
                progressIndex++;
            }
            if (elapsed > timeoutMillis) {
                process.destroyForcibly();
                throw new IllegalStateException(buildAgentTimeoutMessage(outputDir, logPath, timeoutMillis));
            }
        }
        if (process.isAlive()) process.waitFor(5, TimeUnit.SECONDS);
        recordPptMasterAgentUsage(job, modelConfig, agentPrompt, materialPath, logPath, lastMessagePath);
        job.progress(96, "PPT Master Agent 已结束，正在定位并校验 PPTX 文件");

        Path generated = locateGeneratedPptx(outputDir, pptxPath);
        if (generated == null || !Files.isRegularFile(generated) || Files.size(generated) == 0) {
            String logTail = compactLog(readTail(logPath, 1800));
            throw new IllegalStateException(StringUtils.hasText(logTail) ? logTail : "Agent 未生成可下载 PPTX");
        }
        if (!generated.toAbsolutePath().normalize().equals(pptxPath.toAbsolutePath().normalize())) {
            Files.copy(generated, pptxPath, StandardCopyOption.REPLACE_EXISTING);
        }
        Map<String, Object> response = new LinkedHashMap<>(handoff);
        response.put("status", "generated");
        response.put("generated", true);
        response.put("engine", "ppt-master-skill-agent");
        response.put("pptxPath", pptxPath.toAbsolutePath().toString());
        response.put("agentProjectPath", agentProjectDir.toAbsolutePath().toString());
        response.put("agentPromptPath", promptPath.toAbsolutePath().toString());
        response.put("agentLogPath", logPath.toAbsolutePath().toString());
        response.put("agentFinalMessagePath", lastMessagePath.toAbsolutePath().toString());
        response.put("modelProvider", modelConfig.getProviderName());
        response.put("modelName", modelConfig.getModelName());
        response.put("message", "PPT Master 多轮 Agent 已生成 PPTX");
        job.complete(response);
    }

    private String buildPptMasterAgentPrompt(
        Path outputDir,
        Path agentProjectDir,
        Path skillDir,
        Path materialPath,
        Path reportPaperPath,
        Path pptxPath,
        String pythonPath,
        Map<String, Object> handoff
    ) throws Exception {
        String handoffJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(handoff);
        return """
            你正在 PaperPilot 网页后端中作为 PPT Master 生成 Agent 执行任务，不是聊天演示。

            必须完成：读取论文 PDF，按官方 `ppt-master` skill 流程生成高质量、可编辑 PPTX，并把最终文件写到：
            `%s`

            关键路径：
            - PPT Master skill：`%s`
            - 工作目录：`%s`
            - 主论文 PDF：`%s`
            - 网页整理材料：`%s`
            - 任务目录：`%s`
            - Python：`%s`

            强制要求：
            1. 先完整阅读 `%s/SKILL.md`，并按其中 Source → Project → Confirm UI → Strategist → Executor → Quality check → Export 的流程执行。
            2. Confirm UI 已在网页端完成，确认结果在 handoff 的 `confirmResultPath`；不要再打开交互网页，不要等待用户输入。
            3. 严禁调用 PaperPilot 旧渲染器，例如 `backend/pptx-renderer/render-meeting-deck.mjs`。
            4. 必须真正精读 PDF，提炼论文核心问题、方法、证据、贡献、局限和组会讨论点；不要生成泛泛文字堆叠。
            5. 页面必须有设计：封面、目录/路线、背景、方法、结果/证据、贡献、局限、讨论、结论应有不同版式；优先使用论文图表/机制图/流程图/表格/时间线/对比矩阵/证据卡等结构。
            6. Executor 阶段逐页手写 SVG，不允许用简单模板批量堆文字。
            7. 运行官方质检与导出脚本，至少使用 `svg_quality_checker.py`、`total_md_split.py`、`finalize_svg.py`、`svg_to_pptx.py`；如脚本需要 Python，使用上面的 Python 路径。
            8. 如果中途某个辅助资源不可用，继续用本地 SVG/PPTX 工具完成，不要回退到旧版简单 PPT。
            9. 结束前确认 `%s` 存在且大小大于 0。

            Handoff JSON：
            ```json
            %s
            ```

            现在开始执行。最终回复只需要说明 PPTX 是否生成以及关键文件路径。
            """.formatted(
            pptxPath.toAbsolutePath(),
            skillDir.toAbsolutePath(),
            agentProjectDir.toAbsolutePath(),
            reportPaperPath == null ? "" : reportPaperPath.toAbsolutePath(),
            materialPath.toAbsolutePath(),
            outputDir.toAbsolutePath(),
            pythonPath,
            skillDir.toAbsolutePath(),
            pptxPath.toAbsolutePath(),
            handoffJson
        );
    }

    private String resolveCodexExecutable() {
        Path configured = Path.of(Objects.toString(pptMasterCodex, "")).toAbsolutePath().normalize();
        if (Files.isExecutable(configured)) return configured.toString();
        Path bundled = Path.of("/Applications/Codex.app/Contents/Resources/codex");
        if (Files.isExecutable(bundled)) return bundled.toString();
        return "codex";
    }

    private String cleanCodexProviderBaseUrl(String baseUrl) {
        String clean = Objects.toString(baseUrl, "").trim();
        clean = clean.replaceFirst("/(?:v1/)?(?:chat/completions|responses)$", "");
        return clean.replaceAll("/+$", "");
    }

    private void recordPptMasterAgentUsage(
        DeckJob job,
        ModelConfigEntity modelConfig,
        String agentPrompt,
        Path materialPath,
        Path logPath,
        Path lastMessagePath
    ) {
        ensurePptUsageRecorded(job, modelConfig, agentPrompt, materialPath, logPath, lastMessagePath);
    }

    private void ensurePptUsageRecorded(DeckJob job) {
        if (job == null || !"generated".equals(job.status())) return;
        Path outputDir = Path.of(System.getProperty("user.dir"), "ppt-master-jobs", job.jobId());
        ModelConfigEntity modelConfig = modelConfigRepository
            .findFirstBySceneAndActiveTrueOrderByUpdatedAtDesc(ModelConfigService.SCENE_MEETING_DECK)
            .orElse(null);
        ensurePptUsageRecorded(
            job,
            modelConfig,
            readFileIfSmall(outputDir.resolve("ppt-master-agent-prompt.md"), 60000),
            outputDir.resolve("meeting-report-input.md"),
            outputDir.resolve("ppt-master-agent.log"),
            outputDir.resolve("ppt-master-agent-final.txt")
        );
    }

    private void ensurePptUsageRecorded(
        DeckJob job,
        ModelConfigEntity modelConfig,
        String agentPrompt,
        Path materialPath,
        Path logPath,
        Path lastMessagePath
    ) {
        if (job == null || !job.markUsageRecording()) return;
        try {
            String material = readFileIfSmall(materialPath, 24000);
            String finalMessage = readFileIfSmall(lastMessagePath, 12000);
            String logTail = readTail(logPath, 16000);
            long promptTokens = estimateTokens(agentPrompt) + estimateTokens(material);
            long completionTokens = estimateTokens(finalMessage) + Math.max(0L, estimateTokens(logTail) / 3L);
            long loggedTotalTokens = parseLoggedTokenUsage(logTail);
            long totalTokens = Math.max(1L, Math.max(loggedTotalTokens, promptTokens + completionTokens));
            if (loggedTotalTokens > 0) {
                promptTokens = Math.max(1L, Math.round(totalTokens * 0.65D));
                completionTokens = Math.max(1L, totalTokens - promptTokens);
            }
            String modelName = modelConfig == null
                ? Objects.toString(job.result().getOrDefault("modelName", "gpt-5.5"), "gpt-5.5")
                : modelConfig.getModelName();
            aiUsageService.recordAndCharge(
                job.userId(),
                modelName,
                "report",
                "组会PPT Agent执行",
                job.paperTitle(),
                promptTokens,
                completionTokens,
                totalTokens
            );
            job.result().put("usageAccounting", "estimated");
            job.result().put("usagePromptTokens", promptTokens);
            job.result().put("usageCompletionTokens", completionTokens);
            job.result().put("usageTotalTokens", totalTokens);
        } catch (Exception error) {
            job.unmarkUsageRecording();
            job.result().put("usageAccountingError", readableError(error));
            // PPT generation result should not fail because accounting failed.
        }
    }

    private long parseLoggedTokenUsage(String logTail) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("tokens used\\s*\\R\\s*([0-9][0-9,]*)", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(Objects.toString(logTail, ""));
        long last = 0L;
        while (matcher.find()) {
            try {
                last = Long.parseLong(matcher.group(1).replace(",", ""));
            } catch (Exception ignored) {
                // Keep scanning; malformed tool output should not block accounting.
            }
        }
        return last;
    }

    private String readFileIfSmall(Path path, int maxChars) {
        try {
            if (path == null || !Files.isRegularFile(path)) return "";
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return text.length() > maxChars ? text.substring(0, maxChars) : text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private long estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0L;
        long cjk = 0L;
        long compactChars = 0L;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint)) continue;
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL) {
                cjk++;
            } else {
                compactChars++;
            }
        }
        return Math.max(1L, cjk + (long) Math.ceil(compactChars / 4.0));
    }

    private void validateCodexResponsesModel(ModelConfigEntity modelConfig) {
        String apiFormat = Objects.toString(modelConfig.getApiFormat(), "openai_chat").trim();
        String baseUrl = Objects.toString(modelConfig.getBaseUrl(), "").trim();
        String provider = Objects.toString(modelConfig.getProviderName(), "当前模型").trim();
        String model = Objects.toString(modelConfig.getModelName(), "").trim();
        String source = (provider + " " + model + " " + baseUrl).toLowerCase(Locale.ROOT);
        if (source.contains("deepseek") || source.contains("api.deepseek.com")) {
            throw new IllegalStateException(
                "组会 PPT 的 PPT Master Agent 不能使用 DeepSeek 官方 API；"
                    + "DeepSeek 只兼容 Chat Completions，不支持 Codex Agent 需要的 Responses 协议。"
                    + "请在管理员模型池的“组会汇报/PPT生成”单独配置支持 /responses 的 GPT-5.5 中转路由。"
            );
        }
        if ("openai_responses".equalsIgnoreCase(apiFormat) || baseUrl.matches(".*/(?:v1/)?responses/?$")) return;
        throw new IllegalStateException(
            "组会 PPT 的 PPT Master Agent 需要支持 OpenAI Responses 协议的模型路由；"
                + provider
                + (StringUtils.hasText(model) ? " / " + model : "")
                + " 当前是 Chat Completions 协议，不能用于 Codex Agent。"
                + "请在管理员模型池的“组会汇报/PPT生成”单独配置支持 /responses 的中转 GPT-5.5 路由，"
                + "不要填 DeepSeek 官方 https://api.deepseek.com。"
        );
    }

    private String tomlString(String value) {
        return "\"" + Objects.toString(value, "").replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Path locateGeneratedPptx(Path outputDir, Path expectedPath) throws Exception {
        if (Files.isRegularFile(expectedPath) && Files.size(expectedPath) > 0) return expectedPath;
        try (Stream<Path> paths = Files.walk(outputDir)) {
            return paths
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pptx"))
                .filter(path -> {
                    try {
                        return Files.isRegularFile(path) && Files.size(path) > 0;
                    } catch (Exception ignored) {
                        return false;
                    }
                })
                .max(Comparator.comparingLong(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis();
                    } catch (Exception ignored) {
                        return 0L;
                    }
                }))
                .orElse(null);
        }
    }

    private String buildAgentTimeoutMessage(Path outputDir, Path logPath, long timeoutMillis) {
        long minutes = Math.max(1, TimeUnit.MILLISECONDS.toMinutes(timeoutMillis));
        String pageHint = generatedSvgPageHint(outputDir);
        String logHint = meaningfulAgentLogTail(logPath);
        StringBuilder message = new StringBuilder("PPT Master Agent 已运行超过 ")
            .append(minutes)
            .append(" 分钟，已停止本次任务以避免后台无限占用");
        if (StringUtils.hasText(pageHint)) {
            message.append("；当前进度：").append(pageHint);
        }
        if (StringUtils.hasText(logHint)) {
            message.append("；最后状态：").append(logHint);
        }
        message.append("。请重新点击生成，或把 PPT_MASTER_AGENT_TIMEOUT_MINUTES 调大后再试。");
        return message.toString();
    }

    private String generatedSvgPageHint(Path outputDir) {
        try (Stream<Path> paths = Files.walk(outputDir)) {
            long count = paths
                .filter(path -> path.getParent() != null && path.getParent().getFileName().toString().equals("svg_output"))
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".svg"))
                .count();
            return count > 0 ? "已写出 " + count + " 页 SVG" : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    private String meaningfulAgentLogTail(Path logPath) {
        String tail = readTail(logPath, 4000);
        if (!StringUtils.hasText(tail)) return "";
        List<String> lines = Arrays.stream(tail.split("\\R"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .filter(line -> !line.startsWith("+"))
            .filter(line -> !line.startsWith("-"))
            .filter(line -> !line.startsWith("@@"))
            .filter(line -> !line.startsWith("diff --git"))
            .filter(line -> !line.startsWith("index "))
            .filter(line -> !line.startsWith("new file mode"))
            .filter(line -> !line.startsWith("--- "))
            .filter(line -> !line.startsWith("+++ "))
            .toList();
        if (lines.isEmpty()) return "";
        String last = lines.get(lines.size() - 1).replaceAll("\\s+", " ").trim();
        return shorten(last, 180);
    }

    private String readTail(Path path, int maxChars) {
        try {
            if (!Files.isRegularFile(path)) return "";
            String text = Files.readString(path, StandardCharsets.UTF_8);
            return text.length() > maxChars ? text.substring(text.length() - maxChars) : text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private Map<String, Object> runPptMasterConfirmUi(
        DeckJob job,
        Path projectDir,
        Path materialPath,
        Path reportPaperPath,
        String slideCount,
        String audience
    ) {
        Path skillDir = Path.of(Objects.toString(pptMasterSkillDir, "")).toAbsolutePath().normalize();
        Path confirmServer = skillDir.resolve("scripts/confirm_ui/server.py");
        if (!Files.isRegularFile(confirmServer)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "未找到 PPT Master 官方参数确认页脚本：" + confirmServer);
        }
        Optional<String> pythonPath = resolvePptMasterPython();
        if (pythonPath.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "未检测到 Python，无法启动 PPT Master 参数确认页");
        }
        try {
            Path confirmDir = projectDir.resolve("confirm_ui");
            Files.createDirectories(confirmDir);
            Map<String, Object> recommendations = buildConfirmRecommendations(materialPath, reportPaperPath, slideCount, audience);
            Files.writeString(
                confirmDir.resolve("recommendations.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(recommendations)
            );
            List<String> command = List.of(
                pythonPath.get(),
                confirmServer.toAbsolutePath().toString(),
                projectDir.toAbsolutePath().toString(),
                "--daemon",
                "--no-browser"
            );
            Process launch = new ProcessBuilder(command)
                .directory(skillDir.toFile())
                .redirectErrorStream(true)
                .start();
            boolean launched = launch.waitFor(10, TimeUnit.SECONDS);
            String output = new String(launch.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!launched || launch.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PPT Master 参数确认页启动失败：" + compactLog(output));
            }
            String confirmUrl = firstUrl(output);
            if (confirmUrl.isBlank()) {
                confirmUrl = "http://127.0.0.1:5050";
            }
            job.result().put("confirmUrl", confirmUrl);
            job.result().put("confirmProjectPath", projectDir.toAbsolutePath().toString());
            job.progress(24, "已打开 PPT Master 官方参数确认页，请完成确认后继续生成");

            Path resultPath = confirmDir.resolve("result.json");
            long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(18);
            while (System.currentTimeMillis() < deadline) {
                if (Files.isRegularFile(resultPath)) {
                    Map<String, Object> result = objectMapper.readValue(Files.readString(resultPath), new TypeReference<>() {});
                    String status = Objects.toString(result.get("status"), "");
                    String stage = Objects.toString(result.get("stage"), "final");
                    if ("confirmed".equals(status) && ("final".equals(stage) || stage.isBlank())) {
                        job.result().put("confirmResultPath", resultPath.toAbsolutePath().toString());
                        job.progress(28, "PPT Master 参数已确认，正在进入论文精读与设计流程");
                        shutdownPptMasterConfirmUi(projectDir, skillDir, pythonPath.get());
                        return result;
                    }
                }
                Thread.sleep(1000);
            }
            shutdownPptMasterConfirmUi(projectDir, skillDir, pythonPath.get());
            Map<String, Object> fallback = buildDefaultConfirmResult(recommendations, "参数页未在等待时间内确认，已按 PPT Master 推荐参数自动继续。");
            Files.writeString(resultPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fallback), StandardCharsets.UTF_8);
            job.result().put("confirmResultPath", resultPath.toAbsolutePath().toString());
            job.result().put("confirmFallback", true);
            job.progress(28, "参数页未确认，已按推荐参数自动继续生成");
            return fallback;
        } catch (ResponseStatusException error) {
            throw error;
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "PPT Master 参数确认页异常：" + readableError(error));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildDefaultConfirmResult(Map<String, Object> recommendations, String note) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> recommend = (Map<String, Object>) recommendations.getOrDefault("recommend", Map.of());
        result.put("canvas", recommend.getOrDefault("canvas", "ppt169"));
        result.put("audience", nestedValue(recommendations.get("audience"), "导师与课题组"));
        result.put("content_divergence", nestedValue(recommendations.get("content_divergence"), "忠实论文事实，但允许按组会汇报逻辑重组叙事。"));
        result.put("mode", recommend.getOrDefault("mode", "pyramid"));
        result.put("visual_style", recommend.getOrDefault("visual_style", "editorial"));
        result.put("delivery_purpose", recommend.getOrDefault("delivery_purpose", "balanced"));
        result.put("page_count", nestedValue(recommendations.get("page_count"), "10-12"));
        result.put("color", selectedCandidate(recommendations.get("color"), Map.of("name", "深海学术蓝")));
        result.put("icons", recommend.getOrDefault("icons", "tabler-outline"));
        result.put("typography", selectedCandidate(recommendations.get("typography"), Map.of("name", "思源黑体学术版")));
        result.put("formula_policy", recommend.getOrDefault("formula_policy", "mixed"));
        result.put("image_usage", recommend.getOrDefault("image_usage", List.of("provided", "ai")));
        result.put("image_notes", nestedValue(recommendations.get("image_notes"), "优先使用论文 PDF 中的图、表、公式和流程图。"));
        result.put("image_ai_path", recommend.getOrDefault("image_ai_path", "auto"));
        result.put("generation_mode", recommend.getOrDefault("generation_mode", "continuous"));
        result.put("refine_spec", nestedBoolean(recommendations.get("refine_spec"), false));
        result.put("image_strategy", selectedCandidate(recommendations.get("image_strategy"), Map.of("name", "论文资产优先")));
        result.put("stage", "final");
        result.put("status", "confirmed");
        result.put("auto_confirmed", true);
        result.put("note", note);
        result.put("confirmed_at", java.time.LocalDateTime.now().toString());
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object selectedCandidate(Object section, Object fallback) {
        if (!(section instanceof Map<?, ?> raw)) return fallback;
        Map<String, Object> map = (Map<String, Object>) raw;
        Object candidates = map.get("candidates");
        int selected = Number.class.isInstance(map.get("selected")) ? ((Number) map.get("selected")).intValue() : 0;
        if (candidates instanceof List<?> list && !list.isEmpty()) {
            return list.get(Math.max(0, Math.min(selected, list.size() - 1)));
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private Object nestedValue(Object section, Object fallback) {
        if (section instanceof Map<?, ?> raw) {
            Object value = ((Map<String, Object>) raw).get("value");
            return value == null ? fallback : value;
        }
        return section == null ? fallback : section;
    }

    @SuppressWarnings("unchecked")
    private boolean nestedBoolean(Object section, boolean fallback) {
        Object value = section instanceof Map<?, ?> raw ? ((Map<String, Object>) raw).get("value") : section;
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private Map<String, Object> buildConfirmRecommendations(Path materialPath, Path reportPaperPath, String slideCount, String audience) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("lang", "zh");
        root.put("source", Map.of(
            "material_path", materialPath.toAbsolutePath().toString(),
            "report_paper_path", reportPaperPath == null ? "" : reportPaperPath.toAbsolutePath().toString()
        ));
        Map<String, Object> recommend = new LinkedHashMap<>();
        recommend.put("canvas", "ppt169");
        recommend.put("mode", "pyramid");
        recommend.put("visual_style", "editorial");
        recommend.put("icons", "tabler-outline");
        recommend.put("image_usage", List.of("provided", "ai"));
        recommend.put("image_ai_path", "auto");
        recommend.put("formula_policy", "mixed");
        recommend.put("generation_mode", "continuous");
        recommend.put("delivery_purpose", "balanced");
        root.put("recommend", recommend);
        root.put("page_count", Map.of("value", StringUtils.hasText(slideCount) ? slideCount : "10-12"));
        root.put("audience", Map.of("value", StringUtils.hasText(audience) ? audience : "导师与课题组"));
        root.put("content_divergence", Map.of("value", "忠实论文事实，但允许按组会汇报逻辑重组叙事，突出研究问题、方法、证据、贡献和可讨论局限。"));
        root.put("image_notes", Map.of("value", "优先使用论文 PDF 中的图、表、公式和流程图；封面、章节过渡或抽象机制页可使用 AI 生成学术风格辅助图像；不要用无关装饰图。"));
        root.put("color", Map.of(
            "selected", 0,
            "candidates", List.of(
                colorCandidate("深海学术蓝", "稳重、克制，适合论文精读和导师组会。", "#F7FAFC", "#EEF4FA", "#123A63", "#0EA5A4", "#7C3AED", "#152033"),
                colorCandidate("墨绿研究室", "更像研究机构报告，强调方法链条和证据感。", "#FAFBF8", "#EEF6EF", "#174C43", "#D97706", "#2563EB", "#17211F"),
                colorCandidate("黑白编辑部", "更接近高级期刊专题，适合理论和概念密集论文。", "#FFFFFF", "#F2F4F7", "#111827", "#2563EB", "#10B981", "#111827")
            )
        ));
        root.put("typography", Map.of(
            "selected", 0,
            "candidates", List.of(
                typographyCandidate("思源黑体学术版", "清晰稳妥，中文论文汇报优先。", "论文核心问题", "Research Question", "方法、证据与贡献链条", "Method, evidence and contribution", "Source Han Sans SC", "Inter", 24),
                typographyCandidate("霞鹜文楷标题版", "标题更有讲述感，正文保持清晰。", "研究背景与方法路径", "Background and Method", "从问题到验证的叙事", "From problem to validation", "LXGW WenKai", "Aptos", 24),
                typographyCandidate("苹方现代版", "更像现代产品研究汇报，页面更轻。", "关键结果与讨论", "Results and Discussion", "结论、局限与启发", "Findings, limits and insights", "PingFang SC", "Inter", 24)
            )
        ));
        root.put("image_strategy", Map.of(
            "selected", 0,
            "candidates", List.of(
                imageStrategy("论文资产优先", "paper-native", "cool-academic", "保留论文图表和公式，少量 AI 背景辅助。", "浅底、蓝绿强调、低饱和", "严谨、可信、可讲解"),
                imageStrategy("机制图重绘", "vector-diagram", "research-green", "将方法流程和变量关系重绘成矢量机制图。", "白底、墨绿主线、橙色强调", "清楚、结构化"),
                imageStrategy("编辑部专题", "editorial-abstract", "mono-accent", "用抽象几何和章节大标题增强节奏。", "黑白灰为主、单一亮色点题", "高级、克制")
            )
        ));
        root.put("refine_spec", Map.of("value", false));
        return root;
    }

    private Map<String, Object> colorCandidate(String name, String note, String background, String secondaryBg, String primary, String accent, String secondaryAccent, String bodyText) {
        return Map.of(
            "name", name,
            "note", note,
            "palette", Map.of(
                "background", background,
                "secondary_bg", secondaryBg,
                "primary", primary,
                "accent", accent,
                "secondary_accent", secondaryAccent,
                "body_text", bodyText
            )
        );
    }

    private Map<String, Object> typographyCandidate(
        String name,
        String note,
        String sampleHeading,
        String sampleHeadingLatin,
        String sampleBody,
        String sampleBodyLatin,
        String cjk,
        String latin,
        int bodySize
    ) {
        String css = "'" + cjk + "','" + latin + "',sans-serif";
        return Map.of(
            "name", name,
            "note", note,
            "sample_heading", sampleHeading,
            "sample_heading_latin", sampleHeadingLatin,
            "sample_body", sampleBody,
            "sample_body_latin", sampleBodyLatin,
            "heading", Map.of("cjk", cjk, "latin", latin, "css", css),
            "body", Map.of("cjk", cjk, "latin", latin, "css", css),
            "body_size", bodySize,
            "sizes", Map.of("title", 42, "subtitle", 30, "annotation", 18)
        );
    }

    private Map<String, Object> imageStrategy(String name, String rendering, String palette, String visual, String color, String mood) {
        return Map.of(
            "name", name,
            "rendering", rendering,
            "palette", palette,
            "visual", visual,
            "color", color,
            "mood", mood
        );
    }

    private Optional<String> resolvePptMasterPython() {
        if (StringUtils.hasText(pptMasterPython)) return Optional.of(pptMasterPython.trim());
        Optional<String> python3 = resolveCommand("python3");
        return python3.isPresent() ? python3 : resolveCommand("python");
    }

    private void shutdownPptMasterConfirmUi(Path projectDir, Path skillDir, String pythonPath) {
        try {
            new ProcessBuilder(
                pythonPath,
                skillDir.resolve("scripts/confirm_ui/server.py").toAbsolutePath().toString(),
                projectDir.toAbsolutePath().toString(),
                "--shutdown"
            )
                .directory(skillDir.toFile())
                .redirectErrorStream(true)
                .start()
                .waitFor(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private String firstUrl(String output) {
        if (output == null || output.isBlank()) return "";
        for (String token : output.split("\\s+")) {
            String clean = token.replaceAll("[,.;)\\]]+$", "");
            if (clean.startsWith("http://") || clean.startsWith("https://")) return clean;
        }
        return "";
    }

    private void mergeRendererMetadata(Map<String, Object> response, String output) {
        if (output == null || output.isBlank()) return;
        int start = output.lastIndexOf('{');
        int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) return;
        try {
            Map<String, Object> parsed = objectMapper.readValue(output.substring(start, end + 1), new TypeReference<>() {});
            mergeIfPresent(response, parsed, "renderer");
            mergeIfPresent(response, parsed, "projectDir");
            mergeIfPresent(response, parsed, "slideCount");
            mergeIfPresent(response, parsed, "previewPath");
            mergeIfPresent(response, parsed, "qualityLogPath");
            mergeIfPresent(response, parsed, "qualitySummaryPath");
        } catch (Exception ignored) {
        }
    }

    private List<Map<String, Object>> readDeckDimensions(Object value) {
        if (!(value instanceof List<?> rows)) return List.of();
        List<Map<String, Object>> dimensions = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map) {
                String key = Objects.toString(map.get("key"), "").trim();
                String label = Objects.toString(map.get("label"), "").trim();
                String hint = Objects.toString(map.get("hint"), "").trim();
                if (key.isBlank() && label.isBlank()) continue;
                Map<String, Object> dimension = new LinkedHashMap<>();
                dimension.put("key", key.isBlank() ? label : key);
                dimension.put("label", label.isBlank() ? key : label);
                dimension.put("hint", hint);
                dimensions.add(dimension);
            }
        }
        return dimensions;
    }

    private Map<String, Object> normalizeMatrix(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> row) {
                    Map<String, Object> normalizedRow = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> cell : row.entrySet()) {
                        normalizedRow.put(Objects.toString(cell.getKey(), ""), Objects.toString(cell.getValue(), ""));
                    }
                    result.put(Objects.toString(entry.getKey(), ""), normalizedRow);
                }
            }
            return result;
        }
        return Map.of();
    }

    private String extractBestStructuredText(PaperEntity paper) {
        Optional<String> mineruText = extractMineruText(paper.getWorkspaceId());
        return mineruText.filter(text -> text.length() > 400).orElseGet(() -> extractPaperText(paper));
    }

    private Optional<String> extractMineruText(String workspaceId) {
        Path root = Path.of("mineru-output").resolve(workspaceId);
        if (!Files.isDirectory(root)) return Optional.empty();
        try (var paths = Files.walk(root)) {
            Optional<Path> contentList = paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("_content_list.json")
                    || path.getFileName().toString().equals("content_list.json"))
                .findFirst();
            if (contentList.isEmpty()) return Optional.empty();
            List<Map<String, Object>> raw = objectMapper.readValue(Files.readString(contentList.get()), new TypeReference<>() {});
            StringBuilder builder = new StringBuilder();
            for (Map<String, Object> item : raw) {
                String type = Objects.toString(item.get("type"), "").toLowerCase(Locale.ROOT);
                if (type.equals("header") || type.equals("footer") || type.equals("page_number")) continue;
                String text = Objects.toString(item.getOrDefault("text", item.getOrDefault("table_body", "")), "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
                if (text.length() < 12) continue;
                builder.append(text).append("\n");
                if (builder.length() > 16000) break;
            }
            String text = builder.toString().trim();
            return text.isBlank() ? Optional.empty() : Optional.of(text);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String extractUploadedReportPaperText(Path reportPaperPath) {
        if (reportPaperPath == null || !Files.isRegularFile(reportPaperPath)) return "";
        String filename = reportPaperPath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".pdf")) {
            return "用户已上传汇报论文：" + reportPaperPath.getFileName();
        }
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(reportPaperPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document)
                .replaceAll("-\\s*\\R\\s*", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\R{3,}", "\n\n")
                .trim();
        } catch (Exception error) {
            return "用户已上传汇报论文，但正文提取失败：" + reportPaperPath.getFileName();
        }
    }

    private Map<String, Object> buildPrimaryReportPaper(Path reportPaperPath, String reportPaperText) {
        Map<String, Object> primary = new LinkedHashMap<>();
        String fileName = reportPaperPath == null ? "" : reportPaperPath.getFileName().toString();
        String title = detectReportPaperTitle(reportPaperText, fileName);
        primary.put("title", title);
        primary.put("shortTitle", shortTitle(title));
        primary.put("fileName", fileName);
        primary.put("textPreview", reportPaperText == null || reportPaperText.isBlank()
            ? "未提取到上传论文正文"
            : reportPaperText.substring(0, Math.min(1200, reportPaperText.length())));
        primary.put("role", "primary_report_paper");
        return primary;
    }

    private String detectReportPaperTitle(String text, String fileName) {
        if (text != null && !text.isBlank()) {
            String[] lines = text.split("\\R");
            List<String> candidates = new ArrayList<>();
            for (String line : lines) {
                String clean = cleanMarkdown(line).replaceAll("\\s+", " ").trim();
                if (clean.length() < 12 || clean.length() > 180) continue;
                String lower = clean.toLowerCase(Locale.ROOT);
                if (lower.startsWith("abstract") || lower.startsWith("keywords") || lower.startsWith("introduction")) break;
                if (clean.matches(".*\\d{4}.*") && clean.length() < 28) continue;
                candidates.add(clean);
                if (candidates.size() >= 3) break;
            }
            if (!candidates.isEmpty()) return candidates.get(0);
        }
        if (fileName != null && !fileName.isBlank()) {
            return fileName.replaceFirst("(?i)\\.pdf$", "")
                .replaceFirst("^report-paper-", "")
                .replace('-', ' ')
                .replace('_', ' ')
                .trim();
        }
        return "汇报主论文";
    }

    private String shortTitle(String title) {
        String clean = cleanMarkdown(Optional.ofNullable(title).orElse("组会论文"));
        if (clean.length() <= 42) return clean;
        return clean.substring(0, 42) + "…";
    }

    private Optional<String> resolveCommand(String command) {
        List<String> candidates = new ArrayList<>();
        candidates.add(command);
        String home = System.getProperty("user.home", "");
        if (!home.isBlank()) {
            candidates.add(home + "/Library/Python/3.9/bin/" + command);
            candidates.add(home + "/Library/Python/3.10/bin/" + command);
            candidates.add(home + "/Library/Python/3.11/bin/" + command);
            candidates.add(home + "/Library/Python/3.12/bin/" + command);
            candidates.add(home + "/.local/bin/" + command);
        }
        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (candidate.contains("/") && Files.isExecutable(path)) return Optional.of(candidate);
        }
        try {
            Process process = new ProcessBuilder("sh", "-lc", "command -v " + command)
                .redirectErrorStream(true)
                .start();
            if (process.waitFor(3, TimeUnit.SECONDS) && process.exitValue() == 0) {
                String path = new String(process.getInputStream().readAllBytes()).trim();
                if (!path.isBlank()) return Optional.of(path);
            }
        } catch (Exception error) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private String compactLog(String output) {
        String text = Optional.ofNullable(output).orElse("").replaceAll("\\s+", " ").trim();
        if (text.isBlank()) return "未返回错误日志";
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("free-models-per-day")) {
            return "OpenRouter 免费模型今日额度已用完。请在模型中心切换到可用付费 Key/模型，或明天额度重置后再生成。";
        }
        if (lower.contains("free-models-per-min") || lower.contains("rate limit exceeded") || lower.contains("error code: 429")) {
            return "OpenRouter 免费模型触发限流。请稍后重试，或在模型中心切换到可用付费 Key/模型。";
        }
        return text.length() > 180 ? text.substring(text.length() - 180) : text;
    }

    private List<String> readDeckWorkspaceIds(Object rawPaperIds) {
        if (!(rawPaperIds instanceof List<?> paperIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择 3-5 篇论文");
        }
        return paperIds.stream()
            .map(item -> Objects.toString(item, "").trim())
            .filter(id -> !id.isBlank())
            .distinct()
            .toList();
    }

    private List<String> readDeckDimensionKeys(Object value) {
        if (!(value instanceof List<?> rows)) {
            return List.of("researchProblem", "method", "dataExperiment", "results", "contribution", "limitation", "discussion");
        }
        List<String> keys = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map) {
                String key = Objects.toString(map.get("key"), "").trim();
                if (!key.isBlank()) keys.add(key);
            }
        }
        if (keys.isEmpty()) {
            return List.of("researchProblem", "method", "dataExperiment", "results", "contribution", "limitation", "discussion");
        }
        return keys;
    }

    private List<String> readDeckDimensionLabels(Object value) {
        if (!(value instanceof List<?> rows)) {
            return List.of("研究问题", "方法路线", "数据与场景", "结果证据", "创新点", "局限性");
        }
        List<String> labels = new ArrayList<>();
        for (Object row : rows) {
            if (row instanceof Map<?, ?> map) {
                String label = Objects.toString(map.get("label"), "").trim();
                if (!label.isBlank()) labels.add(label);
            }
        }
        if (labels.isEmpty()) {
            return List.of("研究问题", "方法路线", "数据与场景", "结果证据", "创新点", "局限性");
        }
        return labels;
    }

    private String buildDeckMaterial(
        List<PaperEntity> papers,
        List<String> dimensions,
        String templateName,
        String slideCount,
        String audience,
        String focus,
        Path reportPaperPath
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# 组会汇报 PPT 生成材料\n\n");
        builder.append("## 汇报设置\n\n");
        builder.append("- 模版：").append(templateName).append("\n");
        builder.append("- 页数：").append(slideCount).append("\n");
        builder.append("- 汇报对象：").append(audience).append("\n");
        builder.append("- 汇报重点：").append(focus.isBlank() ? "多论文横向对比" : focus).append("\n\n");
        if (reportPaperPath != null) {
            builder.append("- 用户上传的汇报论文：").append(reportPaperPath.getFileName()).append("\n\n");
        }
        builder.append("## 对比维度\n\n");
        for (String dimension : dimensions) {
            builder.append("- ").append(dimension).append("\n");
        }
        builder.append("\n## 文献列表\n\n");
        int index = 1;
        for (PaperEntity paper : papers) {
            builder.append("### ").append(index++).append(". ").append(cleanMarkdown(paper.getTitle())).append("\n\n");
            builder.append("- 作者：").append(cleanMarkdown(Optional.ofNullable(paper.getAuthors()).orElse("作者待补全"))).append("\n");
            builder.append("- 来源：").append(cleanMarkdown(Optional.ofNullable(paper.getSource()).orElse("来源待补全"))).append("\n");
            builder.append("- 年份：").append(cleanMarkdown(Optional.ofNullable(paper.getPublishYear()).orElse("年份待补全"))).append("\n");
            builder.append("- 类型：").append(cleanMarkdown(Optional.ofNullable(paper.getVenueType()).orElse("待分类"))).append("\n");
            builder.append("- 标签：").append(cleanMarkdown(Optional.ofNullable(paper.getJournalTags()).orElse("标签待补全"))).append("\n");
            builder.append("- 摘要：").append(cleanMarkdown(Optional.ofNullable(paper.getAbstractText()).orElse("摘要待补全"))).append("\n");
            builder.append("- 阅读笔记：").append(cleanMarkdown(Optional.ofNullable(paper.getNote()).orElse("暂无笔记"))).append("\n\n");
        }
        builder.append("## PPT Master skill 生成要求\n\n");
        builder.append("1. 使用中文生成可编辑科研 PPT。\n");
        builder.append("2. 首页说明汇报主论文、汇报对象和研究主线。\n");
        builder.append("3. 主体按 Background / Methodology / Experiment / Results / Conclusion / Outlook 组织。\n");
        builder.append("4. 优先保留主论文的公式、图、表、方法流程与核心证据。\n");
        builder.append("5. 结尾给出组会讨论问题和下一步研究建议。\n");
        builder.append("6. 不要编造论文中没有的实验结果；信息不足处标注“待核对”。\n");
        return builder.toString();
    }

    private String cleanMarkdown(String value) {
        return value == null ? "" : value.replace("\r", " ").replace("\n", " ").trim();
    }

    private String shellJoin(List<String> command) {
        return command.stream()
            .map(part -> part.matches("[A-Za-z0-9_./:=+-]+") ? part : "'" + part.replace("'", "'\"'\"'") + "'")
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }

    private void addTitleSlide(XMLSlideShow ppt, String title, String authors) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide);
        addText(slide, "PAPERSLOVER · 组会汇报", 72, 90, 1000, 40, 18, new Color(0, 102, 255), true);
        addText(slide, title, 72, 180, 1080, 210, 36, new Color(28, 28, 30), true);
        addText(slide, authors, 72, 430, 1000, 80, 20, new Color(105, 105, 110), false);
    }

    private void addContentSlide(XMLSlideShow ppt, String title, String content, int index) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide);
        addText(slide, String.format("%02d", index), 70, 60, 80, 45, 20, new Color(0, 102, 255), true);
        addText(slide, title, 70, 115, 1080, 70, 30, new Color(28, 28, 30), true);
        String clean = content.replaceAll("\\n{3,}", "\n\n");
        if (clean.length() > 900) clean = clean.substring(0, 900) + "…";
        addText(slide, clean, 75, 220, 1100, 390, 20, new Color(60, 64, 70), false);
        addText(slide, "PaperSlover AI 生成 · 内容可继续编辑", 75, 655, 900, 24, 12, new Color(142, 142, 147), false);
    }

    private void addBackground(XSLFSlide slide) {
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setShapeType(org.apache.poi.sl.usermodel.ShapeType.RECT);
        bg.setAnchor(new Rectangle2D.Double(0, 0, 1280, 720));
        bg.setFillColor(new Color(250, 249, 247));
        bg.setLineColor(new Color(250, 249, 247));
    }

    private void addText(XSLFSlide slide, String text, double x, double y, double w, double h,
                         double size, Color color, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, w, h));
        box.setText(text == null ? "" : text);
        box.setWordWrap(true);
        for (XSLFTextParagraph paragraph : box.getTextParagraphs()) {
            paragraph.setTextAlign(TextParagraph.TextAlign.LEFT);
            for (XSLFTextRun run : paragraph.getTextRuns()) {
                run.setFontFamily("Microsoft YaHei");
                run.setFontSize(size);
                run.setFontColor(color);
                run.setBold(bold);
            }
        }
    }

    private PaperEntity requirePaper(String workspaceId, Long userId) {
        PaperEntity paper = paperRepository.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "论文不存在"));
        if (!Objects.equals(paper.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该论文");
        }
        return paper;
    }

    private PaperEntity requireDeckPaper(String workspaceId, Long userId) {
        PaperEntity paper = paperRepository.findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "组会文献不存在：" + workspaceId));
        if (!Objects.equals(paper.getUserId(), userId)) {
            System.out.println("Meeting deck uses library paper from another local user. workspaceId="
                + workspaceId + ", paperUserId=" + paper.getUserId() + ", currentUserId=" + userId);
        }
        return paper;
    }

    private Map<String, Object> response(PaperEntity paper, MeetingReportEntity report) {
        Map<String, String> sections = report == null ? emptySections() : readSections(report.getContent());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paper", Map.of(
            "workspaceId", paper.getWorkspaceId(),
            "title", paper.getTitle(),
            "authors", Optional.ofNullable(paper.getAuthors()).orElse("作者信息未补全"),
            "source", paper.getSource(),
            "publishYear", Optional.ofNullable(paper.getPublishYear()).orElse("年份未知"),
            "abstract", Optional.ofNullable(paper.getAbstractText()).orElse(""),
            "note", Optional.ofNullable(paper.getNote()).orElse("")
        ));
        result.put("sections", sections);
        result.put("modelName", report == null ? "" : report.getModelName());
        result.put("generated", report != null);
        result.put("updatedAt", report == null ? null : report.getUpdatedAt());
        return result;
    }

    private Map<String, String> emptySections() {
        Map<String, String> map = new LinkedHashMap<>();
        SECTION_KEYS.forEach(key -> map.put(key, ""));
        return map;
    }

    private Map<String, String> readSections(String json) {
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, String> result = emptySections();
            SECTION_KEYS.forEach(key -> result.put(key, String.valueOf(raw.getOrDefault(key, ""))));
            return result;
        } catch (Exception error) {
            return emptySections();
        }
    }

    private String normalizeJson(String raw, PaperEntity paper) throws Exception {
        Map<String, Object> parsed = objectMapper.readValue(extractJson(raw), new TypeReference<>() {});
        Map<String, String> result = emptySections();
        SECTION_KEYS.forEach(key -> {
            String text = String.valueOf(parsed.getOrDefault(key, "")).trim();
            result.put(key, ensureSectionBlocks(key, text, paper));
        });
        if (result.get("basicInfo").isBlank()) result.put("basicInfo", basicInfo(paper));
        return objectMapper.writeValueAsString(result);
    }

    private String normalizeSection(String key, String raw, PaperEntity paper) {
        String text = cleanGeneratedText(Optional.ofNullable(raw).orElse("").trim());
        try {
            Map<String, Object> parsed = objectMapper.readValue(extractJson(text), new TypeReference<>() {});
            Object section = parsed.get("section");
            if (section == null) section = parsed.get(key);
            if (section != null) text = cleanGeneratedText(String.valueOf(section));
        } catch (Exception ignored) {
            String extracted = extractSectionStringFromLooseJson(text, key);
            if (!extracted.isBlank()) text = extracted;
        }
        return ensureSectionBlocks(key, text, paper);
    }

    private String extractSectionStringFromLooseJson(String text, String key) {
        if (text == null || text.isBlank()) return "";
        List<String> fields = List.of("section", key);
        for (String field : fields) {
            String marker = "\"" + field + "\"";
            int index = text.indexOf(marker);
            if (index < 0) continue;
            int colon = text.indexOf(':', index + marker.length());
            if (colon < 0) continue;
            String value = text.substring(colon + 1).trim();
            value = value.replaceFirst("^\"", "").replaceFirst("\"?\\s*}\\s*$", "");
            return cleanGeneratedText(value);
        }
        return "";
    }

    private String extractJson(String raw) {
        String text = Optional.ofNullable(raw).orElse("").trim()
            .replaceFirst("^```(?:json)?\\s*", "")
            .replaceFirst("\\s*```$", "")
            .trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }

    private String ensureSectionBlocks(String key, String text, PaperEntity paper) {
        List<String> titles = SECTION_BLOCKS.getOrDefault(key, List.of());
        if (titles.isEmpty()) return text;
        String rawClean = cleanGeneratedText(Optional.ofNullable(text).orElse("").trim());
        String clean = (isPromptLeak(rawClean) || isMetaAnswer(rawClean)) ? "" : rawClean;
        List<String> missing = titles.stream()
            .filter(title -> !clean.contains(title + "：") && !clean.contains(title + ":"))
            .toList();
        if (clean.length() >= 80 && missing.isEmpty()) return clean;

        String base = clean.isBlank() ? fallbackSectionText(key, paper) : clean;
        StringBuilder builder = new StringBuilder();
        for (String title : titles) {
            String existing = extractBlock(base, title, titles);
            if (isPromptLeak(existing) || isMetaAnswer(existing)) existing = "";
            if (existing.isBlank()) existing = fallbackBlockText(key, title, paper);
            builder.append(title).append("：").append(existing).append("\n");
        }
        return builder.toString().trim();
    }

    private boolean isPromptLeak(String text) {
        if (text == null || text.isBlank()) return false;
        String compact = text.replaceAll("\\s+", "");
        return compact.contains("每个小标题至少")
            || compact.contains("不要输出Markdown")
            || compact.contains("只输出JSON")
            || compact.contains("禁止分析本次提示词")
            || compact.contains("请生成章节")
            || compact.contains("当前没有可提取的PDF正文")
            || compact.contains("以下是系统从PDF正文抽取")
            || compact.contains("分析材料")
            || compact.contains("正文读取状态");
    }

    private boolean isMetaAnswer(String text) {
        if (text == null || text.isBlank()) return false;
        String compact = text.replaceAll("\\s+", "");
        return compact.contains("我们被要求")
            || compact.contains("用户要求")
            || compact.contains("可以写")
            || compact.contains("我们可以")
            || compact.contains("但不确定")
            || compact.contains("同样不确定")
            || compact.contains("所以我们")
            || compact.contains("需要生成")
            || compact.contains("题目提到")
            || compact.contains("作为AI")
            || compact.contains("无法确定具体")
            || compact.contains("没有提供足够信息");
    }

    private String extractBlock(String text, String title, List<String> allTitles) {
        String marker = title + "：";
        int start = text.indexOf(marker);
        int markerLength = marker.length();
        if (start < 0) {
            marker = title + ":";
            start = text.indexOf(marker);
            markerLength = marker.length();
        }
        if (start < 0) return "";
        int contentStart = start + markerLength;
        int end = text.length();
        for (String nextTitle : allTitles) {
            if (nextTitle.equals(title)) continue;
            int nextCn = text.indexOf(nextTitle + "：", contentStart);
            int nextEn = text.indexOf(nextTitle + ":", contentStart);
            int next = nextCn >= 0 ? nextCn : nextEn;
            if (next >= 0 && next < end) end = next;
        }
        return cleanGeneratedText(text.substring(contentStart, end).replaceAll("^[-\\s]+", "").trim());
    }

    private String cleanGeneratedText(String text) {
        if (text == null) return "";
        String cleaned = text
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\t", " ")
            .replaceAll("(?m)([：:])\\s*分析内容\\s*", "$1")
            .replaceAll("（\\s*(?:来自)?摘要\\s*）", "")
            .replaceAll("\\(\\s*(?:from\\s+)?abstract\\s*\\)", "")
            .replaceAll("（\\s*正文片段(?:未明确)?\\s*）", "")
            .replaceAll("\\(\\s*正文片段(?:未明确)?\\s*\\)", "")
            .replaceAll("(?m)^\\s*\"?(section|basicInfo|overview|background|method|results|conclusion|datasets)\"?\\s*:\\s*\"?", "")
            .replaceAll("(?m)^\\s*[{}\"]+\\s*$", "")
            .replaceAll("(?m)^\\s*[\\\\|/]+\\s*$", "")
            .replaceAll("(?m)^\\s*[{}\"\\\\|/]+\\s*$", "")
            .replaceAll("(?m)[\"}]+\\s*$", "")
            .replaceAll("(?m)[\\\\|/]+\\s*$", "")
            .replaceAll("(?m)^\\s*[○◦]\\s*$", "")
            .replaceAll("\\n{3,}", "\n\n")
            .replaceAll("[ \\t]{2,}", " ")
            .trim();
        return dropIncompleteTail(cleaned)
            .replaceAll("^[{}\"]+", "")
            .replaceAll("[{}\"\\\\|/]+$", "")
            .trim();
    }

    private String dropIncompleteTail(String text) {
        if (text == null || text.isBlank()) return "";
        String clean = text.trim();
        if (clean.matches("(?s).*[。！？.!?）)]\\s*$")) return clean;
        int lastBullet = Math.max(clean.lastIndexOf("\n• "), clean.lastIndexOf("\n- "));
        if (lastBullet > 0) {
            String tail = clean.substring(lastBullet).trim();
            if (tail.length() < 45 || !tail.matches("(?s).*[。！？.!?）)]\\s*$")) {
                return clean.substring(0, lastBullet).trim();
            }
        }
        int lastStop = Math.max(
            Math.max(clean.lastIndexOf('。'), clean.lastIndexOf('！')),
            Math.max(clean.lastIndexOf('？'), Math.max(clean.lastIndexOf('.'), clean.lastIndexOf('!')))
        );
        if (lastStop > Math.max(80, clean.length() - 80)) return clean.substring(0, lastStop + 1).trim();
        return clean;
    }

    private String fallbackSectionText(String key, PaperEntity paper) {
        Map<String, String> fallback = readSections(fallbackJson(paper));
        return fallback.getOrDefault(key, "");
    }

    private String fallbackSectionForTimeout(String key, PaperEntity paper, Exception error) {
        String text = ensureSectionBlocks(key, fallbackSectionText(key, paper), paper);
        return text + "\n生成状态：本章 AI 请求未完成（" + readableError(error) + "），已先保存可编辑草稿；可稍后点击 AI 重新分析补全。";
    }

    private boolean isGeneratedFallback(String text) {
        if (text == null || text.isBlank()) return false;
        return text.contains("生成状态：本章 AI 请求未完成")
            || text.contains("已先保存可编辑草稿")
            || text.contains("接口返回 HTTP 530")
            || text.contains("error code: 1016");
    }

    private String fallbackBlockText(String key, String title, PaperEntity paper) {
        String titleText = paper.getTitle();
        String abstractText = Optional.ofNullable(paper.getAbstractText()).orElse("当前论文摘要缺失，需结合正文继续补充。");
        return switch (key + ":" + title) {
            case "basicInfo:论文定位" -> "该论文围绕《" + titleText + "》展开，可作为组会中介绍研究问题与领域脉络的核心材料。";
            case "basicInfo:发表信息" -> "来源为 " + paper.getSource() + "，年份为 " + Optional.ofNullable(paper.getPublishYear()).orElse("未知") + "，作者为 " + Optional.ofNullable(paper.getAuthors()).orElse("未补全") + "。";
            case "basicInfo:汇报价值" -> "适合从研究动机、方法设计、实验验证和局限展望四条线组织汇报。";
            case "overview:核心要点" -> "论文摘要显示，研究围绕题目所指问题提出分析或方法贡献。摘要依据：" + shorten(abstractText, 90);
            case "overview:研究问题" -> "需要解释该工作要解决的主要任务、既有方法不足以及本文希望改善的指标或能力。";
            case "overview:主要贡献" -> "可将贡献归纳为问题定义、方法方案、实验验证与应用价值，具体细节需结合正文核对。";
            case "background:核心要点" -> "研究背景应从领域需求、已有方案和当前瓶颈展开，突出本文切入问题。";
            case "background:关键问题" -> "重点说明为什么现有方法仍不足，例如数据覆盖、推理可靠性、评测偏差或部署成本。";
            case "background:本文思想" -> "本文思想可概括为围绕核心任务构建更明确的建模、评测或优化路径。";
            case "background:关键贡献" -> "贡献需要回到论文摘要与正文证据，避免编造具体数值。";
            case "method:整体框架" -> "方法部分建议按输入、核心模块、训练或推理流程、输出结果来讲。";
            case "method:关键模块" -> "关键模块应从论文方法章节提取，包括模型结构、数据处理、损失函数、检索或评估机制。";
            case "method:实现流程" -> "流程可按数据准备、模型处理、实验设置和结果输出串联。";
            case "results:主要发现" -> "结果部分应优先引用主实验结论，说明相对基线的表现变化。";
            case "results:对比结果" -> "需要补充基线方法、评价指标、消融设置和不同场景下的结果差异。";
            case "results:实验结论" -> "实验结论应区分论文已验证事实与仍需进一步验证的推断。";
            case "conclusion:研究结论" -> "论文结论可围绕方法有效性、适用场景和对领域的启发归纳。";
            case "conclusion:现有不足" -> "不足可从数据规模、泛化能力、计算成本、评测覆盖和真实场景迁移分析。";
            case "conclusion:未来展望" -> "后续可关注更大规模验证、跨领域迁移、可解释性和工程化部署。";
            case "datasets:数据来源" -> "摘要中未必明确数据集，需到实验章节核对具体来源、许可和预处理。";
            case "datasets:数据设置" -> "建议补充训练/验证/测试划分、样本规模、任务定义和数据清洗策略。";
            case "datasets:评测指标" -> "评测指标需结合任务类型说明，例如准确率、召回率、F1、BLEU、人工评价或成本指标。";
            default -> "请结合论文正文进一步补充该部分。";
        };
    }

    private String extractPaperText(PaperEntity paper) {
        Optional<byte[]> bytes = loadPdfBytes(paper);
        if (bytes.isEmpty()) {
            return metadataText(paper);
        }
        try (PDDocument document = Loader.loadPDF(bytes.get())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document)
                .replaceAll("-\\s*\\R\\s*", "")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\R{3,}", "\n\n")
                .trim();
            if (text.length() < 300) return metadataText(paper);
            return text;
        } catch (Exception error) {
            return metadataText(paper);
        }
    }

    private Optional<byte[]> loadPdfBytes(PaperEntity paper) {
        String paperUrl = resolveReadablePdfUrl(Optional.ofNullable(paper.getPaperUrl()).orElse("").trim());
        Path localPath = Path.of("uploads").resolve(paper.getWorkspaceId() + ".pdf");
        if (Files.exists(localPath)) {
            try {
                return Optional.of(Files.readAllBytes(localPath));
            } catch (Exception ignored) {
            }
        }
        if (!paperUrl.isBlank() && paperUrl.startsWith("/api/papers/uploads/")) {
            String filename = paperUrl.substring(paperUrl.lastIndexOf('/') + 1);
            Path uploaded = Path.of("uploads").resolve(filename);
            if (Files.exists(uploaded)) {
                try {
                    return Optional.of(Files.readAllBytes(uploaded));
                } catch (Exception ignored) {
                }
            }
        }
        if (!paperUrl.startsWith("http://") && !paperUrl.startsWith("https://")) {
            return Optional.empty();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(paperUrl))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "application/pdf,application/octet-stream,*/*")
                .header("User-Agent", "Mozilla/5.0 PaperSlover/1.0 MeetingReport")
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() >= 200 && response.statusCode() < 300
                && body != null && body.length > 4
                && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F') {
                return Optional.of(body);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private String resolveReadablePdfUrl(String paperUrl) {
        if (paperUrl == null || paperUrl.isBlank()) return "";
        try {
            SearchPaperVO paper = externalSearchService.searchByUrlOrDoi(paperUrl);
            if (paper != null && paper.getPdfUrl() != null && !paper.getPdfUrl().isBlank()) {
                return paper.getPdfUrl();
            }
        } catch (Exception ignored) {
        }
        return paperUrl;
    }

    private String metadataText(PaperEntity paper) {
        return "【题目】" + paper.getTitle()
            + "\n【作者】" + Optional.ofNullable(paper.getAuthors()).orElse("未补全")
            + "\n【来源】" + paper.getSource()
            + "\n【年份】" + Optional.ofNullable(paper.getPublishYear()).orElse("未知")
            + "\n【摘要】" + Optional.ofNullable(paper.getAbstractText()).orElse("暂无摘要");
    }

    private String relevantPaperText(String key, String text) {
        String clean = Optional.ofNullable(text).orElse("").replaceAll("\\s+", " ").trim();
        if (clean.length() <= 16000) return clean;
        List<String> needles = switch (key) {
            case "basicInfo" -> List.of("abstract", "introduction", "keywords", "摘要", "引言", "关键词");
            case "overview" -> List.of("abstract", "introduction", "contribution", "main result", "摘要", "引言", "贡献", "主要结果");
            case "background" -> List.of("introduction", "related work", "background", "引言", "相关工作", "背景");
            case "method" -> List.of("method", "methodology", "approach", "model", "framework", "algorithm", "方法", "模型", "框架", "算法");
            case "results" -> List.of("experiment", "experimental", "result", "evaluation", "ablation", "performance", "实验", "结果", "评估", "消融", "性能");
            case "conclusion" -> List.of("conclusion", "discussion", "limitation", "future", "结论", "讨论", "局限", "未来");
            case "datasets" -> List.of("dataset", "data", "benchmark", "setting", "metric", "baseline", "数据", "基准", "指标", "设置", "基线");
            default -> List.of();
        };
        String lower = clean.toLowerCase();
        LinkedHashSet<String> chunks = new LinkedHashSet<>();
        chunks.add("论文速读包：" + paperReadingMap(clean));
        chunks.add("论文开头：" + clean.substring(0, Math.min(4200, clean.length())));
        if (clean.length() > 3400) {
            chunks.add("论文结尾：" + clean.substring(Math.max(0, clean.length() - 2600)));
        }
        for (String needle : needles) {
            int index = lower.indexOf(needle.toLowerCase());
            while (index >= 0 && chunks.size() < 9) {
                int start = Math.max(0, index - 1200);
                int end = Math.min(clean.length(), index + 3600);
                chunks.add("本章相关段落：" + clean.substring(start, end));
                index = lower.indexOf(needle.toLowerCase(), index + needle.length());
            }
        }
        String joined = String.join("\n\n---\n\n", chunks);
        return joined.length() > 16000 ? joined.substring(0, 16000) : joined;
    }

    private String relevantReportText(String text) {
        String clean = Optional.ofNullable(text).orElse("").replaceAll("\\s+", " ").trim();
        if (clean.length() <= 26000) return clean;
        LinkedHashSet<String> chunks = new LinkedHashSet<>();
        chunks.add("论文速读包：" + paperReadingMap(clean));
        chunks.add(clean.substring(0, Math.min(5200, clean.length())));
        for (String key : SECTION_KEYS) {
            chunks.add(relevantPaperText(key, clean));
        }
        String joined = String.join("\n\n--- 章节相关片段 ---\n\n", chunks);
        return joined.length() > 28000 ? joined.substring(0, 28000) : joined;
    }

    private String paperReadingMap(String clean) {
        String text = Optional.ofNullable(clean).orElse("").replaceAll("\\s+", " ").trim();
        if (text.isBlank()) return "";
        LinkedHashSet<String> chunks = new LinkedHashSet<>();
        chunks.add(sliceAround(text, "abstract", 0, 1800));
        chunks.add(sliceAround(text, "introduction", 0, 2400));
        chunks.add(sliceAround(text, "related work", 600, 1800));
        chunks.add(sliceAround(text, "method", 700, 2600));
        chunks.add(sliceAround(text, "approach", 700, 2600));
        chunks.add(sliceAround(text, "experiment", 700, 2800));
        chunks.add(sliceAround(text, "evaluation", 700, 2600));
        chunks.add(sliceAround(text, "result", 700, 2600));
        chunks.add(sliceAround(text, "discussion", 600, 1800));
        chunks.add(sliceAround(text, "conclusion", 600, 2200));
        chunks.removeIf(String::isBlank);
        if (chunks.isEmpty()) {
            chunks.add(text.substring(0, Math.min(5200, text.length())));
            if (text.length() > 7600) chunks.add(text.substring(Math.max(0, text.length() - 2600)));
        }
        String joined = String.join("\n\n", chunks);
        return joined.length() > 11000 ? joined.substring(0, 11000) : joined;
    }

    private String sliceAround(String text, String needle, int before, int after) {
        String lower = text.toLowerCase();
        int index = lower.indexOf(needle.toLowerCase());
        if (index < 0) return "";
        int start = Math.max(0, index - before);
        int end = Math.min(text.length(), index + needle.length() + after);
        return text.substring(start, end);
    }

    private String readableError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "模型未返回可解析内容";
        if (message.contains("HTTP 530") || message.contains("error code: 1016")) {
            return "9Router 中转隧道不可用（HTTP 530 / 1016）";
        }
        if (message.contains("HTTP 502") || message.contains("HTTP 503") || message.contains("HTTP 504")) {
            return "中转站或上游模型临时不可用";
        }
        if (message.toLowerCase().contains("timeout") || message.toLowerCase().contains("timed out")) {
            return "模型响应超时";
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }

    private String shorten(String text, int maxLength) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > maxLength ? clean.substring(0, maxLength) + "…" : clean;
    }

    private String fallbackJson(PaperEntity paper) {
        Map<String, String> result = emptySections();
        String abstractText = Optional.ofNullable(paper.getAbstractText()).orElse("当前文献未同步摘要。");
        result.put("synthesis", "本文围绕《" + paper.getTitle() + "》展开研究。从研究背景看，该工作针对既有方法在相关任务上的不足提出改进。研究方法上，作者设计了相应的模型或分析框架，并在公开数据集上进行实验验证。主要结论显示所提方法在关键指标上取得改善，创新点集中在问题建模与方案设计层面。局限性包括数据规模、泛化能力与计算成本等方面。总体而言，该研究为相关领域提供了可参考的思路，后续可关注更大规模验证与跨场景迁移。");
        result.put("basicInfo", basicInfo(paper));
        result.put("overview", abstractText);
        result.put("background", "该研究围绕“" + paper.getTitle() + "”所对应的学术问题展开。建议结合论文引言进一步核对研究动机、领域现状与关键挑战。");
        result.put("method", "当前分析依据题录与摘要生成。核心研究思路可按“问题定义—方法设计—实验验证—结果讨论”梳理，建议阅读正文后补充关键模块与流程。");
        result.put("results", "摘要显示作者完成了相应实验验证。请从正文表格与图中补充基线、指标、提升幅度及消融实验结果。");
        result.put("conclusion", "研究为该方向提供了可复用的方法或证据。不足主要是当前元数据无法覆盖全部实验细节；后续可关注泛化性、数据规模、计算成本与真实场景验证。");
        result.put("datasets", "摘要中未识别到明确数据集名称，请在实验设置章节核对训练集、验证集、测试集及数据许可。");
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private String basicInfo(PaperEntity paper) {
        return "题目：" + paper.getTitle() + "\n作者：" + Optional.ofNullable(paper.getAuthors()).orElse("未补全")
            + "\n来源：" + paper.getSource() + "\n年份：" + Optional.ofNullable(paper.getPublishYear()).orElse("未知");
    }

    private String systemPrompt() {
        return """
            你是一名严谨的科研组会论文汇报助手。请基于用户提供的论文题录和摘要，用简体中文输出严格 JSON，
            只能包含 basicInfo、overview、background、method、results、conclusion、datasets 七个字符串字段。
            每个字段必须包含指定小标题，格式为“小标题：分析内容”，每个小标题独占一行或一段：
            basicInfo 必须包含：论文定位、发表信息、汇报价值。
            overview 必须包含：核心要点、研究问题、主要贡献。
            background 必须包含：核心要点、关键问题、本文思想、关键贡献。
            method 必须包含：整体框架、关键模块、实现流程。
            results 必须包含：主要发现、对比结果、实验结论。
            conclusion 必须包含：研究结论、现有不足、未来展望。
            datasets 必须包含：数据来源、数据设置、评测指标。
            每个小标题至少 2 句，适合研究生组会直接讲述；明确区分论文事实和基于摘要的合理判断。
            不要编造具体数值、数据集、实验结论或作者没有给出的事实；信息不足时说明“摘要未明确，需要查阅正文”。
            """;
    }

    private String fullReportSystemPrompt() {
        return """
            你是一名严谨、高效的科研组会论文精读助手。你的任务是阅读用户给出的论文题录、摘要和 PDF 正文抽取片段，生成七章组会分析。
            必须输出严格 JSON，不要输出 Markdown、解释、代码块或任何提示词复述。
            JSON 只能包含 basicInfo、overview、background、method、results、conclusion、datasets 七个字符串字段。
            每个字段必须按指定小标题展开，格式为“小标题：分析内容”。
            basicInfo：论文定位、发表信息、汇报价值。
            overview：核心要点、研究问题、主要贡献。
            background：核心要点、关键问题、本文思想、关键贡献。
            method：整体框架、关键模块、实现流程。
            results：主要发现、对比结果、实验结论。
            conclusion：研究结论、现有不足、未来展望。
            datasets：数据来源、数据设置、评测指标。
            每个小标题下写 3-5 个中文要点，每个要点要具体、可直接放进研究生组会笔记。
            能从正文片段判断的内容要说明依据来自引言、方法、实验、结果或结论；没有证据时明确写“正文片段未明确，需要查阅原文对应章节”，并说明要查什么。
            不要编造论文没有给出的数据集、数值、实验结论、作者观点或引用。
            禁止出现“用户要求”“我们被要求”“可以写”“我将”“提示词”“JSON字段”等元叙述。
            """;
    }

    private String sectionSystemPrompt(String key) {
        String headings = String.join("、", SECTION_BLOCKS.getOrDefault(key, List.of()));
        if ("synthesis".equals(key)) {
            return """
            你是一名论文精读助手。请对下面的论文进行内容详解，不要简单翻译或摘要，而是提炼核心贡献，并分析研究背景、问题、方法与数据、实验结论、创新启示和局限性。
            输出严格 JSON，只包含一个字段 section，字段值为中文字符串。
            section 内容必须按这些小标题展开：%s。
            每个小标题格式必须是“小标题：”，冒号后先写 1-2 句完整段落描述（正式学术语言，提炼而非复述），再用换行列出 2-4 个要点，每个要点是完整句子，不要用“• ”或“- ”开头。
            冒号后不得为空；不得把“研究背景：”“研究问题：”“要点：”等只有标签、没有内容的文字当作要点。
            要求：使用正式学术语言；提炼核心贡献而非复述原文；分析优势与不足；最后总结对该领域的启示。
            不要输出 Markdown、解释或多余文字，只输出 JSON。不要出现我们被要求、用户要求、可以写、我将等元叙述。
            """.formatted(headings);
        }
        return """
            你是一名像“小绿鲸文献阅读器”那样工作的科研论文精读助手，但输出要更适合研究生组会汇报。
            你的目标不是摘要复述，而是帮助用户快速抓住：论文解决什么关键问题、为什么重要、作者怎么做、实验证据是否支撑、有什么不足。
            请只分析一个章节，输出严格 JSON。
            JSON 只能包含一个字段 section，字段值为中文字符串。
            section 内容必须按这些小标题展开：%s。
            每个小标题格式必须是“小标题：”，冒号后直接写 5-8 个中文要点，禁止写“分析内容”四个字。
            冒号后不得为空；每个要点必须包含完整判断和具体内容，禁止单独输出“小标题：”“要点：”或其他空标签。
            每个要点至少 45 个汉字，必须足够具体，适合直接放进研究生组会讲稿。
            写作方法：
            1. 先在内部通读材料，形成“论文主线”：研究对象、核心痛点、方法/理论工具、数据或材料、实验/论证证据、结论和局限。
            2. 每个要点都要围绕这条主线，不要按模板硬凑；优先写论文里真正有信息密度的内容。
            3. 每个小标题都按固定逻辑组织：先写核心判断，再写依据或机制，最后写研究含义或可追问问题。
            4. 每个小标题第一条必须是总论句，后续要点必须分别回答不同问题，禁止相邻两条表达同一件事。
            5. 对方法章节，要拆成输入、核心机制、处理流程、输出/目标四段；对结果章节，要写清基线、指标、主要现象、消融或对照的意义；对背景章节，要写清已有工作缺口。
            6. 如果是综述、理论、系统或人文社科论文，不要强行套机器学习实验结构；应按该论文实际的论证材料、案例、文本、制度、系统功能或理论框架分析。
            7. 信息不足时不要反复写“原文未明确”。每个小标题最多只允许 1 条“待核对”要点，而且必须放在该小标题最后，用“待核对：……”开头。
            质量标准：
            - 所有分析必须紧扣用户给出的论文题目、摘要和正文片段，不得套用其他论文、其他任务或通用模板。
            - 每个要点必须包含具体信息：论文中的对象/概念/方法/数据/材料/指标/结论至少命中一项。
            - 不要把作者姓名、普通术语、搜索关键词或论文题目中的孤立词当成贡献。
            - 不要只写“采用 Vue / SpringBoot / MySQL”这种技术栈摘要；除非论文就是软件系统研究，并且必须说明技术选择服务了什么研究目标或验证环节。
            - 同一小标题内不得重复“本文聚焦/旨在/通过……实现……”这类同义开头；每条要点的功能必须不同：定义问题、解释机制、列证据、评价结果、指出边界。
            - 不要在每个要点末尾机械标注“来自摘要”“来自正文片段”“摘要”。不要重复写“原文未明确”“正文片段未明确”。
            - 如果材料没有提供某项事实，不要编造；将不足压缩成最后一条“待核对：建议查看……章节确认……”，同一小标题只能出现一次。
            - 优先提取论文自己的专有概念、任务定义、数据来源、方法模块、实验指标和结论；每个要点都要能回答“这篇论文为什么重要、证据在哪里、下一步能追问什么”。
            禁止分析本次提示词或写作任务本身，禁止出现“我们被要求”“用户要求”“可以写”“我认为应该写”“汇报时可”“可简要说明”“可以提到”这类元叙述。
            每个小标题内容必须直接回答该小标题，不要把数据来源内容写到评测指标，也不要把研究背景内容写到数据集。
            每个要点直接写成完整句子，不要用“• ”或“- ”开头。不要在每条后面反复写“正文片段”“正文片段未明确”“原文未明确”“需要查阅实验章节”这类尾注；必要时统一写成最后一条“待核对：……”。
            输出必须完整闭合：最后一个要点必须以中文句号、问号或感叹号结束；如果篇幅不够，宁可减少一条要点，也不要输出半句话。
            不要输出 Markdown，不要输出解释，只输出 JSON。不要输出 "\\n"、"\\n\\n"、"\\t"、"}"、"}\"" 这类转义字符或 JSON 残留文本。
            """.formatted(headings);
    }

    private String paperPrompt(PaperEntity paper) {
        return "题目：" + paper.getTitle() + "\n作者：" + paper.getAuthors() + "\n来源：" + paper.getSource()
            + "\n年份：" + paper.getPublishYear() + "\n摘要：" + paper.getAbstractText();
    }

    private String sectionPrompt(PaperEntity paper, String key, String paperText, boolean fullTextAvailable) {
        String sourceLabel = fullTextAvailable ? "下面是从 PDF 通读抽取的论文材料包，包含全文速读包、开头、结尾和本章相关段落；请综合这些材料分析，不要只看题目或摘要。" : "当前没有可提取的 PDF 正文，只能基于题录和摘要；必须明确哪些信息不足。";
        if ("synthesis".equals(key)) {
            return "请生成章节：" + sectionName(key)
                + "\n\n" + paperPrompt(paper)
                + "\n\n【任务】请以学术文献综述的写作方式分析这篇论文，从研究背景、研究问题、研究方法与数据、实验与结论、创新点与启示、局限性等方面总结。"
                + "\n【要求】使用正式学术语言；提炼核心贡献而非复述原文；分析该研究与现有研究相比的优势和不足；最后总结对本研究领域的启示。"
                + "\n【格式】按小标题展开，每个小标题先写1-2句完整段落描述，再列出2-4个要点（完整句子，不要用•或-开头）。"
                + "\n\n【正文读取状态】" + (fullTextAvailable ? "已读取 PDF 文本层" : "未读取到可用 PDF 文本层")
                + "\n【分析材料】" + sourceLabel
                + "\n" + relevantReportText(paperText);
        }
        return "请生成章节：" + sectionName(key)
            + "\n\n" + paperPrompt(paper)
            + "\n\n【强约束】本章内容必须围绕论文《" + paper.getTitle() + "》；不要引用与该题目无关的研究对象、数据集、方法名或结论。"
            + "\n【小绿鲸式精读目标】输出要像资深研究生读完论文后的研究笔记：先抓主线，再拆方法，再解释结果意义，最后指出不足和可追问点；不要机械复述栏目。"
            + "\n【重点判断】先识别本文真正的研究对象、问题、方法、证据和结论；不要把作者姓名、年份、普通概念或搜索关键词误当成论文贡献。"
            + "\n【证据优先】能从材料中找到证据的写成具体判断；找不到证据时不要反复写“原文未明确”，只在小标题最后用一条“待核对：……”说明需要核对的正文位置。"
            + "\n【表达限制】不要写“汇报时可”“可简要说明”“可以提到”这类提示性口吻；直接写论文判断、研究含义和追问方向。"
            + "\n\n【正文读取状态】" + (fullTextAvailable ? "已读取 PDF 文本层" : "未读取到可用 PDF 文本层")
            + "\n【分析材料】" + sourceLabel
            + "\n" + relevantPaperText(key, paperText);
    }

    private String fullReportPrompt(PaperEntity paper, String paperText, boolean fullTextAvailable) {
        String sourceLabel = fullTextAvailable
            ? "已读取 PDF 文本层。下面材料包含题录、摘要、开头和各章节相关正文片段，请优先基于正文片段分析。"
            : "未读取到可用 PDF 文本层。下面只有题录和摘要，必须明确正文待核对信息。";
        return "请一次性生成七章组会汇报分析。"
            + "\n\n" + paperPrompt(paper)
            + "\n\n【正文读取状态】" + sourceLabel
            + "\n【论文材料】\n" + relevantReportText(paperText);
    }

    private String sectionName(String key) {
        return switch (key) {
            case "synthesis" -> "论文内容详解";
            case "basicInfo" -> "一、基本信息";
            case "overview" -> "二、文章概述";
            case "background" -> "三、研究背景";
            case "method" -> "四、研究思路";
            case "results" -> "五、研究结果";
            case "conclusion" -> "六、研究结论、不足与展望";
            case "datasets" -> "七、数据集";
            default -> key;
        };
    }

    private String jobKey(Long userId, String workspaceId) {
        return userId + ":" + workspaceId;
    }

    private void markStaleJob(ReportJob job) {
        if (!"running".equals(job.status())) return;
        long idleMillis = System.currentTimeMillis() - job.updatedAt();
        if (idleMillis < STALE_JOB_MILLIS) return;
        job.fail("AI 分析在“" + job.message() + "”阶段长时间无响应，已自动释放任务。请重新生成，或在模型与额度中切换更稳定的模型。");
    }

    private Map<String, Object> jobResponse(ReportJob job) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("workspaceId", job.workspaceId());
        response.put("status", job.status());
        response.put("progress", job.progress());
        response.put("message", job.message());
        response.put("done", "completed".equals(job.status()) || "failed".equals(job.status()));
        response.put("success", "completed".equals(job.status()));
        response.put("updatedAt", job.updatedAt());
        return response;
    }

    private Map<String, Object> deckJobResponse(DeckJob job) {
        if ("generated".equals(job.status())) ensurePptUsageRecorded(job);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.jobId());
        response.put("status", job.status());
        response.put("progress", job.progress());
        response.put("stage", job.stage());
        response.put("message", job.message());
        response.put("done", "generated".equals(job.status()) || "failed".equals(job.status()));
        response.put("success", "generated".equals(job.status()));
        if ("generated".equals(job.status())) {
            response.put("downloadUrl", "/api/meeting-reports/deck/jobs/" + job.jobId() + "/download");
        }
        response.put("statusUrl", "/api/meeting-reports/deck/jobs/" + job.jobId() + "/status");
        response.put("updatedAt", job.updatedAt());
        response.putAll(job.result());
        return response;
    }

    private static final class ReportJob {
        private final String workspaceId;
        private final Long userId;
        private volatile String status = "running";
        private volatile int progress = 0;
        private volatile String message = "后台分析已开始";
        private volatile String paperTitle = "所选论文";
        private volatile long updatedAt = System.currentTimeMillis();

        private ReportJob(String workspaceId, Long userId) {
            this.workspaceId = workspaceId;
            this.userId = userId;
        }

        String workspaceId() { return workspaceId; }
        Long userId() { return userId; }
        String status() { return status; }
        int progress() { return progress; }
        String message() { return message; }
        String paperTitle() { return paperTitle; }
        long updatedAt() { return updatedAt; }

        void paperTitle(String paperTitle) {
            if (paperTitle != null && !paperTitle.isBlank()) this.paperTitle = paperTitle;
        }

        void progress(int progress, String message) {
            this.progress = Math.max(0, Math.min(100, progress));
            this.message = message;
            this.updatedAt = System.currentTimeMillis();
        }

        void message(String message) {
            this.message = message;
            this.updatedAt = System.currentTimeMillis();
        }

        void complete() {
            this.status = "completed";
            this.progress = 100;
            this.message = "组会汇报已生成并保存";
            this.updatedAt = System.currentTimeMillis();
        }

        void fail(String message) {
            this.status = "failed";
            this.message = message;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    private static final class DeckJob {
        private final String jobId;
        private final Map<String, Object> result = new ConcurrentHashMap<>();
        private final AtomicBoolean usageRecorded = new AtomicBoolean(false);
        private volatile Long userId;
        private volatile String paperTitle = "组会汇报PPT";
        private volatile String status = "running";
        private volatile int progress = 1;
        private volatile String stage = "排队中";
        private volatile String message = "PPT Master 任务已创建";
        private volatile long updatedAt = System.currentTimeMillis();

        private DeckJob(String jobId) {
            this.jobId = jobId;
        }

        String jobId() { return jobId; }
        String status() { return status; }
        int progress() { return progress; }
        String stage() { return stage; }
        String message() { return message; }
        Long userId() { return userId; }
        String paperTitle() { return paperTitle; }
        long updatedAt() { return updatedAt; }
        Map<String, Object> result() { return result; }

        boolean markUsageRecording() {
            return usageRecorded.compareAndSet(false, true);
        }

        void unmarkUsageRecording() {
            usageRecorded.set(false);
        }

        void userId(Long userId) {
            if (userId != null) this.userId = userId;
        }

        void paperTitle(String paperTitle) {
            if (StringUtils.hasText(paperTitle)) this.paperTitle = paperTitle;
        }

        void progress(int progress, String message) {
            this.status = "running";
            this.progress = Math.max(this.progress, Math.max(0, Math.min(96, progress)));
            this.stage = stageFor(this.progress);
            this.message = message;
            this.updatedAt = System.currentTimeMillis();
        }

        void complete(Map<String, Object> response) {
            this.status = "generated";
            this.progress = 100;
            this.stage = "已完成";
            this.message = Objects.toString(response.getOrDefault("message", "PPT Master 生成完成"), "PPT Master 生成完成");
            this.result.clear();
            this.result.putAll(response);
            this.updatedAt = System.currentTimeMillis();
        }

        void awaitingAgent(Map<String, Object> response) {
            this.status = "awaiting_agent";
            this.progress = 36;
            this.stage = "等待 Agent 接管";
            this.message = Objects.toString(response.getOrDefault("message", "等待 PPT Master agent 接管"), "等待 PPT Master agent 接管");
            this.result.clear();
            this.result.putAll(response);
            this.updatedAt = System.currentTimeMillis();
        }

        void fail(String message) {
            this.status = "failed";
            this.stage = "生成失败";
            this.message = message;
            this.updatedAt = System.currentTimeMillis();
        }

        private static String stageFor(int progress) {
            if (progress < 12) return "校验材料";
            if (progress < 30) return "整理论文";
            if (progress < 48) return "生成内容";
            if (progress < 80) return "逐页设计";
            if (progress < 96) return "质检导出";
            return "收尾";
        }
    }

    public record GeneratedDeck(byte[] bytes, String filename) {}
}
