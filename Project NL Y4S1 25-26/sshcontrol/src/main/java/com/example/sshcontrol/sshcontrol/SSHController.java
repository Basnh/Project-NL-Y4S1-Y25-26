package com.example.sshcontrol.sshcontrol;

import com.example.sshcontrol.model.MultiSSHRequest;
import com.example.sshcontrol.model.SSHRequest;
import com.example.sshcontrol.service.SSHService;
import com.example.sshcontrol.model.MultiServiceRequest;
import com.example.sshcontrol.model.MultiConfigRequest;
import com.example.sshcontrol.model.FileInfo;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
public class SSHController {

    @Autowired
    private SSHService sshService;

    // ===============================
    // MAIN PAGES
    // ===============================

    @GetMapping("/ssh-dashboard") 
    public String sshDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        // Kiểm tra trạng thái online cho từng server
        for (Server server : user.getServers()) {
            boolean isOnline = checkServerOnline(server);
            server.setOnline(isOnline);
        }
        model.addAttribute("remoteUser", user.getUsername());
        model.addAttribute("studentName", user.getUsername());
        model.addAttribute("studentEmail", user.getUsername() != null ? user.getUsername() : "");
        model.addAttribute("serverList", user.getServers());
        return "dashboard"; // hoặc template phù hợp
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // ===============================
    // SERVER MANAGEMENT
    // ===============================

    @GetMapping("/server-info-detail")
    @ResponseBody
    public String getServerInfo(@RequestParam String ip, HttpSession session) {
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (username == null || password == null) {
            return "Chưa đăng nhập SSH.";
        }

        try {
            return sshService.executeCommand(ip, username, password, """
                    echo "OS: $(lsb_release -d | cut -f2)"
                    echo "CPU: $(lscpu | grep 'Model name' | cut -d ':' -f2)"
                    echo "RAM: $(free -h | grep Mem | awk '{print $2}')"
                    echo "Disk: $(df -h / | tail -1 | awk '{print $2}')"
                    """);
        } catch (Exception e) {
            return "Lỗi khi lấy thông tin máy chủ: " + e.getMessage();
        }
    }

    private boolean checkServerOnline(Server server) {
        try {
            return java.net.InetAddress.getByName(server.getIp()).isReachable(1000);
        } catch (Exception e) {
            return false;
        }
    }

    // ===============================
    // COMMAND EXECUTION
    // ===============================

    @PostMapping("/execute-page")
    public String executeCommand(@ModelAttribute SSHRequest sshRequest, Model model, HttpSession session) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        sshRequest.setHost(host);
        sshRequest.setUsername(username);
        sshRequest.setPassword(password);

        String command = sshRequest.getCommand();
        if (command != null && command.trim().startsWith("sudo")) {
            command = "echo '" + sshRequest.getPassword() + "' | sudo -S " + command.substring(5).trim();
        }

        String result = sshService.executeCommand(
            sshRequest.getHost(),
            sshRequest.getUsername(),
            sshRequest.getPassword(),
            command
        );
        
