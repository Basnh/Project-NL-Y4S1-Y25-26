package com.example.sshcontrol.model;

import jakarta.persistence.*;

@Entity
@Table(name = "firewall_rules")
public class FirewallRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
    
    @Column(nullable = false)
    private String ruleType; // "service", "port", "forward"
    
    @Column(nullable = false)
    private String ruleValue; // "http", "80", "8080->192.168.1.100:80"
    
    @Column(nullable = false)
    private String protocol; // "TCP", "UDP", "TCP,UDP"
    
    @Column(nullable = false)
    private String action; // "allow", "block"
    
    @Column(nullable = false)
    private String zone; // "public", "private", "dmz"
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(nullable = false)
    private Boolean permanent = false;
    
    @Column(nullable = false)
    private Long createdAt = System.currentTimeMillis();
    
    @Column(nullable = false)
    private Long updatedAt = System.currentTimeMillis();
    
    // Constructors
    public FirewallRule() {}
    
    public FirewallRule(Server server, String ruleType, String ruleValue, 
                       String protocol, String action, String zone) {
        this.server = server;
        this.ruleType = ruleType;
        this.ruleValue = ruleValue;
        this.protocol = protocol;
        this.action = action;
        this.zone = zone;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    
    public String getRuleValue() { return ruleValue; }
    public void setRuleValue(String ruleValue) { this.ruleValue = ruleValue; }
    
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    
    public Boolean getPermanent() { return permanent; }
    public void setPermanent(Boolean permanent) { this.permanent = permanent; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
