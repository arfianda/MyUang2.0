# MyUang Android App with Firebase Backend

Aplikasi Android native untuk MyUang, dikembangkan menggunakan Java dan XML dengan backend Firebase (Authentication, Firestore) dan integrasi Google ML Kit OCR.

## Cara Buka & Menjalankan

1. Buka folder ini di Android Studio: `D:\MyUang2.0`
2. Pastikan Android Studio memakai JDK bawaan Android Studio atau JDK 17+.
3. Sync Gradle lalu jalankan modul `app`.

## Alur Aplikasi
Splash (Deteksi Lokasi/Bahasa) -> Login/Register -> Dashboard (Ringkasan Real-time & Tren) -> Tambah Transaksi (Manual/Scan) -> Scan Struk AI -> Analisis (Bar Chart) -> Tips AI -> Profil (Ubah Saldo & Mode Gelap).

Semua data keuangan, pengguna, dan tips AI terintegrasi secara real-time dengan Firebase Firestore dan Firebase Authentication.
