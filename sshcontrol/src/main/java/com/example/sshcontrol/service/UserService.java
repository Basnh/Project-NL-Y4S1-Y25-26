package com.example.sshcontrol.service;

import com.example.sshcontrol.model.User;

public interface UserService {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void save(User user);
}