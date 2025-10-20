package com.example.sshcontrol.service;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class SSHService {

    public boolean testConnection(String host, String username, String password) {
        JSch jsch = new JSch();
        Session session = null;
        
        try {
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            
            // Thiết lập timeout 10 giây
            session.connect(10000);
            
            return session.isConnected();
        } catch (Exception e) {
            // Log lỗi hoặc xử lý tùy theo yêu cầu
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public String executeCommand(String host, String user, String password, String command) {
        StringBuilder output = new StringBuilder();
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            // Nếu là lệnh sudo thì wrap lại thành echo 'password' | sudo -S ...
            String realCommand = command;
            if (command.trim().startsWith("sudo") || command.contains("sudo ")) {
                // Escape single quote in password if needed
                String safePassword = password.replace("'", "'\\''");
                realCommand = "echo '" + safePassword + "' | " + command.replaceFirst("sudo", "sudo -S");
            }
            channel.setCommand(realCommand);
            channel.setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) break;
                Thread.sleep(100);
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            output.append("[Lỗi kết nối/SSH] ").append(e.getMessage());
        }
        return output.toString();
    }

    public String executeCommandWithInput(String host, String user, String password, String command, String input) {
        StringBuilder output = new StringBuilder();
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            
            // Nếu là lệnh sudo thì cần sử dụng -S để đọc password từ stdin
            String realCommand = command;
            String fullInput = input;
            
            if (command.trim().startsWith("sudo") || command.contains("sudo ")) {
                realCommand = command.replaceFirst("sudo", "sudo -S");
                fullInput = password + "\n" + input;
            }
            
            channel.setCommand(realCommand);
            channel.setInputStream(new ByteArrayInputStream(fullInput.getBytes(StandardCharsets.UTF_8)));
            channel.setErrStream(System.err);

            InputStream in = channel.getInputStream();
            channel.connect();

            byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) break;
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) break;
                Thread.sleep(100);
            }
            channel.disconnect();
            session.disconnect();
        } catch (Exception e) {
            output.append("[Lỗi SSH] ").append(e.getMessage());
        }
        return output.toString();
    }

    public List<String> executeCommandOnMultipleHosts(List<String> hosts, String user, String password, String command) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), 10));
        List<Future<String>> futures = new ArrayList<>();
        for (String host : hosts) {
            futures.add(executor.submit(() -> executeCommand(host, user, password, command)));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("Error: " + e.getMessage());
            }
        }
        executor.shutdown();
        return results;
    }

    // Thực thi lệnh trên nhiều hosts, mỗi host có user và password riêng
    public List<String> executeCommandOnMultipleHosts(List<String> hosts, String command, List<String> users, List<String> passwords) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), 10));
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            final String hostFinal = hosts.get(i);
            final String userFinal = users.get(i);
            final String passwordFinal = passwords.get(i);
            futures.add(executor.submit(() -> executeCommand(hostFinal, userFinal, passwordFinal, command)));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                results.add("Error: " + e.getMessage());
            }
        }
        executor.shutdown();
        return results;
    }

    public List<String> controlServiceOnMultipleHosts(List<String> hosts, String user, String password, String serviceName, String action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), 10));
        List<Future<String>> futures = new ArrayList<>();
        for (String host : hosts) {
            futures.add(executor.submit(() -> {
                String cmd;
                switch (action) {
                    case "start": cmd = "sudo systemctl start " + serviceName; break;
                    case "stop": cmd = "sudo systemctl stop " + serviceName; break;
                    case "restart": cmd = "sudo systemctl restart " + serviceName; break;
                    default: cmd = "systemctl status " + serviceName;
                }
                return executeCommand(host, user, password, cmd);
            }));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try { results.add(future.get()); } catch (Exception e) { results.add("Error: " + e.getMessage()); }
        }
        executor.shutdown();
        return results;
    }

    public List<String> saveConfigOnMultipleHosts(List<String> hosts, String user, String password, String configPath, String content) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), 10));
        List<Future<String>> futures = new ArrayList<>();
        for (String host : hosts) {
            futures.add(executor.submit(() -> {
                String cmd = "sudo -S tee " + configPath;
                String fullInput = password + "\n" + content;
                return executeCommandWithInput(host, user, password, cmd, fullInput);
            }));
        }
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try { results.add(future.get()); } catch (Exception e) { results.add("Error: " + e.getMessage()); }
        }
        executor.shutdown();
        return results;
    }
}