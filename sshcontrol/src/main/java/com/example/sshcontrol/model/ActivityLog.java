package com.example.sshcontrol.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "activity_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String action;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private String serverId;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private String status; // SUCCESS, FAILED, PENDING
    
    @Column(columnDefinition = "TEXT")
    private String details;

    // Constructors
    public ActivityLog() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public ActivityLog(User user, String action, String description) {
        this();
        this.user = user;
        this.action = action;
        this.description = description;
        this.status = "SUCCESS";
    }

    public ActivityLog(User user, String action, String description, String serverId) {
        this();
        this.user = user;
        this.action = action;
        this.description = description;
        this.serverId = serverId;
        this.status = "SUCCESS";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    // Helper methods
    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return createdAt.format(formatter);
    }

    public String getFormattedDateShort() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        return createdAt.format(formatter);
    }

    public String getStatusBadgeClass() {
        switch(status) {
            case "SUCCESS": return "bg-green-100 text-green-800";
            case "FAILED": return "bg-red-100 text-red-800";
            case "PENDING": return "bg-yellow-100 text-yellow-800";
            default: return "bg-gray-100 text-gray-800";
        }
    }

    public String getActionIcon() {
        switch(action.toLowerCase()) {
            case "login": return "fa-sign-in-alt";
            case "logout": return "fa-sign-out-alt";
            case "server_connect": return "fa-link";
            case "server_disconnect": return "fa-unlink";
            case "file_upload": return "fa-cloud-upload-alt";
            case "file_download": return "fa-cloud-download-alt";
            case "file_delete": return "fa-trash-alt";
            case "command_execute": return "fa-terminal";
            case "server_add": return "fa-plus-circle";
            case "server_delete": return "fa-minus-circle";
            case "server_edit": return "fa-edit";
            case "service_start": return "fa-play";
            case "service_stop": return "fa-stop";
            case "service_restart": return "fa-sync-alt";
            default: return "fa-info-circle";
        }
    }
}
