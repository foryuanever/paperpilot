package com.paperpilot.server.vo;

public class AuthSessionVO {

    private Long userId;
    private String name;
    private String email;
    private String inviteCode;
    private String role;
    private String avatarUrl;
    private String backgroundUrl;
    private Integer fruitScore;
    private String schoolName;
    private boolean campusVerified;
    private String qq;
    private String wechat;
    private String qqOpenid;
    private String registerTime;

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl) {
        this(userId, name, email, inviteCode, role, avatarUrl, backgroundUrl, 0);
    }

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl, Integer fruitScore) {
        this(userId, name, email, inviteCode, role, avatarUrl, backgroundUrl, fruitScore, null, false);
    }

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl, Integer fruitScore, String schoolName, boolean campusVerified) {
        this(userId, name, email, inviteCode, role, avatarUrl, backgroundUrl, fruitScore, schoolName, campusVerified, null, null, null, null);
    }

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl, Integer fruitScore, String schoolName, boolean campusVerified, String qq, String wechat, String qqOpenid, String registerTime) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.inviteCode = inviteCode;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.backgroundUrl = backgroundUrl;
        this.fruitScore = fruitScore;
        this.schoolName = schoolName;
        this.campusVerified = campusVerified;
        this.qq = qq;
        this.wechat = wechat;
        this.qqOpenid = qqOpenid;
        this.registerTime = registerTime;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public String getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public Integer getFruitScore() {
        return fruitScore;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public boolean isCampusVerified() {
        return campusVerified;
    }

    public String getQq() {
        return qq;
    }

    public String getWechat() {
        return wechat;
    }

    public String getQqOpenid() {
        return qqOpenid;
    }

    public String getRegisterTime() {
        return registerTime;
    }
}
