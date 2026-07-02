package com.paperpilot.server.vo;

public class RecentPaperVO {

    private String title;
    private String meta;
    private String tag;
    private String paperUrl;
    private String source;

    public RecentPaperVO(String title, String meta, String tag, String paperUrl, String source) {
        this.title = title;
        this.meta = meta;
        this.tag = tag;
        this.paperUrl = paperUrl;
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public String getMeta() {
        return meta;
    }

    public String getTag() {
        return tag;
    }

    public String getPaperUrl() {
        return paperUrl;
    }

    public String getSource() {
        return source;
    }
}
