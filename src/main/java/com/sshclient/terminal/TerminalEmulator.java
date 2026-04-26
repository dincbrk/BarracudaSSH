package com.sshclient.terminal;

import com.sshclient.config.ConnectionConfig;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class TerminalEmulator {
    
    private final TextArea console;
    private final InputStream sshIn;
    private final OutputStream sshOut;
    private boolean running = true;
    
    private final ConnectionConfig config;
    
    // Simple regex to strip ANSI escape codes for basic rendering
    private static final Pattern ANSI_PATTERN = Pattern.compile("\\x1B\\[[0-?]*[ -/]*[@-~]");

    public TerminalEmulator(ConnectionConfig config, TextArea console, InputStream sshIn, OutputStream sshOut) {
        this.config = config;
        this.console = console;
        this.sshIn = sshIn;
        this.sshOut = sshOut;
    }

    public void start() {
        Thread readerThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try {
                int bytesRead;
                while (running && (bytesRead = sshIn.read(buffer)) != -1) {
                    String output = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    String cleanOutput = ANSI_PATTERN.matcher(output).replaceAll("");
                    
                    Platform.runLater(() -> {
                        for (char c : cleanOutput.toCharArray()) {
                            if (c == '\b' || c == 127) {
                                int len = console.getLength();
                                if (len > 0) {
                                    console.deleteText(len - 1, len);
                                }
                            } else if (c == '\r') {
                                if (config.isImplicitLF()) {
                                    console.appendText("\n");
                                }
                                // Otherwise ignore \r to prevent double line breaks in JavaFX TextArea
                            } else if (c == '\n') {
                                // TextArea natively drops to a new line and goes to col 0 on \n.
                                // If implicitCR is enabled, this matches native TextArea behavior anyway.
                                console.appendText("\n");
                            } else {
                                console.appendText(String.valueOf(c));
                            }
                        }
                    });
                }
            } catch (IOException e) {
                if (running) {
                    Platform.runLater(() -> console.appendText("\n[Connection lost: " + e.getMessage() + "]\n"));
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void sendInput(String input) {
        try {
            sshOut.write(input.getBytes(StandardCharsets.UTF_8));
            sshOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}
