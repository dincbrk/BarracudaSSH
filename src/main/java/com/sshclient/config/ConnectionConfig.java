package com.sshclient.config;

public class ConnectionConfig {
    private String host;
    private int port;
    private String username;
    private String password;
    
    // Auth
    private String privateKeyPath;
    
    // Window
    private int columns = 80;
    private int rows = 24;
    
    // Connection
    private int keepaliveInterval = 0; // seconds, 0 = off

    // Terminal
    private boolean implicitCR = false;
    private boolean implicitLF = false;

    public ConnectionConfig(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public void clearPassword() { this.password = null; }
    
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public int getColumns() { return columns; }
    public void setColumns(int columns) { this.columns = columns; }

    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }

    public int getKeepaliveInterval() { return keepaliveInterval; }
    public void setKeepaliveInterval(int keepaliveInterval) { this.keepaliveInterval = keepaliveInterval; }

    public boolean isImplicitCR() { return implicitCR; }
    public void setImplicitCR(boolean implicitCR) { this.implicitCR = implicitCR; }

    public boolean isImplicitLF() { return implicitLF; }
    public void setImplicitLF(boolean implicitLF) { this.implicitLF = implicitLF; }

    public boolean useKeyAuth() {
        return privateKeyPath != null && !privateKeyPath.trim().isEmpty();
    }
}
