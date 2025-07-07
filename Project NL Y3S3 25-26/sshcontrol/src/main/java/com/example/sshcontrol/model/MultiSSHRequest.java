package com.example.sshcontrol.model;

import java.util.List;

public class MultiSSHRequest {
    private List<String> hosts;
    private String user;
    private String password;
    private String command;

    // getters and setters
    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}

