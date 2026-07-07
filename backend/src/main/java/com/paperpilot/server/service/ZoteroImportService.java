package com.paperpilot.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paperpilot.server.dto.PaperImportRequest;
import com.paperpilot.server.vo.PaperWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private final PaperWorkspaceService paperWorkspaceService;
    private final ObjectMapper objectMapper;

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
