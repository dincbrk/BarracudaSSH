package com.sshclient.core;

import com.sshclient.config.ConnectionConfig;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SessionManager {
    private final Map<String, ConnectionConfig> savedSessions;
    private final File settingsFile;

    public SessionManager() {
        this.savedSessions = new HashMap<>();
        this.settingsFile = new File(System.getProperty("user.home"), ".javaputty_sessions.properties");
        loadAllFromDisk();
    }

    public void saveSession(String sessionName, ConnectionConfig config) {
        savedSessions.put(sessionName, config);
        saveAllToDisk();
    }

    public ConnectionConfig loadSession(String sessionName) {
        return savedSessions.get(sessionName);
    }

    public void deleteSession(String sessionName) {
        savedSessions.remove(sessionName);
        saveAllToDisk();
    }
    
    public Map<String, ConnectionConfig> getAllSessions() {
        return savedSessions;
    }

    private void loadAllFromDisk() {
        if (!settingsFile.exists()) return;
        try (InputStream is = new FileInputStream(settingsFile)) {
            Properties props = new Properties();
            props.load(is);
            
            for (String key : props.stringPropertyNames()) {
                if (key.endsWith(".host")) {
                    String prefix = key.substring(0, key.lastIndexOf(".host"));
                    String sessionName = prefix.replace("session.", "");
                    
                    String host = props.getProperty(prefix + ".host", "localhost");
                    int port = Integer.parseInt(props.getProperty(prefix + ".port", "22"));
                    String user = props.getProperty(prefix + ".user", "");
                    
                    ConnectionConfig config = new ConnectionConfig(host, port, user);
                    
                    config.setColumns(Integer.parseInt(props.getProperty(prefix + ".columns", "80")));
                    config.setRows(Integer.parseInt(props.getProperty(prefix + ".rows", "24")));
                    config.setKeepaliveInterval(Integer.parseInt(props.getProperty(prefix + ".keepalive", "0")));
                    config.setPrivateKeyPath(props.getProperty(prefix + ".privateKeyPath", ""));
                    config.setImplicitCR(Boolean.parseBoolean(props.getProperty(prefix + ".implicitCR", "false")));
                    config.setImplicitLF(Boolean.parseBoolean(props.getProperty(prefix + ".implicitLF", "false")));
                    
                    savedSessions.put(sessionName, config);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAllToDisk() {
        Properties props = new Properties();
        for (Map.Entry<String, ConnectionConfig> entry : savedSessions.entrySet()) {
            String prefix = "session." + entry.getKey();
            ConnectionConfig config = entry.getValue();
            props.setProperty(prefix + ".host", config.getHost());
            props.setProperty(prefix + ".port", String.valueOf(config.getPort()));
            props.setProperty(prefix + ".user", config.getUsername());
            props.setProperty(prefix + ".columns", String.valueOf(config.getColumns()));
            props.setProperty(prefix + ".rows", String.valueOf(config.getRows()));
            props.setProperty(prefix + ".keepalive", String.valueOf(config.getKeepaliveInterval()));
            props.setProperty(prefix + ".implicitCR", String.valueOf(config.isImplicitCR()));
            props.setProperty(prefix + ".implicitLF", String.valueOf(config.isImplicitLF()));
            if (config.getPrivateKeyPath() != null) {
                props.setProperty(prefix + ".privateKeyPath", config.getPrivateKeyPath());
            }
        }
        
        try (OutputStream os = new FileOutputStream(settingsFile)) {
            props.store(os, "BarracudaSSH Saved Sessions");
            
            // SECURITY FIX 2: Restrict file permissions to owner-only (chmod 600)
            try {
                java.nio.file.attribute.PosixFileAttributeView posixView = java.nio.file.Files.getFileAttributeView(
                    settingsFile.toPath(), 
                    java.nio.file.attribute.PosixFileAttributeView.class
                );
                if (posixView != null) {
                    java.util.Set<java.nio.file.attribute.PosixFilePermission> perms = java.util.EnumSet.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
                    );
                    posixView.setPermissions(perms);
                }
            } catch (Exception ex) {
                // Ignore on non-POSIX OS (Windows)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
