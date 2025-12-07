package com.example.sshcontrol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class để ghi log hoạt động của người dùng trên hệ thống
 * Hiển thị log đẹp và dễ đọc trên terminal
 */
public class SystemLogger {
    
    private static final Logger logger = LoggerFactory.getLogger("SYSTEM_ACTIVITY");
    
    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String BLUE = "\u001B[34m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String CYAN = "\u001B[36m";
    private static final String MAGENTA = "\u001B[35m";
    
    /**
     * Log đăng nhập người dùng
     */
    public static void logUserLogin(String username) {
        String message = String.format(
            "%s[✓ LOGIN]%s Người dùng '%s' đã đăng nhập vào hệ thống",
            GREEN, RESET, username
        );
        logger.info(message);
    }
    
    /**
     * Log đăng xuất người dùng
     */
    public static void logUserLogout(String username) {
        String message = String.format(
            "%s[✓ LOGOUT]%s Người dùng '%s' đã đăng xuất khỏi hệ thống",
            CYAN, RESET, username
        );
        logger.info(message);
    }
    
    /**
     * Log lỗi đăng nhập
     */
    public static void logLoginFailed(String username, String reason) {
        String message = String.format(
            "%s[✗ LOGIN FAILED]%s Người dùng '%s' - Lý do: %s",
            RED, RESET, username, reason
        );
        logger.info(message);
    }
    
    /**
     * Log đăng ký tài khoản mới
     */
    public static void logUserRegistration(String username, String email) {
        String message = String.format(
            "%s[+ REGISTER]%s Tài khoản mới được tạo - Username: '%s', Email: '%s'",
            BLUE, RESET, username, email
        );
        logger.info(message);
    }
    
    /**
     * Log thêm server
     */
    public static void logAddServer(String username, String serverName, String ip) {
        String message = String.format(
            "%s[+ SERVER]%s Người dùng '%s' thêm server mới - Name: '%s', IP: '%s'",
            BLUE, RESET, username, serverName, ip
        );
        logger.info(message);
    }
    
    /**
     * Log xóa server
     */
    public static void logDeleteServer(String username, String serverName, String ip) {
        String message = String.format(
            "%s[- SERVER]%s Người dùng '%s' xóa server - Name: '%s', IP: '%s'",
            YELLOW, RESET, username, serverName, ip
        );
        logger.info(message);
    }
    
    /**
     * Log kết nối đến server
     */
    public static void logServerConnect(String username, String serverIp) {
        String message = String.format(
            "%s[→ CONNECT]%s Người dùng '%s' kết nối đến server %s",
            MAGENTA, RESET, username, serverIp
        );
        logger.info(message);
    }
    
    /**
     * Log thực thi lệnh
     */
    public static void logCommandExecution(String username, String serverIp, String command) {
        String message = String.format(
            "%s[⚡ EXEC]%s Người dùng '%s' thực thi lệnh trên %s: '%s'",
            YELLOW, RESET, username, serverIp, command
        );
        logger.info(message);
    }
    
    /**
     * Log upload file
     */
    public static void logFileUpload(String username, String serverIp, String fileName) {
        String message = String.format(
            "%s[↑ UPLOAD]%s Người dùng '%s' tải file lên %s: '%s'",
            BLUE, RESET, username, serverIp, fileName
        );
        logger.info(message);
    }
    
    /**
     * Log download file
     */
    public static void logFileDownload(String username, String serverIp, String fileName) {
        String message = String.format(
            "%s[↓ DOWNLOAD]%s Người dùng '%s' tải file từ %s: '%s'",
            BLUE, RESET, username, serverIp, fileName
        );
        logger.info(message);
    }
    
    /**
     * Log xóa file
     */
    public static void logFileDelete(String username, String serverIp, String filePath) {
        String message = String.format(
            "%s[✗ DELETE]%s Người dùng '%s' xóa file trên %s: '%s'",
            RED, RESET, username, serverIp, filePath
        );
        logger.info(message);
    }
    
    /**
     * Log lỗi hệ thống
     */
    public static void logSystemError(String username, String action, String error) {
        String message = String.format(
            "%s[✗ ERROR]%s Người dùng '%s' - Hành động: %s - Lỗi: %s",
            RED, RESET, username, action, error
        );
        logger.info(message);
    }
    
    /**
     * Log hoạt động tổng quát
     */
    public static void logActivity(String username, String action, String details) {
        String message = String.format(
            "%s[→ ACTIVITY]%s Người dùng '%s' - %s: %s",
            CYAN, RESET, username, action, details
        );
        logger.info(message);
    }
    
    /**
     * Log cấp/hạ quyền admin
     */
    public static void logAdminPrivilegeChange(String adminUsername, String targetUsername, boolean isGranting) {
        String action = isGranting ? "cấp quyền" : "hạ quyền";
        String icon = isGranting ? "+" : "-";
        String message = String.format(
            "%s[%s ADMIN]%s Admin '%s' %s cho người dùng '%s'",
            MAGENTA, icon, RESET, adminUsername, action, targetUsername
        );
        logger.info(message);
    }
    
    /**
     * Log kích hoạt/vô hiệu hóa tài khoản
     */
    public static void logAccountStatusChange(String adminUsername, String targetUsername, boolean isActivating) {
        String action = isActivating ? "kích hoạt" : "vô hiệu hóa";
        String icon = isActivating ? "+" : "-";
        String message = String.format(
            "%s[%s ACCOUNT]%s Admin '%s' %s tài khoản '%s'",
            MAGENTA, icon, RESET, adminUsername, action, targetUsername
        );
        logger.info(message);
    }
}
