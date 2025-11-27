package com.example.sshcontrol.model;

public enum UserRole {
    ADMIN("Admin", "Toàn quyền quản lý hệ thống"),
    USER("User", "Người dùng thường");

    private final String displayName;
    private final String description;

    UserRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
