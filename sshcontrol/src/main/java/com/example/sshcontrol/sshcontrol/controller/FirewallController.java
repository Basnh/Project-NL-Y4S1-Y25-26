package com.example.sshcontrol.sshcontrol.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.sshcontrol.service.SSHService;
import com.example.sshcontrol.model.User;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@Controller
public class FirewallController {

    private final SSHService sshService;

    public FirewallController(SSHService sshService) {
        this.sshService = sshService;
    }

    // Page mapping for firewall management
    @GetMapping("/firewall")
    public String firewallPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers());
        return "firewall";
    }

    // API endpoints for firewall operations
    @PostMapping("/firewall/status")
    @ResponseBody
    public ResponseEntity<String> getStatus(@RequestBody Map<String, String> request) {
        try {
            String host = request.get("host");
            String username = request.get("username");
            String password = request.get("password");
            
            System.out.println("Getting firewall status on host: " + host + " with user: " + username);
            
            String command = "sudo ufw status";
            String output = sshService.executeCommand(host, username, password, command);
            
            if (output == null || output.trim().isEmpty()) {
                return ResponseEntity.ok("Không thể lấy trạng thái firewall");
            }
            
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            System.err.println("Error getting firewall status: " + e.getMessage());
            return ResponseEntity.ok("Lỗi khi kiểm tra trạng thái firewall: " + e.getMessage());
        }
    }

    @PostMapping("/firewall/enable")
    @ResponseBody
    public ResponseEntity<String> enableFirewall(@RequestBody Map<String, String> request) {
        try {
            String host = request.get("host");
            String username = request.get("username");
            String password = request.get("password");
            
            System.out.println("Enabling firewall on host: " + host + " with user: " + username);
            
            // Sử dụng --force để tránh câu hỏi xác nhận
            String command = "sudo ufw --force enable";
            String output = sshService.executeCommand(host, username, password, command);
            
            System.out.println("Firewall enable output: " + output);
            
            if (output == null || output.trim().isEmpty()) {
                return ResponseEntity.ok("Lệnh đã thực hiện nhưng không có output từ server");
            }
            
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            System.err.println("Error enabling firewall: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok("Lỗi khi bật firewall: " + e.getMessage());
        }
    }

    @PostMapping("/firewall/disable")
    @ResponseBody
    public ResponseEntity<String> disableFirewall(@RequestBody Map<String, String> request) {
        try {
            String host = request.get("host");
            String username = request.get("username");
            String password = request.get("password");
            
            System.out.println("Disabling firewall on host: " + host + " with user: " + username);
            
            String command = "sudo ufw disable";
            String output = sshService.executeCommand(host, username, password, command);
            
            if (output == null || output.trim().isEmpty()) {
                return ResponseEntity.ok("Lệnh đã thực hiện nhưng không có output từ server");
            }
            
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            System.err.println("Error disabling firewall: " + e.getMessage());
            return ResponseEntity.ok("Lỗi khi tắt firewall: " + e.getMessage());
        }
    }

    @PostMapping("/firewall/add-rule")
    @ResponseBody
    public ResponseEntity<String> addRule(@RequestBody Map<String, String> request) {
        String host = request.get("host");
        String username = request.get("username");
        String password = request.get("password");
        String port = request.get("port");
        String protocol = request.get("protocol");
        
        String command = "sudo ufw allow " + port + "/" + protocol;
        String output = sshService.executeCommand(host, username, password, command);
        return ResponseEntity.ok(output);
    }

    @PostMapping("/firewall/delete-rule")
    @ResponseBody
    public ResponseEntity<String> deleteRule(@RequestBody Map<String, String> request) {
        String host = request.get("host");
        String username = request.get("username");
        String password = request.get("password");
        String port = request.get("port");
        String protocol = request.get("protocol");
        
        String command = "sudo ufw delete allow " + port + "/" + protocol;
        String output = sshService.executeCommand(host, username, password, command);
        return ResponseEntity.ok(output);
    }
}
