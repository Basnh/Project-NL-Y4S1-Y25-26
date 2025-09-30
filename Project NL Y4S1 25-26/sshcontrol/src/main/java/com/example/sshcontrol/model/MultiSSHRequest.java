package com.example.sshcontrol.model;

import java.util.List;

public class MultiSSHRequest {
    private List<String> hosts;
    private String command;

    // getters and setters
    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}

