package com.example.sshcontrol.model;

import jakarta.persistence.*;

@Entity
@Table(name = "servers")
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String ip;
    private String sshUsername;
    private String sshPassword;
    private boolean online;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Constructors
    public Server() {}

    public Server(String name, String ip, String sshUsername, String sshPassword, User user) {
        this.name = name;
        this.ip = ip;
        this.sshUsername = sshUsername;
        this.sshPassword = sshPassword;
        this.user = user;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }

    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Thêm alias methods để tương thích với ServerInfo
    public String getUsername() { return sshUsername; }
    public void setUsername(String username) { this.sshUsername = username; }

    public String getPassword() { return sshPassword; }
    public void setPassword(String password) { this.sshPassword = password; }
}
