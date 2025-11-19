package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServerStatusController {

    @GetMapping("/api/server-status")
    public ResponseEntity<Map<String, Object>> getServerStatus(@RequestParam(required = false) String ip, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        Map<String, Object> result = new HashMap<>();
        
        if (ip != null) {
            // Kiểm tra trạng thái của một máy chủ cụ thể
            boolean isOnline = checkServerStatus(ip);
            result.put("ip", ip);
            result.put("online", isOnline);
        } else {
            // Kiểm tra trạng thái của tất cả máy chủ
            Map<String, Boolean> statuses = new HashMap<>();
            for (Server server : user.getServers()) {
                boolean isOnline = checkServerStatus(server.getIp());
                statuses.put(server.getIp(), isOnline);
                // Cập nhật trạng thái trong session
                server.setOnline(isOnline);
            }
            result.put("statuses", statuses);
        }

        return ResponseEntity.ok(result);
    }

    private boolean checkServerStatus(String ip) {
        try {
            // Ping test
            InetAddress address = InetAddress.getByName(ip);
            boolean reachable = address.isReachable(3000); // 3 seconds timeout
            
            if (!reachable) {
                return false;
            }
            
            // SSH port test (port 22)
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(ip, 22), 3000);
                return true;
            } catch (IOException e) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
