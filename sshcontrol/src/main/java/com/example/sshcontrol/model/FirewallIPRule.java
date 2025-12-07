package com.example.sshcontrol.model;

import jakarta.persistence.*;

@Entity
@Table(name = "firewall_ip_rules")
public class FirewallIPRule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "server_id", nullable = false)
    private Server server;
    
    @Column(nullable = false)
    private String ipAddress; // IP or CIDR
    
    @Column(nullable = false)
    private String action; // "allow", "block"
    
    @Column(nullable = false)
    private Boolean permanent = false;
    
    @Column(nullable = false)
    private Long createdAt = System.currentTimeMillis();
    
    // Constructors
    public FirewallIPRule() {}
    
    public FirewallIPRule(Server server, String ipAddress, String action) {
        this.server = server;
        this.ipAddress = ipAddress;
        this.action = action;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Server getServer() { return server; }
    public void setServer(Server server) { this.server = server; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public Boolean getPermanent() { return permanent; }
    public void setPermanent(Boolean permanent) { this.permanent = permanent; }
    
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
}
