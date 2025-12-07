package com.example.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.FirewallRule;
import com.example.sshcontrol.model.FirewallIPRule;
import com.example.sshcontrol.service.SSHService;
import com.example.sshcontrol.service.ActivityLogService;
import com.example.sshcontrol.service.FirewallRuleService;
import com.example.sshcontrol.repository.ServerRepository;
import com.example.sshcontrol.util.SystemLogger;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Firewall Controller - UFW only management
 */
@Controller
@RequestMapping("/firewall")
public class FirewallController {

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private SSHService sshService;

    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private FirewallRuleService firewallRuleService;
    
    @Autowired
    private com.example.sshcontrol.repository.FirewallRuleRepository ruleRepository;

    /**
     * Hiển thị trang quản lý firewall
     */
    @GetMapping
    public String showFirewall(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<Server> userServers = serverRepository.findByUser(user);
        model.addAttribute("userServers", userServers);
        
        return "firewall";
    }

    /**
     * Lấy trạng thái UFW firewall
     */
    @PostMapping("/api/status")
    @ResponseBody
    public Map<String, Object> getFirewallStatus(
            @RequestParam String ip,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            // Get server from user's servers list
            Server server = serverRepository.findByUser(user).stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Server không tìm thấy");
                return response;
            }

            String result = "";
            boolean isRunning = false;
            
            // Check UFW status
            result = sshService.executeCommand(ip, 
                server.getSshUsername(), 
                server.getSshPassword(), 
                "sudo ufw status");
            
            isRunning = result.toLowerCase().contains("active");
            System.out.println("[UFW] Status check: " + result + " (active=" + isRunning + ")");
            
            response.put("success", true);
            response.put("isRunning", isRunning);
            response.put("status", isRunning ? "Firewall đang chạy" : "Firewall không chạy");
            response.put("type", "ufw");
            response.put("details", result);
            
