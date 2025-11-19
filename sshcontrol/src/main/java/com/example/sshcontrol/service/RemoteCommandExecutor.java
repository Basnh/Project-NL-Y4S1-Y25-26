package com.example.sshcontrol.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RemoteCommandExecutor {

    // Thực thi lệnh trên nhiều host với user/password riêng
    public List<String> executeCommandOnMultipleHosts(List<String> hosts, List<String> users, List<String> passwords, String command) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(hosts.size(), 10));
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < hosts.size(); i++) {
            final String host = hosts.get(i);
            final String user = users.get(i);
            final String password = passwords.get(i);
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

    private String executeCommand(String host, String user, String password, String command) {
        // Dummy implementation for demonstration purposes
        // Replace this with actual SSH command execution logic
        return "Executed command on host: " + host;
    }
}
