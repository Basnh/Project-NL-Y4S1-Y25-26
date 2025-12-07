package com.example.sshcontrol.controller;

import com.example.sshcontrol.model.ActivityLog;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.service.ActivityLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ActivityLogController {

    @Autowired
    private ActivityLogService activityLogService;

    /**
     * Hiển thị trang lịch sử hoạt động cho user (route: /activity-log)
     */
    @GetMapping("/activity-log")
    public String getActivityLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            HttpSession session,
            Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, 20);
        Page<ActivityLog> activities;

        if (action != null && !action.isEmpty()) {
            activities = activityLogService.getActivitiesByAction(user, action, pageable);
            model.addAttribute("filterAction", action);
        } else if (status != null && !status.isEmpty()) {
            activities = activityLogService.getActivitiesByStatus(user, status, pageable);
            model.addAttribute("filterStatus", status);
        } else {
            activities = activityLogService.getUserActivities(user, pageable);
        }

        model.addAttribute("activities", activities);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", activities.getTotalPages());
        
        // Thêm thống kê
        ActivityLogService.ActivityStatistics stats = activityLogService.getStatistics(user);
        model.addAttribute("stats", stats);

        return "log";
    }

    /**
     * Hiển thị trang lịch sử hoạt động cho admin (xem tất cả users) - route: /admin/activity-logs
     */
    @GetMapping("/admin/activity-logs")
    public String getAdminActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/";
        }

        Pageable pageable = PageRequest.of(page, 20);
        Page<ActivityLog> activities = activityLogService.getAllActivities(pageable);

        model.addAttribute("activities", activities);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", activities.getTotalPages());
        model.addAttribute("isAdmin", true);

        return "admin/activity-logs";
    }

    /**
     * Lịch sử hoạt động endpoint (legacy)
     */
    @GetMapping("/log")
    public String getActivitiesByDateRange(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Pageable pageable = PageRequest.of(page, 20);
        Page<ActivityLog> activities = activityLogService.getUserActivities(user, pageable);
        
        model.addAttribute("activities", activities);
        model.addAttribute("stats", activityLogService.getStatistics(user));
        model.addAttribute("recentActivities", activityLogService.getRecentActivities(user));
        
        return "log";
    }

    /**
     * API lấy hoạt động theo server
     */
    @PostMapping("/api/activities/server/{serverId}")
    @ResponseBody
    public Page<ActivityLog> getActivitiesByServer(
            @PathVariable String serverId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return null;
        }

        Pageable pageable = PageRequest.of(page, 20);
        return activityLogService.getActivitiesByServer(user, serverId, pageable);
    }

    /**
     * API lấy thống kê
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ActivityLogService.ActivityStatistics getStatistics(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return null;
        }
        return activityLogService.getStatistics(user);
    }

    /**
     * API lấy hoạt động gần nhất
     */
    @GetMapping("/api/recent")
    @ResponseBody
    public List<ActivityLog> getRecentActivities(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return null;
        }
        return activityLogService.getRecentActivities(user);
    }
}
