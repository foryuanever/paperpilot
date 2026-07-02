package com.paperpilot.server.vo;

public class AuthSessionVO {

    private Long userId;
    private String name;
    private String email;
    private String inviteCode;
    private String role;

    public AuthSessionVO(Long userId, String name, String email, String inviteCode, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.inviteCode = inviteCode;
        this.role = role;
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
}
