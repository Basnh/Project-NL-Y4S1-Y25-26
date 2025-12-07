package com.example.sshcontrol.service;

import com.example.sshcontrol.model.ActivityLog;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.repository.ActivityLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository activityLogRepository;

    /**
     * Ghi nhận một hoạt động
     */
    public ActivityLog logActivity(User user, String action, String description) {
        return logActivity(user, action, description, null, "SUCCESS");
    }

    /**
     * Ghi nhận một hoạt động với server
     */
    public ActivityLog logActivity(User user, String action, String description, String serverId) {
        return logActivity(user, action, description, serverId, "SUCCESS");
    }

    /**
     * Ghi nhận một hoạt động với trạng thái
     */
    public ActivityLog logActivity(User user, String action, String description, String serverId, String status) {
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setDescription(description);
        log.setServerId(serverId);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.now());
        return activityLogRepository.save(log);
    }

    /**
     * Ghi nhận hoạt động với chi tiết đầy đủ
     */
    public ActivityLog logActivityWithDetails(User user, String action, String description, 
                                             String serverId, String status, String details) {
        ActivityLog log = logActivity(user, action, description, serverId, status);
        log.setDetails(details);
        return activityLogRepository.save(log);
    }

    /**
     * Lấy tất cả hoạt động của người dùng (phân trang)
     */
    public Page<ActivityLog> getUserActivities(User user, Pageable pageable) {
        return activityLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Lấy hoạt động theo loại hành động (phân trang)
     */
    public Page<ActivityLog> getActivitiesByAction(User user, String action, Pageable pageable) {
        return activityLogRepository.findByUserAndActionOrderByCreatedAtDesc(user, action, pageable);
    }

    /**
     * Lấy hoạt động theo trạng thái (phân trang)
     */
    public Page<ActivityLog> getActivitiesByStatus(User user, String status, Pageable pageable) {
        return activityLogRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable);
    }

    /**
     * Lấy hoạt động theo server (phân trang)
     */
    public Page<ActivityLog> getActivitiesByServer(User user, String serverId, Pageable pageable) {
        return activityLogRepository.findByUserAndServerIdOrderByCreatedAtDesc(user, serverId, pageable);
    }

    /**
     * Lấy hoạt động trong khoảng thời gian
     */
    public List<ActivityLog> getActivitiesByDateRange(User user, LocalDateTime startDate, LocalDateTime endDate) {
        return activityLogRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    /**
     * Lấy 10 hoạt động gần nhất
     */
    public List<ActivityLog> getRecentActivities(User user) {
        return activityLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Lấy tất cả hoạt động (cho admin - xem toàn bộ hệ thống)
     */
    public Page<ActivityLog> getAllActivities(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Lấy chi tiết một hoạt động
     */
    public Optional<ActivityLog> getActivityById(Long id) {
        return activityLogRepository.findById(id);
    }

    /**
     * Đếm hoạt động theo loại hành động
     */
    public long countActivitiesByAction(User user, String action) {
        return activityLogRepository.countByUserAndAction(user, action);
    }

    /**
     * Đếm hoạt động thất bại
     */
    public long countFailedActivities(User user) {
        return activityLogRepository.countByUserAndStatus(user, "FAILED");
    }

    /**
     * Xóa hoạt động cũ (hơn 90 ngày)
     */
    public void deleteOldActivities() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        activityLogRepository.deleteByCreatedAtBefore(ninetyDaysAgo);
    }

    /**
     * Lấy thống kê hoạt động
     */
    public ActivityStatistics getStatistics(User user) {
        List<ActivityLog> recentActivities = getRecentActivities(user);
        
        ActivityStatistics stats = new ActivityStatistics();
        stats.setTotalActivities(recentActivities.size());
        stats.setLoginCount(countActivitiesByAction(user, "LOGIN"));
        stats.setServerConnectCount(countActivitiesByAction(user, "SERVER_CONNECT"));
        stats.setFileOperationCount(countActivitiesByAction(user, "FILE_UPLOAD") + 
                                    countActivitiesByAction(user, "FILE_DOWNLOAD") +
                                    countActivitiesByAction(user, "FILE_DELETE"));
        stats.setFailedActivities(countFailedActivities(user));
        
        return stats;
    }

    /**
     * Đối tượng thống kê hoạt động
     */
    public static class ActivityStatistics {
        private long totalActivities;
        private long loginCount;
        private long serverConnectCount;
        private long fileOperationCount;
        private long failedActivities;

        public long getTotalActivities() { return totalActivities; }
        public void setTotalActivities(long totalActivities) { this.totalActivities = totalActivities; }

        public long getLoginCount() { return loginCount; }
        public void setLoginCount(long loginCount) { this.loginCount = loginCount; }

        public long getServerConnectCount() { return serverConnectCount; }
        public void setServerConnectCount(long serverConnectCount) { this.serverConnectCount = serverConnectCount; }

        public long getFileOperationCount() { return fileOperationCount; }
        public void setFileOperationCount(long fileOperationCount) { this.fileOperationCount = fileOperationCount; }

        public long getFailedActivities() { return failedActivities; }
        public void setFailedActivities(long failedActivities) { this.failedActivities = failedActivities; }
    }
}