            // Log activity
            activityLogService.logActivity(user, "FIREWALL_CHECK", 
                "Kiểm tra trạng thái UFW trên " + ip);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Status check error: " + e.getMessage());
            activityLogService.logActivity(user, "FIREWALL_CHECK_FAILED", 
                "Lỗi kiểm tra firewall: " + e.getMessage(), null, "FAILED");
        }
        
        return response;
    }

    /**
     * Bật Firewall (hỗ trợ firewalld và ufw)
     */
    @PostMapping("/api/enable")
    @ResponseBody
    public Map<String, Object> enableFirewall(
            @RequestParam String ip,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            Server server = serverRepository.findByUser(user).stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Máy chủ không tìm thấy");
                return response;
            }

            String result = "";
            String message = "";
            
            System.out.println("[Firewall Enable] Attempting to enable UFW on " + ip);
            
            result = sshService.executeUFWCommand(ip, 
                server.getSshUsername(), 
                server.getSshPassword(), 
                "sudo ufw enable");
            
            System.out.println("[Firewall Enable] UFW result: '" + result + "'");
            
            // Check if UFW succeeded
            if (!result.toLowerCase().contains("not found") && 
                !result.toLowerCase().contains("command not found")) {
                message = "Bật Firewall (UFW) thành công";
                System.out.println("[UFW] Enable succeeded");
                response.put("success", true);
                response.put("message", message);
                response.put("result", result.isEmpty() ? "UFW Enabled" : result);
                response.put("firewall_type", "ufw");
                activityLogService.logActivity(user, "FIREWALL_ENABLE", message + " trên " + ip);
                return response;
            }
            
            // If failed
            response.put("success", false);
            response.put("message", "Lỗi: Không thể bật UFW hoặc không có quyền. " + result);
            System.err.println("[Firewall] Enable failed - result: " + result);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Enable error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Tắt Firewall (hỗ trợ firewalld và ufw)
     */
    @PostMapping("/api/disable")
    @ResponseBody
    public Map<String, Object> disableFirewall(
            @RequestParam String ip,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            Server server = serverRepository.findByUser(user).stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Máy chủ không tìm thấy");
                return response;
            }

            String result = "";
            String message = "";
            
            System.out.println("[Firewall Disable] Attempting to disable UFW on " + ip);
            
            result = sshService.executeUFWCommand(ip, 
                server.getSshUsername(), 
                server.getSshPassword(), 
                "sudo ufw disable");
            
            System.out.println("[Firewall Disable] UFW result: '" + result + "'");
            
            // If UFW succeeded, return success
            if (!result.toLowerCase().contains("not found") && 
                !result.toLowerCase().contains("command not found")) {
                message = "Tắt Firewall (UFW) thành công";
                System.out.println("[UFW] Disable succeeded");
                response.put("success", true);
                response.put("message", message);
                response.put("result", result.isEmpty() ? "UFW Disabled" : result);
                response.put("firewall_type", "ufw");
                activityLogService.logActivity(user, "FIREWALL_DISABLE", message + " trên " + ip);
                return response;
            }
            
            // If failed
            response.put("success", false);
            response.put("message", "Lỗi: Không thể tắt UFW hoặc không có quyền. " + result);
            System.err.println("[Firewall] Disable failed - result: " + result);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Disable error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Khởi động lại Firewall (UFW)
     */
    @PostMapping("/api/restart")
    @ResponseBody
    public Map<String, Object> restartFirewall(
            @RequestParam String ip,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            Server server = serverRepository.findByUser(user).stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Máy chủ không tìm thấy");
                return response;
            }

            String result = "";
            String message = "";
            
            System.out.println("[Firewall Restart] Attempting to restart UFW on " + ip);
            
            result = sshService.executeUFWCommand(ip, 
                server.getSshUsername(), 
                server.getSshPassword(), 
                "sudo systemctl restart ufw");
            
            System.out.println("[Firewall Restart] UFW result: '" + result + "'");
            
            // If UFW restart succeeded
            if (!result.toLowerCase().contains("not found") && 
                !result.toLowerCase().contains("command not found")) {
                message = "Khởi động lại Firewall (UFW) thành công";
                System.out.println("[UFW] Restart succeeded");
                response.put("success", true);
                response.put("message", message);
                response.put("result", result.isEmpty() ? "UFW Restarted" : result);
                response.put("firewall_type", "ufw");
                activityLogService.logActivity(user, "FIREWALL_RESTART", message + " trên " + ip);
                return response;
            }
            
            // If failed
            response.put("success", false);
            response.put("message", "Lỗi: Không thể khởi động lại UFW hoặc không có quyền. " + result);
            System.err.println("[Firewall] Restart failed - result: " + result);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Restart error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Tải lại Firewall (UFW) - Reload rules
     */
    @PostMapping("/api/reload")
    @ResponseBody
    public Map<String, Object> reloadFirewall(
            @RequestParam String ip,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            Server server = serverRepository.findByUser(user).stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Máy chủ không tìm thấy");
                return response;
            }

            String result = "";
            String message = "";
            
            System.out.println("[Firewall Reload] Attempting to reload UFW on " + ip);
            
            result = sshService.executeUFWCommand(ip, 
                server.getSshUsername(), 
                server.getSshPassword(), 
                "sudo ufw reload");
            
            System.out.println("[Firewall Reload] UFW result: '" + result + "'");
            
            // If UFW reload succeeded
            if (!result.toLowerCase().contains("not found") && 
                !result.toLowerCase().contains("command not found")) {
                message = "Tải lại Firewall (UFW) thành công";
                System.out.println("[UFW] Reload succeeded");
                response.put("success", true);
                response.put("message", message);
                response.put("result", result.isEmpty() ? "UFW Reloaded" : result);
                response.put("firewall_type", "ufw");
                activityLogService.logActivity(user, "FIREWALL_RELOAD", message + " trên " + ip);
                return response;
            }
            
            // If failed
            response.put("success", false);
            response.put("message", "Lỗi: Không thể tải lại UFW hoặc không có quyền. " + result);
            System.err.println("[Firewall] Reload failed - result: " + result);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Reload error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Lấy danh sách quy tắc firewall
     */
    @GetMapping("/api/rules")
    @ResponseBody
    public Map<String, Object> getRules(
            @RequestParam String zone,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("rules", new ArrayList<>());
                return response;
            }
            
            Server server = userServers.get(0);
            
            // Get active rules from server via SSH
            List<Map<String, Object>> rules = firewallRuleService.getActiveRulesFromServer(server, zone);
            
            response.put("success", true);
            response.put("rules", rules);
            response.put("count", rules.size());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("rules", new ArrayList<>());
            System.err.println("[Firewall] Get rules error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Thêm quy tắc firewall mới
     */
    @PostMapping("/api/firewall/add-rule")
    @ResponseBody
    public Map<String, Object> addRule(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            
            String type = (String) body.get("type");
            String value = (String) body.get("value");
            String protocol = (String) body.get("protocol");
            String action = (String) body.get("action");
            String zone = (String) body.get("zone");
            
            FirewallRule rule = firewallRuleService.addRule(
                server, type, value, protocol, action, zone
            );
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_ADD_RULE", 
                "Thêm quy tắc: " + type + " " + value + " (" + protocol + ")");
            
            activityLogService.logActivity(user, "FIREWALL_ADD_RULE", 
                "Thêm quy tắc firewall: " + type + " " + value);
            
            response.put("success", true);
            response.put("message", "Đã thêm quy tắc thành công");
            response.put("ruleId", rule.getId());
            response.put("ruleValue", rule.getRuleValue());
            response.put("protocol", rule.getProtocol());
            response.put("ruleType", rule.getRuleType());
            response.put("action", rule.getAction());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Add rule error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Xóa một quy tắc
     */
    @DeleteMapping("/api/firewall/delete-rule/{ruleId}")
    @ResponseBody
    public Map<String, Object> deleteRule(
            @PathVariable Long ruleId,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không được xác thực");
                return response;
            }
            
            // Verify rule exists
            if (ruleId == null || ruleId <= 0) {
                response.put("success", false);
                response.put("message", "ID quy tắc không hợp lệ");
                return response;
            }
            
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            
            // Try to find the rule to get details for UFW deletion
            Optional<com.example.sshcontrol.model.FirewallRule> ruleOpt = 
                java.util.Optional.ofNullable(ruleRepository.findById(ruleId).orElse(null));
            
            if (ruleOpt.isPresent()) {
                com.example.sshcontrol.model.FirewallRule rule = ruleOpt.get();
                // Execute UFW delete command
                String protocol = rule.getProtocol() != null ? rule.getProtocol().toLowerCase() : "tcp";
                String command = String.format("echo 'y' | sudo ufw delete %s %s/%s", 
                    rule.getAction().toLowerCase(), 
                    rule.getRuleValue(), 
                    protocol);
                
                System.out.println("[Firewall] Deleting rule via SSH: " + command);
                String result = sshService.executeCommandWithInput(
                    server.getIp(),
                    server.getSshUsername(),
                    server.getSshPassword(),
                    command,
                    server.getSshPassword() + "\ny\n"
                );
                System.out.println("[Firewall] Delete SSH result: " + result);
            }
            
            firewallRuleService.deleteRule(ruleId);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc ID: " + ruleId);
            
            activityLogService.logActivity(user, "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc firewall ID: " + ruleId);
            
            response.put("success", true);
            response.put("message", "Đã xóa quy tắc thành công");
            System.out.println("[Firewall] Deleted rule ID: " + ruleId);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa quy tắc: " + e.getMessage());
            System.err.println("[Firewall] Delete rule error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Xóa quy tắc theo chi tiết (ruleValue, protocol, action)
     */
    @PostMapping("/api/firewall/delete-rule-by-details")
    @ResponseBody
    public Map<String, Object> deleteRuleByDetails(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không được xác thực");
                return response;
            }
            
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            
            String ruleValue = (String) body.get("ruleValue");
            String protocol = (String) body.get("protocol");
            String action = (String) body.get("action");
            
            if (ruleValue == null || protocol == null || action == null) {
                response.put("success", false);
                response.put("message", "Thiếu thông tin quy tắc");
                return response;
            }
            
            // Step 1: Get numbered rules
            String statusCommand = "sudo ufw status numbered";
            System.out.println("[Firewall] Getting numbered rules via SSH: " + statusCommand);
            String statusResult = sshService.executeCommand(
                server.getIp(),
                server.getSshUsername(),
                server.getSshPassword(),
                statusCommand
            );
            System.out.println("[Firewall] Status result:\n" + statusResult);
            
            // Step 2: Find the rule number
            int ruleNumber = findRuleNumber(statusResult, ruleValue, protocol, action);
            
            if (ruleNumber == -1) {
                response.put("success", false);
                response.put("message", "Không tìm thấy quy tắc để xóa");
                return response;
            }
            
            // Step 3: Delete by number
            String deleteCommand = String.format("echo 'y' | sudo ufw delete %d", ruleNumber);
            System.out.println("[Firewall] Deleting rule via SSH: " + deleteCommand);
            String deleteResult = sshService.executeCommandWithInput(
                server.getIp(),
                server.getSshUsername(),
                server.getSshPassword(),
                deleteCommand,
                "y\n"
            );
            System.out.println("[Firewall] Delete SSH result: " + deleteResult);
            
            // Also delete from database if exists
            try {
                com.example.sshcontrol.model.FirewallRule dbRule = 
                    ruleRepository.findByRuleValueAndProtocolAndAction(ruleValue, protocol, action);
                if (dbRule != null) {
                    ruleRepository.delete(dbRule);
                }
            } catch (Exception e) {
                System.out.println("[Firewall] Note: Rule not found in database: " + e.getMessage());
            }
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc: " + ruleValue + "/" + protocol);
            
            activityLogService.logActivity(user, "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc firewall: " + ruleValue + "/" + protocol);
            
            response.put("success", true);
            response.put("message", "Đã xóa quy tắc thành công");
            System.out.println("[Firewall] Deleted rule: " + ruleValue + "/" + protocol);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa quy tắc: " + e.getMessage());
            System.err.println("[Firewall] Delete rule error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }
    
    /**
     * Find rule number by matching rule details
     */
    private int findRuleNumber(String statusOutput, String ruleValue, String protocol, String action) {
        String[] lines = statusOutput.split("\n");
        
        for (String line : lines) {
            // Look for lines with format: [ 1] 22                         ALLOW IN    Anywhere
            if (line.contains("[") && line.contains("]")) {
                try {
                    // Extract rule number
                    int startIdx = line.indexOf("[") + 1;
                    int endIdx = line.indexOf("]");
                    String numberStr = line.substring(startIdx, endIdx).trim();
                    int number = Integer.parseInt(numberStr);
                    
                    // Check if this line matches our rule
                    String upperLine = line.toUpperCase();
                    String searchValue = ruleValue.trim();
                    String searchProtocol = protocol.trim().toUpperCase();
                    String searchAction = action.trim().toUpperCase();
                    
                    // Normalize protocol (tcp/udp might appear as separate entries or combined)
                    if (searchProtocol.equals("TCP/UDP")) {
                        searchProtocol = "TCP";
                    }
                    
                    // Check if line contains the rule details
                    if ((line.contains(searchValue) || line.contains(searchProtocol)) &&
                        (upperLine.contains("ALLOW") && searchAction.equals("ALLOW")) ||
                        (upperLine.contains("DENY") && searchAction.equals("DENY"))) {
                        return number;
                    }
                } catch (NumberFormatException e) {
                    // Skip lines that don't have valid rule numbers
                }
            }
        }
        
        return -1;
    }

    /**
     * Xóa nhiều quy tắc
     */
    @PostMapping("/api/firewall/delete-rules")
    @ResponseBody
    public Map<String, Object> deleteRules(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> rules = (List<Map<String, String>>) body.get("rules");
            
            if (rules == null || rules.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không có quy tắc nào để xóa");
                return response;
            }
            
            int deletedCount = 0;
            for (Map<String, String> rule : rules) {
                try {
                    String ruleValue = rule.get("ruleValue");
                    String protocol = rule.get("protocol");
                    String action = rule.get("action");
                    
                    if (ruleValue == null || ruleValue.isEmpty()) {
                        continue;
                    }
                    
                    // Execute UFW delete command
                    // Format: sudo ufw delete [allow/deny] [port/protocol]
                    String protocol_lower = protocol != null ? protocol.toLowerCase() : "tcp";
                    String command;
                    
                    if (ruleValue.equals("Anywhere")) {
                        // For "Anywhere" rules, need to figure out the port from context
                        command = String.format("echo 'y' | sudo ufw delete %s", action);
                    } else {
                        // Normal port rule
                        if (protocol_lower.contains("/")) {
                            command = String.format("echo 'y' | sudo ufw delete %s %s/%s", action, ruleValue, protocol_lower);
                        } else {
                            command = String.format("echo 'y' | sudo ufw delete %s %s/%s", action, ruleValue, protocol_lower);
                        }
                    }
                    
                    System.out.println("[Firewall] Deleting rule via SSH: " + command);
                    String result = sshService.executeCommandWithInput(
                        server.getIp(),
                        server.getSshUsername(),
                        server.getSshPassword(),
                        command,
                        server.getSshPassword() + "\ny\n"
                    );
                    
                    System.out.println("[Firewall] Delete result: " + result);
                    deletedCount++;
                    
                } catch (Exception e) {
                    System.err.println("[Firewall] Error deleting rule: " + e.getMessage());
                }
            }
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_DELETE_RULES", 
                "Xóa " + deletedCount + " quy tắc từ UFW");
            
            activityLogService.logActivity(user, "FIREWALL_DELETE_RULES", 
                "Xóa " + deletedCount + " quy tắc firewall từ UFW");
            
            response.put("success", true);
            response.put("message", "Đã xóa " + deletedCount + " quy tắc từ UFW");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Delete rules error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Thêm IP vào danh sách cho phép
     */
    @PostMapping("/api/firewall/allow-ip")
    @ResponseBody
    public Map<String, Object> allowIP(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            String ip = (String) body.get("ip");
            Boolean permanent = (Boolean) body.getOrDefault("permanent", true);
            
            if (ip == null || ip.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "IP không hợp lệ");
                return response;
            }
            
            // Execute UFW command on server
            String command = String.format("echo 'y' | sudo ufw allow from %s", ip.trim());
            String result = sshService.executeCommandWithInput(
                server.getIp(),
                server.getSshUsername(),
                server.getSshPassword(),
                command,
                server.getSshPassword() + "\ny\n"
            );
            
            System.out.println("[Firewall] Allow IP result: " + result);
            
            // Save to database for record keeping
            FirewallIPRule rule = firewallRuleService.allowIP(server, ip, permanent);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_ALLOW_IP", 
                "Cho phép IP: " + ip);
            
            activityLogService.logActivity(user, "FIREWALL_ALLOW_IP", 
                "Cho phép IP: " + ip);
            
            response.put("success", true);
            response.put("message", "Đã cho phép IP " + ip + " thành công");
            response.put("ruleId", rule.getId());
            response.put("ipAddress", rule.getIpAddress());
            response.put("action", rule.getAction());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Allow IP error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return response;
    }

    /**
     * Thêm IP vào danh sách chặn
     */
    @PostMapping("/api/firewall/block-ip")
    @ResponseBody
    public Map<String, Object> blockIP(
            @RequestBody Map<String, Object> body,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            String ip = (String) body.get("ip");
            Boolean permanent = (Boolean) body.getOrDefault("permanent", true);
            
            if (ip == null || ip.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "IP không hợp lệ");
                return response;
            }
            
            // Execute UFW command on server
            String command = String.format("echo 'y' | sudo ufw deny from %s", ip.trim());
            String result = sshService.executeCommandWithInput(
                server.getIp(),
                server.getSshUsername(),
                server.getSshPassword(),
                command,
                server.getSshPassword() + "\ny\n"
            );
            
            System.out.println("[Firewall] Block IP result: " + result);
            
            // Save to database for record keeping
            FirewallIPRule rule = firewallRuleService.blockIP(server, ip, permanent);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_BLOCK_IP", 
                "Chặn IP: " + ip);
            
            activityLogService.logActivity(user, "FIREWALL_BLOCK_IP", 
                "Chặn IP: " + ip);
            
            response.put("success", true);
            response.put("message", "Đã chặn IP " + ip + " thành công");
            response.put("ruleId", rule.getId());
            response.put("ipAddress", rule.getIpAddress());
            response.put("action", rule.getAction());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Block IP error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Áp dụng quy tắc firewall
     */
    @PostMapping("/api/firewall/apply")
    @ResponseBody
    public Map<String, Object> applyRules(
            @RequestParam String zone,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            List<Server> userServers = serverRepository.findByUser(user);
            if (userServers.isEmpty()) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            Server server = userServers.get(0);
            boolean applied = firewallRuleService.applyRules(server, zone);
            
            if (applied) {
                SystemLogger.logActivity(user.getUsername(), "FIREWALL_APPLY", 
                    "Áp dụng quy tắc zone: " + zone);
                
                activityLogService.logActivity(user, "FIREWALL_APPLY", 
                    "Áp dụng quy tắc firewall zone: " + zone);
                
                response.put("success", true);
                response.put("message", "Đã áp dụng quy tắc thành công");
            } else {
                response.put("success", false);
                response.put("message", "Lỗi khi áp dụng quy tắc");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Apply rules error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Clear all firewall rules from database (admin only)
     */
    @PostMapping("/api/clear-all-rules")
    @ResponseBody
    public Map<String, Object> clearAllRules(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            // Security check - only admin can clear
            if (user == null || !("ADMIN".equals(user.getRole()))) {
                response.put("success", false);
                response.put("message", "Chỉ admin mới có quyền");
                return response;
            }
            
            // Get all rules and delete
            List<FirewallRule> allRules = firewallRuleService.getAllRules();
            for (FirewallRule rule : allRules) {
                firewallRuleService.deleteRule(rule.getId());
            }
            
            response.put("success", true);
            response.put("message", "Đã xóa tất cả " + allRules.size() + " quy tắc");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Clear rules error: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Update server IP (for fixing connection issues)
     */
    @PostMapping("/api/update-server-ip")
    @ResponseBody
    public Map<String, Object> updateServerIP(
            @RequestParam String serverId,
            @RequestParam String newIP,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        
        try {
            if (user == null) {
                response.put("success", false);
                response.put("message", "Không được xác thực");
                return response;
            }
            
            Long id = Long.parseLong(serverId);
            Server server = serverRepository.findById(id).orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy server");
                return response;
            }
            
            // Verify ownership
            if (!server.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Không có quyền");
                return response;
            }
            
            String oldIP = server.getIp();
            server.setIp(newIP);
            serverRepository.save(server);
            
            response.put("success", true);
            response.put("message", "Cập nhật IP từ " + oldIP + " thành " + newIP);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
        }
        
        return response;
    }

}
