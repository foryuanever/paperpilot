package com.paperpilot.server.service;

import com.paperpilot.server.vo.SearchPaperVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ExternalSearchService {

    private static final String CROSSREF_API = "https://api.crossref.org/works?query=%s&rows=20";
    private static final String UNPAYWALL_API = "https://api.unpaywall.org/v2/%s?email=paperpilot-app@outlook.com";
    private final RestTemplate restTemplate;
    private final RestTemplate unpaywallRestTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public ExternalSearchService() {
        org.springframework.http.client.SimpleClientHttpRequestFactory standardFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        standardFactory.setConnectTimeout(6000); // 6s connection timeout
        standardFactory.setReadTimeout(8000);    // 8s read timeout
        this.restTemplate = new RestTemplate(standardFactory);

        org.springframework.http.client.SimpleClientHttpRequestFactory unpaywallFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        unpaywallFactory.setConnectTimeout(1000); // 1s connection timeout
        unpaywallFactory.setReadTimeout(1200);    // 1.2s read timeout
        this.unpaywallRestTemplate = new RestTemplate(unpaywallFactory);
    }

    public com.paperpilot.server.vo.SearchResultVO searchByQuery(String query, int page, int pageSize) {
        return searchByQuery(query, "crossref", page, pageSize);
    }

    public com.paperpilot.server.vo.SearchResultVO searchByQuery(String query, String source, int page, int pageSize) {
        String normalizedSource = source == null ? "crossref" : source.toLowerCase(Locale.ROOT).trim();
        return switch (normalizedSource) {
            case "semantic-scholar" -> searchSemanticScholar(query);
            case "pubmed" -> searchPubMed(query);
            case "sciencedirect" -> searchScienceDirect(query);
            case "web-of-science" -> searchWebOfScience(query);
            case "cnki" -> searchConfiguredOfficialSource(
                query,
                "cnki",
                "知网",
                "PAPERPILOT_CNKI_SEARCH_URL_TEMPLATE",
                "知网官网检索需要机构授权接口。请配置 PAPERPILOT_CNKI_SEARCH_URL_TEMPLATE，模板中用 {query} 表示搜索词。"
            );
            case "wanfang" -> searchConfiguredOfficialSource(
                query,
                "wanfang",
                "万方",
                "PAPERPILOT_WANFANG_SEARCH_URL_TEMPLATE",
                "万方官网检索需要机构授权接口。请配置 PAPERPILOT_WANFANG_SEARCH_URL_TEMPLATE，模板中用 {query} 表示搜索词。"
            );
            case "research-rabbit" -> configurationRequired(
                "research-rabbit",
                "Research Rabbit 不提供传统关键词论文列表开放 API；它的官方能力是基于已导入论文构图。请用 DOI/URL 导入论文后再做关联图谱。"
            );
            case "connected-papers" -> configurationRequired(
                "connected-papers",
                "Connected Papers 不提供可直接复刻官网图谱结果的开放关键词检索 API。请用 DOI/URL 导入论文后再打开官方图谱。"
            );
            case "crossref" -> searchCrossref(query);
            default -> searchCrossref(query);
        };
    }

    private com.paperpilot.server.vo.SearchResultVO withMeta(
            com.paperpilot.server.vo.SearchResultVO result,
            String source,
            boolean official,
            String message
    ) {
        result.setSource(source);
        result.setOfficial(official);
        result.setMessage(message == null ? "" : message);
        return result;
    }

    private com.paperpilot.server.vo.SearchResultVO configurationRequired(String source, String message) {
        com.paperpilot.server.vo.SearchResultVO result = new com.paperpilot.server.vo.SearchResultVO(List.of(), 0);
        result.setSource(source);
        result.setOfficial(true);
        result.setRequiresConfiguration(true);
        result.setMessage(message);
        return result;
    }

    private com.paperpilot.server.vo.SearchResultVO searchCrossref(String query) {
        if (query == null) query = "";
        String cleanQuery = query.trim();
        String encoded = URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8);
        List<SearchPaperVO> results = new ArrayList<>();
        int fetchRows = 100;
        int maxBatches = 6;
        int maxResults = 180;
        for (int batch = 0; batch < maxBatches && results.size() < maxResults; batch++) {
            int offset = batch * fetchRows;
            String url = "https://api.crossref.org/works?query.bibliographic=" + encoded
                + "&rows=" + fetchRows
                + "&offset=" + offset
                + "&select=DOI,title,container-title,author,published-print,published-online,abstract,URL,type,subject";
            try {
                String response = restTemplate.getForObject(URI.create(url), String.class);
                JsonNode root = mapper.readTree(response);
                JsonNode items = root.path("message").path("items");
                if (!items.isArray() || items.isEmpty()) break;
                List<java.util.concurrent.CompletableFuture<SearchPaperVO>> futures = new ArrayList<>();
                for (JsonNode item : items) {
                    futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                        try {
                            return parseItem(item, false);
                        } catch (Exception e) {
                            System.err.println("Error parsing search item: " + e.getMessage());
                            return null;
                        }
                    }));
                }
                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
                for (java.util.concurrent.CompletableFuture<SearchPaperVO> future : futures) {
                    try {
                        SearchPaperVO vo = future.get();
                        if (vo != null && isUsefulSearchResult(vo, cleanQuery)) {
                            results.add(vo);
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                if (items.size() < fetchRows) break;
            } catch (Exception e) {
                System.err.println("Error in searchByQuery: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
        List<SearchPaperVO> ranked = dedupeByTitle(results).stream()
            .sorted(Comparator.comparingInt((SearchPaperVO paper) -> relevanceScore(paper, cleanQuery)).reversed())
            .limit(maxResults)
            .toList();
        return withMeta(new com.paperpilot.server.vo.SearchResultVO(ranked, ranked.size()), "crossref", false, "当前为 Crossref/Unpaywall 开放索引结果。");
    }

    private com.paperpilot.server.vo.SearchResultVO searchSemanticScholar(String query) {
        if (query == null || query.isBlank()) {
            return new com.paperpilot.server.vo.SearchResultVO(List.of(), 0);
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String fields = "paperId,title,abstract,year,authors,venue,url,externalIds,openAccessPdf,publicationTypes,fieldsOfStudy";
        String url = "https://api.semanticscholar.org/graph/v1/paper/search?query=" + encoded
            + "&limit=100&fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8);
        try {
            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = mapper.readTree(response);
            List<SearchPaperVO> results = new ArrayList<>();
            for (JsonNode item : root.path("data")) {
                String id = item.path("externalIds").path("DOI").asText(item.path("paperId").asText(""));
                String title = item.path("title").asText("");
                if (title.isBlank()) continue;
                String authors = extractSemanticAuthors(item.path("authors"));
                String pdfUrl = item.path("openAccessPdf").path("url").asText("");
                String sourceUrl = item.path("url").asText("");
                List<String> subjects = extractStringList(item.path("fieldsOfStudy"), 6);
                String type = extractFirst(item.path("publicationTypes"));
                results.add(new SearchPaperVO(
                    id,
                    title,
                    item.path("venue").asText("Semantic Scholar"),
                    authors,
                    item.path("year").asText(""),
                    item.path("abstract").asText(""),
                    pdfUrl,
                    sourceUrl,
                    type.isBlank() ? "Research article" : type,
                    subjects
                ));
            }
            List<SearchPaperVO> deduped = dedupeByTitle(results);
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(deduped, deduped.size()), "semantic-scholar", true, "Semantic Scholar 官方 Graph API 结果。");
        } catch (Exception error) {
            System.err.println("Error in searchSemanticScholar: " + error.getMessage());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), "semantic-scholar", true, "Semantic Scholar 官方接口暂时不可用，可能触发限流。");
        }
    }

    private com.paperpilot.server.vo.SearchResultVO searchScienceDirect(String query) {
        String apiKey = System.getenv("PAPERPILOT_ELSEVIER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return configurationRequired(
                "sciencedirect",
                "ScienceDirect 官网结果必须使用 Elsevier 官方 API Key。请配置 PAPERPILOT_ELSEVIER_API_KEY；未配置时不能用 Crossref 冒充官网结果。"
            );
        }
        if (query == null || query.isBlank()) {
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), "sciencedirect", true, "");
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = "https://api.elsevier.com/content/search/sciencedirect?query=" + encoded
            + "&count=100&apiKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        try {
            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode entries = root.path("search-results").path("entry");
            List<SearchPaperVO> papers = new ArrayList<>();
            if (entries.isArray()) {
                for (JsonNode item : entries) {
                    String title = item.path("dc:title").asText("");
                    if (title.isBlank()) continue;
                    String doi = item.path("prism:doi").asText(item.path("doi").asText(""));
                    papers.add(new SearchPaperVO(
                        doi.isBlank() ? item.path("pii").asText(title) : doi,
                        title,
                        item.path("prism:publicationName").asText("ScienceDirect"),
                        item.path("dc:creator").asText(""),
                        item.path("prism:coverDate").asText("").replaceAll("^(\\d{4}).*$", "$1"),
                        item.path("dc:description").asText(""),
                        "",
                        item.path("prism:url").asText(item.path("link").path(0).path("@href").asText("")),
                        item.path("subtypeDescription").asText("Research article"),
                        List.of("ScienceDirect")
                    ));
                }
            }
            int total = root.path("search-results").path("opensearch:totalResults").asInt(papers.size());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(papers, total), "sciencedirect", true, "ScienceDirect 官方 Elsevier API 结果。");
        } catch (Exception error) {
            System.err.println("Error in searchScienceDirect: " + error.getMessage());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), "sciencedirect", true, "ScienceDirect 官方接口调用失败，请检查 Elsevier API Key 权限。");
        }
    }

    private com.paperpilot.server.vo.SearchResultVO searchWebOfScience(String query) {
        String apiKey = System.getenv("PAPERPILOT_WOS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return configurationRequired(
                "web-of-science",
                "Web of Science 官网结果必须使用 Clarivate 官方 API Key。请配置 PAPERPILOT_WOS_API_KEY；未配置时不能用其他库冒充 WoS。"
            );
        }
        return configurationRequired(
            "web-of-science",
            "已检测到 Web of Science API Key 配置入口，但当前项目还需要按你的 Clarivate API 版本补充 endpoint 映射。请确认使用 Starter API 还是 Expanded API。"
        );
    }

    private com.paperpilot.server.vo.SearchResultVO searchConfiguredOfficialSource(
            String query,
            String source,
            String label,
            String envKey,
            String missingMessage
    ) {
        String template = System.getenv(envKey);
        if (template == null || template.isBlank()) {
            return configurationRequired(source, missingMessage);
        }
        if (query == null || query.isBlank()) {
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), source, true, "");
        }
        String url = template.replace("{query}", URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        try {
            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = mapper.readTree(response);
            List<SearchPaperVO> papers = parseGenericOfficialJson(root, label);
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(papers, papers.size()), source, true, label + "机构官方接口结果。");
        } catch (Exception error) {
            System.err.println("Error in searchConfiguredOfficialSource " + source + ": " + error.getMessage());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), source, true, label + "官方接口调用失败，请检查接口模板和授权。");
        }
    }

    private List<SearchPaperVO> parseGenericOfficialJson(JsonNode root, String label) {
        JsonNode items = root.path("items");
        if (!items.isArray()) items = root.path("data");
        if (!items.isArray()) items = root.path("results");
        List<SearchPaperVO> papers = new ArrayList<>();
        if (!items.isArray()) return papers;
        for (JsonNode item : items) {
            String title = firstText(item, "title", "name", "paperTitle", "articleTitle");
            if (title.isBlank()) continue;
            String id = firstText(item, "doi", "id", "paperId", "uid");
            String source = firstText(item, "source", "journal", "publicationName", "venue");
            String authors = firstText(item, "authors", "author", "creator");
            String year = firstText(item, "year", "publishYear", "publicationYear");
            String abstractText = firstText(item, "abstract", "abstractText", "summary");
            String pdfUrl = firstText(item, "pdfUrl", "pdf", "fullTextUrl");
            String sourceUrl = firstText(item, "sourceUrl", "url", "link");
            papers.add(new SearchPaperVO(
                id.isBlank() ? title : id,
                title,
                source.isBlank() ? label : source,
                authors,
                year,
                abstractText,
                pdfUrl,
                sourceUrl,
                firstText(item, "articleType", "type", "documentType"),
                List.of(label)
            ));
        }
        return papers;
    }

    private String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.path(key);
            if (value.isTextual() || value.isNumber()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) return text;
            }
            if (value.isArray() && value.size() > 0) {
                if (value.get(0).isTextual()) return value.get(0).asText("").trim();
                if (value.get(0).path("name").isTextual()) return value.get(0).path("name").asText("").trim();
            }
        }
        return "";
    }

    private com.paperpilot.server.vo.SearchResultVO searchPubMed(String query) {
        if (query == null || query.isBlank()) {
            return new com.paperpilot.server.vo.SearchResultVO(List.of(), 0);
        }
        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        try {
            String searchUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&retmode=json&retmax=100&term=" + encoded;
            JsonNode searchRoot = mapper.readTree(restTemplate.getForObject(URI.create(searchUrl), String.class));
            JsonNode idList = searchRoot.path("esearchresult").path("idlist");
            if (!idList.isArray() || idList.isEmpty()) {
                return new com.paperpilot.server.vo.SearchResultVO(List.of(), 0);
            }
            List<String> ids = new ArrayList<>();
            for (JsonNode id : idList) ids.add(id.asText());
            String summaryUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&id="
                + URLEncoder.encode(String.join(",", ids), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(restTemplate.getForObject(URI.create(summaryUrl), String.class));
            JsonNode result = root.path("result");
            List<SearchPaperVO> papers = new ArrayList<>();
            for (String id : ids) {
                JsonNode item = result.path(id);
                String title = item.path("title").asText("");
                if (title.isBlank()) continue;
                String year = item.path("pubdate").asText("").replaceAll("^(\\d{4}).*$", "$1");
                papers.add(new SearchPaperVO(
                    "pubmed-" + id,
                    title,
                    item.path("fulljournalname").asText("PubMed"),
                    extractPubMedAuthors(item.path("authors")),
                    year,
                    "",
                    "",
                    "https://pubmed.ncbi.nlm.nih.gov/" + id + "/",
                    "Research article",
                    List.of("Medicine and Life Sciences")
                ));
            }
            int total = searchRoot.path("esearchresult").path("count").asInt(papers.size());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(papers, total), "pubmed", true, "PubMed 官方 NCBI E-utilities 结果。");
        } catch (Exception error) {
            System.err.println("Error in searchPubMed: " + error.getMessage());
            return withMeta(new com.paperpilot.server.vo.SearchResultVO(List.of(), 0), "pubmed", true, "PubMed 官方接口暂时不可用。");
        }
    }

    public SearchPaperVO searchByUrlOrDoi(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        input = unwrapProxyUrl(input.trim());

        // 1. Check for DOI in the input
        String doi = extractDoi(input);
        if (doi != null) {
            try {
                String url = "https://api.crossref.org/works/" + URLEncoder.encode(doi, StandardCharsets.UTF_8);
                String response = restTemplate.getForObject(URI.create(url), String.class);
                return parseSingleCrossrefWork(response);
            } catch (Exception e) {
                // If direct lookup fails, fall back to searching DOI as query
                return searchFirstItem("https://api.crossref.org/works?query=" + URLEncoder.encode(doi, StandardCharsets.UTF_8) + "&rows=1");
            }
        }

        // 2. Check for arXiv URL or ID
        String arxivId = extractArxivId(input);
        if (arxivId != null) {
            try {
                String url = "https://export.arxiv.org/api/query?id_list=" + arxivId;
                String response = restTemplate.getForObject(URI.create(url), String.class);
                return parseArxivXml(response, arxivId);
            } catch (Exception e) {
                // Ignore and fall through
            }
        }

        // 3. Check for ScienceDirect PII
        String pii = extractPii(input);
        if (pii != null) {
            SearchPaperVO scienceDirectPaper = fetchScienceDirectArticleMetadata(pii);
            if (scienceDirectPaper != null && scienceDirectPaper.getTitle() != null && !scienceDirectPaper.getTitle().isBlank()) {
                return scienceDirectPaper;
            }
            return new SearchPaperVO(
                "pii-" + pii,
                "",
                "ScienceDirect",
                "",
                "",
                "",
                normalizeScienceDirectPdfUrl(input, pii),
                "https://www.sciencedirect.com/science/article/pii/" + pii,
                "Research article",
                List.of("ScienceDirect")
            );
        }

        // 4. Default: If it's a URL, clean it up and try to query Crossref
        if (input.startsWith("http://") || input.startsWith("https://")) {
            if (isPublisherAssetUrl(input)) {
                return null;
            }
            return searchFirstItem("https://api.crossref.org/works?query=" + URLEncoder.encode(input, StandardCharsets.UTF_8) + "&rows=1");
        }

        // 5. If it's just a general query/identifier, search it
        return searchFirstItem("https://api.crossref.org/works?query=" + URLEncoder.encode(input, StandardCharsets.UTF_8) + "&rows=1");
    }

    private String extractDoi(String input) {
        if (input.startsWith("10.") && input.contains("/")) {
            return trimDoi(input);
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return trimDoi(matcher.group());
        }
        return null;
    }

    private String trimDoi(String doi) {
        if (doi == null) return null;
        return doi.trim().replaceAll("[)\\].,;，。；、]+$", "");
    }

    private String unwrapProxyUrl(String input) {
        if (input == null || input.isBlank()) return "";
        try {
            URI uri = URI.create(input);
            String path = uri.getPath();
            String query = uri.getRawQuery();
            if (path != null && path.contains("/api/papers/proxy") && query != null) {
                for (String part : query.split("&")) {
                    int equals = part.indexOf('=');
                    if (equals <= 0) continue;
                    String key = java.net.URLDecoder.decode(part.substring(0, equals), StandardCharsets.UTF_8);
                    if ("url".equals(key)) {
                        return java.net.URLDecoder.decode(part.substring(equals + 1), StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return input;
    }

    private String extractArxivId(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:arxiv\\.org/(?:abs|pdf)/|arxiv:)?(\\d{4}\\.\\d{4,5})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractPii(String input) {
        try {
            URI uri = URI.create(input);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String part : query.split("&")) {
                    int equals = part.indexOf('=');
                    if (equals <= 0) continue;
                    String key = java.net.URLDecoder.decode(part.substring(0, equals), StandardCharsets.UTF_8);
                    if ("pii".equalsIgnoreCase(key)) {
                        String value = java.net.URLDecoder.decode(part.substring(equals + 1), StandardCharsets.UTF_8).trim();
                        if (value.matches("(?i)S[A-Z0-9]{15,30}")) return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        java.util.regex.Pattern articlePathPattern = java.util.regex.Pattern.compile("(?:/pii/|1-s2\\.0-)(S[A-Za-z0-9]{15,30})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher articlePathMatcher = articlePathPattern.matcher(input);
        String last = null;
        while (articlePathMatcher.find()) {
            last = articlePathMatcher.group(1);
        }
        if (last != null) return last;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(S[A-Za-z0-9]{15,25})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            last = matcher.group(1);
        }
        return last;
    }

    private boolean isPublisherAssetUrl(String input) {
        String normalized = String.valueOf(input).toLowerCase(Locale.ROOT);
        return normalized.contains("pdf.sciencedirectassets.com")
            || normalized.contains("els-cdn.com")
            || normalized.contains("sciencedirectassets.com")
            || normalized.matches(".*\\.(pdf)(\\?|#|$).*");
    }

    private SearchPaperVO fetchScienceDirectArticleMetadata(String pii) {
        SearchPaperVO apiPaper = fetchScienceDirectCoredata(pii);
        if (apiPaper != null && apiPaper.getTitle() != null && !apiPaper.getTitle().isBlank()) {
            return apiPaper;
        }
        try {
            String articleUrl = "https://www.sciencedirect.com/science/article/pii/" + pii;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 PaperSolver/1.0");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                URI.create(articleUrl),
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                String.class
            );
            String html = response.getBody();
            if (html == null || html.isBlank()) return null;
            String title = firstHtmlMeta(html, "citation_title", "dc.Title", "og:title");
            String doi = firstHtmlMeta(html, "citation_doi", "prism.doi", "dc.identifier");
            String source = firstHtmlMeta(html, "citation_journal_title", "prism.publicationName");
            String authors = String.join(", ", allHtmlMeta(html, "citation_author"));
            String year = firstHtmlMeta(html, "citation_publication_date", "prism.coverDate").replaceAll("^(\\d{4}).*$", "$1");
            String abstractText = firstHtmlMeta(html, "description", "dc.description");
            String pdfUrl = firstHtmlMeta(html, "citation_pdf_url");
            if (pdfUrl.isBlank()) pdfUrl = "https://www.sciencedirect.com/science/article/pii/" + pii + "/pdfft";
            return new SearchPaperVO(
                doi.isBlank() ? "pii-" + pii : doi,
                title,
                source.isBlank() ? "ScienceDirect" : source,
                authors,
                year,
                abstractText,
                pdfUrl,
                articleUrl,
                "Research article",
                List.of("ScienceDirect")
            );
        } catch (Exception error) {
            return null;
        }
    }

    private SearchPaperVO fetchScienceDirectCoredata(String pii) {
        try {
            String apiUrl = "https://api.elsevier.com/content/article/pii/"
                + URLEncoder.encode(pii, StandardCharsets.UTF_8)
                + "?httpAccept=application/json";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36 PaperSolver/1.0");
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                URI.create(apiUrl),
                org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers),
                String.class
            );
            JsonNode root = mapper.readTree(response.getBody()).path("full-text-retrieval-response");
            JsonNode coredata = root.path("coredata");
            if (coredata.isMissingNode() || coredata.isNull()) return null;
            String doi = stripDoiPrefix(coredata.path("prism:doi").asText(coredata.path("dc:identifier").asText("")));
            String title = coredata.path("dc:title").asText("").replaceAll("\\s+", " ").trim();
            String source = coredata.path("prism:publicationName").asText("ScienceDirect").trim();
            String year = coredata.path("prism:coverDate").asText(coredata.path("prism:coverDisplayDate").asText("")).replaceAll("^(\\d{4}).*$", "$1");
            String abstractText = coredata.path("dc:description").asText("");
            String authors = firstNonBlank(
                extractElsevierCreators(coredata.path("dc:creator")),
                extractElsevierAuthors(root.path("authors").path("author"))
            );
            if (authors.isBlank() && doi.startsWith("10.")) {
                try {
                    String crossrefUrl = "https://api.crossref.org/works/" + URLEncoder.encode(doi, StandardCharsets.UTF_8);
                    SearchPaperVO crossrefPaper = parseSingleCrossrefWork(restTemplate.getForObject(URI.create(crossrefUrl), String.class));
                    if (crossrefPaper != null) {
                        authors = crossrefPaper.getAuthors();
                        if (abstractText.isBlank()) abstractText = crossrefPaper.getAbstractText();
                    }
                } catch (Exception ignored) {
                }
            }
            String articleUrl = firstElsevierLink(coredata.path("link"), "scidir");
            if (articleUrl.isBlank()) articleUrl = "https://www.sciencedirect.com/science/article/pii/" + pii;
            String pdfUrl = firstElsevierLink(coredata.path("link"), "scidirpdf");
            if (pdfUrl.isBlank()) pdfUrl = articleUrl + "/pdfft";
            return new SearchPaperVO(
                doi.isBlank() ? "pii-" + pii : doi,
                title,
                source.isBlank() ? "ScienceDirect" : source,
                authors,
                year,
                abstractText,
                pdfUrl,
                articleUrl,
                "Research article",
                List.of("ScienceDirect")
            );
        } catch (Exception error) {
            return null;
        }
    }

    private String stripDoiPrefix(String value) {
        String normalized = String.valueOf(value == null ? "" : value).trim();
        return normalized.replaceFirst("(?i)^doi:", "").trim();
    }

    private String extractElsevierCreators(JsonNode creators) {
        List<String> names = new ArrayList<>();
        if (creators.isArray()) {
            for (JsonNode creator : creators) {
                String name = extractElsevierName(creator);
                if (!name.isBlank() && !names.contains(name)) names.add(name);
            }
        } else {
            String name = extractElsevierName(creators);
            if (!name.isBlank()) names.add(name);
        }
        return String.join(", ", names);
    }

    private String extractElsevierAuthors(JsonNode authors) {
        List<String> names = new ArrayList<>();
        if (authors.isArray()) {
            for (JsonNode author : authors) {
                String name = extractElsevierName(author);
                if (!name.isBlank() && !names.contains(name)) names.add(name);
            }
        } else {
            String name = extractElsevierName(authors);
            if (!name.isBlank()) names.add(name);
        }
        return String.join(", ", names);
    }

    private String extractElsevierName(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        if (node.isTextual()) return node.asText("").trim();
        String indexed = firstNonBlank(
            node.path("ce:indexed-name").asText(""),
            node.path("indexed-name").asText(""),
            node.path("preferred-name").path("ce:indexed-name").asText(""),
            node.path("preferred-name").path("indexed-name").asText("")
        );
        if (!indexed.isBlank()) return indexed;
        String given = firstNonBlank(
            node.path("ce:given-name").asText(""),
            node.path("given-name").asText(""),
            node.path("preferred-name").path("ce:given-name").asText(""),
            node.path("preferred-name").path("given-name").asText("")
        );
        String surname = firstNonBlank(
            node.path("ce:surname").asText(""),
            node.path("surname").asText(""),
            node.path("preferred-name").path("ce:surname").asText(""),
            node.path("preferred-name").path("surname").asText("")
        );
        return (given + " " + surname).replaceAll("\\s+", " ").trim();
    }

    private String firstElsevierLink(JsonNode links, String rel) {
        if (!links.isArray()) return "";
        for (JsonNode link : links) {
            String linkRel = link.path("@rel").asText("");
            String href = link.path("@href").asText("");
            if (rel.equalsIgnoreCase(linkRel) && !href.isBlank()) return href;
        }
        return "";
    }

    private String normalizeScienceDirectPdfUrl(String input, String pii) {
        if (input != null && input.startsWith("http")) return input;
        return "https://www.sciencedirect.com/science/article/pii/" + pii + "/pdfft";
    }

    private String firstHtmlMeta(String html, String... names) {
        for (String name : names) {
            List<String> values = allHtmlMeta(html, name);
            if (!values.isEmpty()) return values.get(0);
        }
        return "";
    }

    private List<String> allHtmlMeta(String html, String name) {
        List<String> values = new ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "<meta\\b(?=[^>]*(?:name|property)=[\"']" + java.util.regex.Pattern.quote(name) + "[\"'])(?=[^>]*content=[\"']([^\"']*)[\"'])[^>]*>",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String value = decodeHtml(matcher.group(1)).trim();
            if (!value.isBlank() && !values.contains(value)) values.add(value);
        }
        return values;
    }

    private String decodeHtml(String text) {
        return String.valueOf(text)
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    private SearchPaperVO searchFirstItem(String url) {
        try {
            String response = restTemplate.getForObject(URI.create(url), String.class);
            JsonNode root = mapper.readTree(response);
            JsonNode items = root.path("message").path("items");
            if (items.isArray() && items.size() > 0) {
                return parseItem(items.get(0), true);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private SearchPaperVO parseSingleCrossrefWork(String jsonStr) {
        try {
            JsonNode root = mapper.readTree(jsonStr);
            JsonNode message = root.path("message");
            if (message.isMissingNode() || message.isNull()) {
                return null;
            }
            return parseItem(message, true);
        } catch (Exception e) {
            return null;
        }
    }

    private SearchPaperVO parseItem(JsonNode item) {
        return parseItem(item, true);
    }

    private SearchPaperVO parseItem(JsonNode item, boolean includePdfLookup) {
        String id = item.path("DOI").asText("");
        String type = item.path("type").asText("");
        String title = extractFirst(item.path("title"));
        if (isNonPaperCrossrefType(type, id, title)) {
            return null;
        }
        String source = extractFirst(item.path("container-title"));
        String authors = extractAuthors(item.path("author"));
        String year = extractYear(item);
        String articleType = readableType(type);
        List<String> subjects = extractStringList(item.path("subject"), 6);
        String abstractText = item.path("abstract").asText("");
        abstractText = cleanAbstract(abstractText);
        String pdfUrl = inferPdfUrl(id);
        if (pdfUrl.isBlank() && includePdfLookup) {
            pdfUrl = fetchPdfUrlFromUnpaywall(id);
        }
        String sourceUrl = item.path("URL").asText("");
        if (sourceUrl.isBlank() && !id.isBlank()) {
            sourceUrl = "https://doi.org/" + id;
        }
        return new SearchPaperVO(id, title, source, authors, year, abstractText, pdfUrl, sourceUrl, articleType, subjects);
    }

    private String readableType(String type) {
        return switch (String.valueOf(type).toLowerCase(Locale.ROOT)) {
            case "journal-article" -> "Research article";
            case "proceedings-article" -> "Conference paper";
            case "posted-content" -> "Preprint";
            case "book-chapter" -> "Book chapter";
            case "book" -> "Book";
            case "dissertation" -> "Dissertation";
            case "report" -> "Report";
            default -> type == null || type.isBlank() ? "Other" : type.replace("-", " ");
        };
    }

    private List<String> extractStringList(JsonNode arrayNode, int limit) {
        List<String> values = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode node : arrayNode) {
                String value = node.asText("").trim();
                if (!value.isBlank() && !values.contains(value)) {
                    values.add(value);
                }
                if (values.size() >= limit) break;
            }
        }
        return values;
    }

    private String inferPdfUrl(String doi) {
        if (doi == null || doi.isBlank()) return "";
        String normalized = doi.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("10.18653/v1/")) {
            return "https://aclanthology.org/" + doi.substring("10.18653/v1/".length()) + ".pdf";
        }
        return "";
    }

    private boolean isNonPaperCrossrefType(String type, String doi, String title) {
        String normalizedType = String.valueOf(type).toLowerCase(Locale.ROOT);
        if (normalizedType.contains("component")
            || normalizedType.contains("book-chapter")
            || normalizedType.contains("journal-issue")
            || normalizedType.contains("proceedings-series")
            || normalizedType.contains("reference-entry")
            || normalizedType.contains("dataset")) {
            return true;
        }
        String normalizedDoi = String.valueOf(doi).toLowerCase(Locale.ROOT);
        if (normalizedDoi.matches(".*[/.-](fig|figure|table|supp|supplement|media|dataset)[-_]?[0-9a-z]+.*")) {
            return true;
        }
        String normalizedTitle = String.valueOf(title).trim().toLowerCase(Locale.ROOT);
        return normalizedTitle.matches("^(figure|fig\\.?|table|supplementary|appendix)\\s+\\d+[:：].*");
    }

    private boolean isUsefulSearchResult(SearchPaperVO paper, String query) {
        if (paper == null || paper.getTitle() == null || paper.getTitle().isBlank()) {
            return false;
        }
        String title = paper.getTitle().toLowerCase(Locale.ROOT);
        String source = String.valueOf(paper.getSource()).toLowerCase(Locale.ROOT);
        String abstractText = String.valueOf(paper.getAbstractText()).toLowerCase(Locale.ROOT);
        if (title.length() < 6) return false;
        if (title.matches("^(figure|fig\\.?|table|supplementary|appendix)\\s+.*")) return false;
        return relevanceScore(paper, query) > 0 || !paper.getAuthors().isBlank() || !abstractText.isBlank() || !source.isBlank();
    }

    private List<SearchPaperVO> dedupeByTitle(List<SearchPaperVO> papers) {
        Map<String, SearchPaperVO> byTitle = new LinkedHashMap<>();
        for (SearchPaperVO paper : papers) {
            String key = paper.getTitle().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
            SearchPaperVO existing = byTitle.get(key);
            if (existing == null || qualityScore(paper) > qualityScore(existing)) {
                byTitle.put(key, paper);
            }
        }
        return new ArrayList<>(byTitle.values());
    }

    private int relevanceScore(SearchPaperVO paper, String query) {
        String normalizedQuery = String.valueOf(query).toLowerCase(Locale.ROOT).trim();
        if (normalizedQuery.isBlank()) return 0;
        String title = String.valueOf(paper.getTitle()).toLowerCase(Locale.ROOT);
        String source = String.valueOf(paper.getSource()).toLowerCase(Locale.ROOT);
        String abstractText = String.valueOf(paper.getAbstractText()).toLowerCase(Locale.ROOT);
        int score = 0;
        if (title.contains(normalizedQuery)) score += 12;
        for (String token : normalizedQuery.split("[^\\p{L}\\p{N}]+")) {
            if (token.length() < 3) continue;
            if (title.contains(token)) score += 4;
            if (abstractText.contains(token)) score += 2;
            if (source.contains(token)) score += 1;
        }
        return score + qualityScore(paper);
    }

    private int qualityScore(SearchPaperVO paper) {
        int score = 0;
        if (paper.getAbstractText() != null && !paper.getAbstractText().isBlank()) score += 4;
        if (paper.getPdfUrl() != null && !paper.getPdfUrl().isBlank()) score += 3;
        if (paper.getAuthors() != null && !paper.getAuthors().isBlank()) score += 2;
        if (paper.getSource() != null && !paper.getSource().isBlank()) score += 1;
        if (paper.getYear() != null && !paper.getYear().isBlank()) score += 1;
        return score;
    }

    private String cleanAbstract(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "").trim();
    }

    private SearchPaperVO parseArxivXml(String xml, String arxivId) {
        if (xml == null || !xml.contains("<entry>")) {
            return null;
        }
        int entryStart = xml.indexOf("<entry>");
        int entryEnd = xml.indexOf("</entry>", entryStart);
        if (entryStart == -1 || entryEnd == -1) {
            return null;
        }
        String entryXml = xml.substring(entryStart, entryEnd);

        String title = extractXmlTag(entryXml, "title");
        title = title.replaceAll("\\s+", " ");

        String summary = extractXmlTag(entryXml, "summary");
        summary = summary.replaceAll("\\s+", " ");

        String published = extractXmlTag(entryXml, "published");
        String year = published.length() >= 4 ? published.substring(0, 4) : "";

        List<String> authorsList = new ArrayList<>();
        int index = 0;
        while (true) {
            int authorStart = entryXml.indexOf("<author>", index);
            if (authorStart == -1) break;
            int authorEnd = entryXml.indexOf("</author>", authorStart);
            if (authorEnd == -1) break;
            String authorXml = entryXml.substring(authorStart, authorEnd);
            String name = extractXmlTag(authorXml, "name");
            if (!name.isEmpty()) {
                authorsList.add(name);
            }
            index = authorEnd;
        }
        String authors = String.join(", ", authorsList);
        String pdfUrl = "https://arxiv.org/pdf/" + arxivId + ".pdf";

        return new SearchPaperVO(
            "arxiv-" + arxivId,
            title,
            "arXiv",
            authors,
            year,
            summary,
            pdfUrl,
            "https://arxiv.org/abs/" + arxivId
        );
    }

    private String extractXmlTag(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";
        int start = xml.indexOf(startTag);
        if (start == -1) return "";
        int end = xml.indexOf(endTag, start + startTag.length());
        if (end == -1) return "";
        return xml.substring(start + startTag.length(), end).trim();
    }

    private String extractFirst(JsonNode arrayNode) {
        if (arrayNode.isArray() && arrayNode.size() > 0) {
            return arrayNode.get(0).asText("");
        }
        return "";
    }

    private String extractAuthors(JsonNode authorsNode) {
        if (authorsNode.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode a : authorsNode) {
                String given = a.path("given").asText("");
                String family = a.path("family").asText("");
                String name = (given + " " + family).trim();
                if (!name.isEmpty()) parts.add(name);
            }
            return String.join(", ", parts);
        }
        return "";
    }

    private String extractSemanticAuthors(JsonNode authorsNode) {
        if (!authorsNode.isArray()) return "";
        List<String> parts = new ArrayList<>();
        for (JsonNode author : authorsNode) {
            String name = author.path("name").asText("");
            if (!name.isBlank()) parts.add(name);
            if (parts.size() >= 8) break;
        }
        return String.join(", ", parts);
    }

    private String extractPubMedAuthors(JsonNode authorsNode) {
        if (!authorsNode.isArray()) return "";
        List<String> parts = new ArrayList<>();
        for (JsonNode author : authorsNode) {
            String name = author.path("name").asText("");
            if (!name.isBlank()) parts.add(name);
            if (parts.size() >= 8) break;
        }
        return String.join(", ", parts);
    }

    private String extractYear(JsonNode item) {
        JsonNode yearNode = item.path("published-print").path("date-parts");
        if (!yearNode.isArray() || yearNode.size() == 0) {
            yearNode = item.path("published-online").path("date-parts");
        }
        if (yearNode.isArray() && yearNode.size() > 0 && yearNode.get(0).isArray() && yearNode.get(0).size() > 0) {
            return yearNode.get(0).get(0).asText("");
        }
        return "";
    }

    private String fetchPdfUrlFromUnpaywall(String doi) {
        if (doi == null || doi.isBlank()) return "";
        try {
            String url = String.format(UNPAYWALL_API, URLEncoder.encode(doi, StandardCharsets.UTF_8));
            String resp = unpaywallRestTemplate.getForObject(URI.create(url), String.class);
            JsonNode node = mapper.readTree(resp);
            JsonNode best = node.path("best_oa_location");
            if (best.isMissingNode() || best.isNull()) return "";
            return best.path("url").asText("");
        } catch (Exception ex) {
            System.err.println("Error fetching PDF from Unpaywall for DOI " + doi + ": " + ex.getMessage());
            ex.printStackTrace();
            return "";
        }
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
