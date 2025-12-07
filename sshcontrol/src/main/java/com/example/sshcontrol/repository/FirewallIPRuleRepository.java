package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.FirewallIPRule;
import com.example.sshcontrol.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirewallIPRuleRepository extends JpaRepository<FirewallIPRule, Long> {
    List<FirewallIPRule> findByServer(Server server);
    List<FirewallIPRule> findByServerAndAction(Server server, String action);
}
