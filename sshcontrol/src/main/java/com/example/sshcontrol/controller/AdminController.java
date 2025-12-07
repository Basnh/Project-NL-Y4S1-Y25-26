package com.example.sshcontrol.controller;

import com.example.sshcontrol.annotation.RequireAdmin;
import com.example.sshcontrol.model.ActivityLog;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.model.UserRole;
import com.example.sshcontrol.service.ActivityLogService;
import com.example.sshcontrol.sshcontrol.service.UserService;
import com.example.sshcontrol.util.SystemLogger;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    /**
     * Trang dashboard admin - xem tổng quan hệ thống
     */
    @GetMapping
    @RequireAdmin("Chỉ admin mới có thể truy cập")
    public String dashboard(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        List<User> allUsers = userService.findAll();
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("admins", allUsers.stream().filter(User::isAdmin).count());
        model.addAttribute("activeUsers", allUsers.stream().filter(User::isActive).count());
        
        return "admin/dashboard";
    }
    
    /**
     * Xem danh sách tất cả users
     */
    @GetMapping("/users")
    @RequireAdmin("Chỉ admin mới có thể xem danh sách người dùng")
    public String listUsers(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        
        return "admin/users";
    }
    
    /**
     * Xem chi tiết user
     */
    @GetMapping("/users/{id}")
    @RequireAdmin("Chỉ admin mới có thể xem chi tiết người dùng")
    public String viewUser(@PathVariable Long id, 
                          @RequestParam(defaultValue = "0") int page,
                          HttpSession session, Model model) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        User user = userService.findById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }
        
        // Lấy lịch sử hoạt động của user (10 items per page)
        Pageable pageable = PageRequest.of(page, 10);
        Page<ActivityLog> activities = activityLogService.getUserActivities(user, pageable);
        
        model.addAttribute("user", user);
        model.addAttribute("servers", user.getServers());
        model.addAttribute("activities", activities);
        model.addAttribute("currentPage", page);
        
        return "admin/user-detail";
    }
    
    /**
     * Cấp quyền admin cho user
     */
    @PostMapping("/users/{id}/make-admin")
    @RequireAdmin("Chỉ admin mới có thể cấp quyền")
    public String makeAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        User user = userService.findById(id);
        if (user != null) {
            user.setRole(UserRole.ADMIN);
            userService.save(user);
            
            // Log admin privilege change
            SystemLogger.logAdminPrivilegeChange(admin.getUsername(), user.getUsername(), true);
            
            redirectAttributes.addFlashAttribute("success", "Đã cấp quyền admin cho: " + user.getUsername());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Hạ quyền user từ admin
     */
    @PostMapping("/users/{id}/remove-admin")
    @RequireAdmin("Chỉ admin mới có thể thay đổi quyền")
    public String removeAdmin(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        User user = userService.findById(id);
        if (user != null) {
            user.setRole(UserRole.USER);
            userService.save(user);
            
            // Log admin privilege removal
            SystemLogger.logAdminPrivilegeChange(admin.getUsername(), user.getUsername(), false);
            
            redirectAttributes.addFlashAttribute("success", "Đã hạ quyền admin cho: " + user.getUsername());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Deactivate user
     */
    @PostMapping("/users/{id}/deactivate")
    @RequireAdmin("Chỉ admin mới có thể vô hiệu hóa tài khoản")
    public String deactivateUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        User user = userService.findById(id);
        if (user != null) {
            user.setActive(false);
            userService.save(user);
            
            // Log account deactivation
            SystemLogger.logAccountStatusChange(admin.getUsername(), user.getUsername(), false);
            
            redirectAttributes.addFlashAttribute("success", "Đã vô hiệu hóa tài khoản: " + user.getUsername());
        }
        
        return "redirect:/admin/users/" + id;
    }
    
    /**
     * Activate user
     */
    @PostMapping("/users/{id}/activate")
    @RequireAdmin("Chỉ admin mới có thể kích hoạt tài khoản")
    public String activateUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }
        
        User user = userService.findById(id);
        if (user != null) {
            user.setActive(true);
            userService.save(user);
            
            // Log account activation
            SystemLogger.logAccountStatusChange(admin.getUsername(), user.getUsername(), true);
            
            redirectAttributes.addFlashAttribute("success", "Đã kích hoạt tài khoản: " + user.getUsername());
        }
        
        return "redirect:/admin/users/" + id;
    }
}
