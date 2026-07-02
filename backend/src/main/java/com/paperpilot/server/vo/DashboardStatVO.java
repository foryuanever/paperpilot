package com.paperpilot.server.vo;

public class DashboardStatVO {

    private String label;
    private String value;
    private String detail;

    public DashboardStatVO(String label, String value, String detail) {
        this.label = label;
        this.value = value;
        this.detail = detail;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public String getDetail() {
        return detail;
    }
}
