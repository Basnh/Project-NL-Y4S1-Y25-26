package com.example.sshcontrol.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ServerInfo {
    @Id
    private String ip;
    private String name;
    private String sshUsername;
    private String sshPassword;

    public ServerInfo() {}

    public ServerInfo(String name, String ip, String sshUsername, String sshPassword) {
        this.name = name;
        this.ip = ip;
        this.sshUsername = sshUsername;
        this.sshPassword = sshPassword;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }

    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }
}