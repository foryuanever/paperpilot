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

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl) {
        this(userId, name, email, inviteCode, role, avatarUrl, backgroundUrl, 0);
    }

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role, String avatarUrl, String backgroundUrl, Integer fruitScore) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.inviteCode = inviteCode;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.backgroundUrl = backgroundUrl;
        this.fruitScore = fruitScore;
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
}
