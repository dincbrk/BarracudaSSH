## [1.2.1] - The Stability & Security Update

Bu versiyon, uygulamanın çekirdek mimarisinde köklü değişiklikler yapılan, özellikle Apple Silicon (M1/M2/M3) işlemcilerindeki kronik çökmeleri (SIGABRT) çözen ve kurumsal düzeyde güvenlik önlemleri getiren en büyük güncellemelerden biridir.

### ✨ Yeni Özellikler ve İyileştirmeler (Features)
* **Xterm.js Entegrasyonu:** Eski, kısıtlı metin kutusu (TextArea) mimarisi tamamen terk edildi. Tam teşekküllü bir terminal emülatörü olan `xterm.js` entegre edildi. Artık `Nano`, `Vim`, `htop` gibi gelişmiş TUI (Metin Tabanlı Arayüz) uygulamaları ve Linux renk paletleri (ANSI Color Codes) kusursuz çalışıyor.
* **Akıllı Pencere Boyutlandırma:** Terminal penceresi yeniden boyutlandırıldığında arka plandaki SSH (PTY) sunucusuna yeni satır/sütun koordinatları saniyesinde iletilir. `ResizeObserver` ve `Debouncing` kullanılarak `Nano` gibi editörlerin ekrana tam esnemesi sağlandı.
* **Gelişmiş Cross-Platform Desteği:** Artık uygulama hem macOS için izole bir `.app` formatında, hem de Windows için `.exe` (Launch4j) formatında tek tıkla derlenip çalışabiliyor.

### 🛠 Mimari Değişiklikler (Architecture Overhaul)
* **JNI/WebKit Çökmelerine Kesin Çözüm:** JavaFX WebView'ın Mac ARM mimarisinde JNI (`executeScript` ve `JSObject`) üzerinden haberleşirken yarattığı native bellek çökmeleri (EXC_BAD_ACCESS) tamamen izole edildi. Java ve Javascript arasındaki tüm eski veri köprüleri yıkıldı.
* **Yerel HTTP Sunucusu (Local Polling Server):** Uygulamanın içine asenkron bir mini-HTTP sunucusu entegre edildi. Artık Xterm.js arayüzü ile Java arka planı birbirlerine JNI ile değil, `http://localhost:<port>/` üzerinden "Polling (Sürekli Veri Çekme)" yöntemiyle konuşuyor. Bu sayede binlerce satırlık log akışları bile UI'ı dondurmadan ve çöktürmeden ekrana yansıtılıyor.
* **Sıfır Hayalet Süreç (Zero Dangling Threads):** Uygulama kapatıldığında (X butonuna basıldığında) arkada asılı kalan ve uygulamanın Dock'ta takılı kalmasına neden olan HTTP ve SSH işçi parçacıkları (Worker Threads) için "Graceful Shutdown" eklendi. Çıkış anında JVM hafızası tertemiz boşaltılıyor.

### 🔒 Güvenlik (Security Fixes)
* **CORS ve Network Binding Kısıtlaması:** Yerel HTTP sunucusunun sadece `127.0.0.1` adresinden gelen isteklere yanıt vermesi sağlandı. Böylece aynı Wi-Fi ağındaki yabancı cihazların terminale sızması engellendi. (Network Access Control).
* **Uygulamalar Arası Güvenlik (Cross-Process IPC Security):** Aynı bilgisayarda arka planda çalışan zararlı yazılımların veya zararlı web sitelerinin SSH terminalinize komut göndermesini engellemek için **UUID Token Doğrulaması** getirildi. Java tarafından rastgele üretilen kırılamaz şifreler, sadece ekrandaki WebView'a aktarılarak yetkisiz yerel girişler bloklandı (403 Forbidden).
* **Güvenli SSH Anahtar Doğrulaması:** SSH Sunucu anahtarlarının kontrolsüz kabul edilmesi engellenerek, bilinen anahtarların listesiyle (Known Hosts / TOFU) doğrulanması prensibine geçildi.
* **Bellek Temizliği:** Kullanıcı oturum açtıktan hemen sonra bellekte tutulan parolalar güvenlik amacıyla anında siliniyor (Wipe-on-Connect).
