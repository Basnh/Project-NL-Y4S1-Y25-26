package com.example.sshcontrol.service;

import com.example.sshcontrol.model.FirewallRule;
import com.example.sshcontrol.model.FirewallIPRule;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.repository.FirewallRuleRepository;
import com.example.sshcontrol.repository.FirewallIPRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class FirewallRuleService {
    
    @Autowired
    private FirewallRuleRepository ruleRepository;
    
    @Autowired
    private FirewallIPRuleRepository ipRuleRepository;
    
    @Autowired
    private SSHService sshService;
    
    /**
     * Thêm quy tắc tường lửa mới
     */
    public FirewallRule addRule(Server server, String ruleType, String ruleValue,
                               String protocol, String action, String zone) {
        FirewallRule rule = new FirewallRule(server, ruleType, ruleValue, protocol, action, zone);
        FirewallRule savedRule = ruleRepository.save(rule);
        
        // Apply rule to firewall via SSH
        try {
            applyRuleOnServer(server, ruleType, ruleValue, protocol, action, zone);
        } catch (Exception e) {
            System.err.println("[Firewall] Error applying rule on server: " + e.getMessage());
            // Rule is saved in DB even if SSH fails
        }
        
        return savedRule;
    }
    
    /**
     * Áp dụng quy tắc lên server thực tế
     */
    private void applyRuleOnServer(Server server, String ruleType, String ruleValue, 
                                    String protocol, String action, String zone) throws Exception {
        if (sshService == null) {
            System.err.println("[Firewall] SSHService not available");
            return;
        }
        
        String command = "";
        
        if ("port".equals(ruleType)) {
            // ufw allow/deny 80/tcp
            String dir = "allow".equals(action) ? "allow" : "deny";
            String proto = protocol != null ? protocol.toLowerCase() : "tcp";
            command = String.format("sudo ufw %s %s/%s", dir, ruleValue, proto);
        } else if ("ip".equals(ruleType)) {
            // ufw allow/deny from 192.168.1.1
            String dir = "allow".equals(action) ? "allow" : "deny";
            command = String.format("sudo ufw %s from %s", dir, ruleValue);
        }
        
        if (!command.isEmpty()) {
            System.out.println("[Firewall] Executing on server " + server.getIp() + ": " + command);
            // Pass password for sudo
            String result = sshService.executeCommandWithInput(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                command, 
                server.getSshPassword() + "\ny\n" // password + auto-confirm
            );
            System.out.println("[Firewall] Result: " + result);
        }
    }
    
    /**
     * Lấy tất cả quy tắc của server
     */
    public List<FirewallRule> getRulesByServer(Server server) {
        return ruleRepository.findByServer(server);
    }
    
    /**
     * Lấy tất cả quy tắc trong hệ thống
     */
    public List<FirewallRule> getAllRules() {
        return ruleRepository.findAll();
    }
    
    /**
     * Lấy quy tắc theo zone
     */
    public List<FirewallRule> getRulesByZone(Server server, String zone) {
        return ruleRepository.findByServerAndZone(server, zone);
    }
    
    /**
     * Lấy rules trực tiếp từ UFW trên server
     */
    public List<Map<String, Object>> getActiveRulesFromServer(Server server, String zone) {
        List<Map<String, Object>> rules = new ArrayList<>();
        
        if (sshService == null) {
            System.err.println("[Firewall] SSHService not available");
            return rules;
        }
        
        try {
            // Execute: sudo ufw status
            String command = "sudo ufw status";
            String output = sshService.executeCommand(server.getIp(), server.getSshUsername(), server.getSshPassword(), command);
            
            System.out.println("[Firewall] UFW Status Output:\n" + output);
            
            // Parse output - handle both numbered and regular format
            String[] lines = output.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("Status:") || line.startsWith("--") || line.startsWith("To")) {
                    continue;
                }
                
                Map<String, Object> rule = new HashMap<>();
                
                String port = "";
                String protocol = "TCP"; // default
                String action = "allow"; // default
                
                // Handle different formats
                if (line.contains("[") && line.contains("]")) {
                    // Numbered format: [ 1] 22                         ALLOW IN    Anywhere
                    // Extract port/service from the middle
                    int closeIdx = line.indexOf("]");
                    String rest = line.substring(closeIdx + 1).trim();
                    
                    String[] parts = rest.split("\\s+");
                    if (parts.length >= 3) {
                        port = parts[0]; // "22" or "http" or "22/tcp"
                        
                        // Extract action (ALLOW, DENY, REJECT, etc)
                        String actionPart = parts[1].toUpperCase(); // "ALLOW", "DENY"
                        
                        // Check if it's "ALLOW IN" or just "ALLOW"
                        if (actionPart.equals("ALLOW")) {
                            action = "allow";
                        } else if (actionPart.equals("DENY")) {
                            action = "deny";
                        } else if (actionPart.equals("REJECT")) {
                            action = "reject";
                        }
                    }
                } else {
                    // Regular format: 22/tcp                     ALLOW       Anywhere
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String portProtoField = parts[0];
                        
                        // Extract port and protocol
                        if (portProtoField.contains("/")) {
                            String[] pp = portProtoField.split("/");
                            port = pp[0];
                            protocol = pp[1].toUpperCase();
                        } else if (portProtoField.matches("\\d+")) {
                            port = portProtoField;
                            protocol = "TCP";
                        } else {
                            port = portProtoField; // service name
                            protocol = "TCP";
                        }
                        
                        // Extract action
                        String actionPart = parts[1].toUpperCase();
                        if (actionPart.equals("ALLOW")) {
                            action = "allow";
                        } else if (actionPart.equals("DENY")) {
                            action = "deny";
                        } else if (actionPart.equals("REJECT")) {
                            action = "reject";
                        }
                    }
                }
                
                // Parse protocol from port field if present
                if (port.contains("/")) {
                    String[] pp = port.split("/");
                    port = pp[0];
                    protocol = pp[1].toUpperCase();
                }
                
                if (!port.isEmpty()) {
                    rule.put("port", port);
                    rule.put("protocol", protocol);
                    rule.put("action", action);
                    rule.put("zone", zone);
                    rule.put("ruleType", "port");
                    rule.put("ruleValue", port);
                    
                    rules.add(rule);
                }
            }
            
        } catch (Exception e) {
            System.err.println("[Firewall] Error getting rules from server: " + e.getMessage());
            e.printStackTrace();
        }
        
        return rules;
    }
    
    /**
     * Xóa quy tắc
     */
    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }
    
    /**
     * Cập nhật quy tắc
     */
    public FirewallRule updateRule(FirewallRule rule) {
        rule.setUpdatedAt(System.currentTimeMillis());
        return ruleRepository.save(rule);
    }
    
    /**
     * Thêm IP vào danh sách cho phép
     */
    public FirewallIPRule allowIP(Server server, String ip, Boolean permanent) {
        FirewallIPRule rule = new FirewallIPRule(server, ip, "allow");
        rule.setPermanent(permanent);
        return ipRuleRepository.save(rule);
    }
    
    /**
     * Thêm IP vào danh sách chặn
     */
    public FirewallIPRule blockIP(Server server, String ip, Boolean permanent) {
        FirewallIPRule rule = new FirewallIPRule(server, ip, "block");
        rule.setPermanent(permanent);
        return ipRuleRepository.save(rule);
    }
    
    /**
     * Lấy danh sách IP cho phép
     */
    public List<FirewallIPRule> getAllowedIPs(Server server) {
        return ipRuleRepository.findByServerAndAction(server, "allow");
    }
    
    /**
     * Lấy danh sách IP bị chặn
     */
    public List<FirewallIPRule> getBlockedIPs(Server server) {
        return ipRuleRepository.findByServerAndAction(server, "block");
    }
    
    /**
     * Xóa quy tắc IP
     */
    public void deleteIPRule(Long ruleId) {
        ipRuleRepository.deleteById(ruleId);
    }
    
    /**
     * Áp dụng quy tắc lên server thực tế
     */
    public boolean applyRules(Server server, String zone) {
        try {
            List<FirewallRule> rules = getRulesByZone(server, zone);
            
            for (FirewallRule rule : rules) {
                if (!rule.getEnabled()) continue;
                
                String command = buildFirewallCommand(rule);
                System.out.println("[Firewall] Applying rule: " + command);
                
                sshService.executeCommand(
                    server.getIp(),
                    server.getSshUsername(),
                    server.getSshPassword(),
                    command
                );
            }
            
            // Apply IP rules
            if ("allow".equalsIgnoreCase(zone)) {
                List<FirewallIPRule> ipRules = getAllowedIPs(server);
                for (FirewallIPRule ipRule : ipRules) {
                    String cmd = "sudo ufw allow from " + ipRule.getIpAddress();
                    sshService.executeCommand(
                        server.getIp(),
                        server.getSshUsername(),
                        server.getSshPassword(),
                        cmd
                    );
                }
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("[Firewall] Error applying rules: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Xây dựng lệnh UFW từ quy tắc
     */
    private String buildFirewallCommand(FirewallRule rule) {
        StringBuilder cmd = new StringBuilder("sudo ufw ");
        
        cmd.append(rule.getAction()).append(" ");
        
        if ("service".equals(rule.getRuleType())) {
            cmd.append(rule.getRuleValue());
        } else if ("port".equals(rule.getRuleType())) {
            cmd.append(rule.getRuleValue()).append("/").append(rule.getProtocol().toLowerCase());
        } else if ("forward".equals(rule.getRuleType())) {
            cmd.append("route allow from 0.0.0.0/0 to 0.0.0.0/0 port ")
               .append(rule.getRuleValue());
        }
        
        if (rule.getPermanent()) {
            // UFW rules are permanent by default, no need for extra flag
        }
        
        return cmd.toString();
    }
}
