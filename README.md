🤖 Glosgano — Offline AI Assistant

Aplikasi AI lokal (offline) berbasis Android yang dirancang untuk menjadi asisten pribadi yang powerful, aman, dan sepenuhnya berjalan di perangkat Anda. Aplikasi ini menggabungkan kekuatan model bahasa besar (LLM) lokal dengan kemampuan otomasi sistem melalui akses Root.

---

🚀 Coba Aplikasi (Tanpa Compile!)

Anda tidak perlu meng-compile kode sendiri untuk mencoba aplikasi ini. Kami telah menyediakan file APK siap pakai yang bisa langsung Anda unduh dan instal di perangkat Android Anda.

👉 Klik di sini untuk mengunduh APK versi terbaru 👈

(Catatan: Pastikan Anda mengizinkan "Install from Unknown Sources" di pengaturan HP Anda).

---

✨ Fitur Utama

Aplikasi ini bukan sekadar chatbot biasa. Berikut adalah fitur-fitur canggih yang tertanam di dalamnya:

🧠 1. Offline LLM Engine

· Berjalan 100% tanpa internet untuk chat dasar.
· Mendukung model format .gguf (Llama, Qwen, SmolLM, dll).
· Dilengkapi dengan sistem RAG (Retrieval-Augmented Generation) untuk membaca dokumen lokal (PDF/TXT).

🔧 2. Root Shell Agent

· Eksekusi Perintah Otomatis: AI dapat menjalankan perintah shell Android (via Root) secara langsung.
· Agent Loop: AI dapat berpikir, mengeluarkan perintah [SHELL], menerima umpan balik, dan melanjutkan ke langkah berikutnya secara mandiri.
· Keamanan Terkontrol: Dilengkapi dengan Toggle ON/OFF dan tombol STOP darurat.

📋 3. Task Queue (Antrean Tugas)

· Multi-Step Automation: Pengguna dapat menambahkan daftar tugas (misal: "Buka WhatsApp", "Cari kontak Budi", "Kirim pesan").
· Aplikasi akan mengeksekusi tugas tersebut satu per satu secara otomatis hingga antrean selesai.
· Dilengkapi indikator status (Menunggu, Sedang Berjalan, Selesai) dan penanganan error yang cerdas.

🌐 4. Web Search (Online)

· Pencarian Internet Terintegrasi: Saat fitur ini aktif, AI akan mencari informasi real-time di internet (via DuckDuckGo) dan menyuntikkannya ke dalam konteks jawaban.
· Sumber Referensi: Menampilkan tautan sumber berita/website di bawah jawaban AI.

⚡ 5. Turbo Mode (Performance Boost)

· Optimasi CPU via Root: Mengubah CPU Governor ke mode performance dan membersihkan RAM (drop caches) sebelum AI menghasilkan teks.
· Kecepatan Maksimal: Menghasilkan output AI yang jauh lebih cepat pada perangkat yang mendukung.
· Kembali ke mode normal secara otomatis setelah selesai (mencegah overheat).

🎨 6. Customisasi Tanpa Batas

· Dark/Light Mode: Tampilan yang nyaman di mata, mendukung tema sistem.
· Custom Persona & Agent Prompt: Pengguna bisa mengubah kepribadian AI dan instruksi shell langsung dari menu Pengaturan.
· Command Dictionary (Multi-Profile): Kamus perintah yang bisa disesuaikan untuk berbagai aplikasi (WhatsApp, Facebook, General).

🖼️ 7. Vision UI (Persiapan ARM64)

· Antarmuka pengguna sudah mendukung pengiriman gambar (Kamera, Galeri, File).
· Siap digunakan pada perangkat 64-bit (ARM64) dengan library libmtmd.so yang sesuai.

🥚 8. Launcher Mode & Safety Menu (Easter Egg)

· Ketuk 2x (Double Tap) pada judul "Glosgano" di bagian atas layar untuk membuka menu darurat.
· Memungkinkan Anda menjadikan aplikasi sebagai Launcher Default, membuka Launcher lain, atau keluar dari aplikasi jika terjadi masalah (Safety).

---

🛠️ Teknologi yang Digunakan

· Kotlin & Jetpack Compose: Untuk UI yang modern dan responsif.
· Llama.cpp (JNI): Engine inferensi AI lokal berkecepatan tinggi.
· RootShellExecutor: Jembatan komunikasi antara aplikasi dan sistem operasi Android.
· DuckDuckGo HTML API: Untuk pencarian internet tanpa API Key.

---

⚠️ Catatan Penting

· Akses Root: Aplikasi ini membutuhkan Akses Root (Superuser) untuk fitur Agent dan Turbo Mode. Gunakan dengan bijak.
· Bahasa: Antarmuka aplikasi (UI) dan teks bawaan (default prompt) saat ini menggunakan Bahasa Indonesia. Namun, pengguna dapat mengubah System Prompt, Agent Prompt, dan ChatML Template secara bebas di menu Pengaturan Lanjutan (mendukung bahasa apa pun).
· Vision (VLM): Fitur Vision saat ini hanya tersedia untuk perangkat ARM64 (64-bit).

---

📄 Lisensi

Proyek ini dilisensikan di bawah Apache License 2.0. Lihat file LICENSE untuk detailnya.

---

🤝 Kontribusi

Kontribusi selalu terbuka! Jika Anda menemukan bug atau ingin menambahkan fitur baru, silakan buat Pull Request atau buka Issue.

Dibuat dengan ❤️ oleh Ongki350
