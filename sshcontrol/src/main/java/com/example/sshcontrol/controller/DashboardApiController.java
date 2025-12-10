package com.example.sshcontrol.controller;

import com.example.sshcontrol.model.ActivityLog;
import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.service.ActivityLogService;
import com.example.sshcontrol.service.UserService;
import com.example.sshcontrol.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DashboardApiController {

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private ServerRepository serverRepository;

    /**
     * Get recent activities for dashboard
     * Returns up to 10 recent activities
     */
    @GetMapping("/activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        User currentUser = userService.findByUsername(user.getUsername());
        if (currentUser == null) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }

        try {
            // Get recent activities
            List<ActivityLog> activities = activityLogService.getRecentActivities(currentUser);
            if (activities == null || activities.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>()); // Return empty list if no activities
            }

            List<Map<String, Object>> result = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

            for (ActivityLog activity : activities) {
                if (activity == null) continue;
                
                Map<String, Object> activityMap = new HashMap<>();
                activityMap.put("type", getActivityType(activity.getAction()));
                activityMap.put("message", formatActivityMessage(activity));
                
                if (activity.getCreatedAt() != null) {
                    activityMap.put("timestamp", activity.getCreatedAt().format(formatter));
                } else {
                    activityMap.put("timestamp", "N/A");
                }
                
                activityMap.put("status", activity.getStatus() != null ? activity.getStatus() : "UNKNOWN");
                result.add(activityMap);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list on error
        }
    }

    /**
     * Get recent servers accessed by user
     * Returns up to 5 recent servers
     */
    @GetMapping("/recent-servers")
    public ResponseEntity<List<Map<String, Object>>> getRecentServers(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        User currentUser = userService.findByUsername(user.getUsername());
        if (currentUser == null) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }

        try {
            List<Server> servers = serverRepository.findByUser(currentUser);
            if (servers == null || servers.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>()); // Return empty list if no servers
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // Limit to 5 servers
            int limit = Math.min(servers.size(), 5);
            for (int i = 0; i < limit; i++) {
                Server server = servers.get(i);
                if (server == null) continue;

                Map<String, Object> serverMap = new HashMap<>();
                serverMap.put("name", server.getName() != null ? server.getName() : "Unknown Server");
                serverMap.put("ip", server.getIp() != null ? server.getIp() : "N/A");
                serverMap.put("status", server.isOnline() ? "online" : "offline");
                result.add(serverMap);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list on error
        }
    }

    /**
     * Get system alerts
     * Returns critical alerts that need user attention
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<Map<String, Object>>> getAlerts(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(new ArrayList<>());
        }

        User currentUser = userService.findByUsername(user.getUsername());
        if (currentUser == null) {
            return ResponseEntity.status(404).body(new ArrayList<>());
        }

        try {
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // Check for failed activities in the last hour - treated as alerts
            Pageable pageable = PageRequest.of(0, 5);
            List<ActivityLog> failedActivities = new ArrayList<>();
            try {
                failedActivities = activityLogService.getActivitiesByStatus(currentUser, "FAILED", pageable).getContent();
            } catch (Exception ex) {
                // If error getting failed activities, continue
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM");
            
            // Get all servers for this user to cache server names
            List<Server> userServers = serverRepository.findByUser(currentUser);
            Map<String, String> serverIdToName = new HashMap<>();
            if (userServers != null && !userServers.isEmpty()) {
                serverIdToName = userServers.stream()
                        .collect(Collectors.toMap(s -> s.getId().toString(), Server::getName, (a, b) -> a));
            }

            // Add failed activity alerts
            for (ActivityLog activity : failedActivities) {
                if (activity == null) continue;
                
                Map<String, Object> alert = new HashMap<>();
                alert.put("severity", "warning");
                String serverId = activity.getServerId();
                String serverName = (serverId != null && !serverId.isEmpty()) 
                    ? serverIdToName.getOrDefault(serverId, "unknown") 
                    : "unknown";
                alert.put("message", "Lệnh thực thi thất bại: " + (activity.getAction() != null ? activity.getAction() : "unknown") + " trên " + serverName);
                
                if (activity.getCreatedAt() != null) {
                    alert.put("timestamp", activity.getCreatedAt().format(formatter));
                } else {
                    alert.put("timestamp", "N/A");
                }
                alerts.add(alert);
            }

            // Check for offline servers - treated as critical alerts
            if (userServers != null && !userServers.isEmpty()) {
                List<Server> offlineServers = userServers.stream()
                        .filter(s -> !s.isOnline())
                        .limit(3)
                        .collect(Collectors.toList());

                for (Server server : offlineServers) {
                    if (server == null) continue;
                    
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("severity", "critical");
                    alert.put("message", "Server " + (server.getName() != null ? server.getName() : "unknown") 
                        + " (" + (server.getIp() != null ? server.getIp() : "N/A") + ") hiện đang offline");
                    alert.put("timestamp", DateTimeFormatter.ofPattern("HH:mm dd/MM").format(LocalDateTime.now()));
                    alerts.add(alert);
                }
            }

            return ResponseEntity.ok(alerts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>()); // Return empty list on error
        }
    }

    /**
     * Helper method to categorize activity type
     */
    private String getActivityType(String action) {
        if (action == null) return "info";
        
        String lowerAction = action.toLowerCase();
        if (lowerAction.contains("connect") || lowerAction.contains("ssh") || lowerAction.contains("link")) {
            return "connect";
        } else if (lowerAction.contains("disconnect") || lowerAction.contains("unlink")) {
            return "disconnect";
        } else if (lowerAction.contains("execute") || lowerAction.contains("run") || lowerAction.contains("command")) {
            return "execute";
        } else if (lowerAction.contains("edit") || lowerAction.contains("modify") || lowerAction.contains("file")) {
            return "edit";
        }
        return "info";
    }

    /**
     * Helper method to format activity message
     */
    private String formatActivityMessage(ActivityLog activity) {
        String action = activity.getAction() != null ? activity.getAction() : "Thao tác";
        String description = activity.getDescription() != null ? activity.getDescription() : "";
        
        // If description is available, use it directly
        if (!description.isEmpty()) {
            return description;
        }
        
        // Otherwise, format based on action type
        String type = getActivityType(activity.getAction());
        
        switch (type) {
            case "connect":
                return "Kết nối máy chủ";
            case "disconnect":
                return "Ngắt kết nối máy chủ";
            case "execute":
                return "Thực thi lệnh";
            case "edit":
                return "Sửa đổi file/cấu hình";
            default:
                // Convert action name to readable format
                return formatActionName(action);
        }
    }
    
    /**
     * Helper method to convert action name to readable format
     * e.g., "server_connect" -> "Kết nối máy chủ"
     */
    private String formatActionName(String action) {
        if (action == null) return "Thao tác không xác định";
        
        String lower = action.toLowerCase();
        
        // Map common actions
        switch (lower) {
            case "login":
                return "Đăng nhập";
            case "logout":
                return "Đăng xuất";
            case "page_view":
                return "Xem trang";
            case "server_connect":
                return "Kết nối máy chủ";
            case "server_disconnect":
                return "Ngắt kết nối máy chủ";
            case "file_upload":
                return "Tải lên file";
            case "file_download":
                return "Tải xuống file";
            case "file_delete":
                return "Xóa file";
            case "command_execute":
                return "Thực thi lệnh";
            case "server_add":
                return "Thêm máy chủ mới";
            case "server_delete":
                return "Xóa máy chủ";
            case "server_edit":
                return "Chỉnh sửa máy chủ";
            case "service_start":
                return "Khởi động dịch vụ";
            case "service_stop":
                return "Dừng dịch vụ";
            case "service_restart":
                return "Khởi động lại dịch vụ";
            default:
                // Replace underscores with spaces and capitalize
                return action.replace("_", " ");
        }
    }
}
