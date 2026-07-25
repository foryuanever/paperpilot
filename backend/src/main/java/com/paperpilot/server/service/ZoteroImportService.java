package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.dto.PaperImportRequest;
import com.paperpilot.server.vo.PaperWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ZoteroImportService {
    private static final int MAX_ITEMS = 200;
    private static final String ZOTERO_API_BASE = "https://api.zotero.org";

    private final PaperWorkspaceService paperWorkspaceService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    public ZoteroImportService(PaperWorkspaceService paperWorkspaceService, ObjectMapper objectMapper) {
        this.paperWorkspaceService = paperWorkspaceService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> importFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 Zotero 导出的 BibTeX、RIS 或 CSL JSON 文件");
        }
        String fileName = Objects.toString(file.getOriginalFilename(), "zotero-export");
        String text = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Zotero 导出文件为空");
        }

        List<PaperImportRequest> requests = parseExport(fileName, text);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("没有识别到可导入的 Zotero 文献，请确认导出格式为 BibTeX、RIS 或 CSL JSON");
        }

        List<Map<String, Object>> items = new ArrayList<>();
        int imported = 0;
        int failed = 0;
        for (PaperImportRequest request : requests.stream().limit(MAX_ITEMS).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", request.getTitle());
            try {
                PaperWorkspaceVO workspace = paperWorkspaceService.importPaper(request);
                imported++;
                row.put("status", "imported");
                row.put("workspaceId", workspace.getWorkspaceId());
            } catch (Exception error) {
                failed++;
                row.put("status", "failed");
                row.put("message", readableError(error));
            }
            items.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("detected", requests.size());
        result.put("imported", imported);
        result.put("failed", failed);
        result.put("limited", requests.size() > MAX_ITEMS);
        result.put("items", items);
        return result;
    }

    public Map<String, Object> importOnline(String userId, String apiKey, int requestedLimit) throws IOException, InterruptedException {
        String cleanUserId = cleanText(userId).replaceAll("[^0-9]", "");
        String cleanApiKey = cleanText(apiKey);
        int limit = Math.max(1, Math.min(MAX_ITEMS, requestedLimit <= 0 ? 100 : requestedLimit));
        if (cleanUserId.isBlank() || cleanApiKey.isBlank()) {
            throw new IllegalArgumentException("请填写 Zotero User ID 和 API Key");
        }

        JsonNode keyInfo = verifyZoteroKey(cleanUserId, cleanApiKey);
        List<PaperImportRequest> requests = fetchZoteroItems(cleanUserId, cleanApiKey, limit);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Zotero 账号验证成功，但没有读取到可导入的文献条目。请确认 API Key 允许读取 Library。");
        }

        Map<String, Object> result = importRequests("Zotero 在线同步", requests, limit);
        result.put("zoteroUserId", cleanUserId);
        result.put("zoteroUsername", keyInfo.path("username").asText(""));
        result.put("verified", true);
        return result;
    }

    public Map<String, Object> importLocal(int requestedLimit) throws IOException, InterruptedException {
        int limit = Math.max(1, Math.min(MAX_ITEMS, requestedLimit <= 0 ? 100 : requestedLimit));
        List<PaperImportRequest> requests = fetchLocalZoteroItems(limit);
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("未从本机 Zotero 读取到文献。请确认 Zotero Desktop 已打开，并已在设置中允许本地客户端访问。");
        }
        Map<String, Object> result = importRequests("Zotero 本机同步", requests, limit);
        result.put("verified", true);
        result.put("local", true);
        return result;
    }

    private Map<String, Object> importRequests(String sourceName, List<PaperImportRequest> requests, int limit) {
        List<Map<String, Object>> items = new ArrayList<>();
        int imported = 0;
        int failed = 0;
        for (PaperImportRequest request : requests.stream().limit(Math.min(limit, MAX_ITEMS)).toList()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", request.getTitle());
            try {
                PaperWorkspaceVO workspace = paperWorkspaceService.importPaper(request);
                imported++;
                row.put("status", "imported");
                row.put("workspaceId", workspace.getWorkspaceId());
            } catch (Exception error) {
                failed++;
                row.put("status", "failed");
                row.put("message", readableError(error));
            }
            items.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", sourceName);
        result.put("detected", requests.size());
        result.put("imported", imported);
        result.put("failed", failed);
        result.put("limited", requests.size() > limit || requests.size() > MAX_ITEMS);
        result.put("items", items);
        return result;
    }

    private JsonNode verifyZoteroKey(String userId, String apiKey) throws IOException, InterruptedException {
        JsonNode info = getZoteroJson("/keys/" + apiKey, apiKey);
        String owner = info.path("userID").asText("");
        if (!owner.isBlank() && !owner.equals(userId)) {
            throw new IllegalArgumentException("API Key 与填写的 Zotero User ID 不一致");
        }
        JsonNode access = info.path("access");
        if (access.isObject() && !access.path("user").asBoolean(true) && !access.path("all").asBoolean(false)) {
            throw new IllegalArgumentException("当前 Zotero API Key 没有读取个人文献库权限");
        }
        return info;
    }

    private List<PaperImportRequest> fetchZoteroItems(String userId, String apiKey, int limit) throws IOException, InterruptedException {
        List<PaperImportRequest> requests = new ArrayList<>();
        int start = 0;
        while (requests.size() < limit && start < MAX_ITEMS) {
            String path = "/users/" + userId + "/items/top?format=json&limit=100&start=" + start;
            JsonNode root = getZoteroJson(path, apiKey);
            if (!root.isArray() || root.size() == 0) break;
            for (JsonNode item : root) {
                PaperImportRequest request = requestFromZoteroItem(item);
                if (request != null) requests.add(request);
                if (requests.size() >= limit) break;
            }
            if (root.size() < 100) break;
            start += 100;
        }
        return dedupe(requests);
    }

    private JsonNode getZoteroJson(String path, String apiKey) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ZOTERO_API_BASE + path))
            .timeout(Duration.ofSeconds(30))
            .header("Zotero-API-Version", "3")
            .header("Zotero-API-Key", apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new IllegalArgumentException("Zotero API Key 验证失败或权限不足");
        }
        if (response.statusCode() >= 400) {
            throw new IllegalArgumentException("Zotero 在线接口返回错误：" + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private List<PaperImportRequest> fetchLocalZoteroItems(int limit) throws IOException, InterruptedException {
        List<PaperImportRequest> requests = new ArrayList<>();
        int start = 0;
        while (requests.size() < limit && start < MAX_ITEMS) {
            JsonNode root = getLocalZoteroJson("/api/users/0/items?format=json&limit=100&sort=dateAdded&direction=desc&start=" + start);
            if (!root.isArray() || root.size() == 0) break;
            for (JsonNode item : root) {
                PaperImportRequest request = requestFromZoteroItem(item);
                if (request != null) {
                    request.setImportSource("Zotero 本机同步");
                    String localPdfUrl = localPdfUrlForItem(item);
                    if (!localPdfUrl.isBlank()) {
                        request.setPaperUrl(localPdfUrl);
                    }
                    requests.add(request);
                }
                if (requests.size() >= limit) break;
            }
            if (root.size() < 100) break;
            start += 100;
        }
        return dedupe(requests);
    }

    private String localPdfUrlForItem(JsonNode item) {
        String linkedAttachment = localPdfUrlFromAttachmentLink(item);
        if (!linkedAttachment.isBlank()) return linkedAttachment;

        String parentKey = firstNonBlank(textValue(item, "key"), textValue(item.path("data"), "key"));
        if (parentKey.isBlank()) return "";
        String parentHref = textValue(item.path("links").path("self"), "href");
        String childrenPath = localApiPath(parentHref);
        if (childrenPath.isBlank()) {
            childrenPath = "/api/users/0/items/" + parentKey + "/children";
        } else {
            childrenPath = childrenPath + "/children";
        }
        try {
            JsonNode children = getLocalZoteroJson(childrenPath + "?format=json&limit=100");
            if (!children.isArray()) return "";
            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                String itemType = textValue(data, "itemType");
                String contentType = textValue(data, "contentType").toLowerCase(Locale.ROOT);
                String filename = textValue(data, "filename").toLowerCase(Locale.ROOT);
                if (!"attachment".equals(itemType)) continue;
                if (!contentType.contains("pdf") && !filename.endsWith(".pdf")) continue;
                String enclosure = localPdfEnclosure(child);
                if (!enclosure.isBlank()) return enclosure;
                String childHref = textValue(child.path("links").path("self"), "href");
                String childPath = localApiPath(childHref);
                if (!childPath.isBlank()) return "http://127.0.0.1:23119" + childPath + "/file";
            }
        } catch (Exception ignored) {
            // 没有本地 PDF 附件时仍然允许导入题录。
        }
        return "";
    }

    private String localPdfUrlFromAttachmentLink(JsonNode item) {
        JsonNode attachment = item.path("links").path("attachment");
        String type = textValue(attachment, "attachmentType").toLowerCase(Locale.ROOT);
        if (!type.contains("pdf")) return "";
        String path = localApiPath(textValue(attachment, "href"));
        if (path.isBlank()) return "";
        try {
            JsonNode attachmentItem = getLocalZoteroJson(path + "?format=json");
            String enclosure = localPdfEnclosure(attachmentItem);
            if (!enclosure.isBlank()) return enclosure;
        } catch (Exception ignored) {
        }
        return "http://127.0.0.1:23119" + path + "/file";
    }

    private String localPdfEnclosure(JsonNode item) {
        JsonNode enclosure = item.path("links").path("enclosure");
        String type = textValue(enclosure, "type").toLowerCase(Locale.ROOT);
        String href = textValue(enclosure, "href");
        if (href.startsWith("file://") && (type.contains("pdf") || href.toLowerCase(Locale.ROOT).contains(".pdf"))) {
            return href;
        }
        return "";
    }

    private String localApiPath(String href) {
        String value = firstNonBlank(href);
        if (value.isBlank()) return "";
        try {
            URI uri = URI.create(value.replace("localhost", "127.0.0.1"));
            String path = uri.getPath();
            if (path != null && path.startsWith("/api/")) return path;
        } catch (Exception ignored) {
        }
        return "";
    }

    private JsonNode getLocalZoteroJson(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:23119" + path))
            .timeout(Duration.ofSeconds(12))
            .header("Zotero-API-Version", "3")
            .header("Accept", "application/json")
            .GET()
            .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new IllegalArgumentException("未检测到本机 Zotero。请先打开 Zotero Desktop 后再导入。");
        }
        if (response.statusCode() == 403) {
            throw new IllegalArgumentException("本机 Zotero 拒绝访问。请打开 Zotero → 设置 → 高级，勾选“允许此计算机上的其他应用与 Zotero 通信”，然后重启 Zotero 再试。");
        }
        if (response.statusCode() >= 400) {
            throw new IllegalArgumentException("本机 Zotero 返回错误：" + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private PaperImportRequest requestFromZoteroItem(JsonNode item) {
        JsonNode data = item.path("data");
        String itemType = textValue(data, "itemType");
        if (Set.of("attachment", "note", "annotation").contains(itemType)) return null;
        String title = firstNonBlank(textValue(data, "title"), textValue(data, "shortTitle"));
        if (title.isBlank()) return null;
        String doi = textValue(data, "DOI");
        String url = firstNonBlank(textValue(data, "url"), doiUrl(doi));
        String source = firstNonBlank(
            textValue(data, "publicationTitle"),
            textValue(data, "conferenceName"),
            textValue(data, "proceedingsTitle"),
            textValue(data, "publisher"),
            "Zotero"
        );
        PaperImportRequest request = buildRequest(
            title,
            authorsFromZoteroCreators(data.path("creators")),
            source,
            yearFromZoteroDate(textValue(data, "date")),
            firstNonBlank(textValue(data, "abstractNote"), textValue(data, "extra")),
            doi,
            url
        );
        request.setImportSource("Zotero 在线同步");
        request.setArticleType(firstNonBlank(itemType, "journal-article"));
        return request;
    }

    private String authorsFromZoteroCreators(JsonNode creators) {
        if (!creators.isArray()) return "";
        List<String> names = new ArrayList<>();
        for (JsonNode creator : creators) {
            String name = firstNonBlank(
                textValue(creator, "name"),
                (textValue(creator, "firstName") + " " + textValue(creator, "lastName")).trim(),
                textValue(creator, "lastName")
            );
            if (!name.isBlank()) names.add(name);
        }
        return String.join(", ", names);
    }

    private String yearFromZoteroDate(String date) {
        Matcher matcher = Pattern.compile("(19|20)\\d{2}").matcher(Objects.toString(date, ""));
        return matcher.find() ? matcher.group() : "";
    }

    private List<PaperImportRequest> parseExport(String fileName, String text) throws IOException {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".json") || text.startsWith("[") || text.startsWith("{")) {
            return parseCslJson(text);
        }
        if (lower.endsWith(".ris") || text.startsWith("TY  -")) {
            return parseRis(text);
        }
        return parseBibtex(text);
    }

    private List<PaperImportRequest> parseCslJson(String text) throws IOException {
        JsonNode root = objectMapper.readTree(text);
        List<PaperImportRequest> requests = new ArrayList<>();
        if (root.isObject() && root.has("items")) root = root.path("items");
        if (!root.isArray()) root = objectMapper.createArrayNode().add(root);
        for (JsonNode item : root) {
            String title = textValue(item, "title");
            if (title.isBlank()) continue;
            String authors = authorsFromCsl(item.path("author"));
            String source = firstNonBlank(textValue(item, "container-title"), textValue(item, "publisher"), "Zotero");
            String year = yearFromCsl(item.path("issued"));
            String doi = textValue(item, "DOI");
            String url = firstNonBlank(textValue(item, "URL"), doi.isBlank() ? "" : "https://doi.org/" + doi);
            requests.add(buildRequest(title, authors, source, year, textValue(item, "abstract"), doi, url));
        }
        return dedupe(requests);
    }

    private List<PaperImportRequest> parseRis(String text) {
        List<PaperImportRequest> requests = new ArrayList<>();
        Map<String, List<String>> current = new LinkedHashMap<>();
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.stripTrailing();
            if (line.length() < 6 || !line.substring(2, 6).equals("  - ")) continue;
            String key = line.substring(0, 2);
            String value = line.substring(6).trim();
            if ("TY".equals(key) && !current.isEmpty()) {
                addRisRequest(requests, current);
                current = new LinkedHashMap<>();
            }
            if ("ER".equals(key)) {
                addRisRequest(requests, current);
                current = new LinkedHashMap<>();
                continue;
            }
            current.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        addRisRequest(requests, current);
        return dedupe(requests);
    }

    private List<PaperImportRequest> parseBibtex(String text) {
        List<PaperImportRequest> requests = new ArrayList<>();
        Matcher matcher = Pattern.compile("@\\w+\\s*\\{", Pattern.CASE_INSENSITIVE).matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) starts.add(matcher.start());
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
            String entry = text.substring(start, end);
            Map<String, String> fields = parseBibtexFields(entry);
            String title = cleanBibValue(fields.get("title"));
            if (title.isBlank()) continue;
            String source = firstNonBlank(
                cleanBibValue(fields.get("journal")),
                cleanBibValue(fields.get("booktitle")),
                cleanBibValue(fields.get("publisher")),
                "Zotero BibTeX"
            );
            String url = firstNonBlank(cleanBibValue(fields.get("url")), doiUrl(cleanBibValue(fields.get("doi"))));
            requests.add(buildRequest(
                title,
                cleanBibValue(fields.get("author")).replace(" and ", ", "),
                source,
                cleanBibValue(fields.get("year")),
                cleanBibValue(fields.get("abstract")),
                cleanBibValue(fields.get("doi")),
                url
            ));
        }
        return dedupe(requests);
    }

    private Map<String, String> parseBibtexFields(String entry) {
        Map<String, String> fields = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("(?m)([A-Za-z][A-Za-z0-9_-]*)\\s*=\\s*([\\{\\\"].*?)(?=,\\s*[A-Za-z][A-Za-z0-9_-]*\\s*=|\\n\\s*\\})", Pattern.DOTALL).matcher(entry);
        while (matcher.find()) {
            fields.put(matcher.group(1).toLowerCase(Locale.ROOT), matcher.group(2).trim().replaceAll(",$", ""));
        }
        return fields;
    }

    private void addRisRequest(List<PaperImportRequest> requests, Map<String, List<String>> entry) {
        if (entry == null || entry.isEmpty()) return;
        String title = firstRis(entry, "TI", "T1", "CT", "BT");
        if (title.isBlank()) return;
        String source = firstNonBlank(firstRis(entry, "JO", "JF", "T2", "PB"), "Zotero RIS");
        String doi = firstRis(entry, "DO");
        String url = firstNonBlank(firstRis(entry, "UR", "L1", "LK"), doiUrl(doi));
        requests.add(buildRequest(
            title,
            String.join(", ", entry.getOrDefault("AU", List.of())),
            source,
            firstRis(entry, "PY", "Y1").replaceAll("[^0-9].*$", ""),
            firstRis(entry, "AB", "N2"),
            doi,
            url
        ));
    }

    private PaperImportRequest buildRequest(String title, String authors, String source, String year, String abstractText, String doi, String url) {
        PaperImportRequest request = new PaperImportRequest();
        request.setSource(firstNonBlank(source, "Zotero"));
        request.setImportSource("Zotero");
        request.setTitle(limit(cleanText(title), 512));
        request.setAuthors(limit(cleanText(authors), 255));
        request.setPublishYear(limit(cleanText(year).replaceAll("[^0-9]", ""), 16));
        request.setAbstractText(cleanText(abstractText));
        request.setPaperId(firstNonBlank(cleanText(doi), cleanText(url), request.getTitle()));
        request.setSourceUrl(cleanText(url));
        request.setPaperUrl(isLikelyPdfUrl(url) ? cleanText(url) : "");
        request.setArticleType("journal-article");
        return request;
    }

    private List<PaperImportRequest> dedupe(List<PaperImportRequest> requests) {
        Set<String> seen = new LinkedHashSet<>();
        List<PaperImportRequest> rows = new ArrayList<>();
        for (PaperImportRequest request : requests) {
            String key = firstNonBlank(request.getPaperId(), request.getTitle()).toLowerCase(Locale.ROOT);
            if (seen.add(key)) rows.add(request);
        }
        return rows;
    }

    private String authorsFromCsl(JsonNode authors) {
        if (!authors.isArray()) return "";
        List<String> names = new ArrayList<>();
        for (JsonNode author : authors) {
            String literal = textValue(author, "literal");
            String family = textValue(author, "family");
            String given = textValue(author, "given");
            names.add(firstNonBlank(literal, (given + " " + family).trim(), family));
        }
        return String.join(", ", names);
    }

    private String yearFromCsl(JsonNode issued) {
        JsonNode parts = issued.path("date-parts");
        if (parts.isArray() && parts.size() > 0 && parts.get(0).isArray() && parts.get(0).size() > 0) {
            return parts.get(0).get(0).asText("");
        }
        return "";
    }

    private String firstRis(Map<String, List<String>> entry, String... keys) {
        for (String key : keys) {
            List<String> values = entry.get(key);
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.isBlank()) return cleanText(value);
                }
            }
        }
        return "";
    }

    private String textValue(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? "" : cleanText(value.asText(""));
    }

    private String cleanBibValue(String value) {
        if (value == null) return "";
        String text = value.trim();
        while ((text.startsWith("{") && text.endsWith("}")) || (text.startsWith("\"") && text.endsWith("\""))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        return cleanText(text.replace("{", "").replace("}", ""));
    }

    private String cleanText(String value) {
        return Objects.toString(value, "")
            .replaceAll("\\s+", " ")
            .replace("\\&", "&")
            .trim();
    }

    private String doiUrl(String doi) {
        String clean = cleanText(doi);
        return clean.isBlank() ? "" : "https://doi.org/" + clean.replaceFirst("^https?://(?:dx\\.)?doi\\.org/", "");
    }

    private boolean isLikelyPdfUrl(String url) {
        String clean = cleanText(url).toLowerCase(Locale.ROOT);
        return clean.startsWith("http") && (clean.contains(".pdf") || clean.contains("/pdf/"));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String limit(String value, int max) {
        String clean = cleanText(value);
        return clean.length() > max ? clean.substring(0, max) : clean;
    }

    private String readableError(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "导入失败";
        int marker = message.indexOf("\"message\":\"");
        if (marker >= 0) {
            String tail = message.substring(marker + 11);
            int end = tail.indexOf('"');
            if (end > 0) return tail.substring(0, end);
        }
        return message.length() > 160 ? message.substring(0, 160) : message;
    }
}
