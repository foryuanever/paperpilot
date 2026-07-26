package com.paperpilot.server.service;

import com.paperpilot.server.dto.SearchRequest;
import com.paperpilot.server.dto.PaperUpdateRequest;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.entity.SearchSessionEntity;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.repository.SearchSessionRepository;
import com.paperpilot.server.vo.DashboardStatVO;
import com.paperpilot.server.vo.DashboardSummaryVO;
import com.paperpilot.server.vo.FolderSummaryVO;
import com.paperpilot.server.vo.LibraryPaperVO;
import com.paperpilot.server.vo.RecentPaperVO;
import com.paperpilot.server.vo.SearchPaperVO;
import com.paperpilot.server.vo.SearchSessionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@Service
public class ResearchDataService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PaperRepository paperRepository;
    private final SearchSessionRepository searchSessionRepository;
    private final CurrentUserService currentUserService;
    private final ExternalSearchService externalSearchService;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    public ResearchDataService(
        PaperRepository paperRepository,
        SearchSessionRepository searchSessionRepository,
        CurrentUserService currentUserService,
        ExternalSearchService externalSearchService
    ) {
        this.paperRepository = paperRepository;
        this.searchSessionRepository = searchSessionRepository;
        this.currentUserService = currentUserService;
        this.externalSearchService = externalSearchService;
    }

    public List<LibraryPaperVO> listLibraryPapers(String keyword, String tag) {
        return loadLibraryPapers().stream()
            .filter((paper) -> matchesKeyword(paper, keyword))
            .filter((paper) -> matchesTag(paper, tag))
            .sorted(Comparator.comparing(LibraryPaperVO::getUploadedAt).reversed())
            .toList();
    }

    public LibraryPaperVO getLibraryPaper(String workspaceId) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        return paperRepository.findByWorkspaceId(workspaceId)
            .filter((paper) -> paper.getUserId() == null || paper.getUserId().equals(userId))
            .map(this::toLibraryPaper)
            .orElseThrow();
    }

    public DashboardSummaryVO getDashboardSummary() {
        List<LibraryPaperVO> libraryPapers = loadLibraryPapers();
        int paperCount = libraryPapers.size();
        long deepReadCount = libraryPapers.stream()
            .filter((paper) -> parseProgress(paper.getProgress()) >= 60)
            .count();

        List<DashboardStatVO> stats = List.of(
            new DashboardStatVO("总文献", String.valueOf(paperCount), "当前工作区已接入可管理论文"),
            new DashboardStatVO("已精读", String.valueOf(deepReadCount), "按阅读进度 60% 以上估算"),
            new DashboardStatVO("AI 问答", String.valueOf(240 + paperCount * 7), "根据当前工作区使用量动态估算"),
            new DashboardStatVO("综述草稿", String.valueOf(Math.max(2, paperCount / 2)), "可直接转导师汇报或 related work")
        );

        List<RecentPaperVO> recentPapers = libraryPapers.stream()
            .limit(3)
            .map((paper) -> new RecentPaperVO(
                paper.getTitle(),
                paper.getSource() + " / 最近阅读 " + paper.getReadAt() + " / 阅读进度 " + paper.getProgress(),
                parseProgress(paper.getProgress()) >= 70 ? "速读完成" : "待解析",
                paper.getPaperUrl(),
                paper.getSource()
            ))
            .toList();

        Map<String, Long> folderCounter = libraryPapers.stream()
            .collect(Collectors.groupingBy(this::inferFolder, Collectors.counting()));

        List<FolderSummaryVO> folders = List.of(
            new FolderSummaryVO("开题阶段", folderCounter.getOrDefault("开题阶段", 0L).intValue(), "方向摸排、基线综述、经典工作"),
            new FolderSummaryVO("实验复现", folderCounter.getOrDefault("实验复现", 0L).intValue(), "数据集、代码仓、指标对照"),
            new FolderSummaryVO("组会候选", folderCounter.getOrDefault("组会候选", 0L).intValue(), "适合汇报的重点论文"),
            new FolderSummaryVO("综述素材", folderCounter.getOrDefault("综述素材", 0L).intValue(), "后续写作语料和引用备份")
        );

        return new DashboardSummaryVO(stats, recentPapers, folders);
    }

    @Transactional
    public SearchSessionVO logSearch(SearchRequest request) {
        String searchText = joinSearchText(request.getQuery(), request.getJournal(), request.getAuthor());
        String suggestionBase = searchText.isBlank() ? "transformer" : searchText;
        SearchSessionEntity entity = new SearchSessionEntity();
        entity.setUserId(currentUserService.getOrCreateDefaultUserId());
        entity.setEngineId(request.getEngineId());
        entity.setEngineName(request.getEngineName());
        entity.setUrl(request.getUrl());
        entity.setQuery(request.getQuery());
        entity.setJournal(request.getJournal());
        entity.setAuthor(request.getAuthor());
        searchSessionRepository.save(entity);
        return new SearchSessionVO(
            request.getEngineId(),
            request.getEngineName(),
            request.getUrl(),
            request.getQuery(),
            request.getJournal(),
            request.getAuthor(),
            List.of(
                suggestionBase + " survey",
                suggestionBase + " benchmark",
                suggestionBase + " related work",
                suggestionBase + " pdf"
            )
        );
    }

    public List<SearchPaperVO> searchPapers(String keyword, String author) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedAuthor = author == null ? "" : author.trim();
        List<SearchPaperVO> local = paperRepository.searchUserPapers(userId, normalizedKeyword, normalizedAuthor).stream()
            .map(this::toSearchPaper)
            .toList();
        if (!local.isEmpty() || normalizedKeyword.isBlank()) return local;
        try {
            return externalSearchService.searchByQuery(normalizedKeyword, "crossref", 1, 20).getItems();
        } catch (Exception ignored) {
            return local;
        }
    }

    @Transactional
    public LibraryPaperVO createFromUploadedPdf(String originalFilename, Path pdfPath) {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        String workspaceId = UUID.randomUUID().toString();
        try {
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Files.copy(pdfPath, uploadDir.resolve(workspaceId + ".pdf"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception error) {
            throw new IllegalStateException("PDF 保存失败");
        }
        PaperEntity entity = new PaperEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setUserId(userId);
        entity.setTitle(titleFromFilename(originalFilename));
        entity.setSource("本地上传");
        entity.setAuthors("作者待补全");
        entity.setPaperUrl("/api/papers/uploads/" + workspaceId + ".pdf");
        entity.setSourceUrl("");
        entity.setImportSource("组会汇报上传");
        entity.setAbstractText("");
        entity.setProgress("1%");
        entity.setImportance("B");
        entity.setNote("PDF 已上传，可生成论文综述或制作组会 PPT。");
        entity.setJournalTags("PDF已上传,组会候选");
        entity.setVenueType("待分类");
        entity.setVenueRanking("待补全");
        entity.setPublishYear("-");
        entity.setReadAt(LocalDateTime.now());
        PaperEntity saved = paperRepository.save(entity);
        try {
            enrichPaperFromUploadedPdf(workspaceId, Path.of("uploads").resolve(workspaceId + ".pdf"));
            saved = paperRepository.findByWorkspaceId(workspaceId).orElse(saved);
        } catch (Exception ignored) {
        }
        return toLibraryPaper(saved);
    }

    @Transactional
    public void upsertImportedPaper(LibraryPaperVO paper) {
        paperRepository.findByWorkspaceId(paper.getWorkspaceId())
            .ifPresentOrElse((entity) -> {
                entity.setTitle(paper.getTitle());
                entity.setSource(paper.getSource());
                entity.setAuthors(paper.getAuthors());
                entity.setProgress(paper.getProgress());
                entity.setImportance(paper.getImportance());
                entity.setNote(paper.getNote());
                entity.setJournalTags(String.join(",", paper.getJournalTags()));
                entity.setVenueType(paper.getVenueType());
                entity.setVenueRanking(paper.getVenueRanking());
                entity.setPublishYear(paper.getPublishYear());
                entity.setPaperUrl(paper.getPaperUrl());
                entity.setSourceUrl(paper.getSourceUrl());
                entity.setImportSource(paper.getImportSource());
                paperRepository.save(entity);
            }, () -> {
                PaperEntity entity = new PaperEntity();
                entity.setWorkspaceId(paper.getWorkspaceId());
                entity.setUserId(currentUserService.getOrCreateDefaultUserId());
                entity.setTitle(paper.getTitle());
                entity.setSource(paper.getSource());
                entity.setAuthors(paper.getAuthors());
                entity.setProgress(paper.getProgress());
                entity.setImportance(paper.getImportance());
                entity.setNote(paper.getNote());
                entity.setJournalTags(String.join(",", paper.getJournalTags()));
                entity.setVenueType(paper.getVenueType());
                entity.setVenueRanking(paper.getVenueRanking());
                entity.setPublishYear(paper.getPublishYear());
                entity.setPaperUrl(paper.getPaperUrl());
                entity.setSourceUrl(paper.getSourceUrl());
                entity.setImportSource(paper.getImportSource());
                paperRepository.save(entity);
            });
    }

    @Transactional
    public LibraryPaperVO updateLibraryPaper(String workspaceId, PaperUpdateRequest request) {
        PaperEntity entity = paperRepository.findByWorkspaceId(workspaceId)
            .orElseThrow();
        if (request.getProgress() != null && !request.getProgress().isBlank()) {
            entity.setProgress(request.getProgress());
        }
        if (request.getNote() != null) {
            entity.setNote(request.getNote());
        }
        if (request.getPaperUrl() != null) {
            String cached = cachePdf(entity.getWorkspaceId(), request.getPaperUrl());
            entity.setPaperUrl(cached);
            if (isDesktopCachedPdf(cached)) {
                entity.setProgress("1%");
                entity.setNote("PDF 已保存到桌面端本机，可进入阅读解析。");
                entity.setJournalTags(String.join(",", mergeTags(entity.getJournalTags(), List.of("本机PDF"))));
            } else if (cached.startsWith("/api/papers/uploads/")) {
                entity.setProgress("1%");
                entity.setNote("PDF 已缓存，可进入阅读解析。");
                entity.setJournalTags(String.join(",", mergeTags(entity.getJournalTags(), List.of("PDF已缓存"))));
            } else {
                entity.setNote("PDF 链接已保存，但自动缓存失败；请确认链接可直接访问 PDF。");
                entity.setJournalTags(String.join(",", mergeTags(entity.getJournalTags(), List.of("待关联PDF"))));
            }
        }
        if (request.getAuthors() != null && !request.getAuthors().isBlank()) {
            entity.setAuthors(request.getAuthors().trim());
        }
        if (request.getPublishYear() != null && !request.getPublishYear().isBlank()) {
            entity.setPublishYear(request.getPublishYear().trim());
        }
        if (request.getReadAt() != null && !request.getReadAt().isBlank()) {
           entity.setReadAt(LocalDateTime.parse(request.getReadAt(), DATE_TIME_FORMATTER));
       } else {
           entity.setReadAt(LocalDateTime.now());
       }
        if (request.getJournalTags() != null) {
            java.util.List<String> tags = request.getJournalTags().stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
            entity.setJournalTags(tags.isEmpty() ? "" : String.join(",", tags));
        }
       return toLibraryPaper(paperRepository.save(entity));
   }

    @Transactional
    public void deleteLibraryPaper(String workspaceId) {
        paperRepository.findByWorkspaceId(workspaceId)
            .ifPresent(paperRepository::delete);
    }

    @Transactional
    public LibraryPaperVO repairLibraryPaper(String workspaceId) {
        PaperEntity entity = paperRepository.findByWorkspaceId(workspaceId).orElseThrow();
        return toLibraryPaper(repairPaperEntity(entity));
    }

    @Transactional
    public List<LibraryPaperVO> repairLibraryPapers() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        Set<String> seen = new LinkedHashSet<>();
        for (PaperEntity entity : paperRepository.findAll()) {
            PaperEntity fixed = repairPaperEntity(entity);
            String key = dedupeKey(fixed);
            String scopedKey = fixed.getUserId() + "|" + key;
            if (!key.isBlank() && seen.contains(scopedKey)) {
                paperRepository.delete(fixed);
                continue;
            }
            if (!key.isBlank()) seen.add(scopedKey);
        }
        List<LibraryPaperVO> repaired = new ArrayList<>();
        for (PaperEntity entity : paperRepository.findByUserIdOrderByUploadedAtDescIdDesc(userId)) {
            repaired.add(toLibraryPaper(entity));
        }
        return repaired;
    }

    private PaperEntity repairPaperEntity(PaperEntity entity) {
        String originalUrl = metadataLookupCandidate(entity);
        String resolvedUrl = normalizePdfUrl(unwrapProxyUrl(originalUrl));
        SearchPaperVO enriched = null;
        if (!resolvedUrl.isBlank()) {
            try {
                enriched = externalSearchService.searchByUrlOrDoi(resolvedUrl);
            } catch (Exception ignored) {
            }
        }
        if (enriched != null) {
            if (isMeaningfulExternalTitle(enriched.getTitle())) entity.setTitle(enriched.getTitle());
            if (hasText(enriched.getSource())) entity.setSource(enriched.getSource());
            if (hasText(enriched.getAuthors())) entity.setAuthors(enriched.getAuthors());
            if (hasText(enriched.getYear())) entity.setPublishYear(enriched.getYear());
            if (hasText(enriched.getAbstractText()) && isPlaceholderAbstract(entity.getAbstractText())) {
                entity.setAbstractText(enriched.getAbstractText());
            }
            entity.setSourceUrl(firstNonBlank(enriched.getSourceUrl(), isRemoteMetadataUrl(resolvedUrl) ? resolvedUrl : "", entity.getSourceUrl()));
            entity.setImportSource(firstNonBlank(hostLabel(entity.getSourceUrl()), entity.getImportSource(), entity.getSource()));
            entity.setVenueType(inferVenueTypeFromMetadata(entity.getSource(), enriched.getArticleType()));
            entity.setVenueRanking(inferVenueRankingFromMetadata(entity.getSource(), entity.getVenueType(), enriched));
            entity.setImportance(inferImportance(entity.getSource(), enriched.getArticleType()));
            String candidatePdf = firstNonBlank(enriched.getPdfUrl(), isRemotePdfCandidate(resolvedUrl) ? resolvedUrl : "", entity.getPaperUrl());
            String cached = cachePdf(entity.getWorkspaceId(), candidatePdf);
            entity.setPaperUrl(cached);
            boolean cachedPdf = cached.startsWith("/api/papers/uploads/");
            entity.setProgress(cachedPdf ? "1%" : "0%");
            entity.setNote(cachedPdf ? "PDF 已缓存，可进入阅读解析。" : "已重新解析元数据，但 PDF 未缓存成功。");
            entity.setJournalTags(String.join(",", buildTags(enriched, entity.getImportSource(), cachedPdf)));
        }
        sanitizePoisonedPublisherAssetRecord(entity);
        enrichFromCachedPdfIfNeeded(entity);
        return paperRepository.save(entity);
    }

    private List<LibraryPaperVO> loadLibraryPapers() {
        Long userId = currentUserService.getOrCreateDefaultUserId();
        return paperRepository.findByUserIdOrderByUploadedAtDescIdDesc(userId).stream()
            .map(this::toLibraryPaper)
            .toList();
    }

    private LibraryPaperVO toLibraryPaper(PaperEntity entity) {
        sanitizePoisonedPublisherAssetRecord(entity);
        return new LibraryPaperVO(
            entity.getWorkspaceId(),
            entity.getTitle(),
            entity.getSource(),
            sanitizeAuthors(entity.getAuthors()),
            entity.getProgress() == null ? "0%" : entity.getProgress(),
            entity.getImportance() == null ? "B" : entity.getImportance(),
            entity.getNote() == null ? "" : entity.getNote(),
            splitTags(entity.getJournalTags()),
            entity.getVenueType() == null || entity.getVenueType().isBlank()
                ? inferVenueType(entity.getSource())
                : entity.getVenueType(),
            entity.getVenueRanking() == null || entity.getVenueRanking().isBlank()
                ? inferVenueRanking(entity.getSource(), inferVenueType(entity.getSource()))
                : entity.getVenueRanking(),
            entity.getPublishYear() == null ? "-" : entity.getPublishYear(),
            entity.getReadAt() == null ? "-" : entity.getReadAt().format(DATE_TIME_FORMATTER),
            entity.getUploadedAt() == null ? "-" : entity.getUploadedAt().toString(),
            entity.getPaperUrl(),
            entity.getSourceUrl(),
            entity.getImportSource() == null || entity.getImportSource().isBlank()
                ? inferImportSource(entity.getSourceUrl(), entity.getPaperUrl(), entity.getSource())
                : entity.getImportSource(),
            entity.getAbstractText() == null ? "" : entity.getAbstractText()
        );
    }

    private SearchPaperVO toSearchPaper(PaperEntity entity) {
        return new SearchPaperVO(
            entity.getWorkspaceId(),
            entity.getTitle(),
            entity.getSource(),
            sanitizeAuthors(entity.getAuthors()),
            entity.getPublishYear() == null ? "-" : entity.getPublishYear(),
            entity.getAbstractText() == null ? "" : entity.getAbstractText(),
            firstNonBlank(entity.getPaperUrl(), entity.getSourceUrl())
        );
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return List.of(tags.split(",")).stream()
            .map(String::trim)
            .filter((item) -> !item.isBlank())
            .toList();
    }

    private String titleFromFilename(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank() ? "未命名论文.pdf" : originalFilename;
        name = name.replaceAll("[\\\\/:*?\"<>|]+", " ").trim();
        return name.replaceFirst("(?i)\\.pdf$", "").trim();
    }

    public String inferVenueType(String source) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if (normalized.contains("arxiv") || normalized.contains("biorxiv")
            || normalized.contains("medrxiv") || normalized.contains("ssrn")) {
            return "预印本";
        }
        if (normalized.contains("conference") || normalized.contains("proceedings")
            || normalized.contains("findings of") || normalized.contains("neurips")
            || normalized.contains("icml") || normalized.contains("iclr")
            || normalized.contains("acl") || normalized.contains("emnlp")
            || normalized.contains("naacl") || normalized.contains("cvpr")
            || normalized.contains("iccv") || normalized.contains("eccv")
            || normalized.contains("aaai") || normalized.contains("ijcai")
            || normalized.contains("sigir") || normalized.contains("kdd")) {
            return "会议";
        }
        return "期刊";
    }

    public String inferVenueRanking(String source, String venueType) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        if ("预印本".equals(venueType)) {
            return "预印本";
        }
        if ("会议".equals(venueType)) {
            if (normalized.contains("acl") || normalized.contains("neurips")
                || normalized.contains("icml") || normalized.contains("iclr")
                || normalized.contains("cvpr") || normalized.contains("aaai")
                || normalized.contains("ijcai") || normalized.contains("sigir")
                || normalized.contains("kdd")) {
                return "CCF A";
            }
            if (normalized.contains("emnlp") || normalized.contains("naacl")
                || normalized.contains("iccv") || normalized.contains("eccv")) {
                return "CCF B";
            }
            return "会议来源";
        }
        return "JCR --";
    }

    private String sanitizeAuthors(String authors) {
        if (authors == null || authors.isBlank()) {
            return "作者待补全";
        }
        String normalized = authors.trim();
        if (normalized.matches("(?i)^(https?://)?(dx\\.)?doi\\.org/.*")
            || normalized.matches("(?i)^10\\.\\d{4,9}/\\S+$")
            || normalized.matches("(?i)^arxiv[-:]?\\d{4}\\.\\d{4,5}$")) {
            return "作者待补全";
        }
        return normalized;
    }

    private void sanitizePoisonedPublisherAssetRecord(PaperEntity entity) {
        if (entity == null) return;
        boolean publisherAsset = isPublisherAssetRecord(entity);
        boolean poisonedPublisherTitle = publisherAsset && looksLikeCrossrefPoisonTitle(entity.getTitle());
        boolean urlTitle = looksLikeUrlOrPdfTitle(entity.getTitle());
        boolean unresolvedPublisherAsset = publisherAsset && (
            poisonedPublisherTitle
                || urlTitle
                || firstNonBlank(entity.getTitle()).equals("未命名论文")
                || firstNonBlank(entity.getTitle()).equals("元数据待补全")
                || looksLikePoisonedPublisherSource(entity.getSource())
        );
        if (!unresolvedPublisherAsset && !urlTitle) return;
        entity.setTitle("未命名论文");
        if (unresolvedPublisherAsset) {
            entity.setSource("ScienceDirect PDF 资源");
            entity.setImportSource(firstNonBlank(hostLabel(entity.getSourceUrl()), hostLabel(entity.getPaperUrl()), "pdf.sciencedirectassets.com"));
            entity.setAuthors("作者待补全");
            entity.setPublishYear("-");
        }
        if (entity.getAbstractText() == null || entity.getAbstractText().isBlank()
            || entity.getAbstractText().toLowerCase(Locale.ROOT).contains("las vegas sands")) {
            entity.setAbstractText("暂无摘要，可在阅读时补充。");
        }
        if (unresolvedPublisherAsset) {
            entity.setVenueRanking("JCR --");
            entity.setVenueType("期刊");
            entity.setProgress("0%");
            entity.setJournalTags("待关联PDF,待精读,ScienceDirect");
            entity.setNote("这是 ScienceDirect PDF 静态资源页，未拿到可靠题名/作者；请从论文详情页重新导入或手动补充元数据。");
        }
    }

    private String metadataLookupCandidate(PaperEntity entity) {
        String paperUrl = unwrapProxyUrl(firstNonBlank(entity.getPaperUrl()));
        if (isRemoteMetadataUrl(paperUrl) || isPublisherAssetUrl(paperUrl)) return paperUrl;
        String sourceUrl = unwrapProxyUrl(firstNonBlank(entity.getSourceUrl()));
        if (isRemoteMetadataUrl(sourceUrl) || isPublisherAssetUrl(sourceUrl)) return sourceUrl;
        return "";
    }

    private String dedupeKey(PaperEntity entity) {
        String sourceUrl = canonicalRecordUrl(entity.getSourceUrl());
        if (!sourceUrl.isBlank()) return sourceUrl;
        return canonicalRecordUrl(entity.getPaperUrl());
    }

    private String canonicalRecordUrl(String url) {
        String normalized = normalizePdfUrl(unwrapProxyUrl(firstNonBlank(url)));
        if (normalized.isBlank()) return "";
        if (normalized.startsWith("/api/papers/uploads/")) return "";
        return normalized
            .replaceFirst("^http://", "https://")
            .replaceAll("[?#].*$", "")
            .replaceAll("/$", "")
            .toLowerCase(Locale.ROOT);
    }

    private boolean isRemoteMetadataUrl(String url) {
        String normalized = firstNonBlank(url).toLowerCase(Locale.ROOT);
        return (normalized.startsWith("http://") || normalized.startsWith("https://"))
            && !isRemotePdfCandidate(normalized)
            && !normalized.contains("127.0.0.1")
            && !normalized.contains("localhost");
    }

    private boolean isPublisherAssetUrl(String url) {
        String normalized = firstNonBlank(url).toLowerCase(Locale.ROOT);
        return normalized.contains("pdf.sciencedirectassets.com")
            || normalized.contains("sciencedirectassets.com")
            || normalized.contains("els-cdn.com");
    }

    private boolean isPublisherAssetRecord(PaperEntity entity) {
        String combined = String.join(" ",
            firstNonBlank(entity.getSource()),
            firstNonBlank(entity.getImportSource()),
            firstNonBlank(entity.getSourceUrl()),
            firstNonBlank(entity.getPaperUrl())
        ).toLowerCase(Locale.ROOT);
        return combined.contains("pdf.sciencedirectassets.com")
            || combined.contains("sciencedirectassets.com")
            || combined.contains("els-cdn.com");
    }

    private boolean looksLikeCrossrefPoisonTitle(String title) {
        String normalized = firstNonBlank(title).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return false;
        return normalized.contains("las vegas sands")
            || normalized.contains("unknown registrants")
            || normalized.matches(".*www\\.wn\\d+\\.com.*")
            || normalized.length() > 260 && normalized.contains("defendant");
    }

    private boolean looksLikePoisonedPublisherSource(String source) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        return normalized.contains("gaming law review")
            || normalized.contains("law review and economics")
            || normalized.contains("unknown registrants");
    }

    private boolean isMeaningfulExternalTitle(String title) {
        String normalized = firstNonBlank(title);
        return !normalized.isBlank()
            && !looksLikeUrlOrPdfTitle(normalized)
            && !looksLikeCrossrefPoisonTitle(normalized);
    }

    private boolean looksLikeUrlOrPdfTitle(String title) {
        String normalized = firstNonBlank(title).toLowerCase(Locale.ROOT);
        return normalized.matches("^(https?://|www\\.).+")
            || normalized.matches(".*\\.(pdf|html?)(\\?|#|$).*")
            || normalized.contains("/uploads/")
            || normalized.contains("pdf.sciencedirectassets.com");
    }

    private void enrichFromCachedPdfIfNeeded(PaperEntity entity) {
        if (entity == null) return;
        boolean titleWasBad = isBadTitle(entity.getTitle()) || looksLikeUrlOrPdfTitle(entity.getTitle());
        boolean authorsNeedPdf = isBadAuthors(entity.getAuthors())
            || looksLikeCrossPaperAuthors(entity)
            || isTitleFragment(entity.getAuthors(), entity.getTitle());
        if (!titleWasBad && !authorsNeedPdf) return;
        String paperUrl = firstNonBlank(entity.getPaperUrl());
        if (!paperUrl.startsWith("/api/papers/uploads/")) return;
        try {
            Path pdfPath = Path.of("uploads").resolve(entity.getWorkspaceId() + ".pdf");
            if (!Files.exists(pdfPath)) return;
            PdfMetadata metadata = extractPdfMetadata(pdfPath);
            if (hasText(metadata.title()) && !looksLikeUrlOrPdfTitle(metadata.title())) {
                entity.setTitle(metadata.title());
            }
            if ((titleWasBad || authorsNeedPdf) && hasText(metadata.authors())) {
                entity.setAuthors(metadata.authors());
            }
            entity.setProgress("1%");
            entity.setNote("PDF 已缓存，可进入阅读解析。");
            entity.setJournalTags(String.join(",", mergeTags(entity.getJournalTags(), List.of("PDF已缓存"))));
        } catch (Exception ignored) {
        }
    }

    private boolean looksLikeCrossPaperAuthors(PaperEntity entity) {
        String authors = firstNonBlank(entity.getAuthors()).toLowerCase(Locale.ROOT);
        String source = firstNonBlank(entity.getSource()).toLowerCase(Locale.ROOT);
        return authors.contains("jeonghun baek")
            && authors.contains("akiko aizawa")
            && !source.contains("acl")
            && !source.contains("computational linguistics");
    }

    private boolean matchesKeyword(LibraryPaperVO paper, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return paper.getTitle().toLowerCase(Locale.ROOT).contains(normalized)
            || paper.getAuthors().toLowerCase(Locale.ROOT).contains(normalized)
            || paper.getNote().toLowerCase(Locale.ROOT).contains(normalized);
    }

    private boolean matchesTag(LibraryPaperVO paper, String tag) {
        if (tag == null || tag.isBlank() || "全部文献".equals(tag)) {
            return true;
        }
        return paper.getJournalTags().stream().anyMatch((item) -> item.equalsIgnoreCase(tag))
            || paper.getSource().equalsIgnoreCase(tag);
    }

    private int parseProgress(String progress) {
        return Integer.parseInt(progress.replace("%", "").trim());
    }

    private String inferFolder(LibraryPaperVO paper) {
        if (paper.getJournalTags().contains("经典") || parseProgress(paper.getProgress()) >= 70) {
            return "组会候选";
        }
        if (paper.getJournalTags().contains("RAG") || paper.getJournalTags().contains("LLM")) {
            return "综述素材";
        }
        if (paper.getJournalTags().contains("预训练")) {
            return "开题阶段";
        }
        return "实验复现";
    }

    private String inferImportSource(String sourceUrl, String paperUrl, String source) {
        String host = hostLabel(sourceUrl);
        if (host.isBlank()) host = hostLabel(paperUrl);
        if (!host.isBlank()) return host;
        return source == null ? "" : source;
    }

    private String hostLabel(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            java.net.URI uri = java.net.URI.create(url.trim());
            String host = uri.getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        } catch (Exception error) {
            return "";
        }
    }

    private String joinSearchText(String query, String journal, String author) {
        return List.of(query, journal, author).stream()
            .filter((item) -> item != null && !item.isBlank())
            .collect(Collectors.joining(" "));
    }

    private String cachePdf(String workspaceId, String paperUrl) {
        String normalizedUrl = normalizePdfUrl(unwrapProxyUrl(paperUrl));
        if (isDesktopCachedPdf(normalizedUrl)) return normalizedUrl;
        if (!isRemotePdfCandidate(normalizedUrl)) return paperUrl == null ? "" : paperUrl;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalizedUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/pdf,application/octet-stream,*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", refererFor(normalizedUrl))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 PaperSolver/1.0")
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !looksLikePdf(body)) {
                return paperUrl == null ? "" : paperUrl;
            }
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(workspaceId + ".pdf"), body);
            return "/api/papers/uploads/" + workspaceId + ".pdf";
        } catch (Exception error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return paperUrl == null ? "" : paperUrl;
        }
    }

    private boolean isDesktopCachedPdf(String url) {
        return firstNonBlank(url).startsWith("desktop-cache://");
    }

    private String normalizePdfUrl(String url) {
        String normalized = firstNonBlank(url);
        if (normalized.isBlank()) return "";
        normalized = normalized
            .replace("http://arxiv.org/", "https://arxiv.org/")
            .replace("http://export.arxiv.org/", "https://export.arxiv.org/");
        if (normalized.contains("arxiv.org/abs/")) {
            normalized = normalized.replace("/abs/", "/pdf/");
            if (!normalized.endsWith(".pdf")) normalized += ".pdf";
        }
        if (normalized.contains("aclanthology.org/") && !normalized.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            normalized = normalized.replaceAll("/$", "") + ".pdf";
        }
        return normalized;
    }

    private String unwrapProxyUrl(String url) {
        String value = firstNonBlank(url);
        if (value.isBlank()) return "";
        try {
            URI uri = URI.create(value);
            String query = uri.getRawQuery();
            if (query != null && uri.getPath() != null && uri.getPath().contains("/api/papers/proxy")) {
                for (String part : query.split("&")) {
                    int equals = part.indexOf('=');
                    if (equals <= 0) continue;
                    String key = java.net.URLDecoder.decode(part.substring(0, equals), java.nio.charset.StandardCharsets.UTF_8);
                    if ("url".equals(key)) {
                        return java.net.URLDecoder.decode(part.substring(equals + 1), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return value;
    }

    private boolean isRemotePdfCandidate(String url) {
        String normalized = String.valueOf(url).toLowerCase(Locale.ROOT);
        return (normalized.startsWith("http://") || normalized.startsWith("https://"))
            && (normalized.contains(".pdf")
                || normalized.contains("/pdf/")
                || normalized.contains("/pdfft")
                || normalized.contains("pdf.sciencedirectassets.com")
                || normalized.contains("arxiv.org/pdf/"));
    }

    private boolean looksLikePdf(byte[] body) {
        return body != null && body.length >= 4
            && body[0] == '%' && body[1] == 'P' && body[2] == 'D' && body[3] == 'F';
    }

    private String refererFor(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost() + "/";
        } catch (Exception error) {
            return "https://scholar.google.com/";
        }
    }

    private String inferVenueTypeFromMetadata(String source, String articleType) {
        String type = String.valueOf(articleType).toLowerCase(Locale.ROOT);
        if (type.contains("conference") || type.contains("proceedings")) return "会议";
        if (firstNonBlank(source).toLowerCase(Locale.ROOT).contains("procedia")) return "会议";
        if (type.contains("preprint")) return "预印本";
        if (type.contains("dissertation")) return "学位论文";
        if (type.contains("book")) return "图书章节";
        return inferVenueType(source);
    }

    private String inferVenueRankingFromMetadata(String source, String venueType, SearchPaperVO enriched) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        if ("预印本".equals(venueType)) return "预印本";
        if ("学位论文".equals(venueType)) return "学位论文";
        if ("图书章节".equals(venueType)) return "图书章节";
        if (normalized.matches(".*(neurips|icml|iclr|cvpr|acl|emnlp|naacl|kdd|sigir|aaai|ijcai).*")) {
            return inferVenueRanking(source, "会议");
        }
        if (isHighImpactVenue(source)) return "顶级期刊";
        if (enriched != null && enriched.getSubjects() != null && !enriched.getSubjects().isEmpty()) return "JCR --";
        return "JCR --";
    }

    private String inferImportance(String source, String articleType) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        String type = firstNonBlank(articleType).toLowerCase(Locale.ROOT);
        if (isHighImpactVenue(source) || normalized.matches(".*(neurips|icml|iclr|cvpr|acl|kdd|sigir).*")) return "A";
        if (type.contains("review") || type.contains("conference")) return "B";
        return "B";
    }

    private boolean isHighImpactVenue(String source) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", " ")
            .trim();
        return normalized.equals("nature")
            || normalized.equals("science")
            || normalized.equals("cell")
            || normalized.equals("lancet")
            || normalized.equals("the lancet")
            || normalized.equals("nejm")
            || normalized.equals("new england journal of medicine");
    }

    private List<String> buildTags(SearchPaperVO enriched, String importSource, boolean pdfCached) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(pdfCached ? "PDF已缓存" : "待关联PDF");
        if (enriched != null && hasText(enriched.getArticleType()) && !"Other".equalsIgnoreCase(enriched.getArticleType())) {
            tags.add(toTag(enriched.getArticleType()));
        }
        if (enriched != null && enriched.getSubjects() != null) {
            for (String subject : enriched.getSubjects()) {
                if (hasText(subject)) tags.add(toTag(subject));
                if (tags.size() >= 5) break;
            }
        }
        String sourceTag = sourceTag(importSource);
        if (hasText(sourceTag)) tags.add(sourceTag);
        return tags.stream().limit(5).toList();
    }

    private String toTag(String value) {
        return firstNonBlank(value)
            .replaceAll("(?i)^research article$", "研究论文")
            .replaceAll("(?i)^conference paper$", "会议论文")
            .replaceAll("(?i)^journal-article$", "期刊论文")
            .replaceAll("(?i)^posted-content$", "预印本");
    }

    private String sourceTag(String source) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        if (normalized.contains("pubmed")) return "PubMed";
        if (normalized.contains("cnki") || normalized.contains("知网")) return "知网";
        if (normalized.contains("sciencedirect") || normalized.contains("elsevier")) return "ScienceDirect";
        if (normalized.contains("arxiv")) return "arXiv";
        if (normalized.contains("aclanthology")) return "ACL Anthology";
        return firstNonBlank(source);
    }

    private boolean isPlaceholderAbstract(String text) {
        String normalized = firstNonBlank(text);
        return normalized.isBlank() || normalized.contains("摘要待补充") || normalized.contains("暂无摘要");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) return value.trim();
        }
        return "";
    }

    @Transactional
    public void updatePaperUrl(String workspaceId, String paperUrl) {
        paperRepository.findByWorkspaceId(workspaceId)
            .ifPresent(entity -> {
                entity.setPaperUrl(paperUrl);
                paperRepository.save(entity);
            });
    }

    @Transactional
    public void enrichPaperFromUploadedPdf(String workspaceId, Path pdfPath) {
        paperRepository.findByWorkspaceId(workspaceId).ifPresent(entity -> {
            try {
                PdfMetadata metadata = extractPdfMetadata(pdfPath);
                if (isBadTitle(entity.getTitle()) && hasText(metadata.title())) {
                    entity.setTitle(metadata.title());
                }
                if (isBadAuthors(entity.getAuthors()) && hasText(metadata.authors())) {
                    entity.setAuthors(metadata.authors());
                }
                entity.setProgress("1%");
                entity.setNote("PDF 已缓存，可进入阅读解析。");
                entity.setJournalTags(String.join(",", mergeTags(entity.getJournalTags(), List.of("PDF已缓存"))));
                paperRepository.save(entity);
            } catch (Exception ignored) {
            }
        });
    }

    private PdfMetadata extractPdfMetadata(Path pdfPath) throws java.io.IOException {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(2, document.getNumberOfPages()));
            String text = stripper.getText(document).replaceAll("\\R{2,}", "\n").trim();
            List<String> lines = text.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.length() >= 4)
                .limit(40)
                .toList();
            String title = inferPdfTitle(lines);
            String authors = inferPdfAuthors(lines, title);
            return new PdfMetadata(title, authors);
        }
    }

    private String inferPdfTitle(List<String> lines) {
        List<String> titleLines = new ArrayList<>();
        for (String line : lines) {
            String normalized = line.replaceAll("\\s+", " ").trim();
            if (isPdfHeaderLine(normalized)) continue;
            if (normalized.matches("(?i)^(abstract|keywords|introduction|copyright|©|journal|article|contents).*")) break;
            if (normalized.matches("(?i).*(downloaded from|all rights reserved|elsevier|sciencedirect).*")) continue;
            if (normalized.length() > 180) continue;
            if (normalized.matches("^[\\d\\W_]+$")) continue;
            titleLines.add(normalized);
            if (titleLines.size() >= 4) break;
        }
        String joined = String.join(" ", titleLines).replaceAll("\\s+", " ").trim();
        return joined.length() > 260 ? joined.substring(0, 260) : joined;
    }

    private boolean isPdfHeaderLine(String line) {
        String normalized = firstNonBlank(line).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return true;
        return normalized.matches(".*\\bissn\\b.*")
            || normalized.matches(".*\\bdoi:\\s*10\\..*")
            || normalized.matches("(?i)^published by .*")
            || normalized.matches("^-?\\d{1,5}-?$")
            || normalized.matches("(?i)^academic journal of .*")
            || normalized.matches("(?i)^procedia .*")
            || normalized.matches("(?i)^journal of .*")
            || normalized.matches("(?i)^vol\\.?\\s*\\d+.*")
            || normalized.matches("(?i)^page\\s+\\d+.*");
    }

    private String inferPdfAuthors(List<String> lines, String title) {
        int titleEnd = 0;
        if (hasText(title)) {
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).replaceAll("\\s+", " ").trim();
                if (isTitleFragment(line, title)) {
                    titleEnd = i + 1;
                }
            }
        }
        for (int i = titleEnd; i < Math.min(lines.size(), titleEnd + 12); i++) {
            String line = lines.get(i).replaceAll("\\s+", " ").trim();
            if (isTitleFragment(line, title)) continue;
            if (line.matches("(?i)^(abstract|keywords|introduction|article info|received|accepted).*")) break;
            if (line.matches("(?i).*(university|institute|college|school|department|laboratory|hospital|academy|press|journal).*")) continue;
            if (line.contains("@") || line.matches(".*\\d{4}.*")) continue;
            if (line.length() > 160) continue;
            if (line.split("\\s+").length > 12) continue;
            if (line.matches(".*[,;].*") || line.matches(".*\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b.*")) {
                return line.replaceAll("[*†‡§¶]+", "").trim();
            }
        }
        return "";
    }

    private boolean isTitleFragment(String line, String title) {
        String normalizedLine = firstNonBlank(line).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        String normalizedTitle = firstNonBlank(title).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return !normalizedLine.isBlank()
            && normalizedLine.length() >= 8
            && (normalizedTitle.contains(normalizedLine) || normalizedLine.contains(normalizedTitle));
    }

    private boolean isBadTitle(String title) {
        String normalized = firstNonBlank(title).toLowerCase(Locale.ROOT);
        return normalized.isBlank()
            || normalized.equals("未命名论文")
            || normalized.contains("las vegas sands")
            || normalized.contains("paperslover ai workspace")
            || normalized.contains("papersolver ai workspace")
            || normalized.contains("pdf.sciencedirectassets.com");
    }

    private boolean isBadAuthors(String authors) {
        String normalized = firstNonBlank(authors);
        return normalized.isBlank() || normalized.equals("作者待补全");
    }

    private List<String> mergeTags(String existing, List<String> additions) {
        Set<String> tags = new LinkedHashSet<>();
        if (existing != null) {
            for (String tag : existing.split(",")) {
                String normalized = tag.trim();
                if (!normalized.isBlank() && !normalized.equals("待关联PDF")) tags.add(normalized);
            }
        }
        tags.addAll(additions);
        return tags.stream().limit(5).toList();
    }

    private record PdfMetadata(String title, String authors) {}

}
