package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.ServerInfo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;

@Controller
public class ExecuteController {
    
    @GetMapping("/execute-page")
    public String executePage(HttpSession session, Model model) {
        // Kiểm tra session
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            System.out.println("DEBUG: User not found in session, redirecting to login");
            return "redirect:/login";
        }
        
        System.out.println("DEBUG: User found in session: " + user.getUsername());
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers()); // Tất cả máy chủ của user
        model.addAttribute("selectedHosts", ""); // Không có hosts được chọn từ modal
        
        return "execute-page";
    }
    
    @GetMapping("/execute-command")
    public String executeCommand(@RequestParam(required = false) String hosts, 
                                HttpSession session, Model model) {
        // Kiểm tra session
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            System.out.println("DEBUG: User not found in session, redirecting to login");
            return "redirect:/login";
        }
        
        System.out.println("DEBUG: User found in session: " + user.getUsername());
        System.out.println("DEBUG: Selected hosts: " + hosts);
        
        // Tìm thông tin chi tiết của các máy chủ được chọn
        List<ServerInfo> selectedServers = new ArrayList<>();
        if (hosts != null && !hosts.trim().isEmpty()) {
            String[] hostArray = hosts.split(",");
            for (String host : hostArray) {
                host = host.trim();
                for (ServerInfo server : user.getServers()) {
                    if (server.getIp().equals(host)) {
                        selectedServers.add(server);
                        break;
                    }
                }
            }
        }
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers()); // Tất cả máy chủ của user
        model.addAttribute("selectedServers", selectedServers); // Máy chủ được chọn từ modal
        model.addAttribute("selectedHosts", hosts); // Raw hosts string
        
        return "execute-page";
    }
}