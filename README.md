# MyUang Android Front End Prototype

Prototype Android native untuk MyUang, dibuat dengan Java dan XML tanpa backend atau Firebase.

## Cara buka

1. Buka folder ini di Android Studio: `D:\MyUang2.0`
2. Pastikan Android Studio memakai JDK bawaan Android Studio atau JDK 17+.
3. Sync Gradle lalu jalankan modul `app`.

## Alur demo

Splash -> Login/Register -> Dashboard -> Tambah Transaksi -> Scan Struk AI -> Analisis -> Tips AI -> Profil.

Semua data keuangan masih mock/sample dan aksi backend ditampilkan sebagai toast. Scanner memakai ML Kit Text Recognition untuk membaca teks dari gambar kamera/galeri.
