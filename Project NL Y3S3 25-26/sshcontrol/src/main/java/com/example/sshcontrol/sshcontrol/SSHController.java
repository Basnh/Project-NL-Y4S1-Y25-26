package com.example.sshcontrol.sshcontrol;

import com.example.sshcontrol.model.MultiSSHRequest;
import com.example.sshcontrol.model.SSHRequest;
import com.example.sshcontrol.service.SSHService;
import com.example.sshcontrol.model.MultiServiceRequest;
import com.example.sshcontrol.model.MultiConfigRequest;
import com.example.sshcontrol.model.FileInfo;
import com.example.sshcontrol.model.ServiceInfo;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.ServerInfo;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


@Controller
public class SSHController {

    @Autowired
    private SSHService sshService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sshRequest", new SSHRequest());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/execute-page")
    public String showExecutePage(@RequestParam(value = "host", required = false) String hostParam, Model model, HttpSession session) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        // Nếu có host trên URL, ưu tiên lấy user/ip từ đó và tìm password từ session user
        if (hostParam != null && !hostParam.isEmpty()) {
            String ip = hostParam;
            String user = null;
            if (hostParam.contains("@")) {
                String[] parts = hostParam.split("@", 2);
                user = parts[0];
                ip = parts[1];
            }
            User sessionUser = (User) session.getAttribute("user");
            String pass = null;
            if (sessionUser != null) {
                final String ipFinal = ip;
                ServerInfo s = sessionUser.getServers().stream().filter(server -> server.getIp().equals(ipFinal)).findFirst().orElse(null);
                if (s != null) {
                    if (user == null) user = s.getSshUsername();
                    pass = s.getSshPassword();
                }
            }
            // Nếu không tìm thấy user/pass thì dùng mặc định
            if (user == null) user = "ubuntu";
            if (pass == null) pass = "123456";
            // Lưu lại vào session để các thao tác sau dùng
            session.setAttribute("host", ip);
            session.setAttribute("username", user);
            session.setAttribute("password", pass);
            host = ip;
            username = user;
            password = pass;
        }

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        SSHRequest sshRequest = new SSHRequest();
        sshRequest.setHost(host);
        sshRequest.setUsername(username);
        sshRequest.setPassword(password);

        model.addAttribute("sshRequest", sshRequest);
        return "execute-page";
    }

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

    @GetMapping("/list-services")
    public String listServices(Model model, HttpSession session) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        String result = sshService.executeCommand(
            host,
            username,
            password,
            "systemctl list-units --type=service --all --no-pager --no-legend"
        );
        System.out.println("Kết quả lấy dịch vụ:\n" + result);
        String[] services = result.split("\\r?\\n");
        List<ServiceInfo> serviceList = new ArrayList<>();
        for (String line : services) {
            String[] parts = line.trim().split("\\s+", 5); // name, load, status, sub, description
            if (parts.length >= 5) {
                serviceList.add(new ServiceInfo(parts[0], parts[2], parts[4]));
            }
        }
        model.addAttribute("services", serviceList);
        return "list-services";
    }

    @PostMapping("/list-services")
    public String listServices(@ModelAttribute SSHRequest sshRequest, Model model, HttpSession session) {
        String host = (String) session.getAttribute("host");
        String username = (String) session.getAttribute("username");
        String password = (String) session.getAttribute("password");

        if (host == null || username == null || password == null) {
            return "redirect:/login";
        }

        sshRequest.setHost(host);
        sshRequest.setUsername(username);
        sshRequest.setPassword(password);

        String result = sshService.executeCommand(
            sshRequest.getHost(),
            sshRequest.getUsername(),
            sshRequest.getPassword(),
            "systemctl list-units --type=service --all --no-pager --no-legend"
        );
        String[] services = result.split("\\r?\\n");
        List<ServiceInfo> serviceList = new ArrayList<>();
        for (String line : services) {
            String[] parts = line.trim().split("\\s+", 5);
            if (parts.length >= 5) {
                serviceList.add(new ServiceInfo(parts[0], parts[2], parts[4]));
            }
        }
        model.addAttribute("services", serviceList);
        model.addAttribute("sshRequest", sshRequest);
        return "list-services";
    }

    @GetMapping("/select-config")
    public String selectConfig(@RequestParam(required = false) String path, Model model) {
        if (path == null || path.isEmpty()) {
            model.addAttribute("showPathInput", true);
            return "select-config";
        }
        File folder = new File(path);
        File[] fileArr = folder.listFiles();
        List<FileInfo> files = new ArrayList<>();
        if (fileArr != null) {
            for (File f : fileArr) {
                try {
                    files.add(new FileInfo(
                        f.getName(),
                        f.getAbsolutePath(),
                        f.isDirectory(),
                        Files.readAttributes(f.toPath(), BasicFileAttributes.class).creationTime().toMillis(),
                        f.lastModified()
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        model.addAttribute("files", files);
        model.addAttribute("currentPath", path);
        return "select-config";
    }

    @PostMapping("/select-config")
    public String submitSelectConfig(@RequestParam String configFile) {
        return "redirect:/edit-config?configFile=" + configFile;
    }

    @GetMapping("/edit-config")
    public String showEditConfig(@RequestParam String configFile, Model model, HttpSession session) {
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

        String command = "cat " + configFile;
        String content = sshService.executeCommand(host, username, password, command);

        model.addAttribute("configFile", configFile);
        model.addAttribute("content", content);
        model.addAttribute("sshRequest", sshRequest);
        return "edit-config";
    }

    @PostMapping("/save-config")
    public String saveConfig(
        @ModelAttribute SSHRequest sshRequest,
        @RequestParam String content,
        @RequestParam String configFile,
        @RequestParam(required = false) String customCommand,
        Model model,
        HttpSession session
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

        String result = null;
        if (customCommand != null && !customCommand.trim().isEmpty()) {
            result = sshService.executeCommand(
                sshRequest.getHost(),
                sshRequest.getUsername(),
                sshRequest.getPassword(),
                customCommand
            );
        } else {
            String command = "sudo -S tee " + configFile;
            String fullInput = sshRequest.getPassword() + "\n" + content;
            result = sshService.executeCommandWithInput(
                sshRequest.getHost(),
                sshRequest.getUsername(),
                sshRequest.getPassword(),
                command,
                fullInput
            );
        }
        if (result != null && result.contains("Permission denied")) {
            model.addAttribute("error", "Lưu cấu hình thất bại! Không đủ quyền ghi file.");
        } else if (result != null && result.trim().length() > 0) {
            model.addAttribute("message", "Thao tác thành công.");
        } else {
            model.addAttribute("error", "Lưu cấu hình thất bại!");
        }
        model.addAttribute("sshRequest", sshRequest);
        model.addAttribute("configFile", configFile);
        return "save-config";
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

        // Lấy danh sách dịch vụ đầy đủ thông tin
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

        // Lấy lại danh sách dịch vụ đầy đủ thông tin
        String serviceListRaw = sshService.executeCommand(
            host, username, password,
            "systemctl list-units --type=service --all --no-pager --no-legend"
        );
        String[] services = serviceListRaw.split("\\r?\\n");
        model.addAttribute("services", services);

        // Xử lý kết quả và thông báo
        if ((action.equals("start") || action.equals("stop") || action.equals("restart"))) {
            if (result == null || result.trim().isEmpty() || result.toLowerCase().contains("failed") || result.toLowerCase().contains("error")) {
                errorMessage = "Thao tác " + action + " dịch vụ '" + serviceName + "' thất bại. " + result;
                model.addAttribute("error", errorMessage);
            } else {
                model.addAttribute("message", successMessage);
            }
        }
        model.addAttribute("result", result); // Always show the raw result for status or detailed errors

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

        // Nếu là start/stop, kiểm tra lại trạng thái dịch vụ sau khi thực hiện
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

        // Nếu là status, trả về kết quả luôn
        return (result == null || result.trim().isEmpty()) ? "Không có kết quả trả về!" : result;
    }

    @PostMapping("/execute-multi")
    @ResponseBody
    public List<String> executeMulti(@RequestBody MultiSSHRequest request, HttpSession session) throws InterruptedException {
        // Build users and passwords lists for each host, similar to /multi-command
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
                ServerInfo s = sessionUser.getServers().stream().filter(server -> server.getIp().equals(ipFinal)).findFirst().orElse(null);
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

    @PostMapping("/multi-control-service")
    @ResponseBody
    public List<String> multiControlService(@RequestBody MultiServiceRequest request) throws InterruptedException {
        // MultiServiceRequest: hosts, user, password, serviceName, action
        return sshService.controlServiceOnMultipleHosts(
            request.getHosts(), request.getUser(), request.getPassword(), request.getServiceName(), request.getAction()
        );
    }

    @PostMapping("/multi-save-config")
    @ResponseBody
    public List<String> multiSaveConfig(@RequestBody MultiConfigRequest request) throws InterruptedException {
        // MultiConfigRequest: hosts, user, password, configPath, content
        return sshService.saveConfigOnMultipleHosts(
            request.getHosts(), request.getUser(), request.getPassword(), request.getConfigPath(), request.getContent()
        );
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

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
            // Lệnh lấy file/folder, hiển thị: quyền, user, group, size, thời gian sửa, tên
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
                    modified, // dùng modified cho cả created và modified (nếu muốn lấy created cần lệnh khác)
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

    @PostMapping("/multi-command")
    @ResponseBody
    public Map<String, String> multiCommand(@RequestBody MultiSSHRequest request, HttpSession session) {
        List<String> hosts = request.getHosts();
        List<String> ipList = new ArrayList<>();
        List<String> users = new ArrayList<>();
        List<String> passwords = new ArrayList<>();
        // Lấy user/pass từng host từ session user (nếu có), hoặc parse từ user@host
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
                ServerInfo s = sessionUser.getServers().stream().filter(server -> server.getIp().equals(ipFinal)).findFirst().orElse(null);
                if (s != null) {
                    if (user == null) user = s.getSshUsername();
                    pass = s.getSshPassword();
                }
            }
            ipList.add(ip); // chỉ IP
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

    @GetMapping("/multi-execute-page")
    public String showMultiExecutePage() {
        return "multi-execute-page";
    }
}

