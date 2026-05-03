package com.sshclient.terminal;

import com.sshclient.config.ConnectionConfig;
import com.sshclient.ssh.SSHClientService;
import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.util.concurrent.atomic.AtomicBoolean;
import netscape.javascript.JSObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TerminalEmulator {
    
    private final WebView webView;
    private final InputStream sshIn;
    private final OutputStream sshOut;
    private boolean running = true;
    private final ConnectionConfig config;
    private SSHClientService sshService;
    private volatile boolean isReady = false;
    private LocalTerminalServer localServer;

    public TerminalEmulator(ConnectionConfig config, WebView webView, InputStream sshIn, OutputStream sshOut) {
        this.config = config;
        this.webView = webView;
        this.sshIn = sshIn;
        this.sshOut = sshOut;
        try {
            this.localServer = new LocalTerminalServer(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSshService(SSHClientService sshService) {
        this.sshService = sshService;
    }

    public void initialize() {
        WebEngine engine = webView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                isReady = true;

                // Print initial connection info
                writeToTerminal("Looking up host \"" + config.getHost() + "\"...\r\n");
                writeToTerminal("Connecting to " + config.getHost() + " port " + config.getPort() + "...\r\n");
            }
        });
        
        // Load the xterm.js wrapper over localhost HTTP to completely bypass File URI restrictions 
        // and eliminate the need for ANY executeScript calls.
        String url = "http://localhost:" + (localServer != null ? localServer.getPort() : 8080) + "/";
        engine.load(url);
    }

    public void start() {
        Thread readerThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                int bytesRead;
                while (running && (bytesRead = sshIn.read(buffer)) != -1) {
                    byte[] actualBytes = new byte[bytesRead];
                    System.arraycopy(buffer, 0, actualBytes, 0, bytesRead);
                    if (localServer != null) {
                        localServer.writeData(actualBytes);
                    }
                }
            } catch (IOException e) {
                if (running) {
                    writeToTerminal("\r\n[Connection lost: " + e.getMessage() + "]\r\n");
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void writeToTerminal(String text) {
        if (localServer != null) {
            localServer.writeData(text.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void sendInput(String input) {
        sendInputRaw(input.getBytes(StandardCharsets.UTF_8));
    }

    public void sendInputRaw(byte[] input) {
        try {
            sshOut.write(input);
            sshOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resize(int cols, int rows) {
        if (sshService != null) {
            sshService.resizePty(cols, rows);
        }
    }

    public void stop() {
        running = false;
        if (localServer != null) {
            localServer.stop();
        }
    }
}
