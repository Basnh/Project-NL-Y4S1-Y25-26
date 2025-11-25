package com.example.sshcontrol.service;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class SshFileService {

    private final String username = "ubuntu";
    private final String host = "10.13.137.234"; // Hoặc lấy từ DB cấu hình server
    private final int port = 22;
    private final String password = "your_password"; // Có thể dùng key nếu cần

    private ChannelSftp setupSftpChannel() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);

        // Bypass xác thực host key
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    // Overloaded method với tham số server động
    private ChannelSftp setupSftpChannel(String serverIp, String serverUsername, String serverPassword) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(serverUsername, serverIp, port);
        session.setPassword(serverPassword);

        // Bypass xác thực host key
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        Channel channel = session.openChannel("sftp");
        channel.connect();
        return (ChannelSftp) channel;
    }

    public List<String> listDirectory(String path) throws Exception {
        ChannelSftp sftp = setupSftpChannel();
        List<String> files = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<ChannelSftp.LsEntry> entries = sftp.ls(path);
            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    files.add(entry.getFilename());
                }
            }
        } finally {
            disconnect(sftp);
        }
        return files;
    }

    // Overloaded method với tham số server động
    public List<String> listDirectory(String path, String serverIp, String serverUsername, String serverPassword) throws Exception {
        ChannelSftp sftp = setupSftpChannel(serverIp, serverUsername, serverPassword);
        List<String> files = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<ChannelSftp.LsEntry> entries = sftp.ls(path);
            for (ChannelSftp.LsEntry entry : entries) {
                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
                    files.add(entry.getFilename());
                }
            }
        } finally {
            disconnect(sftp);
        }
        return files;
    }

    public void deleteFile(String path, String name) throws Exception {
        ChannelSftp sftp = setupSftpChannel();
        try {
            String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
            sftp.rm(fullPath);
        } finally {
            disconnect(sftp);
        }
    }

    // Overloaded method với tham số server động
    public void deleteFile(String path, String serverIp, String serverUsername, String serverPassword) throws Exception {
        ChannelSftp sftp = setupSftpChannel(serverIp, serverUsername, serverPassword);
        try {
            sftp.rm(path);
        } finally {
            disconnect(sftp);
        }
    }

    public byte[] downloadFile(String path, String name) throws Exception {
        ChannelSftp sftp = setupSftpChannel();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String fullPath = path.endsWith("/") ? path + name : path + "/" + name;
            InputStream inputStream = sftp.get(fullPath);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            disconnect(sftp);
        }
        return outputStream.toByteArray();
    }

    // Overloaded method với tham số server động
    public byte[] downloadFile(String path, String serverIp, String serverUsername, String serverPassword) throws Exception {
        ChannelSftp sftp = setupSftpChannel(serverIp, serverUsername, serverPassword);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            InputStream inputStream = sftp.get(path);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            disconnect(sftp);
        }
        return outputStream.toByteArray();
    }

    private void disconnect(ChannelSftp sftp) {
        if (sftp != null && sftp.isConnected()) {
            try {
                sftp.disconnect();
                try {
                    sftp.getSession().disconnect();
                } catch (JSchException e) {
                    // Log or handle the exception as needed
                }
            } catch (Exception e) {
                // Log or handle the exception as needed
            }
        }
    }
}