        sshRequest.setCommand("");
        model.addAttribute("result", result);
        model.addAttribute("sshRequest", sshRequest);
        return "execute-page";
    }

    @PostMapping("/execute-multi")
    @ResponseBody
    public List<String> executeMulti(@RequestBody MultiSSHRequest request, HttpSession session) throws InterruptedException {
        List<String> hosts = request.getHosts();
        List<String> users = new ArrayList<>();
        List<String> passwords = new ArrayList<>();
        
        User sessionUser = (User) session.getAttribute("user");
        for (String host : hosts) {
            String ip = host;
            String user = null;
            String pass = null;
            
            if (host.contains("@")) {
                String[] parts = host.split("@", 2);
                user = parts[0];
                ip = parts[1];
            }
            
            final String ipFinal = ip;
            if (sessionUser != null) {
                Server s = sessionUser.getServers().stream()
                    .filter(server -> server.getIp().equals(ipFinal))
                    .findFirst()
                    .orElse(null);
                if (s != null) {
                    if (user == null) user = s.getSshUsername();
                    pass = s.getSshPassword();
                }
            }
            
            users.add(user != null ? user : "ubuntu");
            passwords.add(pass != null ? pass : "123456");
        }
        
        return sshService.executeCommandOnMultipleHosts(hosts, request.getCommand(), users, passwords);
    }

    @PostMapping("/multi-command")
    @ResponseBody
    public Map<String, String> multiCommand(@RequestBody MultiSSHRequest request, HttpSession session) {
        List<String> hosts = request.getHosts();
        List<String> ipList = new ArrayList<>();
        List<String> users = new ArrayList<>();
        List<String> passwords = new ArrayList<>();
        
        User sessionUser = (User) session.getAttribute("user");
        for (String host : hosts) {
            String ip = host;
            String user = null;
            String pass = null;
            
            if (host.contains("@")) {
                String[] parts = host.split("@", 2);
                user = parts[0];
                ip = parts[1];
            }
            
            final String ipFinal = ip;
            if (sessionUser != null) {
                Server s = sessionUser.getServers().stream()
                    .filter(server -> server.getIp().equals(ipFinal))
                    .findFirst()
                    .orElse(null);
                if (s != null) {
                    if (user == null) user = s.getSshUsername();
                    pass = s.getSshPassword();
                }
            }
            
            ipList.add(ip);
            users.add(user != null ? user : "ubuntu");
            passwords.add(pass != null ? pass : "123456");
        }
        
        String command = request.getCommand();
        Map<String, String> result = new HashMap<>();
        try {
            List<String> outputs = sshService.executeCommandOnMultipleHosts(ipList, command, users, passwords);
            for (int i = 0; i < hosts.size(); i++) {
                result.put(hosts.get(i), outputs.size() > i ? outputs.get(i) : "Không nhận được phản hồi.");
            }
        } catch (Exception e) {
            for (String host : hosts) {
                result.put(host, "Lỗi: " + e.getMessage());
            }
        }
        return result;
    }

    // ===============================
    // SERVICE MANAGEMENT
    // ===============================

    @GetMapping("/list-services")
    public String showListServicesPage(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            model.addAttribute("userServers", sessionUser.getServers());
        }
        return "list-services";
    }

    @PostMapping("/api/list-services")
    @ResponseBody
    public Map<String, Object> listServices(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = request.get("host");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        try {
            String servicesOutput = sshService.executeCommand(host, username, password, 
                "systemctl list-units --type=service --all --no-pager --plain");
            
            if (servicesOutput != null) {
                List<Map<String, String>> services = parseServicesOutput(servicesOutput);
                result.put("success", true);
                result.put("services", services);
            } else {
                result.put("success", false);
                result.put("error", "Không thể lấy danh sách dịch vụ");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Lỗi: " + e.getMessage());
        }
        
        return result;
    }

    @PostMapping("/api/service-action")
    @ResponseBody
    public Map<String, Object> serviceAction(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = request.get("host");
        String serviceName = request.get("serviceName");
        String action = request.get("action");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        try {
            String command;
            switch (action) {
                case "start":
                    command = "echo '" + password + "' | sudo -S systemctl start " + serviceName;
                    break;
                case "stop":
                    command = "echo '" + password + "' | sudo -S systemctl stop " + serviceName;
                    break;
                case "restart":
                    command = "echo '" + password + "' | sudo -S systemctl restart " + serviceName;
                    break;
                case "status":
                    command = "systemctl status " + serviceName;
                    break;
                default:
                    result.put("success", false);
                    result.put("error", "Hành động không hợp lệ");
                    return result;
            }
            
            String output = sshService.executeCommand(host, username, password, command);
            
            if (output != null) {
                result.put("success", true);
                result.put("output", output);
            } else {
                result.put("success", false);
                result.put("error", "Không thể thực hiện hành động");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Lỗi: " + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/control-service")
    public String showControlService(Model model, HttpSession session) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        SSHRequest sshRequest = new SSHRequest();
        sshRequest.setHost(host);
        sshRequest.setUsername(username);
        sshRequest.setPassword(password);

        String serviceListRaw = sshService.executeCommand(
            host, username, password,
            "systemctl list-units --type=service --all --no-pager --no-legend"
        );
        String[] services = serviceListRaw.split("\\r?\\n");

        model.addAttribute("sshRequest", sshRequest);
        model.addAttribute("services", services);
        return "control-service";
    }

    @PostMapping("/control-service")
    public String controlService(
        @ModelAttribute SSHRequest sshRequest,
        @RequestParam String serviceName,
        @RequestParam String action,
        Model model,
        HttpSession session,
        RedirectAttributes redirectAttributes
    ) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        sshRequest.setHost(host);
        sshRequest.setUsername(username);
        sshRequest.setPassword(password);

        String command = "";
        String successMessage = null;
        String errorMessage = null;

        switch (action) {
            case "start":
                command = "sudo systemctl start " + serviceName;
                successMessage = "Dịch vụ '" + serviceName + "' đã được khởi động thành công.";
                break;
            case "stop":
                command = "sudo systemctl stop " + serviceName;
                successMessage = "Dịch vụ '" + serviceName + "' đã được dừng thành công.";
                break;
            case "restart":
                command = "sudo systemctl restart " + serviceName;
                successMessage = "Dịch vụ '" + serviceName + "' đã được khởi động lại thành công.";
                break;
            case "status":
                command = "systemctl status " + serviceName;
                break;
            default:
                command = "systemctl status " + serviceName;
        }

        String result = sshService.executeCommand(
            sshRequest.getHost(),
            sshRequest.getUsername(),
            sshRequest.getPassword(),
            command
        );

        String serviceListRaw = sshService.executeCommand(
            host, username, password,
            "systemctl list-units --type=service --all --no-pager --no-legend"
        );
        String[] services = serviceListRaw.split("\\r?\\n");
        model.addAttribute("services", services);

        if ((action.equals("start") || action.equals("stop") || action.equals("restart"))) {
            if (result == null || result.trim().isEmpty() || result.toLowerCase().contains("failed") || result.toLowerCase().contains("error")) {
                errorMessage = "Thao tác " + action + " dịch vụ '" + serviceName + "' thất bại. " + result;
                model.addAttribute("error", errorMessage);
            } else {
                model.addAttribute("message", successMessage);
            }
        }
        
        model.addAttribute("result", result);
        model.addAttribute("sshRequest", sshRequest);
        model.addAttribute("serviceName", serviceName);
        model.addAttribute("action", action);
        return "control-service";
    }

    @PostMapping("/api/control-service")
    @ResponseBody
    public String apiControlService(
        @RequestParam String serviceName,
        @RequestParam String action,
        HttpSession session
    ) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "Chưa đăng nhập!";
        }

        String command;
        switch (action) {
            case "start":
                command = "echo '" + password + "' | sudo -S systemctl start " + serviceName;
                break;
            case "stop":
                command = "echo '" + password + "' | sudo -S systemctl stop " + serviceName;
                break;
            case "status":
                command = "systemctl status " + serviceName;
                break;
            default:
                return "Hành động không hợp lệ!";
        }

        String result = sshService.executeCommand(host, username, password, command);

        if ((action.equals("start") || action.equals("stop"))) {
            String statusCmd = "systemctl is-active " + serviceName;
            String status = sshService.executeCommand(host, username, password, statusCmd);
            if (status != null && status.trim().equals("active") && action.equals("start")) {
                return "Dịch vụ đã được khởi động thành công!";
            } else if (status != null && status.trim().equals("inactive") && action.equals("stop")) {
                return "Dịch vụ đã được dừng thành công!";
            } else {
                return "Không xác định được trạng thái dịch vụ sau khi thực hiện. Kết quả: " + (result == null ? "" : result);
            }
        }

        return (result == null || result.trim().isEmpty()) ? "Không có kết quả trả về!" : result;
    }

    // ===============================
    // MULTI-SERVER OPERATIONS
    // ===============================

    @PostMapping("/multi-control-service")
    @ResponseBody
    public List<String> multiControlService(@RequestBody MultiServiceRequest request) throws InterruptedException {
        return sshService.controlServiceOnMultipleHosts(
            request.getHosts(), request.getUser(), request.getPassword(), request.getServiceName(), request.getAction()
        );
    }

    @GetMapping("/multi-list-services")
    public String showMultiListServicesPage() {
        return "multi-list-services";
    }

    @PostMapping("/multi-list-services")
    @ResponseBody
    public Map<String, String> multiListServices(@RequestBody Map<String, Object> request, HttpSession session) {
        List<?> rawHosts = (List<?>) request.get("hosts");
        List<String> hosts = new ArrayList<>();
        if (rawHosts != null) {
            for (Object o : rawHosts) {
                if (o != null) hosts.add(o.toString());
            }
        }
        
        Map<String, String> result = new HashMap<>();
        User sessionUser = (User) session.getAttribute("user");
        
        for (String host : hosts) {
            String ip = host;
            String user = null;
            String pass = null;
            
            if (host.contains("@")) {
                String[] parts = host.split("@", 2);
                user = parts[0];
                ip = parts[1];
            }
            
            final String ipFinal = ip;
            if (sessionUser != null) {
                Server s = sessionUser.getServers().stream()
                    .filter(server -> server.getIp().equals(ipFinal))
                    .findFirst()
                    .orElse(null);
                if (s != null) {
                    if (user == null) user = s.getSshUsername();
                    pass = s.getSshPassword();
                }
            }
            
            if (user == null) user = "ubuntu";
            if (pass == null) pass = "123456";
            
            try {
                String cmd = "systemctl list-units --type=service --all --no-pager --no-legend";
                String output = sshService.executeCommand(ip, user, pass, cmd);
                result.put(host, output);
            } catch (Exception e) {
                result.put(host, "Lỗi: " + e.getMessage());
            }
        }
        return result;
    }

    // ===============================
    // FILE MANAGEMENT
    // ===============================

    @GetMapping("/api/list-files")
    @ResponseBody
    public Map<String, Object> listFilesApi(
            @RequestParam String host,
            @RequestParam String path,
            HttpSession session) {
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");
        Map<String, Object> result = new HashMap<>();
        List<FileInfo> files = new ArrayList<>();
        
        try {
            String command = "ls -l --time-style=+%s " + path;
            String output = sshService.executeCommand(host, username, password, command);
            for (String line : output.split("\\n")) {
                if (line.startsWith("total")) continue;
                String[] parts = line.trim().split("\\s+", 8);
                if (parts.length < 8) continue;
                boolean isDirectory = parts[0].startsWith("d");
                String name = parts[7];
                long modified = 0;
                try { modified = Long.parseLong(parts[5]) * 1000L; } catch (Exception e) {}
                files.add(new FileInfo(
                    name,
                    path.endsWith("/") ? path + name : path + "/" + name,
                    isDirectory,
                    modified,
                    modified
                ));
            }
            result.put("files", files);
            result.put("success", true);
        } catch (Exception e) {
            result.put("files", new ArrayList<>());
            result.put("success", false);
            result.put("message", "Không thể lấy danh sách file: " + e.getMessage());
        }
        return result;
    }

    @GetMapping("/edit-config-page")
    public String showEditConfigPage(Model model, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            model.addAttribute("userServers", sessionUser.getServers());
        }
        return "edit-config-page";
    }

    @PostMapping("/api/get-file-content")
    @ResponseBody
    public Map<String, Object> getFileContent(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = request.get("host");
        String filePath = request.get("filePath");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        try {
            String content = sshService.executeCommand(host, username, password, "cat " + filePath);
            if (content != null && !content.toLowerCase().contains("no such file")) {
                result.put("success", true);
                result.put("content", content);
            } else {
                result.put("success", false);
                result.put("error", "File không tồn tại hoặc không có quyền đọc");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Lỗi: " + e.getMessage());
        }
        
        return result;
    }

    @PostMapping("/api/save-file-content")
    @ResponseBody
    public Map<String, Object> saveFileContent(@RequestBody Map<String, String> request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = request.get("host");
        String filePath = request.get("filePath");
        String content = request.get("content");
        
        // Kiểm tra đầu vào
        if (host == null || filePath == null) {
            result.put("success", false);
            result.put("error", "Thiếu thông tin host hoặc đường dẫn file");
            return result;
        }
        
        // Đảm bảo content không null
        if (content == null) {
            content = "";
        }
        
        // Log để debug - QUAN TRỌNG để tìm nguyên nhân
        System.out.println("=== SAVE FILE API DEBUG ===");
        System.out.println("Host: " + host);
        System.out.println("File path: " + filePath);
        System.out.println("Content length: " + content.length());
        System.out.println("Content is null: " + (content == null));
        System.out.println("Content is empty after trim: " + content.trim().isEmpty());
        System.out.println("Content preview (first 200 chars): " + (content.length() > 200 ? content.substring(0, 200) + "..." : content));
        System.out.println("===========================");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        try {
            // Cách tiếp cận an toàn: Tạo temporary file trước, sau đó copy với sudo
            String tempFile = "/tmp/temp_edit_" + System.currentTimeMillis();
            
            // Bước 1: Tạo file tạm với nội dung (không cần sudo)
            String createTempCmd = "cat > " + tempFile + " << 'EDIT_FILE_EOF'\n" + content + "\nEDIT_FILE_EOF";
            
            System.out.println("Step 1: Creating temp file with content...");
            String tempResult = sshService.executeCommand(host, username, password, createTempCmd);
            System.out.println("Temp file creation result: " + tempResult);
            
            // Bước 2: Copy file tạm đến vị trí đích với sudo
            String copyCmd = "echo '" + password + "' | sudo -S cp " + tempFile + " " + filePath;
            
            System.out.println("Step 2: Copying temp file to destination with sudo...");
            String copyResult = sshService.executeCommand(host, username, password, copyCmd);
            System.out.println("Copy result: " + copyResult);
            
            // Bước 3: Xóa file tạm
            String cleanupCmd = "rm -f " + tempFile;
            sshService.executeCommand(host, username, password, cleanupCmd);
            
            // Kiểm tra kết quả
            if (copyResult != null && (copyResult.toLowerCase().contains("permission denied") || 
                                     copyResult.toLowerCase().contains("cannot create") ||
                                     copyResult.toLowerCase().contains("no such file") ||
                                     copyResult.toLowerCase().contains("error"))) {
                result.put("success", false);
                result.put("error", "Không thể lưu file: " + copyResult);
            } else {
                // Verify bằng cách kiểm tra kích thước file
                String verifyCmd = "wc -c " + filePath + " 2>/dev/null || echo 'Error checking file'";
                String verifyResult = sshService.executeCommand(host, username, password, verifyCmd);
                
                result.put("success", true);
                result.put("message", "Lưu file thành công");
                result.put("contentLength", content.length());
                result.put("verification", verifyResult != null ? verifyResult.trim() : "No verification");
                
                System.out.println("✅ File saved successfully!");
                System.out.println("Original content length: " + content.length());
                System.out.println("File verification: " + verifyResult);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Lỗi khi lưu file: " + e.getMessage());
            System.err.println("❌ Error saving file: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    // ===============================
    // CONFIG MANAGEMENT
    // ===============================

    @GetMapping("/multi-config")
    public String showMultiConfigPage() {
        return "multi-config";
    }

    @PostMapping("/get-service-config")
    @ResponseBody
    public Map<String, Object> getServiceConfig(@RequestBody Map<String, String> req, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = req.get("host");
        String serviceName = req.get("serviceName");
        String configPath = req.get("configPath");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        String content = "";
        if (configPath != null && !configPath.trim().isEmpty()) {
            content = sshService.executeCommand(host, username, password, "cat " + configPath);
            result.put("configPath", configPath);
        } else {
            configPath = "/etc/" + serviceName.replace(".service", "") + "/" + serviceName.replace(".service", "") + ".conf";
            content = sshService.executeCommand(host, username, password, "cat " + configPath);
            if (content == null || content.toLowerCase().contains("no such file")) {
                configPath = "/etc/default/" + serviceName.replace(".service", "");
                content = sshService.executeCommand(host, username, password, "cat " + configPath);
            }
            result.put("configPath", configPath);
        }
        
        result.put("content", content == null ? "" : content);
        return result;
    }

    @PostMapping("/save-service-config")
    @ResponseBody
    public Map<String, Object> saveServiceConfig(@RequestBody Map<String, String> req, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        String host = req.get("host");
        String serviceName = req.get("serviceName");
        String configPath = req.get("configPath");
        String content = req.get("content");
        
        String[] credentials = getUserCredentials(host, session);
        String username = credentials[0];
        String password = credentials[1];
        
        if (configPath == null || configPath.trim().isEmpty()) {
            configPath = "/etc/" + serviceName.replace(".service", "") + "/" + serviceName.replace(".service", "") + ".conf";
            String check = sshService.executeCommand(host, username, password, "ls " + configPath);
            if (check == null || check.toLowerCase().contains("no such file")) {
                configPath = "/etc/default/" + serviceName.replace(".service", "");
            }
        }
        
        String cmd = "echo '" + password + "' | sudo -S tee " + configPath;
        String saveResult = sshService.executeCommandWithInput(host, username, password, cmd, content);
        
        if (saveResult != null && saveResult.toLowerCase().contains("permission denied")) {
            result.put("success", false);
            result.put("message", "Không đủ quyền ghi file.");
        } else {
            result.put("success", true);
            result.put("message", "Lưu cấu hình thành công.");
        }
        
        return result;
    }

    @PostMapping("/multi-save-config")
    @ResponseBody
    public List<String> multiSaveConfig(@RequestBody MultiConfigRequest request) throws InterruptedException {
        return sshService.saveConfigOnMultipleHosts(
            request.getHosts(), request.getUser(), request.getPassword(), request.getConfigPath(), request.getContent()
        );
    }

    @GetMapping("/multi-edit-config")
    public String showMultiEditConfig(
            @RequestParam("path") String path,
            @RequestParam("hosts") String hostsParam,
            Model model,
            HttpSession session
    ) {
        User sessionUser = (User) session.getAttribute("user");
        List<Map<String, String>> serverInfos = new ArrayList<>();
        
        if (hostsParam != null && !hostsParam.isEmpty()) {
            String[] hostArr = hostsParam.split(",");
            for (String hostStr : hostArr) {
                String ip = hostStr;
                String username = null;
                
                if (hostStr.contains("@")) {
                    String[] parts = hostStr.split("@", 2);
                    username = parts[0];
                    ip = parts[1];
                }
                
                final String ipFinal = ip;
                String password = null;
                String name = ip;
                
                if (sessionUser != null) {
                    Server s = sessionUser.getServers().stream()
                        .filter(server -> server.getIp().equals(ipFinal))
                        .findFirst()
                        .orElse(null);
                    if (s != null) {
                        if (username == null) username = s.getSshUsername();
                        password = s.getSshPassword();
                        name = s.getName();
                    }
                }
                
                if (username == null) username = "ubuntu";
                if (password == null) password = "123456";
                
                String content = "";
                try {
                    content = sshService.executeCommand(ip, username, password, "cat " + path);
                } catch (Exception e) {
                    content = "Không thể đọc file: " + e.getMessage();
                }
                
                Map<String, String> info = new HashMap<>();
                info.put("name", name);
                info.put("ip", ip);
                info.put("username", username);
                info.put("content", content);
                serverInfos.add(info);
            }
        }
        
        model.addAttribute("servers", serverInfos);
        model.addAttribute("path", path);
        return "multi-edit-config";
    }

    @PostMapping("/multi-edit-config")
    public String saveMultiEditConfig(
            @RequestParam("path") String path,
            @RequestParam("ips") List<String> ips,
            @RequestParam("usernames") List<String> usernames,
            @RequestParam("contents") List<String> contents,
            Model model,
            HttpSession session
    ) {
        User sessionUser = (User) session.getAttribute("user");
        List<Map<String, String>> results = new ArrayList<>();
        
        for (int i = 0; i < ips.size(); i++) {
            String ip = ips.get(i);
            String username = usernames.get(i);
            String content = contents.get(i);
            String password = null;
            String name = ip;
            
            if (sessionUser != null) {
                Server s = sessionUser.getServers().stream()
                    .filter(server -> server.getIp().equals(ip))
                    .findFirst()
                    .orElse(null);
                if (s != null) {
                    if (username == null || username.isEmpty()) username = s.getSshUsername();
                    password = s.getSshPassword();
                    name = s.getName();
                }
            }
            
            if (username == null || username.isEmpty()) username = "ubuntu";
            if (password == null) password = "123456";
            
            String result;
            try {
                String cmd = "echo '" + password + "' | sudo -S tee " + path;
                String saveResult = sshService.executeCommandWithInput(ip, username, password, cmd, content);
                if (saveResult != null && saveResult.toLowerCase().contains("permission denied")) {
                    result = "Không đủ quyền ghi file.";
                } else {
                    result = "Lưu thành công!";
                }
            } catch (Exception e) {
                result = "Lỗi: " + e.getMessage();
            }
            
            Map<String, String> info = new HashMap<>();
            info.put("name", name);
            info.put("ip", ip);
            info.put("username", username);
            info.put("result", result);
            results.add(info);
        }
        
        model.addAttribute("results", results);
        model.addAttribute("path", path);
        return "multi-edit-config";
    }

    // ===============================
    // UTILITY METHODS
    // ===============================

    private String[] getUserCredentials(String host, HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        String username = null;
        String password = null;
        
        if (sessionUser != null) {
            Server server = sessionUser.getServers().stream()
                .filter(s -> s.getIp().equals(host))
                .findFirst()
                .orElse(null);
            
            if (server != null) {
                username = server.getSshUsername();
                password = server.getSshPassword();
            }
        }
        
        if (username == null) username = "ubuntu";
        if (password == null) password = "123456";
        
        return new String[]{username, password};
    }

    private List<Map<String, String>> parseServicesOutput(String output) {
        List<Map<String, String>> services = new ArrayList<>();
        
        if (output == null || output.isEmpty()) {
            return services;
        }
        
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("UNIT") || line.startsWith("●")) {
                continue;
            }
            
            String[] parts = line.trim().split("\\s+", 5);
            if (parts.length >= 4) {
                Map<String, String> service = new HashMap<>();
                service.put("name", parts[0].replace(".service", ""));
                service.put("load", parts[1]);
                service.put("active", parts[2]);
                service.put("sub", parts[3]);
                service.put("status", parts[2] + " (" + parts[3] + ")");
                service.put("description", parts.length > 4 ? parts[4] : "");
                services.add(service);
            }
        }
        
        return services;
    }
}