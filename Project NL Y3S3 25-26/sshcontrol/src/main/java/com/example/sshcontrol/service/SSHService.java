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

    public String executeCommand(String host, String user, String password, String command) {
        StringBuilder output = new StringBuilder();
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
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
            output.append(e.getMessage());
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
            channel.setCommand(command);
            // Truyền mật khẩu sudo + nội dung file
            String fullInput = password + "\n" + input;
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
            output.append(e.getMessage());
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