package com.example.sshcontrol.model;

public class Server {
    private String name;
    private String ip;
    private String sshUsername;
    private String sshPassword;
    private boolean isOnline;

    public Server() {}

    public Server(String name, String ip, String sshUsername, boolean isOnline) {
        this.name = name;
        this.ip = ip;
        this.sshUsername = sshUsername;
        this.isOnline = isOnline;
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
        return isOnline;
    }

    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
}

}
