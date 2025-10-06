package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;

import jakarta.servlet.http.HttpSession;  

import java.util.HashMap;
import java.util.Map;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Controller
public class ServerInfoController {

    @GetMapping("/api/server-info")
    public ResponseEntity<Map<String, Object>> getServerInfo(@RequestParam String ip, Server server, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        // Tìm server theo IP
        server = user.getServers().stream()
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

    
    private String executeCommand(Session session, String command) throws Exception {
        com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setInputStream(null);
        java.io.InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                output.append(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (Exception ee) {
                // Ignore
            }
        }
        channel.disconnect();
        return output.toString().trim();
    }

    private Map<String, String> getServerInfoViaSSH(Server server) throws Exception {
        Map<String, String> info = new HashMap<>();
        JSch jsch = new JSch();
        Session sshSession = jsch.getSession(server.getSshUsername(), server.getIp(), 22);
        sshSession.setPassword(server.getSshPassword());
        sshSession.setConfig("StrictHostKeyChecking", "no");
        sshSession.connect();

        try {
            info.put("os", executeCommand(sshSession, "lsb_release -d | cut -f2- | xargs"));
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
    }

}
