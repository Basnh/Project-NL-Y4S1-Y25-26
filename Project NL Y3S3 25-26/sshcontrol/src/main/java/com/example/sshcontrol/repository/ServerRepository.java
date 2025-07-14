package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    
    // Method to find server by IP address
    Optional<Server> findByIp(String ip);
    
    // Method to find servers by user ID (if you have user-based filtering)
    List<Server> findByUserId(Long userId);
    
    // Method to find servers by online status
    List<Server> findByOnline(boolean online);
    
    // Method to find server by name
    Optional<Server> findByName(String name);
    
    // Method to find servers by SSH username
    List<Server> findBySshUsername(String sshUsername);
    
    // Method to check if server exists by IP
    boolean existsByIp(String ip);
    
    // Method to find servers by IP and user ID (if you have user-based filtering)
    Optional<Server> findByIpAndUserId(String ip, Long userId);
}
