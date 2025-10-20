package com.example.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.service.SystemStatsService;
import com.example.sshcontrol.service.UserService;
import com.example.sshcontrol.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api")
public class StatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private SystemStatsService systemStatsService;

    @GetMapping("/system-stats")
    public ResponseEntity<Map<String, Object>> getSystemStats(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) {
            return ResponseEntity.ok(Map.of("error", "User not logged in"));
        }

        User currentUser = userService.findByUsername(sessionUser.getUsername());
        if (currentUser == null) {
            return ResponseEntity.ok(Map.of("error", "User not found"));
        }

        List<Server> servers = serverRepository.findByUser(currentUser);
        Map<String, Object> stats = systemStatsService.getSystemStats(servers);

        return ResponseEntity.ok(stats);
    }
}