package com.example.sshcontrol.sshcontrol.service;

import com.jcraft.jsch.*;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class FileManagerService {

    public static class FileInfo {
        public String name;
        public String path;
        public String type; // "file" or "dir"
        public long size;
        public long modified;

        public FileInfo(String name, String path, String type, long size, long modified) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.size = size;
            this.modified = modified;
        }
    }

    /**
     * Tạo và trả về JSch session
     */
    private Session createSession(String host, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30000);
        return session;
    }

    /**
     * Liệt kê các file trong thư mục
     */
    public List<FileInfo> listFiles(String host, String username, String password, String path) throws Exception {
        List<FileInfo> files = new ArrayList<>();
        
        Session session = createSession(host, username, password);
        
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            channelSftp.cd(path);
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(".");

            for (ChannelSftp.LsEntry entry : entries) {
                if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                    continue;
                }

                SftpATTRS attrs = entry.getAttrs();
                String filePath = path.endsWith("/") ? path + entry.getFilename() : path + "/" + entry.getFilename();
                String type = attrs.isDir() ? "dir" : "file";

                files.add(new FileInfo(
                    entry.getFilename(),
                    filePath,
                    type,
                    attrs.getSize(),
                    attrs.getMTime() * 1000L
                ));
            }

            files.sort((a, b) -> {
                if (a.type.equals(b.type)) {
                    return a.name.compareTo(b.name);
                }
                return a.type.equals("dir") ? -1 : 1;
            });

        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }

        return files;
    }

    /**
     * Đọc nội dung file
     */
    public String readFile(String host, String username, String password, String filePath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            InputStream inputStream = channelSftp.get(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder content = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            reader.close();
            inputStream.close();

            return content.toString();
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Ghi nội dung vào file
     */
    public void writeFile(String host, String username, String password, String filePath, String content) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes());
            channelSftp.put(inputStream, filePath);
            inputStream.close();
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Xóa file hoặc thư mục
     */
    public void deleteFile(String host, String username, String password, String filePath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            SftpATTRS attrs = channelSftp.stat(filePath);
            if (attrs.isDir()) {
                deleteDirectory(channelSftp, filePath);
            } else {
                channelSftp.rm(filePath);
            }
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Xóa thư mục và toàn bộ nội dung
     */
    private void deleteDirectory(ChannelSftp channelSftp, String path) throws SftpException {
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> entries = channelSftp.ls(path);

        for (ChannelSftp.LsEntry entry : entries) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                continue;
            }

            String filePath = path.endsWith("/") ? path + entry.getFilename() : path + "/" + entry.getFilename();

            if (entry.getAttrs().isDir()) {
                deleteDirectory(channelSftp, filePath);
            } else {
                channelSftp.rm(filePath);
            }
        }

        channelSftp.rmdir(path);
    }

    /**
     * Đổi tên file
     */
    public void renameFile(String host, String username, String password, String oldPath, String newPath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            channelSftp.rename(oldPath, newPath);
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Tạo thư mục mới
     */
    public void createDirectory(String host, String username, String password, String path) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            channelSftp.mkdir(path);
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Lấy file để tải xuống
     */
    public InputStream getFileInputStream(String host, String username, String password, String filePath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            InputStream in = channelSftp.get(filePath);
            // Note: Session sẽ được disconnect sau khi file được download
            return in;
        } catch (Exception e) {
            channelSftp.disconnect();
            session.disconnect();
            throw e;
        }
    }

    /**
     * Kiểm tra file tồn tại
     */
    public boolean fileExists(String host, String username, String password, String filePath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            channelSftp.stat(filePath);
            return true;
        } catch (SftpException e) {
            return false;
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Lấy kích thước file
     */
    public long getFileSize(String host, String username, String password, String filePath) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            return channelSftp.stat(filePath).getSize();
        } finally {
            channelSftp.disconnect();
            session.disconnect();
        }
    }

    /**
     * Tải file lên máy chủ
     */
    public void uploadFile(String host, String username, String password, String destinationPath, InputStream fileInputStream) throws Exception {
        Session session = createSession(host, username, password);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        try {
            channelSftp.put(fileInputStream, destinationPath);
        } finally {
            fileInputStream.close();
            channelSftp.disconnect();
            session.disconnect();
        }
    }
}
