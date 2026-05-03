package com.sshclient.terminal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.UUID;

public class LocalTerminalServer {
    private HttpServer server;
    private int port;
    private final ConcurrentLinkedQueue<byte[]> outputBuffer = new ConcurrentLinkedQueue<>();
    private TerminalEmulator emulator;
    private volatile boolean active = true;

    private ExecutorService executor;
    private final String secureToken;

    public LocalTerminalServer(TerminalEmulator emulator) throws IOException {
        this.emulator = emulator;
        this.secureToken = UUID.randomUUID().toString();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.port = server.getAddress().getPort();
        this.executor = Executors.newCachedThreadPool();
        server.setExecutor(this.executor);

        server.createContext("/poll", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (exchange.getRequestURI().getQuery() == null || !exchange.getRequestURI().getQuery().contains("token=" + secureToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                
                // Block until data is available or connection closed
                byte[] data = null;
                int waitCount = 0;
                while (active && waitCount < 100) { // 100 * 50ms = 5s timeout
                    data = outputBuffer.poll();
                    if (data != null) break;
                    try { Thread.sleep(50); } catch (InterruptedException e) {}
                    waitCount++;
                }

                if (data == null) {
                    // Send empty 200 OK
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();
                } else {
                    exchange.sendResponseHeaders(200, data.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(data);
                    os.close();
                }
            }
        });

        server.createContext("/input", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (exchange.getRequestURI().getQuery() == null || !exchange.getRequestURI().getQuery().contains("token=" + secureToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    byte[] inputData = is.readAllBytes();
                    is.close();
                    if (inputData.length > 0) {
                        emulator.sendInputRaw(inputData);
                    }
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();
                }
            }
        });

        server.createContext("/resize", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (exchange.getRequestURI().getQuery() == null || !exchange.getRequestURI().getQuery().contains("token=" + secureToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes());
                    is.close();
                    String[] dims = body.split(",");
                    if (dims.length == 2) {
                        try {
                            int cols = Integer.parseInt(dims[0]);
                            int rows = Integer.parseInt(dims[1]);
                            emulator.resize(cols, rows);
                        } catch (NumberFormatException e) {}
                    }
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().close();
                }
            }
        });

        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) {
                    path = "/terminal.html";
                }
                if (path.startsWith("/")) {
                    path = path.substring(1); // remove leading slash
                }
                
                // Read from classpath
                InputStream is = getClass().getResourceAsStream("/terminal/" + path);
                if (is == null) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                
                byte[] fileData = is.readAllBytes();
                is.close();
                
                if (path.endsWith(".html")) {
                    String htmlContent = new String(fileData);
                    htmlContent = htmlContent.replace("{{TOKEN}}", secureToken);
                    fileData = htmlContent.getBytes();
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                }
                else if (path.endsWith(".js")) exchange.getResponseHeaders().set("Content-Type", "application/javascript");
                else if (path.endsWith(".css")) exchange.getResponseHeaders().set("Content-Type", "text/css");
                
                exchange.sendResponseHeaders(200, fileData.length);
                OutputStream os = exchange.getResponseBody();
                os.write(fileData);
                os.close();
            }
        });

        server.start();
        System.out.println("Local terminal server started on port " + port);
    }

    public int getPort() {
        return port;
    }

    public void writeData(byte[] data) {
        outputBuffer.offer(data);
    }

    public void stop() {
        active = false;
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
