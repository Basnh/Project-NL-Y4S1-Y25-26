package com.example.sshcontrol.service;

import com.example.sshcontrol.model.FirewallRule;
import com.example.sshcontrol.model.FirewallIPRule;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.repository.FirewallRuleRepository;
import com.example.sshcontrol.repository.FirewallIPRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return ruleRepository.save(rule);
    }
    
    /**
     * Lấy tất cả quy tắc của server
     */
    public List<FirewallRule> getRulesByServer(Server server) {
        return ruleRepository.findByServer(server);
    }
    
    /**
     * Lấy quy tắc theo zone
     */
    public List<FirewallRule> getRulesByZone(Server server, String zone) {
        return ruleRepository.findByServerAndZone(server, zone);
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
