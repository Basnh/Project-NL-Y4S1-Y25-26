package com.example.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.service.SSHService;
import com.example.sshcontrol.service.ActivityLogService;
import com.example.sshcontrol.repository.ServerRepository;
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
            Optional<Server> serverOpt = serverRepository.findByIp(ip);
            if (!serverOpt.isPresent() || !serverOpt.get().getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Server không tìm thấy");
                return response;
            }
            Server server = serverOpt.get();

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
}
