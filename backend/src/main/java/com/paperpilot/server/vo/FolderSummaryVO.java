package com.paperpilot.server.vo;

public class FolderSummaryVO {

    private String name;
    private int count;
    private String desc;

    public FolderSummaryVO(String name, int count, String desc) {
        this.name = name;
        this.count = count;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public String getDesc() {
        return desc;
    }
}
