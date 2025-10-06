package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.jcraft.jsch.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/api")
public class SSHCommandController {

    @PostMapping("/execute-command")
    public ResponseEntity<Map<String, Object>> executeCommand(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                response.put("success", false);
                response.put("error", "Người dùng chưa đăng nhập");
                return ResponseEntity.status(401).body(response);
            }

            String command = (String) request.get("command");
            @SuppressWarnings("unchecked")
            List<String> selectedHosts = (List<String>) request.get("hosts");
            
            if (command == null || command.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Lệnh không được để trống");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            
            // Nếu không có hosts được chọn, thực thi trên tất cả máy chủ
            if (selectedHosts == null || selectedHosts.isEmpty()) {
                for (Server server : user.getServers()) {
                    Map<String, Object> result = executeCommandOnServer(server, command);
                    results.add(result);
                }
            } else {
                // Thực thi trên các máy chủ được chọn
                for (String hostIp : selectedHosts) {
                    Server server = findServerByIp(user, hostIp);
                    if (server != null) {
                        Map<String, Object> result = executeCommandOnServer(server, command);
                        results.add(result);
                    }
                }
            }

            response.put("success", true);
            response.put("results", results);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Lỗi khi thực thi lệnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> executeCommandOnServer(Server server, String command) {
        Map<String, Object> result = new HashMap<>();
        result.put("server", server.getName());
        result.put("ip", server.getIp());
        result.put("command", command);
        
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        
        try {
            session = jsch.getSession(server.getSshUsername(), server.getIp(), 22);
            session.setPassword(server.getSshPassword());
            
            // Tắt strict host key checking
            session.setConfig("StrictHostKeyChecking", "no");
            
            // Timeout 10 giây
            session.connect(10000);
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            
            channel.setOutputStream(outputStream);
            channel.setErrStream(errorStream);
            
            channel.connect(10000);
            
            // Đợi lệnh hoàn thành
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            
            String output = outputStream.toString("UTF-8");
            String error = errorStream.toString("UTF-8");
            int exitStatus = channel.getExitStatus();
            
            result.put("success", true);
            result.put("output", output);
            result.put("error", error);
            result.put("exitStatus", exitStatus);
            
            if (exitStatus != 0 && !error.isEmpty()) {
                result.put("success", false);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Lỗi kết nối SSH: " + e.getMessage());
            result.put("output", "");
            result.put("exitStatus", -1);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        
        return result;
    }
    
    private Server findServerByIp(User user, String ip) {
        return user.getServers().stream()
                .filter(server -> server.getIp().equals(ip))
                .findFirst()
                .orElse(null);
    }
}