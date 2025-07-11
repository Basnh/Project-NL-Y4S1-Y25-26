package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.ServerInfo;
import com.example.sshcontrol.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// ✅ Thay đổi import này:
import jakarta.servlet.http.HttpSession;  // ✅ Dùng jakarta thay vì javax


import java.util.HashMap;
import java.util.Map;

@RestController
public class ServerInfoController {

    @GetMapping("/api/server-info")
    public ResponseEntity<Map<String, Object>> getServerInfo(@RequestParam String ip, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        // Tìm server theo IP
        ServerInfo server = user.getServers().stream()
                .filter(s -> s.getIp().equals(ip))
                .findFirst()
                .orElse(null);

        if (server == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Không tìm thấy máy chủ"));
        }

        try {
            Map<String, String> serverInfo = getServerInfoViaSSH(server);
            return ResponseEntity.ok(Map.of("data", serverInfo));
        } catch (Exception e) {
            // Log lỗi chi tiết
            System.err.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Không thể kết nối SSH: " + e.getMessage()));
        }
    }

    private Map<String, String> getServerInfoViaSSH(ServerInfo server) throws Exception {
        Map<String, String> info = new HashMap<>();
        
        // Tạm thời trả về dữ liệu giả lập để test
        info.put("os", "Ubuntu 22.04.3 LTS");
        info.put("arch", "x86_64");
        info.put("memory", "Total: 4GB, Available: 2.1GB");
        info.put("disk", "Total: 100GB, Used: 45GB, Available: 55GB");
        info.put("cpu", "Intel(R) Core(TM) i5-8265U CPU @ 1.60GHz");
        info.put("processes", "127");
        info.put("uptime", "3 days, 2 hours, 15 minutes");
        
        return info;
        
        // Uncomment phần này khi muốn dùng SSH thật
        /*
        JSch jsch = new JSch();
        Session sshSession = jsch.getSession(server.getSshUsername(), server.getIp(), 22);
        sshSession.setPassword(server.getSshPassword());
        sshSession.setConfig("StrictHostKeyChecking", "no");
        sshSession.connect();

        try {
            info.put("os", executeCommand(sshSession, "lsb_release -d | cut -f2"));
            info.put("arch", executeCommand(sshSession, "uname -m"));
            info.put("memory", executeCommand(sshSession, "free -h | grep Mem | awk '{print \"Total: \"$2\", Available: \"$7}'"));
            info.put("disk", executeCommand(sshSession, "df -h / | tail -1 | awk '{print \"Total: \"$2\", Used: \"$3\", Available: \"$4}'"));
            info.put("cpu", executeCommand(sshSession, "lscpu | grep 'Model name' | cut -d':' -f2 | xargs"));
            info.put("processes", executeCommand(sshSession, "ps aux | wc -l"));
            info.put("uptime", executeCommand(sshSession, "uptime -p"));
        } finally {
            sshSession.disconnect();
        }
        
        return info;
        */
    }

}
