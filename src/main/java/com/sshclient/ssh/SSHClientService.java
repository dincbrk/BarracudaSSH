package com.sshclient.ssh;

import com.sshclient.config.ConnectionConfig;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.core.CoreModuleProperties;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
public class SSHClientService {

    private SshClient client;
    private ClientSession session;
    private ChannelShell channel;

    public void connect(ConnectionConfig config, InputStream in, OutputStream out, OutputStream err) throws Exception {
        // SECURITY FIX / BUG FIX: Do not register BouncyCastle as a JCE Provider.
        // Since we are using a Fat JAR, BouncyCastle loses its cryptographic signature.
        // If MINA SSHD tries to register it with Java's JCE framework, it will throw a JarException.
        // Java 17 has native support for Ed25519 and X25519, so we only need BouncyCastle for parsing PPK keys, not for JCE.
        System.setProperty("org.apache.sshd.security.provider.BC.enabled", "false");
        System.setProperty("org.apache.sshd.security.provider.BouncyCastle.enabled", "false");
        
        client = SshClient.setUpDefaultClient();
        
        // BUG FIX: Support "keyboard-interactive" authentication which many modern servers (Ubuntu etc.) require instead of standard "password" auth.
        client.setUserInteraction(new UserInteraction() {
            @Override
            public boolean isInteractionAllowed(ClientSession session) {
                return true;
            }
            @Override
            public void serverVersionInfo(ClientSession session, java.util.List<String> lines) {}
            @Override
            public void welcome(ClientSession session, String banner, String lang) {}
            @Override
            public String[] interactive(ClientSession session, String name, String instruction, String lang, String[] prompt, boolean[] echo) {
                String[] replies = new String[prompt.length];
                for (int i = 0; i < prompt.length; i++) {
                    replies[i] = config.getPassword() != null ? config.getPassword() : "";
                }
                return replies;
            }
            @Override
            public String getUpdatedPassword(ClientSession session, String prompt, String lang) {
                return null;
            }
        });
        
        // SECURITY FIX 1: Prevent Man-in-the-Middle (MitM) attacks by verifying against known_hosts.
        // It acts as TOFU (Trust On First Use). If the key changes later, it will throw an exception and abort.
        Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
        if (!java.nio.file.Files.exists(sshDir)) {
            java.nio.file.Files.createDirectories(sshDir);
        }
        Path knownHostsPath = sshDir.resolve("known_hosts");
        if (!java.nio.file.Files.exists(knownHostsPath)) {
            java.nio.file.Files.createFile(knownHostsPath);
        }
        
        ServerKeyVerifier verifier = new KnownHostsServerKeyVerifier(
                AcceptAllServerKeyVerifier.INSTANCE,
                knownHostsPath
        );
        client.setServerKeyVerifier(verifier);
        
        client.start();

        session = client.connect(config.getUsername(), config.getHost(), config.getPort())
                .verify(10000)
                .getSession();

        if (config.getKeepaliveInterval() > 0) {
            CoreModuleProperties.HEARTBEAT_INTERVAL.set(session, Duration.ofSeconds(config.getKeepaliveInterval()));
            CoreModuleProperties.HEARTBEAT_REPLY_WAIT.set(session, Duration.ofSeconds(5));
        }

        if (config.useKeyAuth()) {
            session.setKeyIdentityProvider(KeyManager.loadKey(config.getPrivateKeyPath(), null));
        } else {
            session.addPasswordIdentity(config.getPassword());
        }

        session.auth().verify(10000);
        
        // SECURITY FIX 3: Wipe password from memory after authentication is complete
        config.clearPassword();

        channel = session.createShellChannel();
        channel.setIn(in);
        channel.setOut(out);
        channel.setErr(err);

        // Request a PTY
        channel.setPtyType("xterm-256color");
        channel.setPtyColumns(config.getColumns());
        channel.setPtyLines(config.getRows());

        channel.open().verify(10000);
    }

    public void disconnect() {
        try {
            if (channel != null) channel.close(false);
            if (session != null) session.close(false);
            if (client != null) {
                client.stop();
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void resizePty(int columns, int lines) {
        if (channel != null && channel.isOpen()) {
            try {
                channel.sendWindowChange(columns, lines);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen() && channel != null && channel.isOpen();
    }
}
