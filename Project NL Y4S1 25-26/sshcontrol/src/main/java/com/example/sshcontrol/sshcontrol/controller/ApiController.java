package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.sshcontrol.service.UserService;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private UserService userService;

    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> response = new HashMap<>();

        try {
            // Use JSch library for SSH connection test
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, ip, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(5000); // 5 second timeout

            session.connect();
            session.disconnect();

            response.put("success", true);
            response.put("message", "Kết nối SSH thành công");
            return ResponseEntity.ok(response);

        } catch (JSchException e) {
            String errorMessage;
            String details;
            
            if (e.getMessage().contains("Auth fail")) {
                errorMessage = "Sai tên đăng nhập hoặc mật khẩu SSH";
                details = "Authentication failed";
            } else if (e.getMessage().contains("timeout")) {
                errorMessage = "Không thể kết nối tới server (timeout)";
                details = "Connection timeout";
            } else if (e.getMessage().contains("Connection refused")) {
                errorMessage = "Server từ chối kết nối SSH";
                details = "Connection refused";
            } else {
                errorMessage = "Lỗi SSH: " + e.getMessage();
                details = "SSH error";
            }

            response.put("success", false);
            response.put("message", errorMessage);
            response.put("details", details);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi không xác định: " + e.getMessage());
            response.put("details", "Unexpected error");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/check-duplicate")
    public ResponseEntity<Map<String, Object>> checkDuplicateServer(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                response.put("exists", false);
                response.put("message", "Người dùng chưa đăng nhập");
                return ResponseEntity.ok(response);
            }

            String ip = request.get("ip");
            String username = request.get("username");
            String password = request.get("password");

            // Refresh user data from database
            user = userService.findByUsername(user.getUsername());

            // Check if server already exists
            boolean exists = user.getServers().stream().anyMatch(server ->
                    server.getIp().equals(ip) &&
                    server.getSshUsername().equals(username) &&
                    server.getSshPassword().equals(password));

            response.put("exists", exists);
            if (exists) {
                response.put("message", "Server này đã tồn tại trong danh sách của bạn");
            } else {
                response.put("message", "Server chưa tồn tại, có thể thêm mới");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("exists", false);
            response.put("message", "Có lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
