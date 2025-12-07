package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.ActivityLog;
import com.example.sshcontrol.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Find all logs by user
    Page<ActivityLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    // Find logs by user and action
    Page<ActivityLog> findByUserAndActionOrderByCreatedAtDesc(User user, String action, Pageable pageable);
    
    // Find logs by user and status
    Page<ActivityLog> findByUserAndStatusOrderByCreatedAtDesc(User user, String status, Pageable pageable);
    
    // Find logs by user and server
    Page<ActivityLog> findByUserAndServerIdOrderByCreatedAtDesc(User user, String serverId, Pageable pageable);
    
    // Custom query for date range
    @Query("SELECT al FROM ActivityLog al WHERE al.user = :user AND al.createdAt BETWEEN :startDate AND :endDate ORDER BY al.createdAt DESC")
    List<ActivityLog> findByUserAndDateRange(@Param("user") User user, 
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
    
    // Get recent activities
    List<ActivityLog> findTop10ByUserOrderByCreatedAtDesc(User user);
    
    // Find all activities for admin (sorted by date descending)
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Count logs by action
    long countByUserAndAction(User user, String action);
    
    // Count failed activities
    long countByUserAndStatus(User user, String status);
    
    // Delete old logs (older than specified date)
    void deleteByCreatedAtBefore(LocalDateTime date);
}
