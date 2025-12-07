package com.example.sshcontrol.util;

import com.example.sshcontrol.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class để ghi log hoạt động của người dùng vào console
 */
public class ConsoleLogger {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleLogger.class);
    
    /**
     * Ghi log đăng nhập
     */
    public static void logLogin(String username, String ipAddress) {
        logger.info("✓ [LOGIN] Người dùng '{}' đã đăng nhập từ IP: {}", username, ipAddress);
    }
    
    /**
     * Ghi log đăng xuất
     */
    public static void logLogout(String username) {
        logger.info("✗ [LOGOUT] Người dùng '{}' đã đăng xuất", username);
    }
    
    /**
     * Ghi log từ user object
     */
    public static void logUserAction(User user, String action, String details) {
        if (user != null) {
            logger.info("⚡ [{}] Người dùng '{}' - {}", action, user.getUsername(), details);
        }
    }
    
    /**
     * Ghi log thêm server
     */
    public static void logAddServer(User user, String serverName, String serverIp) {
        if (user != null) {
            logger.info("➕ [ADD_SERVER] Người dùng '{}' thêm server: {} ({})", 
                user.getUsername(), serverName, serverIp);
        }
    }
    
    /**
     * Ghi log xóa server
     */
    public static void logDeleteServer(User user, String serverName, String serverIp) {
        if (user != null) {
            logger.info("➖ [DELETE_SERVER] Người dùng '{}' xóa server: {} ({})", 
                user.getUsername(), serverName, serverIp);
        }
    }
    
    /**
     * Ghi log kết nối SSH
     */
    public static void logSSHConnection(User user, String serverIp, boolean success) {
        if (user != null) {
            String status = success ? "thành công" : "thất bại";
            logger.info("🔗 [SSH_CONNECT] Người dùng '{}' kết nối tới {}: {}", 
                user.getUsername(), serverIp, status);
        }
    }
    
    /**
     * Ghi log thực thi lệnh
     */
    public static void logExecuteCommand(User user, String serverIp, String command) {
        if (user != null) {
            logger.info("⌨️  [EXECUTE] Người dùng '{}' thực thi lệnh trên {}: {}", 
                user.getUsername(), serverIp, command);
        }
    }
    
    /**
     * Ghi log upload file
     */
    public static void logUploadFile(User user, String serverIp, String fileName) {
        if (user != null) {
            logger.info("📤 [UPLOAD] Người dùng '{}' tải lên file '{}' lên {}", 
                user.getUsername(), fileName, serverIp);
        }
    }
    
    /**
     * Ghi log download file
     */
    public static void logDownloadFile(User user, String serverIp, String fileName) {
        if (user != null) {
            logger.info("📥 [DOWNLOAD] Người dùng '{}' tải xuống file '{}' từ {}", 
                user.getUsername(), fileName, serverIp);
        }
    }
    
    /**
     * Ghi log lỗi
     */
    public static void logError(User user, String action, String errorMessage) {
        if (user != null) {
            logger.error("❌ [ERROR-{}] Người dùng '{}': {}", action, user.getUsername(), errorMessage);
        } else {
            logger.error("❌ [ERROR-{}] {}", action, errorMessage);
        }
    }
    
    /**
     * Ghi log thay đổi quyền admin
     */
    public static void logRoleChange(User admin, String targetUser, String newRole) {
        if (admin != null) {
            logger.info("👑 [ROLE_CHANGE] Admin '{}' thay đổi quyền của '{}' thành: {}", 
                admin.getUsername(), targetUser, newRole);
        }
    }
}
