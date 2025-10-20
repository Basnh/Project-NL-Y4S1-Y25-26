package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.sshcontrol.service.UserService;
import com.example.sshcontrol.sshcontrol.util.ControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;

@Controller
public class ExecuteController {

    @Autowired
    private UserService userService;
    
    @GetMapping("/execute-page")
    public String executePage(HttpSession session, Model model) {
        if (!ControllerHelper.isUserLoggedIn(session)) {
            return "redirect:/login";
        }
        ControllerHelper.updateUserAndModel(session, model, userService);
        model.addAttribute("selectedHosts", ""); // Không có hosts được chọn từ modal
        return "execute-page";
    }
    
    @GetMapping("/execute-command")
    public String executeCommand(@RequestParam(required = false) String hosts, 
                                HttpSession session, Model model) {
        if (!ControllerHelper.isUserLoggedIn(session)) {
            return "redirect:/login";
        }
        ControllerHelper.updateUserAndModel(session, model, userService);
        
        User user = ControllerHelper.getCurrentUser(session);
        System.out.println("DEBUG: Selected hosts: " + hosts);
        
        // Tìm thông tin chi tiết của các máy chủ được chọn
        List<Server> selectedServers = new ArrayList<>();
        if (hosts != null && !hosts.trim().isEmpty()) {
            String[] hostArray = hosts.split(",");
            for (String host : hostArray) {
                host = host.trim();
                for (Server server : user.getServers()) {
                    if (server.getIp().equals(host)) {
                        selectedServers.add(server);
                        break;
                    }
                }
            }
        }
        
        model.addAttribute("selectedServers", selectedServers); // Máy chủ được chọn từ modal
        model.addAttribute("selectedHosts", hosts); // Raw hosts string
        
        return "execute-page";
    }
}