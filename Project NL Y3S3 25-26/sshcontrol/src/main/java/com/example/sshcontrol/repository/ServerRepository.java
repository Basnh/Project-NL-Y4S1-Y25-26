package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerRepository extends JpaRepository<Server, Long> {
    // Additional query methods if needed
}
