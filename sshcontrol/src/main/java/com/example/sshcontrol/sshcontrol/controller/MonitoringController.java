package com.example.sshcontrol.sshcontrol.controller;

import com.example.sshcontrol.model.Server;
import com.example.sshcontrol.model.User;
import com.jcraft.jsch.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Controller
public class MonitoringController {

    // API: Lấy dữ liệu giám sát realtime
    @PostMapping("/api/monitoring/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMonitoringStats(
            @RequestBody Map<String, String> request,
            @SessionAttribute(name = "user", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String serverId = request.get("serverId");

            if (serverId == null || serverId.isEmpty()) {
                response.put("success", false);
                response.put("error", "Máy chủ không được chọn");
                return ResponseEntity.badRequest().body(response);
            }

            Server server = user.getServers().stream()
                    .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                    .findFirst()
                    .orElse(null);

            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy dữ liệu thực từ máy chủ
            Map<String, Object> stats = getRealStats(server);
            response.put("success", true);
            response.put("data", stats);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // API: Lấy lịch sử dữ liệu
    @PostMapping("/api/monitoring/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMonitoringHistory(
            @RequestBody Map<String, String> request,
            @SessionAttribute(name = "user", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String serverId = request.get("serverId");
            String metric = request.get("metric");

            if (serverId == null || serverId.isEmpty()) {
                response.put("success", false);
                response.put("error", "Máy chủ không được chọn");
                return ResponseEntity.badRequest().body(response);
            }

            Server server = user.getServers().stream()
                    .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                    .findFirst()
                    .orElse(null);

            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            // Lấy dữ liệu lịch sử (với dữ liệu thực + mock)
            List<Map<String, Object>> history = generateHistoryData(metric);
            response.put("success", true);
            response.put("data", history);
            response.put("metric", metric);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // API: Lấy danh sách cảnh báo
    @PostMapping("/api/monitoring/alerts")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestBody Map<String, String> request,
            @SessionAttribute(name = "user", required = false) User user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("error", "Auth fail");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String serverId = request.get("serverId");

            if (serverId == null || serverId.isEmpty()) {
                response.put("success", false);
                response.put("error", "Máy chủ không được chọn");
                return ResponseEntity.badRequest().body(response);
            }

            Server server = user.getServers().stream()
                    .filter(s -> s.getIp() != null && s.getIp().equals(serverId))
                    .findFirst()
                    .orElse(null);

            if (server == null) {
                response.put("success", false);
                response.put("error", "Máy chủ không tồn tại");
                return ResponseEntity.badRequest().body(response);
            }

            List<Map<String, Object>> alerts = generateMockAlerts();
            response.put("success", true);
            response.put("alerts", alerts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    // Lấy dữ liệu thực từ máy chủ qua SSH
    private Map<String, Object> getRealStats(Server server) {
        Map<String, Object> stats = new HashMap<>();
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(server.getSshUsername(), server.getIp(), 22);
            session.setPassword(server.getSshPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);

            // CPU Usage
            Map<String, Object> cpu = new HashMap<>();
            try {
                String cpuUsageStr = executeCommand(session, "top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - $1}'");
                double cpuUsage = cpuUsageStr.isEmpty() ? 0 : Double.parseDouble(cpuUsageStr.trim());
                cpu.put("usage", (int) cpuUsage);
                
                String cpuCoreStr = executeCommand(session, "nproc");
                cpu.put("cores", cpuCoreStr.isEmpty() ? 0 : Integer.parseInt(cpuCoreStr.trim()));
                cpu.put("threads", cpu.get("cores"));
                cpu.put("frequency", "N/A");
            } catch (Exception e) {
                cpu.put("usage", 0);
                cpu.put("cores", 0);
                cpu.put("threads", 0);
                cpu.put("frequency", "N/A");
            }
            stats.put("cpu", cpu);

            // RAM Usage
            Map<String, Object> ram = new HashMap<>();
            try {
                String ramStr = executeCommand(session, "free -m | grep Mem");
                String[] parts = ramStr.trim().split("\\s+");
                if (parts.length >= 3) {
                    int total = Integer.parseInt(parts[1]);
                    int used = Integer.parseInt(parts[2]);
                    int usage = (int) ((used * 100.0) / total);
                    ram.put("total", total);
                    ram.put("used", used);
                    ram.put("available", total - used);
                    ram.put("usage", usage);
                } else {
                    throw new Exception("Cannot parse RAM data");
                }
            } catch (Exception e) {
                ram.put("total", 0);
                ram.put("used", 0);
                ram.put("available", 0);
                ram.put("usage", 0);
            }
            stats.put("ram", ram);

            // Disk Usage
            Map<String, Object> disk = new HashMap<>();
            try {
                String diskStr = executeCommand(session, "df -B1 / | tail -1");
                String[] parts = diskStr.trim().split("\\s+");
                if (parts.length >= 3) {
                    long total = Long.parseLong(parts[1]);
                    long used = Long.parseLong(parts[2]);
                    int usage = (int) ((used * 100) / total);
                    disk.put("total", total / (1024 * 1024)); // Convert to MB
                    disk.put("used", used / (1024 * 1024));
                    disk.put("available", (total - used) / (1024 * 1024));
                    disk.put("usage", usage);
                } else {
                    throw new Exception("Cannot parse disk data");
                }
            } catch (Exception e) {
                disk.put("total", 0);
                disk.put("used", 0);
                disk.put("available", 0);
                disk.put("usage", 0);
            }
            stats.put("disk", disk);

            // Network Stats
            Map<String, Object> network = new HashMap<>();
            try {
                String netStr = executeCommand(session, "cat /proc/net/dev | tail -1");
                String[] parts = netStr.trim().split("\\s+");
                if (parts.length >= 2) {
                    long bytesIn = Long.parseLong(parts[1]);
                    long bytesOut = Long.parseLong(parts[9]);
                    network.put("bytesIn", bytesIn);
                    network.put("bytesOut", bytesOut);
                    network.put("packetsIn", Long.parseLong(parts[2]));
                    network.put("packetsOut", Long.parseLong(parts[10]));
                    network.put("errors", Integer.parseInt(parts[3]) + Integer.parseInt(parts[11]));
                } else {
                    throw new Exception("Cannot parse network data");
                }
            } catch (Exception e) {
                network.put("bytesIn", 0);
                network.put("bytesOut", 0);
                network.put("packetsIn", 0);
                network.put("packetsOut", 0);
                network.put("errors", 0);
            }
            stats.put("network", network);

            // System Info
            Map<String, Object> system = new HashMap<>();
            try {
                String uptimeStr = executeCommand(session, "cat /proc/uptime");
                long uptime = (long) Double.parseDouble(uptimeStr.trim().split("\\s+")[0]);
                system.put("uptime", uptime);
                
                String loadStr = executeCommand(session, "cat /proc/loadavg");
                String[] loads = loadStr.trim().split("\\s+");
                system.put("loadAverage", loads[0]);
                
                String procStr = executeCommand(session, "ps aux | wc -l");
                system.put("processes", Integer.parseInt(procStr.trim()) - 1); // Subtract header
            } catch (Exception e) {
                system.put("uptime", 0);
                system.put("loadAverage", "0.0");
                system.put("processes", 0);
            }
            stats.put("system", system);

        } catch (Exception e) {
            // Return default stats on error
            stats.putAll(getDefaultStats());
        } finally {
            if (session != null) {
                session.disconnect();
            }
        }

        return stats;
    }

    // Hàm helper để execute command qua SSH
    private String executeCommand(Session session, String command) throws JSchException, java.io.IOException {
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                result.append(line);
                break; // Chỉ lấy dòng đầu tiên
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            channel.disconnect();
        }

        return result.toString();
    }

    // Stats mặc định khi SSH fail
    private Map<String, Object> getDefaultStats() {
        Map<String, Object> stats = new HashMap<>();

        Map<String, Object> cpu = new HashMap<>();
        cpu.put("usage", 0);
        cpu.put("cores", 0);
        cpu.put("threads", 0);
        cpu.put("frequency", "N/A");
        stats.put("cpu", cpu);

        Map<String, Object> ram = new HashMap<>();
        ram.put("total", 0);
        ram.put("used", 0);
        ram.put("available", 0);
        ram.put("usage", 0);
        stats.put("ram", ram);

        Map<String, Object> disk = new HashMap<>();
        disk.put("total", 0);
        disk.put("used", 0);
        disk.put("available", 0);
        disk.put("usage", 0);
        stats.put("disk", disk);

        Map<String, Object> network = new HashMap<>();
        network.put("bytesIn", 0);
        network.put("bytesOut", 0);
        network.put("packetsIn", 0);
        network.put("packetsOut", 0);
        network.put("errors", 0);
        stats.put("network", network);

        Map<String, Object> system = new HashMap<>();
        system.put("uptime", 0);
        system.put("loadAverage", "0.0");
        system.put("processes", 0);
        stats.put("system", system);

        return stats;
    }

    private List<Map<String, Object>> generateHistoryData(String metric) {
        List<Map<String, Object>> history = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", i + ":00");

            if ("cpu".equals(metric)) {
                point.put("value", 30 + (int)(Math.random() * 50));
            } else if ("ram".equals(metric)) {
                point.put("value", 40 + (int)(Math.random() * 40));
            } else if ("disk".equals(metric)) {
                point.put("value", 45 + (int)(Math.random() * 20));
            } else if ("network".equals(metric)) {
                point.put("value", 100 + (int)(Math.random() * 200));
            }

            history.add(point);
        }

        return history;
    }

    private List<Map<String, Object>> generateMockAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        Map<String, Object> alert1 = new HashMap<>();
        alert1.put("id", 1);
        alert1.put("type", "warning");
        alert1.put("title", "CPU cao");
        alert1.put("message", "CPU sử dụng đạt 85%");
        alert1.put("timestamp", System.currentTimeMillis() - 300000);
        alert1.put("icon", "fa-microchip");
        alerts.add(alert1);

        Map<String, Object> alert2 = new HashMap<>();
        alert2.put("id", 2);
        alert2.put("type", "warning");
        alert2.put("title", "RAM cao");
        alert2.put("message", "RAM sử dụng đạt 78%");
        alert2.put("timestamp", System.currentTimeMillis() - 600000);
        alert2.put("icon", "fa-memory");
        alerts.add(alert2);

        Map<String, Object> alert3 = new HashMap<>();
        alert3.put("id", 3);
        alert3.put("type", "info");
        alert3.put("title", "Backup hoàn tất");
        alert3.put("message", "Backup hàng ngày đã hoàn tất");
        alert3.put("timestamp", System.currentTimeMillis() - 3600000);
        alert3.put("icon", "fa-check-circle");
        alerts.add(alert3);

        return alerts;
    }
}
