## [1.2.1] - The Stability & Security Update

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
