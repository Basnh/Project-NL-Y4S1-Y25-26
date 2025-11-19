package com.example.sshcontrol.sshcontrol.util;

import com.example.sshcontrol.model.User;
import com.example.sshcontrol.sshcontrol.service.UserService;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

/**
 * Helper class để quản lý session và model attributes cho các controller
 */
public class ControllerHelper {
    
    /**
     * Cập nhật user data và thêm vào cả session và model
     */
    public static void updateUserAndModel(HttpSession session, Model model, UserService userService) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            // Refresh user data
            user = userService.findByUsername(user.getUsername());
            
            // Update session
            session.setAttribute("user", user);
            session.setAttribute("servers", user.getServers());
            
            // Add to model
            model.addAttribute("user", user);
            model.addAttribute("userServers", user.getServers());
        }
    }
    
    /**
     * Kiểm tra user đã login chưa
     */
    public static boolean isUserLoggedIn(HttpSession session) {
        return session.getAttribute("user") != null;
    }
    
    /**
     * Lấy current user từ session
     */
    public static User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }
}