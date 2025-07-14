package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.User;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserRepository {
    private List<User> users = new ArrayList<>();
    
    public User findByUsername(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }
    
    public User save(User user) {
        // Remove existing user with same username
        users.removeIf(u -> u.getUsername().equals(user.getUsername()));
        users.add(user);
        return user;
    }
    
    public List<User> findAll() {
        return users;
    }
}