package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.ServerInfo;
import com.jcraft.jsch.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

@Controller
@RequestMapping("/file-manager") // Đổi base URL
public class FileManagerController {

    @GetMapping // URL sẽ là /file-manager
    public String showEditConfigPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return "redirect:/login";
        }
        
        // Kiểm tra trạng thái máy chủ
        if (user.getServers() != null) {
            for (ServerInfo server : user.getServers()) {
                boolean isOnline = checkServerStatus(server.getIp());
                server.setOnline(isOnline);
            }
        }
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers());
        
        return "edit-config";
    }

    @PostMapping("/api/list-files")
    @ResponseBody
    public Map<String, Object> listFiles(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String path = request.get("path");
            
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            
            List<Map<String, Object>> files = getRemoteFileList(host, username, path);
            
            response.put("success", true);
            response.put("files", files);
            response.put("path", path);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/get-file-content")
    @ResponseBody
    public Map<String, Object> getFileContent(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String filePath = request.get("filePath");
            
            String content = readRemoteFile(host, username, filePath);
            
            response.put("success", true);
            response.put("content", content);
            response.put("filePath", filePath);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/save-file")
    @ResponseBody
    public Map<String, Object> saveFile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String filePath = request.get("filePath");
            String content = request.get("content");
            
            writeRemoteFile(host, username, filePath, content);
            
            response.put("success", true);
            response.put("message", "File đã được lưu thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/create-file")
    @ResponseBody
    public Map<String, Object> createFile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String path = request.get("path");
            String fileName = request.get("fileName");
            
            String fullPath = path.endsWith("/") ? path + fileName : path + "/" + fileName;
            
            // Tạo file trống
            executeRemoteCommand(host, username, "touch \"" + fullPath + "\"");
            
            response.put("success", true);
            response.put("message", "File đã được tạo thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/create-folder")
    @ResponseBody
    public Map<String, Object> createFolder(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String path = request.get("path");
            String folderName = request.get("folderName");
            
            String fullPath = path.endsWith("/") ? path + folderName : path + "/" + folderName;
            
            executeRemoteCommand(host, username, "mkdir -p \"" + fullPath + "\"");
            
            response.put("success", true);
            response.put("message", "Thư mục đã được tạo thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/rename-file")
    @ResponseBody
    public Map<String, Object> renameFile(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = request.get("host");
            String username = request.get("username");
            String oldPath = request.get("oldPath");
            String newName = request.get("newName");
            
            // Lấy thư mục cha
            String parentDir = oldPath.substring(0, oldPath.lastIndexOf('/'));
            if (parentDir.isEmpty()) parentDir = "/";
            String newPath = parentDir.equals("/") ? "/" + newName : parentDir + "/" + newName;
            
            executeRemoteCommand(host, username, "mv \"" + oldPath + "\" \"" + newPath + "\"");
            
            response.put("success", true);
            response.put("message", "Đã đổi tên thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    @PostMapping("/api/delete-file")
    @ResponseBody
    public Map<String, Object> deleteFile(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String host = (String) request.get("host");
            String username = (String) request.get("username");
            String filePath = (String) request.get("filePath");
            boolean isDirectory = (boolean) request.get("isDirectory");
            
            String command = isDirectory ? "rm -rf \"" + filePath + "\"" : "rm \"" + filePath + "\"";
            executeRemoteCommand(host, username, command);
            
            response.put("success", true);
            response.put("message", "Đã xóa thành công");
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    // Helper methods
    private boolean checkServerStatus(String host) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession("ubuntu", host, 22);
            session.setPassword("123456");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);
            
            boolean isConnected = session.isConnected();
            session.disconnect();
            return isConnected;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Map<String, Object>> getRemoteFileList(String host, String username, String path) throws Exception {
        List<Map<String, Object>> files = new ArrayList<>();
        
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword("123456");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        
        // Sử dụng lệnh ls với format đặc biệt
        String command = "ls -la \"" + path + "\" | tail -n +2"; // Bỏ dòng total
        channel.setCommand(command);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channel.setOutputStream(outputStream);
        
        channel.connect();
        
        while (channel.isConnected()) {
            Thread.sleep(100);
        }
        
        String output = outputStream.toString();
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            
            Map<String, Object> file = parseFileInfo(line, path);
            if (file != null) {
                files.add(file);
            }
        }
        
        channel.disconnect();
        session.disconnect();
        
        return files;
    }

    private Map<String, Object> parseFileInfo(String line, String currentPath) {
        try {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 9) return null;
            
            String permissions = parts[0];
            String size = parts[4];
            String month = parts[5];
            String day = parts[6];
            String timeOrYear = parts[7];
            
            // Tên file có thể chứa khoảng trắng
            StringBuilder nameBuilder = new StringBuilder();
            for (int i = 8; i < parts.length; i++) {
                if (i > 8) nameBuilder.append(" ");
                nameBuilder.append(parts[i]);
            }
            String name = nameBuilder.toString();
            
            // Bỏ qua . và ..
            if (name.equals(".") || name.equals("..")) {
                return null;
            }
            
            Map<String, Object> file = new HashMap<>();
            file.put("name", name);
            file.put("permissions", permissions);
            file.put("size", parseSize(size));
            file.put("lastModified", month + " " + day + " " + timeOrYear);
            file.put("isDirectory", permissions.startsWith("d"));
            
            // Tạo full path
            String fullPath = currentPath.endsWith("/") ? currentPath + name : currentPath + "/" + name;
            file.put("path", fullPath);
            
            return file;
            
        } catch (Exception e) {
            return null;
        }
    }

    private long parseSize(String sizeStr) {
        try {
            return Long.parseLong(sizeStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String readRemoteFile(String host, String username, String filePath) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword("123456");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("cat \"" + filePath + "\"");
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        channel.setOutputStream(outputStream);
        
        channel.connect();
        
        while (channel.isConnected()) {
            Thread.sleep(100);
        }
        
        String content = outputStream.toString();
        
        channel.disconnect();
        session.disconnect();
        
        return content;
    }

    private void writeRemoteFile(String host, String username, String filePath, String content) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword("123456");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        
        // Sử dụng tee để ghi file
        channel.setCommand("tee \"" + filePath + "\" > /dev/null");
        channel.setInputStream(new ByteArrayInputStream(content.getBytes()));
        
        channel.connect();
        
        while (channel.isConnected()) {
            Thread.sleep(100);
        }
        
        channel.disconnect();
        session.disconnect();
    }

    private void executeRemoteCommand(String host, String username, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword("123456");
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        channel.setOutputStream(outputStream);
        channel.setErrStream(errorStream);
        
        channel.connect();
        
        while (channel.isConnected()) {
            Thread.sleep(100);
        }
        
        int exitStatus = channel.getExitStatus();
        
        if (exitStatus != 0) {
            String error = errorStream.toString();
            throw new Exception("Command failed with exit code " + exitStatus + ": " + error);
        }
        
        channel.disconnect();
        session.disconnect();
    }
}
