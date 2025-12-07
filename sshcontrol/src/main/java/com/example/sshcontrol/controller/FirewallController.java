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
            List<FirewallRule> rules = firewallRuleService.getRulesByZone(server, zone);
            
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
            response.put("rule", rule);
            
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
            firewallRuleService.deleteRule(ruleId);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc ID: " + ruleId);
            
            activityLogService.logActivity(user, "FIREWALL_DELETE_RULE", 
                "Xóa quy tắc firewall ID: " + ruleId);
            
            response.put("success", true);
            response.put("message", "Đã xóa quy tắc thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Delete rule error: " + e.getMessage());
        }
        
        return response;
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
            @SuppressWarnings("unchecked")
            List<Integer> ruleIds = (List<Integer>) body.get("ruleIds");
            
            for (Integer id : ruleIds) {
                firewallRuleService.deleteRule(Long.valueOf(id));
            }
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_DELETE_RULES", 
                "Xóa " + ruleIds.size() + " quy tắc");
            
            activityLogService.logActivity(user, "FIREWALL_DELETE_RULES", 
                "Xóa " + ruleIds.size() + " quy tắc firewall");
            
            response.put("success", true);
            response.put("message", "Đã xóa " + ruleIds.size() + " quy tắc");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Delete rules error: " + e.getMessage());
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
            
            FirewallIPRule rule = firewallRuleService.allowIP(server, ip, permanent);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_ALLOW_IP", 
                "Cho phép IP: " + ip);
            
            activityLogService.logActivity(user, "FIREWALL_ALLOW_IP", 
                "Cho phép IP: " + ip);
            
            response.put("success", true);
            response.put("message", "Đã thêm IP vào danh sách cho phép");
            response.put("rule", rule);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            System.err.println("[Firewall] Allow IP error: " + e.getMessage());
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
            
            FirewallIPRule rule = firewallRuleService.blockIP(server, ip, permanent);
            
            SystemLogger.logActivity(user.getUsername(), "FIREWALL_BLOCK_IP", 
                "Chặn IP: " + ip);
            
            activityLogService.logActivity(user, "FIREWALL_BLOCK_IP", 
                "Chặn IP: " + ip);
            
            response.put("success", true);
            response.put("message", "Đã thêm IP vào danh sách chặn");
            response.put("rule", rule);
            
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
}
