package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;

import com.example.sshcontrol.repository.UserRepository;
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
    private UserRepository userRepository;

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
            String errorMessage = "Kết nối thất bại: " + e.getMessage();
            if (e.getMessage().contains("Auth fail")) {
                errorMessage = "Xác thực thất bại - Sai username hoặc password";
            } else if (e.getMessage().contains("timeout")) {
                errorMessage = "Kết nối timeout - Kiểm tra IP và port";
            } else if (e.getMessage().contains("Connection refused")) {
                errorMessage = "Kết nối bị từ chối - Kiểm tra SSH service";
            }

            response.put("success", false);
            response.put("message", errorMessage);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
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
            user = userRepository.findByUsername(user.getUsername());

            // Check if server already exists
            boolean exists = user.getServers().stream().anyMatch(server ->
                    server.getIp().equals(ip) &&
                            server.getSshUsername().equals(username) &&
                            server.getSshPassword().equals(password)
            );

            if (exists) {
                response.put("exists", true);
                response.put("message", "Máy chủ với IP, SSH Username và Password này đã tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if just IP + Username exists (different password)
            boolean ipUsernameExists = user.getServers().stream().anyMatch(server ->
                    server.getIp().equals(ip) &&
                            server.getSshUsername().equals(username)
            );

            if (ipUsernameExists) {
                response.put("exists", true);
                response.put("message", "Máy chủ với IP và SSH Username này đã tồn tại (mật khẩu khác)");
                return ResponseEntity.badRequest().body(response);
            }

            response.put("exists", false);
            response.put("message", "Máy chủ chưa tồn tại");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("exists", false);
            response.put("message", "Lỗi khi kiểm tra: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
