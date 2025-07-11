package com.example.sshcontrol.sshcontrol.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Controller
public class VMController {

    // Thay đổi đường dẫn nếu cần
    private static final String VBOX_PATH = "C:\\Program Files\\Oracle\\VirtualBox\\VBoxManage.exe";

    @GetMapping("/vm-control")
    public String showVMControlPage() {
        return "vm-control";
    }

    @PostMapping("/vm-control")
    public String controlVM(@RequestParam String vmName,
                            @RequestParam String action,
                            Model model) {
        try {
            List<String> command;

            switch (action) {
                case "start":
                    command = Arrays.asList(VBOX_PATH, "startvm", vmName, "--type", "headless");
                    break;
                case "shutdown":
                    command = Arrays.asList(VBOX_PATH, "controlvm", vmName, "acpipowerbutton");
                    break;
                case "poweroff":
                    command = Arrays.asList(VBOX_PATH, "controlvm", vmName, "poweroff");
                    break;
                default:
                    model.addAttribute("message", "Hành động không hợp lệ.");
                    return "vm-control";
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.home")));
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                model.addAttribute("message", "✅ Thực hiện lệnh thành công: " + action + " " + vmName);
            } else {
                model.addAttribute("message", "⚠️ Lỗi khi thực thi VBoxManage.");
            }

        } catch (Exception e) {
            model.addAttribute("message", "❌ Lỗi: " + e.getMessage());
        }

        return "vm-control";
    }
}
