package com.sshclient.ssh;

import java.io.File;
import java.security.KeyPair;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;

public class KeyManager {

    /**
     * Loads a key pair from a private key file.
     * Supports PEM and potentially PPK if BouncyCastle is configured appropriately.
     */
    public static FileKeyPairProvider loadKey(String keyPath, String password) {
        FileKeyPairProvider provider = new FileKeyPairProvider(new File(keyPath).toPath());
        if (password != null && !password.isEmpty()) {
            provider.setPasswordFinder(FilePasswordProvider.of(password));
        }
        return provider;
    }
}
