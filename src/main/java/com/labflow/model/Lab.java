package com.labflow.model;

import java.time.LocalDateTime;

public class Lab {
    private int id;
    private String name;
    private String inviteCode;
    private boolean protectedLab;
    private int createdByUserId;
    private Role memberRole;
    private String colorPalette;
    private LocalDateTime createdAt;

    public Lab() {
    }

    public Lab(int id, String name, String inviteCode, boolean protectedLab, int createdByUserId, Role memberRole, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
        this.protectedLab = protectedLab;
        this.createdByUserId = createdByUserId;
        this.memberRole = memberRole;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public boolean isProtectedLab() {
        return protectedLab;
    }

    public void setProtectedLab(boolean protectedLab) {
        this.protectedLab = protectedLab;
    }

    public int getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(int createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Role getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(Role memberRole) {
        this.memberRole = memberRole;
    }

    public String getColorPalette() {
        return colorPalette;
    }

    public void setColorPalette(String colorPalette) {
        this.colorPalette = colorPalette;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
