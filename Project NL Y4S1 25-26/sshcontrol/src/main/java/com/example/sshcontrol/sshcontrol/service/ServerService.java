package com.example.sshcontrol.sshcontrol.service;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.example.sshcontrol.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServerService {
    
    @Autowired
    private ServerRepository serverRepository;
    
    public List<Server> findByUser(User user) {
        return serverRepository.findByUser(user);
    }
    
    public Server save(Server server) {
        return serverRepository.save(server);
    }
    
    public boolean existsByIpAndUsernameAndUser(String ip, String username, User user) {
        return serverRepository.existsByIpAndSshUsernameAndUser(ip, username, user);
    }
    
    public void delete(Server server) {
        serverRepository.delete(server);
    }
    
    public Server findById(Long id) {
        return serverRepository.findById(id).orElse(null);
    }
}