package com.example.sshcontrol.model;

public class FileInfo {

    private String name;
    private String absolutePath;
    private boolean directory;
    private long creationTime;
    private long lastModified;

    public FileInfo(String name, String absolutePath, boolean directory, long creationTime, long lastModified) {
        this.name = name;
        this.absolutePath = absolutePath;
        this.directory = directory;
        this.creationTime = creationTime;
        this.lastModified = lastModified;
    }

public String getName() {
    return name;
}

public String getAbsolutePath() {
    return absolutePath;
}

public boolean isDirectory() {
    return directory;
}

public long getCreationTime() {
    return creationTime;
}

public long getLastModified() {
    return lastModified;
}

public void setName(String name) {
    this.name = name;
}

public void setAbsolutePath(String absolutePath) {
    this.absolutePath = absolutePath;
}

public void setDirectory(boolean directory) {
    this.directory = directory;
}

public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
}

public void setLastModified(long lastModified) {
    this.lastModified = lastModified;
}
}
