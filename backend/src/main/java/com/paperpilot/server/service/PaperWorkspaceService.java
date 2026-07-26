package com.paperpilot.server.service;

import com.paperpilot.server.dto.PaperImportRequest;
import com.paperpilot.server.entity.AppUserEntity;
import com.paperpilot.server.entity.PaperEntity;
import com.paperpilot.server.repository.PaperRepository;
import com.paperpilot.server.vo.LibraryPaperVO;
import com.paperpilot.server.vo.PaperWorkspaceVO;
import com.paperpilot.server.vo.SearchPaperVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PaperWorkspaceService {

    private final ResearchDataService researchDataService;
    private final PaperRepository paperRepository;
    private final CurrentUserService currentUserService;
    private final ExternalSearchService externalSearchService;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    public PaperWorkspaceService(
        ResearchDataService researchDataService,
        PaperRepository paperRepository,
        CurrentUserService currentUserService,
        ExternalSearchService externalSearchService
    ) {
        this.researchDataService = researchDataService;
        this.paperRepository = paperRepository;
        this.currentUserService = currentUserService;
        this.externalSearchService = externalSearchService;
    }

    @Transactional
    public PaperWorkspaceVO importPaper(PaperImportRequest request) {
        SearchPaperVO enriched = enrichImport(request);
        AppUserEntity currentUser = currentUserService.getOrCreateDefaultUser();
        Long userId = currentUser.getId();
        String requestSourceUrl = unwrapProxyUrl(request.getSourceUrl());
        String requestPaperUrl = unwrapProxyUrl(request.getPaperUrl());
        PaperEntity duplicate = findDuplicateImportedPaper(
            userId,
            enriched,
            request,
            firstNonBlank(enriched == null ? "" : enriched.getSourceUrl(), requestSourceUrl),
            firstNonBlank(enriched == null ? "" : enriched.getPdfUrl(), requestPaperUrl),
            requestSourceUrl,
            requestPaperUrl
        );
        if (duplicate != null) {
            mergeDuplicateMetadata(duplicate, enriched, request);
            duplicate.setReadAt(LocalDateTime.now());
            PaperEntity saved = paperRepository.save(duplicate);
            researchDataService.upsertImportedPaper(toLibraryPaper(saved));
            return toWorkspace(saved);
        }
        enforceDailyImportQuota(currentUser);
        String workspaceId = UUID.randomUUID().toString();
        PaperEntity entity = new PaperEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setUserId(userId);
        String enrichedSourceUrl = enriched == null ? "" : enriched.getSourceUrl();
        String importSource = firstNonBlank(hostLabel(enrichedSourceUrl), request.getImportSource(), hostLabel(requestSourceUrl), hostLabel(requestPaperUrl), request.getSource(), "插件导入");
        String src = firstNonBlank(enriched == null ? "" : enriched.getSource(), request.getSource(), importSource);
        boolean trustRequestMetadata = shouldTrustRequestMetadata(request);
        entity.setSource(src);
        entity.setSourceUrl(firstNonBlank(enriched == null ? "" : enriched.getSourceUrl(), requestSourceUrl, requestPaperUrl));
        entity.setImportSource(importSource);
        entity.setTitle(limit(firstMeaningfulTitle(enriched, request, trustRequestMetadata), 512));
        entity.setAuthors(limit(firstNonBlank(enriched == null ? "" : enriched.getAuthors(), trustRequestMetadata ? request.getAuthors() : "", "作者待补全"), 255));
        String candidatePaperUrl = importPdfCandidate(enriched, requestPaperUrl, entity.getSourceUrl());
        String cachedPaperUrl = cacheImportedPdf(workspaceId, candidatePaperUrl);
        entity.setPaperUrl(cachedPaperUrl);
        String abstractText = firstNonBlank(enriched == null ? "" : enriched.getAbstractText(), trustRequestMetadata ? request.getAbstractText() : "");
        entity.setAbstractText(abstractText == null || abstractText.isBlank()
            ? "暂无摘要，可在阅读时补充。"
            : abstractText);
        boolean pdfCached = isLocalCachedPdf(cachedPaperUrl);
        boolean desktopCached = isDesktopCachedPdf(cachedPaperUrl);
        entity.setProgress((pdfCached || desktopCached) ? "1%" : "0%");
        entity.setImportance(initialImportance(enriched, request));
        entity.setNote(desktopCached ? "PDF 已保存到桌面端本机，可进入阅读解析。" : pdfCached ? "PDF 已缓存，可进入阅读解析。" : "已导入元数据，但 PDF 未缓存成功，请关联可访问 PDF。");
        String articleType = firstNonBlank(enriched == null ? "" : enriched.getArticleType(), request.getArticleType());
        entity.setVenueType(inferVenueType(src, articleType));
        entity.setVenueRanking(inferVenueRanking(src, entity.getVenueType(), enriched));
        entity.setJournalTags(String.join(",", buildTags(enriched, request, importSource, pdfCached || desktopCached)));
        String publishYear = firstNonBlank(enriched == null ? "" : enriched.getYear(), request.getPublishYear());
        entity.setPublishYear(publishYear == null || publishYear.isBlank()
            ? String.valueOf(LocalDate.now().getYear())
            : limit(publishYear, 16));
        entity.setReadAt(LocalDateTime.now());
        entity.setUploadedAt(LocalDate.now());
        entity.setFolder("实验复现");
        paperRepository.save(entity);

        PaperWorkspaceVO workspace = toWorkspace(entity);
        researchDataService.upsertImportedPaper(toLibraryPaper(entity));
        return workspace;
    }

    private void enforceDailyImportQuota(AppUserEntity user) {
        int limit = dailyImportLimit(user == null ? "" : user.getRole());
        if (limit < 0) return;
        long used = paperRepository.countByUserIdAndUploadedAt(user.getId(), LocalDate.now());
        if (used >= limit) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "今日论文/PDF导入额度已用完：" + used + "/" + limit + "。普通用户每日最多导入5篇，可升级会员提高额度。"
            );
        }
    }

    private int dailyImportLimit(String role) {
        String value = firstNonBlank(role);
        if ("管理员".equals(value)) return -1;
        if ("导师".equals(value)) return 200;
        if ("特权用户".equals(value)) return 60;
        return 5;
    }

    private void mergeDuplicateMetadata(PaperEntity entity, SearchPaperVO enriched, PaperImportRequest request) {
        if (entity == null) return;
        boolean trustRequestMetadata = shouldTrustRequestMetadata(request);
        if (enriched != null) {
            if (isMeaningfulTitle(enriched.getTitle())) entity.setTitle(limit(enriched.getTitle(), 512));
            if (!firstNonBlank(enriched.getSource()).isBlank()) entity.setSource(enriched.getSource());
            if (!firstNonBlank(enriched.getAuthors()).isBlank()) entity.setAuthors(limit(enriched.getAuthors(), 255));
            if (!firstNonBlank(enriched.getYear()).isBlank()) entity.setPublishYear(limit(enriched.getYear(), 16));
            if (!firstNonBlank(enriched.getAbstractText()).isBlank()
                && isPlaceholderAbstract(entity.getAbstractText())) {
                entity.setAbstractText(enriched.getAbstractText());
            }
            entity.setSourceUrl(firstNonBlank(enriched.getSourceUrl(), entity.getSourceUrl(), request.getSourceUrl()));
            entity.setImportSource(firstNonBlank(hostLabel(entity.getSourceUrl()), entity.getImportSource(), request.getImportSource()));
            String candidatePaperUrl = importPdfCandidate(enriched, request.getPaperUrl(), entity.getPaperUrl());
            if (!isReadableCachedPdf(entity.getPaperUrl()) && !candidatePaperUrl.isBlank()) {
                entity.setPaperUrl(cacheImportedPdf(entity.getWorkspaceId(), candidatePaperUrl));
            }
            boolean pdfCached = isLocalCachedPdf(entity.getPaperUrl());
            boolean desktopCached = isDesktopCachedPdf(entity.getPaperUrl());
            entity.setProgress((pdfCached || desktopCached) ? "1%" : firstNonBlank(entity.getProgress(), "0%"));
            entity.setNote(desktopCached ? "PDF 已保存到桌面端本机，可进入阅读器解析正文。" : pdfCached ? "PDF 已缓存，可进入阅读器解析正文。" : "已更新元数据，但 PDF 未缓存成功，请关联可访问 PDF。");
            entity.setJournalTags(String.join(",", buildTags(enriched, request, entity.getImportSource(), pdfCached || desktopCached)));
            entity.setVenueType(inferVenueType(entity.getSource(), enriched.getArticleType()));
            entity.setVenueRanking(inferVenueRanking(entity.getSource(), entity.getVenueType(), enriched));
            return;
        }
        if (trustRequestMetadata) {
            if (isMeaningfulTitle(request.getTitle())) entity.setTitle(limit(cleanupTitle(request.getTitle()), 512));
            if (!firstNonBlank(request.getAuthors()).isBlank()) entity.setAuthors(limit(request.getAuthors(), 255));
            if (!firstNonBlank(request.getPublishYear()).isBlank()) entity.setPublishYear(limit(request.getPublishYear(), 16));
            if (!firstNonBlank(request.getAbstractText()).isBlank()
                && isPlaceholderAbstract(entity.getAbstractText())) {
                entity.setAbstractText(request.getAbstractText());
            }
        }
        String requestPaperUrl = firstNonBlank(request.getPaperUrl());
        if (!isReadableCachedPdf(entity.getPaperUrl()) && !requestPaperUrl.isBlank()) {
            entity.setPaperUrl(cacheImportedPdf(entity.getWorkspaceId(), requestPaperUrl));
            boolean pdfCached = isLocalCachedPdf(entity.getPaperUrl());
            boolean desktopCached = isDesktopCachedPdf(entity.getPaperUrl());
            entity.setProgress((pdfCached || desktopCached) ? "1%" : firstNonBlank(entity.getProgress(), "0%"));
            entity.setNote(desktopCached ? "PDF 已保存到桌面端本机，可进入阅读器解析正文。" : pdfCached ? "PDF 已缓存，可进入阅读器解析正文。" : firstNonBlank(entity.getNote(), "已更新元数据，但 PDF 未缓存成功，请关联可访问 PDF。"));
        }
    }

    private PaperWorkspaceVO toWorkspace(PaperEntity entity) {
        boolean pdfCached = isReadableCachedPdf(entity.getPaperUrl());
        return new PaperWorkspaceVO(
            entity.getWorkspaceId(),
            entity.getSource(),
            entity.getTitle(),
            entity.getPaperUrl(),
            entity.getAbstractText(),
            List.of(
                "先提取章节结构",
                "对摘要和结论做学术翻译",
                "生成创新点和局限性总结"
            ),
            pdfCached ? "PDF 已缓存，可直接进入阅读器解析正文。" : "未能缓存 PDF，请检查源站权限或手动关联 PDF。"
        );
    }

    private PaperEntity findDuplicateImportedPaper(Long userId, SearchPaperVO enriched, PaperImportRequest request, String... urls) {
        Set<String> targets = new LinkedHashSet<>();
        if (urls != null) {
            for (String url : urls) {
                String normalized = canonicalImportUrl(url);
                if (!normalized.isBlank()) targets.add(normalized);
            }
        }
        Set<String> ids = new LinkedHashSet<>();
        addIdentifier(ids, enriched == null ? "" : enriched.getId());
        addIdentifier(ids, request == null ? "" : request.getPaperId());
        addIdentifier(ids, extractDoi(firstNonBlank(request == null ? "" : request.getTitle(), enriched == null ? "" : enriched.getTitle())));
        String titleKey = titleKey(firstNonBlank(
            enriched == null ? "" : enriched.getTitle(),
            request == null ? "" : request.getTitle()
        ));
        String year = firstNonBlank(
            enriched == null ? "" : enriched.getYear(),
            request == null ? "" : request.getPublishYear()
        ).replaceAll("[^0-9]", "");
        String firstAuthor = firstAuthorKey(firstNonBlank(
            enriched == null ? "" : enriched.getAuthors(),
            request == null ? "" : request.getAuthors()
        ));
        if (targets.isEmpty() && ids.isEmpty() && titleKey.isBlank()) return null;
        for (PaperEntity entity : paperRepository.findByUserIdOrderByUploadedAtDescIdDesc(userId)) {
            String sourceUrl = canonicalImportUrl(entity.getSourceUrl());
            String paperUrl = canonicalImportUrl(entity.getPaperUrl());
            if (targets.contains(sourceUrl) || targets.contains(paperUrl)) {
                return entity;
            }
            Set<String> entityIds = new LinkedHashSet<>();
            addIdentifier(entityIds, extractDoi(entity.getSourceUrl()));
            addIdentifier(entityIds, extractDoi(entity.getPaperUrl()));
            addIdentifier(entityIds, extractDoi(entity.getTitle()));
            for (String id : ids) {
                if (entityIds.contains(id)) return entity;
            }
            String entityTitleKey = titleKey(entity.getTitle());
            if (!titleKey.isBlank() && titleKey.equals(entityTitleKey)) {
                String entityYear = firstNonBlank(entity.getPublishYear()).replaceAll("[^0-9]", "");
                if (year.isBlank() || entityYear.isBlank() || year.equals(entityYear)) {
                    return entity;
                }
            }
            if (!titleKey.isBlank() && titleKey.equals(entityTitleKey) && !firstAuthor.isBlank()) {
                String entityFirstAuthor = firstAuthorKey(entity.getAuthors());
                if (firstAuthor.equals(entityFirstAuthor)) return entity;
            }
        }
        return null;
    }

    private void addIdentifier(Set<String> ids, String value) {
        String doi = extractDoi(value);
        if (!doi.isBlank()) ids.add("doi:" + doi.toLowerCase(Locale.ROOT));
        String pii = extractScienceDirectPii(value);
        if (!pii.isBlank()) ids.add("pii:" + pii.toLowerCase(Locale.ROOT));
    }

    private String extractDoi(String value) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("10\\.\\d{4,9}/[-._;()/:A-Z0-9]+", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(firstNonBlank(value));
        String last = "";
        while (matcher.find()) last = matcher.group();
        return last.replaceAll("[)\\].,;，。；、]+$", "");
    }

    private String titleKey(String title) {
        String normalized = cleanupTitle(firstNonBlank(title)).toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return normalized.length() < 12 ? "" : normalized;
    }

    private String firstAuthorKey(String authors) {
        String normalized = firstNonBlank(authors).toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.contains("作者待")) return "";
        String first = normalized.split("[,;；，]")[0];
        return first.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String canonicalImportUrl(String url) {
        String normalized = normalizePdfUrl(unwrapProxyUrl(firstNonBlank(url))).trim();
        if (normalized.isBlank()) return "";
        String pii = extractScienceDirectPii(normalized);
        if (!pii.isBlank()) return "sciencedirect-pii:" + pii.toLowerCase(Locale.ROOT);
        return normalized
            .replaceFirst("^http://", "https://")
            .replaceAll("[?#].*$", "")
            .replaceAll("/$", "")
            .toLowerCase(Locale.ROOT);
    }

    private String extractScienceDirectPii(String url) {
        String value = firstNonBlank(url);
        if (value.matches("(?i)^S[A-Z0-9]{15,30}$")) return value;
        try {
            URI uri = URI.create(value);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    int equals = part.indexOf('=');
                    if (equals <= 0) continue;
                    String key = java.net.URLDecoder.decode(part.substring(0, equals), java.nio.charset.StandardCharsets.UTF_8);
                    if ("pii".equalsIgnoreCase(key)) {
                        String pii = java.net.URLDecoder.decode(part.substring(equals + 1), java.nio.charset.StandardCharsets.UTF_8).trim();
                        if (pii.matches("(?i)S[A-Z0-9]{15,30}")) return pii;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("(?:/pii/|1-s2\\.0-)(S[A-Za-z0-9]{15,30})", java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(value);
        String last = "";
        while (matcher.find()) last = matcher.group(1);
        return last;
    }

    private SearchPaperVO enrichImport(PaperImportRequest request) {
        if ("Zotero".equalsIgnoreCase(firstNonBlank(request.getImportSource()))) {
            return null;
        }
        String titleCandidate = shouldTrustRequestMetadata(request) ? firstNonBlank(request.getTitle()) : "";
        List<String> candidates = List.of(
            firstNonBlank(request.getPaperId()),
            unwrapProxyUrl(firstNonBlank(request.getSourceUrl())),
            unwrapProxyUrl(firstNonBlank(request.getPaperUrl())),
            titleCandidate
        );
        SearchPaperVO best = null;
        for (String candidate : candidates) {
            if (candidate.isBlank()) continue;
            try {
                SearchPaperVO found = externalSearchService.searchByUrlOrDoi(candidate);
                best = betterPaper(best, found);
                if (metadataScore(best) >= 7) return best;
            } catch (Exception ignored) {
            }
        }
        return best;
    }

    private String cacheImportedPdf(String workspaceId, String paperUrl) {
        String normalizedUrl = normalizePdfUrl(unwrapProxyUrl(paperUrl));
        if (isLocalPdfFileUrl(normalizedUrl)) {
            return cacheLocalPdfFile(workspaceId, normalizedUrl, paperUrl);
        }
        if (!isRemotePdfCandidate(normalizedUrl)) {
            return paperUrl;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(normalizedUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/pdf,application/octet-stream,*/*")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Zotero-API-Version", "3")
                .header("Referer", refererFor(normalizedUrl))
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 PaperSolver/1.0")
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            byte[] body = response.body();
            if (response.statusCode() < 200 || response.statusCode() >= 300 || !looksLikePdf(body)) {
                return paperUrl;
            }
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Files.write(uploadDir.resolve(workspaceId + ".pdf"), body);
            return "/api/papers/uploads/" + workspaceId + ".pdf";
        } catch (IllegalArgumentException | IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return paperUrl;
        }
    }

    private String cacheLocalPdfFile(String workspaceId, String fileUrl, String fallbackUrl) {
        try {
            Path source = Path.of(URI.create(fileUrl));
            if (!Files.exists(source) || !looksLikePdf(Files.readAllBytes(source))) {
                return fallbackUrl;
            }
            Path uploadDir = Path.of("uploads");
            Files.createDirectories(uploadDir);
            Files.copy(source, uploadDir.resolve(workspaceId + ".pdf"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/api/papers/uploads/" + workspaceId + ".pdf";
        } catch (Exception error) {
            return fallbackUrl;
        }
    }

    private String importPdfCandidate(SearchPaperVO enriched, String requestPaperUrl, String fallbackUrl) {
        String requestUrl = firstNonBlank(requestPaperUrl);
        if (isZoteroLocalFileUrl(requestUrl) || isLocalPdfFileUrl(requestUrl)) return requestUrl;
        return firstNonBlank(enriched == null ? "" : enriched.getPdfUrl(), requestUrl, fallbackUrl);
    }

    private SearchPaperVO betterPaper(SearchPaperVO current, SearchPaperVO next) {
        if (next == null) return current;
        if (!isMeaningfulTitle(next.getTitle())) return current;
        if (current == null) return next;
        return metadataScore(next) > metadataScore(current) ? next : current;
    }

    private int metadataScore(SearchPaperVO paper) {
        if (paper == null) return 0;
        int score = 0;
        if (!firstNonBlank(paper.getTitle()).isBlank()) score += 2;
        if (!firstNonBlank(paper.getAuthors()).isBlank()) score += 2;
        if (!firstNonBlank(paper.getPdfUrl()).isBlank()) score += 2;
        if (!firstNonBlank(paper.getSource()).isBlank()) score += 1;
        if (!firstNonBlank(paper.getYear()).isBlank()) score += 1;
        if (!firstNonBlank(paper.getAbstractText()).isBlank()) score += 1;
        return score;
    }

    private String firstMeaningfulTitle(SearchPaperVO enriched, PaperImportRequest request, boolean trustRequestMetadata) {
        String enrichedTitle = firstNonBlank(enriched == null ? "" : enriched.getTitle());
        if (isMeaningfulTitle(enrichedTitle)) return enrichedTitle;
        String requestedTitle = firstNonBlank(request.getTitle());
        if (trustRequestMetadata && isMeaningfulTitle(requestedTitle)) return cleanupTitle(requestedTitle);
        return "未命名论文";
    }

    private boolean shouldTrustRequestMetadata(PaperImportRequest request) {
        String sourceUrl = unwrapProxyUrl(firstNonBlank(request.getSourceUrl()));
        String paperUrl = unwrapProxyUrl(firstNonBlank(request.getPaperUrl()));
        if (!sourceUrl.isBlank() && !isPublisherAssetOrPdfUrl(sourceUrl)) {
            return true;
        }
        return !paperUrl.isBlank() && !paperUrl.equals(sourceUrl) && !isPublisherAssetOrPdfUrl(paperUrl);
    }

    private boolean isMeaningfulTitle(String title) {
        String normalized = cleanupTitle(title).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return false;
        if (normalized.equals("未命名论文")) return false;
        if (normalized.matches(".*\\.(pdf|html?)$")) return false;
        if (normalized.matches("^[a-z0-9.-]+\\.(com|cn|org|net|edu).*$")) return false;
        if (normalized.contains("pdf.sciencedirectassets.com")) return false;
        return normalized.length() >= 8;
    }

    private String cleanupTitle(String title) {
        return String.valueOf(title == null ? "" : title)
            .replaceAll("(?i)\\.pdf$", "")
            .replaceAll("\\s*[-|]\\s*(ScienceDirect|PubMed|Semantic Scholar|Web of Science|CNKI|知网|万方|arXiv|SpringerLink|IEEE Xplore|ACM Digital Library).*$", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean isLocalCachedPdf(String url) {
        return firstNonBlank(url).startsWith("/api/papers/uploads/");
    }

    private boolean isDesktopCachedPdf(String url) {
        return firstNonBlank(url).startsWith("desktop-cache://");
    }

    private boolean isReadableCachedPdf(String url) {
        return isLocalCachedPdf(url) || isDesktopCachedPdf(url);
    }

    private String initialImportance(SearchPaperVO enriched, PaperImportRequest request) {
        String source = firstNonBlank(enriched == null ? "" : enriched.getSource(), request.getSource()).toLowerCase(Locale.ROOT);
        String type = firstNonBlank(enriched == null ? "" : enriched.getArticleType(), request.getArticleType()).toLowerCase(Locale.ROOT);
        if (isHighImpactVenue(source) || source.matches(".*(neurips|icml|iclr|cvpr|acl|kdd|sigir).*")) return "A";
        if (type.contains("review") || type.contains("conference")) return "B";
        return "B";
    }

    private String inferVenueType(String source, String articleType) {
        String type = String.valueOf(articleType).toLowerCase(Locale.ROOT);
        if (type.contains("conference") || type.contains("proceedings")) return "会议";
        if (firstNonBlank(source).toLowerCase(Locale.ROOT).contains("procedia")) return "会议";
        if (type.contains("preprint")) return "预印本";
        if (type.contains("dissertation")) return "学位论文";
        if (type.contains("book")) return "图书章节";
        return researchDataService.inferVenueType(source);
    }

    private String inferVenueRanking(String source, String venueType, SearchPaperVO enriched) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        if ("预印本".equals(venueType)) return "预印本";
        if ("学位论文".equals(venueType)) return "学位论文";
        if ("图书章节".equals(venueType)) return "图书章节";
        if (normalized.matches(".*(neurips|icml|iclr|cvpr|acl|emnlp|naacl|kdd|sigir|aaai|ijcai).*")) {
            return researchDataService.inferVenueRanking(source, "会议");
        }
        if (isHighImpactVenue(source)) return "顶级期刊";
        if (enriched != null && enriched.getSubjects() != null && !enriched.getSubjects().isEmpty()) return "JCR --";
        return "JCR --";
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

    private List<String> buildTags(SearchPaperVO enriched, PaperImportRequest request, String importSource, boolean pdfCached) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(pdfCached ? "PDF已缓存" : "待关联PDF");
        String articleType = firstNonBlank(enriched == null ? "" : enriched.getArticleType(), request.getArticleType());
        if (!articleType.isBlank() && !"Other".equalsIgnoreCase(articleType)) {
            tags.add(toTag(articleType));
        }
        List<String> subjects = new ArrayList<>();
        if (enriched != null && enriched.getSubjects() != null) subjects.addAll(enriched.getSubjects());
        if (request.getSubjects() != null) subjects.addAll(request.getSubjects());
        for (String subject : subjects) {
            String tag = toTag(subject);
            if (!tag.isBlank()) tags.add(tag);
            if (tags.size() >= 5) break;
        }
        String sourceTag = sourceTag(importSource);
        if (!sourceTag.isBlank()) tags.add(sourceTag);
        if (tags.size() < 2) tags.add("待精读");
        return tags.stream().limit(5).toList();
    }

    private String toTag(String value) {
        String tag = firstNonBlank(value)
            .replaceAll("(?i)^research article$", "研究论文")
            .replaceAll("(?i)^conference paper$", "会议论文")
            .replaceAll("(?i)^journal-article$", "期刊论文")
            .replaceAll("(?i)^posted-content$", "预印本");
        return limit(tag, 24);
    }

    private String sourceTag(String source) {
        String normalized = firstNonBlank(source).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return "";
        if (normalized.contains("pubmed")) return "PubMed";
        if (normalized.contains("cnki") || normalized.contains("知网")) return "知网";
        if (normalized.contains("sciencedirect") || normalized.contains("elsevier")) return "ScienceDirect";
        if (normalized.contains("arxiv")) return "arXiv";
        if (normalized.contains("semanticscholar")) return "Semantic Scholar";
        return limit(source, 24);
    }

    private String normalizePdfUrl(String url) {
        String normalized = unwrapProxyUrl(firstNonBlank(url));
        if (normalized.isBlank()) return "";
        normalized = normalized
            .replace("http://arxiv.org/", "https://arxiv.org/")
            .replace("http://export.arxiv.org/", "https://export.arxiv.org/");
        if (normalized.contains("arxiv.org/abs/")) {
            normalized = normalized.replace("/abs/", "/pdf/");
            if (!normalized.endsWith(".pdf")) {
                normalized = normalized + ".pdf";
            }
        }
        if (normalized.contains("aclanthology.org/") && !normalized.toLowerCase().endsWith(".pdf")) {
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
        String normalized = url == null ? "" : url.toLowerCase();
        return (normalized.startsWith("http://") || normalized.startsWith("https://"))
            && (normalized.contains(".pdf")
                || normalized.contains("/pdf/")
                || normalized.contains("/pdfft")
                || isZoteroLocalFileUrl(normalized)
                || normalized.contains("pdf.sciencedirectassets.com")
                || normalized.contains("arxiv.org/pdf/"));
    }

    private boolean isZoteroLocalFileUrl(String url) {
        String normalized = firstNonBlank(url).toLowerCase(Locale.ROOT).replace("localhost", "127.0.0.1");
        return normalized.matches("https?://127\\.0\\.0\\.1:23119/api/users/\\d+/items/[^/]+/file.*");
    }

    private boolean isLocalPdfFileUrl(String url) {
        String normalized = firstNonBlank(url).toLowerCase(Locale.ROOT);
        return normalized.startsWith("file:/") && normalized.contains(".pdf");
    }

    private boolean isPublisherAssetOrPdfUrl(String url) {
        String normalized = firstNonBlank(url).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return false;
        return normalized.contains("pdf.sciencedirectassets.com")
            || normalized.contains("sciencedirectassets.com")
            || normalized.contains("els-cdn.com")
            || normalized.matches(".*\\.(pdf)(\\?|#|$).*");
    }

    private boolean looksLikePdf(byte[] body) {
        return body != null
            && body.length >= 4
            && body[0] == '%'
            && body[1] == 'P'
            && body[2] == 'D'
            && body[3] == 'F';
    }

    private String refererFor(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() + "://" + uri.getHost() + "/";
        } catch (Exception error) {
            return "https://scholar.google.com/";
        }
    }

    private LibraryPaperVO toLibraryPaper(PaperEntity entity) {
        return new LibraryPaperVO(
            entity.getWorkspaceId(),
            entity.getTitle(),
            entity.getSource(),
            entity.getAuthors(),
            entity.getProgress(),
            entity.getImportance(),
            entity.getNote(),
            List.of(entity.getJournalTags().split(",")),
            entity.getVenueType(),
            entity.getVenueRanking(),
            entity.getPublishYear(),
            entity.getReadAt().toString().replace("T", " "),
            entity.getUploadedAt().toString(),
            entity.getPaperUrl(),
            entity.getSourceUrl(),
            entity.getImportSource(),
            entity.getAbstractText()
        );
    }

    private String inferAuthors(PaperImportRequest request) {
        if (request.getAuthors() != null && !request.getAuthors().isBlank()) {
            return request.getAuthors().trim();
        }
        return "作者待补全";
    }

    private String hostLabel(String url) {
        String value = firstNonBlank(url);
        if (value.isBlank()) return "";
        try {
            String host = URI.create(value).getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        } catch (Exception error) {
            return "";
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private boolean isPlaceholderAbstract(String value) {
        String normalized = firstNonBlank(value);
        return normalized.isBlank()
            || normalized.equals("暂无摘要，可在阅读时补充。")
            || normalized.contains("摘要待补充")
            || normalized.contains("由 PaperSolver Capture 从官网页面导入");
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
