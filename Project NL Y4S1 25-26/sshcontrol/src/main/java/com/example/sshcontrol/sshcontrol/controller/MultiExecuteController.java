package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class MultiExecuteController {
    
    @GetMapping("/multi-execute-page")
    public String multiExecutePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            System.out.println("DEBUG: User not found in session, redirecting to login");
            return "redirect:/login";
        }
        
        System.out.println("DEBUG: User found in session: " + user.getUsername());
        System.out.println("DEBUG: User servers: " + user.getServers().size());
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers());
        
        return "multi-execute-page";
    }
    
    @GetMapping("/multi-execute-command")
    public String multiExecuteCommand(@RequestParam(required = false) String hosts, 
                                     HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            System.out.println("DEBUG: User not found in session, redirecting to login");
            return "redirect:/login";
        }
        
        System.out.println("DEBUG: User found in session: " + user.getUsername());
        System.out.println("DEBUG: Selected hosts: " + hosts);
        
        model.addAttribute("user", user);
        model.addAttribute("userServers", user.getServers());
        model.addAttribute("selectedHosts", hosts);
        
        return "multi-execute-page";
    }
}
