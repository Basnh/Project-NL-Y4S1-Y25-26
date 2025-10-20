package com.example.sshcontrol.controller;

import com.example.sshcontrol.service.SSHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class SSHTestController {

    @Autowired
    private SSHService sshService;

    // Class để map request JSON
    private static class SSHConnectionRequest {
        private String ip;
        private String username;
        private String password;

        // Getters and setters
        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @PostMapping("/test-ssh-connection")
    public ResponseEntity<Map<String, Object>> testSSHConnection(@RequestBody SSHConnectionRequest request) {
        String ip = request.getIp();
        String username = request.getUsername();
        String password = request.getPassword();
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isConnected = sshService.testConnection(ip, username, password);
            
            if (isConnected) {
                response.put("success", true);
                response.put("message", "Kết nối SSH thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể kết nối SSH");
                response.put("details", "Vui lòng kiểm tra lại thông tin đăng nhập và đảm bảo máy chủ đang hoạt động");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi kết nối SSH");
            response.put("details", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}