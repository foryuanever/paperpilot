package com.paperpilot.server.vo;

import java.util.List;

public class PaperWorkspaceVO {

    private String workspaceId;
    private String source;
    private String title;
    private String paperUrl;
    private String abstractText;
    private List<String> nextActions;
    private String aiHint;

    public PaperWorkspaceVO(
        String workspaceId,
        String source,
        String title,
        String paperUrl,
        String abstractText,
        List<String> nextActions,
        String aiHint
    ) {
        this.workspaceId = workspaceId;
        this.source = source;
        this.title = title;
        this.paperUrl = paperUrl;
        this.abstractText = abstractText;
        this.nextActions = nextActions;
        this.aiHint = aiHint;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public String getSource() {
        return source;
    }

    public String getTitle() {
        return title;
    }

    public String getPaperUrl() {
        return paperUrl;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public List<String> getNextActions() {
        return nextActions;
    }

    public String getAiHint() {
        return aiHint;
    }
}
