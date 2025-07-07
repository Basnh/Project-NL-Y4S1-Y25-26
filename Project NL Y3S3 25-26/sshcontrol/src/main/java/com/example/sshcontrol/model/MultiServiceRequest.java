package com.example.sshcontrol.model;

import java.util.List;

public class MultiServiceRequest {
    private List<String> hosts;
    private String user;
    private String password;
    private String serviceName;
    private String action;

    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
