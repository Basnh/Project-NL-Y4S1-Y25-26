package com.example.sshcontrol.repository;

import com.example.sshcontrol.model.FirewallRule;
import com.example.sshcontrol.model.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FirewallRuleRepository extends JpaRepository<FirewallRule, Long> {
    List<FirewallRule> findByServer(Server server);
    List<FirewallRule> findByServerAndZone(Server server, String zone);
    List<FirewallRule> findByServerAndAction(Server server, String action);
    void deleteById(@NonNull Long id);
}
