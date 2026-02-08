package com.paste.android;

public class LinuxDestination {
    private String name;
    private String user;
    private String host;
    private int port;
    private String directory;

    public LinuxDestination(String name, String user, String host, int port, String directory) {
        this.name = name;
        this.user = user;
        this.host = host;
        this.port = port;
        this.directory = directory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    @Override
    public String toString() {
        return name + " (" + user + "@" + host + ":" + port + ")";
    }
}
