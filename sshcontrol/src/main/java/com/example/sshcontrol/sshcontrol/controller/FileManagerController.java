package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.sshcontrol.service.FileManagerService;
import com.example.sshcontrol.service.SshFileService;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.sshcontrol.service.UserService;
import com.example.sshcontrol.sshcontrol.util.ControllerHelper;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/file-manager")
public class FileManagerController {

    @Autowired
    private SshFileService sshFileService;

    @Autowired
    private FileManagerService fileManagerService;

    @Autowired
    private UserService userService;

    // Trang chính hiển thị file manager
    @GetMapping
    public String showFileManager(@RequestParam(required = false) String serverId, 
                                 @RequestParam(defaultValue = "~") String path, 
                                 Model model, HttpSession session) {
        System.out.println("FileManager accessed with serverId: " + serverId + ", path: " + path);
        
        if (!ControllerHelper.isUserLoggedIn(session)) {
            System.out.println("No user in session, redirecting to login");
            return "redirect:/login";
        }
        
        User user = ControllerHelper.getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        ControllerHelper.updateUserAndModel(session, model, userService);
        
        // Thêm danh sách server vào model
        List<Server> servers = user.getServers() != null ? user.getServers() : List.of();
        model.addAttribute("servers", servers);
        
        System.out.println("User found: " + user.getUsername() + ", servers count: " + servers.size());
        
        // Nếu có serverId (sử dụng IP làm ID), xử lý file browsing
        if (serverId != null && !serverId.isEmpty()) {
            System.out.println("Searching for server with IP: " + serverId);
            try {
                // Tìm server theo IP
                Server selectedServer = null;
                for (Server server : user.getServers()) {
                    System.out.println("Checking server: " + server.getName() + " - " + server.getIp());
                    if (server.getIp() != null && server.getIp().equals(serverId)) {
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
        if (!ControllerHelper.isUserLoggedIn(session)) {
            return "redirect:/login";
        }
        
        User user = ControllerHelper.getCurrentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            // Tìm server theo IP
            Server selectedServer = user.getServers().stream()
                .filter(server -> server.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (selectedServer != null) {
                sshFileService.deleteFile(path, selectedServer.getIp(), 
                                       selectedServer.getSshUsername(), 
                                       selectedServer.getSshPassword());
            }
        } catch (Exception e) {
            System.out.println("Error deleting file: " + e.getMessage());
        }
        return "redirect:/file-manager?serverId=" + serverId + "&path=" + getParentPath(path);
    }

    // Tải xuống file
    @GetMapping("/download")
    @ResponseBody
    public byte[] downloadFile(@RequestParam String path, @RequestParam String serverId, HttpSession session) {
        if (!ControllerHelper.isUserLoggedIn(session)) {
            return new byte[0];
        }
        
        User user = ControllerHelper.getCurrentUser(session);
        if (user == null) {
            return new byte[0];
        }
        
        try {
            // Tìm server theo IP
            Server selectedServer = user.getServers().stream()
                .filter(server -> server.getIp() != null && server.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
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

    // ============ REST API ENDPOINTS ============

    /**
     * Liệt kê các file trong thư mục
     */
    @PostMapping("/api/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listFiles(@RequestBody Map<String, String> request, 
                                                         @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            
            if (serverId == null || serverId.isEmpty()) {
                response.put("success", false);
                response.put("error", "Máy chủ không được chọn");
                return ResponseEntity.badRequest().body(response);
            }

            if (path == null || path.isEmpty()) {
                path = "/";
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            List<FileManagerService.FileInfo> files = fileManagerService.listFiles(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            response.put("success", true);
            response.put("files", files);
            response.put("currentPath", path);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Đọc nội dung file
     */
    @PostMapping("/api/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> readFile(@RequestBody Map<String, String> request, 
                                                        @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            String content = fileManagerService.readFile(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            response.put("success", true);
            response.put("content", content);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Ghi nội dung vào file
     */
    @PostMapping("/api/write")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> writeFile(@RequestBody Map<String, String> request, 
                                                         @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            String content = request.get("content");
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            fileManagerService.writeFile(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path, 
                content != null ? content : ""
            );
            response.put("success", true);
            response.put("message", "Lưu file thành công");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Xóa file hoặc thư mục
     */
    @PostMapping("/api/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFileApi(@RequestBody Map<String, String> request, 
                                                             @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            fileManagerService.deleteFile(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            response.put("success", true);
            response.put("message", "Xóa file thành công");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Đổi tên file
     */
    @PostMapping("/api/rename")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> renameFileApi(@RequestBody Map<String, String> request, 
                                                             @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String oldPath = request.get("oldPath");
            String newPath = request.get("newPath");
            
            if (serverId == null || serverId.isEmpty() || oldPath == null || oldPath.isEmpty() || newPath == null || newPath.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            fileManagerService.renameFile(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                oldPath, 
                newPath
            );
            response.put("success", true);
            response.put("message", "Đổi tên file thành công");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Tạo thư mục mới
     */
    @PostMapping("/api/mkdir")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createDirectoryApi(@RequestBody Map<String, String> request, 
                                                                  @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            fileManagerService.createDirectory(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            response.put("success", true);
            response.put("message", "Tạo thư mục thành công");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Tải file xuống
     */
    @GetMapping("/api/download")
    public void downloadFileApi(@RequestParam String serverId, @RequestParam String path, HttpServletResponse response,
                               @SessionAttribute(name = "user", required = false) User user) {
        try {
            // Kiểm tra authentication
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Auth fail");
                return;
            }
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tham số không hợp lệ");
                return;
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Máy chủ không tồn tại");
                return;
            }

            InputStream inputStream = fileManagerService.getFileInputStream(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            
            // Set response headers
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + path.substring(path.lastIndexOf('/') + 1) + "\"");
            
            // Copy file to response
            OutputStream outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Lấy thông tin file
     */
    @PostMapping("/api/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFileInfoApi(@RequestBody Map<String, String> request,
                                                              @SessionAttribute(name = "user", required = false) User user) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra authentication
        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String serverId = request.get("serverId");
            String path = request.get("path");
            
            if (serverId == null || serverId.isEmpty() || path == null || path.isEmpty()) {
                response.put("success", false);
                response.put("error", "Tham số không hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Tìm Server từ User
            Server server = user.getServers().stream()
                .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                .findFirst()
                .orElse(null);
            
            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            long size = fileManagerService.getFileSize(
                server.getIp(), 
                server.getSshUsername(), 
                server.getSshPassword(), 
                path
            );
            response.put("success", true);
            response.put("size", size);
            response.put("exists", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }
    }
}
