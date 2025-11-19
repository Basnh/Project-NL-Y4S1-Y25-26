package com.example.sshcontrol.service;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.jcraft.jsch.*;

@Service
public class SystemStatsService {

    @Autowired
    private ServerRepository serverRepository;

    public Map<String, Object> getSystemStats(List<Server> servers) {
        Map<String, Object> stats = new HashMap<>();
        int totalServers = servers.size();
        int activeServers = 0;
        double totalCpuUsage = 0;
        double totalRamUsage = 0;
        String minUptime = "N/A";
        String maxUptime = "N/A";
        String avgUptime = "N/A";

        for (Server server : servers) {
            if (server.isOnline()) {
                activeServers++;
                
                // Get CPU and RAM usage for each server
                Map<String, Object> serverStats = getServerStats(server);
                totalCpuUsage += (double) serverStats.getOrDefault("cpuUsage", 0.0);
                totalRamUsage += (double) serverStats.getOrDefault("ramUsage", 0.0);
            }
        }

        // Calculate percentages and averages
        double avgCpuUsage = totalServers > 0 ? totalCpuUsage / totalServers : 0;
        double avgRamUsage = totalServers > 0 ? totalRamUsage / totalServers : 0;
        double serverUpPercentage = totalServers > 0 ? (activeServers * 100.0 / totalServers) : 0;

        // Format stats
        stats.put("totalServers", totalServers);
        stats.put("activeServers", activeServers);
        stats.put("serverUpPercentage", Math.round(serverUpPercentage));
        stats.put("avgCpuUsage", Math.round(avgCpuUsage));
        stats.put("avgRamUsage", Math.round(avgRamUsage));
        stats.put("minUptime", minUptime);
        stats.put("maxUptime", maxUptime);
        stats.put("avgUptime", avgUptime);

        return stats;
    }

    private Map<String, Object> getServerStats(Server server) {
        Map<String, Object> stats = new HashMap<>();
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(server.getSshUsername(), server.getIp(), 22);
            session.setPassword(server.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(3000);

            // Get CPU usage
            String cpuCmd = "top -bn1 | grep 'Cpu(s)' | awk '{print $2}'";
            Channel cpuChannel = session.openChannel("exec");
            ((ChannelExec) cpuChannel).setCommand(cpuCmd);
            BufferedReader cpuReader = new BufferedReader(new InputStreamReader(cpuChannel.getInputStream()));
            cpuChannel.connect();
            String cpuUsage = cpuReader.readLine();
            stats.put("cpuUsage", Double.parseDouble(cpuUsage != null ? cpuUsage : "0"));
            cpuChannel.disconnect();

            // Get RAM usage
            String ramCmd = "free | grep Mem | awk '{print $3/$2 * 100.0}'";
            Channel ramChannel = session.openChannel("exec");
            ((ChannelExec) ramChannel).setCommand(ramCmd);
            BufferedReader ramReader = new BufferedReader(new InputStreamReader(ramChannel.getInputStream()));
            ramChannel.connect();
            String ramUsage = ramReader.readLine();
            stats.put("ramUsage", Double.parseDouble(ramUsage != null ? ramUsage : "0"));
            ramChannel.disconnect();

            // Get uptime
            String uptimeCmd = "uptime -p";
            Channel uptimeChannel = session.openChannel("exec");
            ((ChannelExec) uptimeChannel).setCommand(uptimeCmd);
            BufferedReader uptimeReader = new BufferedReader(new InputStreamReader(uptimeChannel.getInputStream()));
            uptimeChannel.connect();
            String uptime = uptimeReader.readLine();
            stats.put("uptime", uptime != null ? uptime : "N/A");
            uptimeChannel.disconnect();

        } catch (Exception e) {
            stats.put("cpuUsage", 0.0);
            stats.put("ramUsage", 0.0);
            stats.put("uptime", "N/A");
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }

        return stats;
    }
}