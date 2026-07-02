package com.paperpilot.server.vo;

public class SearchPaperVO {

    private String id;
    private String title;
    private String source;
    private String authors;
    private String year;
    private String abstractText;
    private String pdfUrl;
    private String sourceUrl;
    private String articleType;
    private java.util.List<String> subjects;

    public SearchPaperVO(String id, String title, String source, String authors, String year, String abstractText, String pdfUrl) {
        this(id, title, source, authors, year, abstractText, pdfUrl, defaultSourceUrl(id));
    }

    public SearchPaperVO(String id, String title, String source, String authors, String year, String abstractText, String pdfUrl, String sourceUrl) {
        this(id, title, source, authors, year, abstractText, pdfUrl, sourceUrl, "", java.util.List.of());
    }

    public SearchPaperVO(
            String id,
            String title,
            String source,
            String authors,
            String year,
            String abstractText,
            String pdfUrl,
            String sourceUrl,
            String articleType,
            java.util.List<String> subjects
    ) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.authors = authors;
        this.year = year;
        this.abstractText = abstractText;
        this.pdfUrl = pdfUrl;
        this.sourceUrl = sourceUrl;
        this.articleType = articleType;
        this.subjects = subjects == null ? java.util.List.of() : subjects;
    }

    private static String defaultSourceUrl(String id) {
        if (id == null || id.isBlank()) return "";
        if (id.startsWith("10.")) return "https://doi.org/" + id;
        if (id.startsWith("arxiv-")) return "https://arxiv.org/abs/" + id.substring("arxiv-".length());
        return "";
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getAuthors() {
        return authors;
    }

    public String getYear() {
        return year;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getArticleType() {
        return articleType;
    }

    public java.util.List<String> getSubjects() {
        return subjects;
    }
}
