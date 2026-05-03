# BarracudaSSH - Technical Documentation

**BarracudaSSH** (formerly Java PuTTY Clone) is a modern, secure, and fully-featured cross-platform SSH client developed using JavaFX and Apache MINA SSHD. It aims to deliver the core functionality of PuTTY with a much more modern interface and enhanced security standards.

---

## 🛠️ Tech Stack
- **Language:** Java 17  
- **GUI:** JavaFX 21.0.2 (with cross-platform native support)  
- **SSH Engine:** Apache MINA SSHD 2.12.1  
- **Cryptography:** BouncyCastle (used only for key parsing; JCE Provider disabled)  
- **Build Tool:** Apache Maven  
- **Packaging:** `maven-shade-plugin` (Fat JAR), `jpackage` (macOS .app), `launch4j` (Windows .exe)

---

## 🚀 Core Features

1. **Terminal Emulation:**
   - Cleans ANSI escape sequences.
   - Custom line ending handling (implicit CR/LF support).
   - Interactive terminal support with `xterm-256color` PTY sizing.

2. **Persistent Session Manager:**
   - Stores host, port, username, key path, and terminal settings in `~/.javaputty_sessions.properties`.

3. **Keepalive (Heartbeat) Support:**
   - Sends periodic null packets to prevent idle connections from being dropped by the server.

4. **Key-Based Authentication:**
   - Secure login using SSH keys (PEM, PPK) instead of passwords.

---

## 🛡️ Security Hardening

The application has been hardened to production-grade security standards by addressing critical vulnerabilities:

### 1. Man-in-the-Middle (MitM) Protection
Originally, all host keys were blindly accepted (`AcceptAllServerKeyVerifier`). This has been replaced with `KnownHostsServerKeyVerifier`.
- The application now uses the system's `~/.ssh/known_hosts` file based on a Trust On First Use (TOFU) model.
- If a server's fingerprint changes, the connection is automatically rejected.
- If `~/.ssh` or the known_hosts file does not exist on first run, they are automatically created.

### 2. File Permission Restriction (chmod 600)
Session configuration files are written using `Files.setPosixFilePermissions`, restricting access to only the file owner (`OWNER_READ`, `OWNER_WRITE`). This prevents other users or background services from accessing stored session data.

### 3. Memory Wiping (Credential Cleanup)
Passwords entered in the login screen are a security risk if left in memory. After successful authentication (`session.auth().verify()`), the `clearPassword()` method is triggered to securely erase the password from memory.

### 4. Keyboard-Interactive Authentication Support
Modern Linux servers (especially Ubuntu PAM configurations) often reject plain password authentication. A `UserInteraction` layer was implemented to automatically handle interactive authentication prompts and supply credentials programmatically.

### 5. Fat JAR JCE Signature Issue Fix
When bundled inside a Fat JAR, BouncyCastle loses its digital signature, causing:
`JarException: The JCE Provider is not signed`

To resolve this, `org.apache.sshd.security.provider.BouncyCastle.enabled` is set to `false`. Encryption and key agreement are fully delegated to Java 17’s native JCE implementation. BouncyCastle remains only for parsing PPK key files.

---

## 📦 Packaging & Distribution

To ensure smooth operation across different operating systems, three packaging strategies are used:

### 1. macOS Package (`BarracudaSSH.app`)
- **Tool:** `jpackage`
- The application is bundled as a native macOS App Bundle with full system integration and a dedicated icon.
- Includes only macOS ARM (M1/M2) native JavaFX libraries.

### 2. Windows Package (`BarracudaSSH.exe`)
- **Tool:** `launch4j-maven-plugin`
- The cross-platform Fat JAR is wrapped into a native `.exe`.
- Windows-specific `.dll` graphics libraries are included via Maven classifiers.

### 3. Portable Universal Package (`BarracudaSSH.jar`)
- **Tool:** `maven-shade-plugin`
- A Fat JAR containing both Windows and macOS ARM native libraries.
- Runs anywhere with Java 17 installed via:
  `java -jar BarracudaSSH.jar`
- Mac Intel (`x86_64`) native libraries are intentionally excluded to avoid dependency conflicts.

---

## [v1.2.1] - The Stability & Security Update

This release represents one of the most significant updates to the application, introducing deep architectural changes, resolving chronic crashes (SIGABRT) on Apple Silicon (M1/M2/M3), and implementing enterprise-grade security measures.

### ✨ Features & Improvements
* **Xterm.js Integration:** The old, limited TextArea-based architecture has been completely replaced. A full-featured terminal emulator, `xterm.js`, is now integrated. Advanced TUI (Text-based UI) applications such as `nano`, `vim`, and `htop`, along with full ANSI color support, now work flawlessly.
* **Smart Window Resizing:** When the terminal window is resized, updated row/column dimensions are instantly transmitted to the underlying SSH (PTY) session. Using `ResizeObserver` and debouncing, applications like `nano` dynamically adapt to the available screen space.
* **Enhanced Cross-Platform Support:** The application can now be built and distributed as a standalone `.app` for macOS and a `.exe` (via Launch4j) for Windows with a single click.

### 🛠 Architecture Overhaul
* **Definitive Fix for JNI/WebKit Crashes:** Native memory crashes (EXC_BAD_ACCESS) caused by JavaFX WebView communication over JNI (`executeScript`, `JSObject`) on macOS ARM have been fully eliminated. All legacy Java-JavaScript bridges have been removed.
* **Local HTTP Server (Polling-Based Communication):** An asynchronous embedded HTTP server has been introduced. Instead of JNI, communication between the Xterm.js frontend and the Java backend now occurs via `http://localhost:<port>/` using a polling mechanism. This ensures that even high-volume log streams are rendered smoothly without UI freezes or crashes.
* **Zero Dangling Threads:** A proper "graceful shutdown" mechanism has been implemented for HTTP and SSH worker threads. When the application is closed, no background processes remain, and the JVM memory is cleanly released.

### 🔒 Security Fixes
* **CORS & Network Binding Restrictions:** The local HTTP server is now restricted to respond only to requests from `127.0.0.1`. This prevents unauthorized access from other devices on the same network (Network Access Control).
* **Cross-Process IPC Security:** To prevent malicious local processes or web pages from injecting commands into your SSH terminal, **UUID-based token authentication** has been implemented. Secure, randomly generated tokens are passed only to the WebView, blocking all unauthorized local access (403 Forbidden).
* **Secure SSH Host Key Verification:** Blind acceptance of SSH host keys has been eliminated. The application now follows a Known Hosts / TOFU (Trust On First Use) model for host verification.
* **Memory Sanitization:** User credentials stored in memory are immediately wiped after a successful login (Wipe-on-Connect).

---

*This document reflects the current architecture and technical state of the BarracudaSSH project.*
