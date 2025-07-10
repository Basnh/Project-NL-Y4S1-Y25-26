package com.example.sshcontrol.model;

public class Server {
    private String name;
    private String ip;
    private String sshUsername;
    private boolean online;

    public Server() {}

    public Server(String name, String ip, String sshUsername, boolean online) {
        this.name = name;
        this.ip = ip;
        this.sshUsername = sshUsername;
        this.online = online;
    }

    // Getter và Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
