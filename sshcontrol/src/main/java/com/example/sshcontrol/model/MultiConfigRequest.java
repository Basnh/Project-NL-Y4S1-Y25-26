package com.example.sshcontrol.model;

import java.util.List;

public class MultiConfigRequest {
    private List<String> hosts;
    private String user;
    private String password;
    private String configPath;
    private String content;

    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfigPath() { return configPath; }
    public void setConfigPath(String configPath) { this.configPath = configPath; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
