package com.example.sshcontrol.sshcontrol.service;

import java.util.HashMap;
import java.util.Map;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.example.sshcontrol.repository.ServerRepository;
import com.example.sshcontrol.repository.UserRepository;

@Service
public class StatisticsService {
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private UserRepository userRepository;

    public Map<String, Object> getSystemStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Tổng số máy chủ
        long totalServers = serverRepository.count();
        stats.put("totalServers", totalServers);
        
        // Số máy chủ đang hoạt động
        long activeServers = serverRepository.count();
        stats.put("activeServers", activeServers);
        
        // Tính uptime
        double uptime = calculateUptime();
        stats.put("uptime", String.format("%.1f", uptime) + "%");
        
        // Hiệu suất hệ thống
        int performance = calculateSystemPerformance();
        stats.put("performance", performance);
        
        return stats;
    }

    private double calculateUptime() {
        // Logic tính uptime thực tế của hệ thống
        // Có thể dựa trên logs hoặc monitoring data
        return 99.9; // Tạm thời return giá trị mặc định
    }

    private int calculateSystemPerformance() {
        // Logic tính hiệu suất thực tế của hệ thống
        // Dựa trên CPU, Memory usage, etc.
        return 90; // Tạm thời return giá trị mặc định
    }

    
}