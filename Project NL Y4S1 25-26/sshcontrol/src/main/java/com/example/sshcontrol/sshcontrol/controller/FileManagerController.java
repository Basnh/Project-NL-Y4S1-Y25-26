package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.service.SshFileService;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/file-manager")
public class FileManagerController {

    @Autowired
    private SshFileService sshFileService;

    // Trang chính hiển thị file manager
    @GetMapping
    public String showFileManager(@RequestParam(required = false) String serverId, 
                                 @RequestParam(defaultValue = "~") String path, 
                                 Model model, HttpSession session) {
        System.out.println("FileManager accessed with serverId: " + serverId + ", path: " + path);
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            System.out.println("No user in session, redirecting to login");
            return "redirect:/login";
        }
        
        System.out.println("User found: " + user.getUsername() + ", servers count: " + 
                          (user.getServers() != null ? user.getServers().size() : 0));
        
        // Thêm user servers vào model
        model.addAttribute("user", user);
        model.addAttribute("servers", user.getServers());
        
        // Nếu có serverId (sử dụng IP làm ID), xử lý file browsing
        if (serverId != null && !serverId.isEmpty()) {
            System.out.println("Searching for server with IP: " + serverId);
            try {
                // Tìm server theo IP
                Server selectedServer = null;
                for (Server server : user.getServers()) {
                    System.out.println("Checking server: " + server.getName() + " - " + server.getIp());
                    if (server.getIp().equals(serverId)) {
                        selectedServer = server;
                        break;
                    }
                }
                
                if (selectedServer != null) {
                    System.out.println("Found server: " + selectedServer.getName());
                    List<String> files = sshFileService.listDirectory(path, selectedServer.getIp(), 
                                                                      selectedServer.getSshUsername(), 
                                                                      selectedServer.getSshPassword());
                    model.addAttribute("files", files);
                    model.addAttribute("currentPath", path);
                    model.addAttribute("selectedServer", selectedServer);
                    model.addAttribute("isRoot", path.equals("/") || path.equals("~"));
                    model.addAttribute("parentPath", getParentPath(path));
                    System.out.println("Files found: " + files.size());
                } else {
                    System.out.println("Server not found with IP: " + serverId);
                    model.addAttribute("error", "Không tìm thấy máy chủ được chọn!");
                    model.addAttribute("currentPath", path); // Set currentPath để hiển thị error
                }
            } catch (Exception e) {
                System.out.println("Error accessing directory: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("error", "Không thể truy cập thư mục: " + e.getMessage());
                model.addAttribute("currentPath", path); // Set currentPath để hiển thị error
            }
        }
        
        return "file-manager";
    }
    
    private String getParentPath(String currentPath) {
        if (currentPath == null || currentPath.equals("/") || currentPath.equals("~")) {
            return "/";
        }
        
        String path = currentPath.endsWith("/") ? currentPath.substring(0, currentPath.length() - 1) : currentPath;
        int lastSlash = path.lastIndexOf('/');
        
        if (lastSlash <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlash);
    }

    // Xóa file hoặc thư mục
    @PostMapping("/delete")
    public String deleteFile(@RequestParam String path, @RequestParam String serverId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            // Tìm server theo IP
            Server selectedServer = null;
            for (Server server : user.getServers()) {
                if (server.getIp().equals(serverId)) {
                    selectedServer = server;
                    break;
                }
            }
            
            if (selectedServer != null) {
                sshFileService.deleteFile(path, selectedServer.getIp(), 
                                         selectedServer.getSshUsername(), 
                                         selectedServer.getSshPassword());
            }
        } catch (Exception e) {
            // Log hoặc báo lỗi
            System.out.println("Error deleting file: " + e.getMessage());
        }
        return "redirect:/file-manager?serverId=" + serverId + "&path=" + getParentPath(path);
    }

    // Tải xuống file
    @GetMapping("/download")
    @ResponseBody
    public byte[] downloadFile(@RequestParam String path, @RequestParam String serverId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return new byte[0];
        }
        
        try {
            // Tìm server theo IP
            Server selectedServer = null;
            for (Server server : user.getServers()) {
                if (server.getIp().equals(serverId)) {
                    selectedServer = server;
                    break;
                }
            }
            
            if (selectedServer != null) {
                return sshFileService.downloadFile(path, selectedServer.getIp(), 
                                                  selectedServer.getSshUsername(), 
                                                  selectedServer.getSshPassword());
            }
        } catch (Exception e) {
            System.out.println("Error downloading file: " + e.getMessage());
        }
        return new byte[0];
    }
    
    // Refresh directory
    @PostMapping("/refresh")
    public String refreshDirectory(@RequestParam String serverId, @RequestParam String path) {
        return "redirect:/file-manager?serverId=" + serverId + "&path=" + path;
    }
}
