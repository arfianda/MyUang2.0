# Product Requirements Document (PRD)

# MyUang — Smart Finance Tracker Mobile App

---

## 1. Product Overview

**MyUang** adalah aplikasi mobile pencatatan keuangan pintar berbasis Java dengan backend Firebase dan integrasi AI.

Aplikasi ini membantu pengguna, khususnya mahasiswa dan kalangan muda, untuk mengatur cashflow, mengurangi pengeluaran berlebihan, dan memahami kondisi keuangan mereka secara praktis.

### Fokus Utama Aplikasi
- Tracking keuangan harian
- Analisis pengeluaran
- Scan struk otomatis menggunakan AI Camera
- Tips hemat personal berdasarkan kebiasaan pengguna

---

# 2. Product Vision

Membantu generasi muda mengelola keuangan secara lebih disiplin, praktis, dan cerdas menggunakan teknologi AI.

---

# 3. Target Users

## Primary Users
- Mahasiswa
- Fresh graduate
- Anak kos
- Freelancer muda
- Pengguna usia 18–30 tahun

## User Problems
Pengguna sering:
- Boros karena tidak sadar pengeluaran kecil
- Tidak tahu uang habis untuk apa
- Sulit mencatat keuangan secara manual
- Malas melakukan tracking secara konsisten

---

# 4. Current User Behavior

Saat ini pengguna biasanya:
- Mencatat pengeluaran di notes/chat diri sendiri
- Mengingat pengeluaran secara manual
- Menggunakan Excel sederhana
- Tidak mencatat sama sekali

## Dampak
- Cashflow berantakan
- Pengeluaran sulit dikontrol
- Tidak punya insight finansial

---

# 5. Product Goals

## Business Goals
- Membuat aplikasi finance tracker yang menarik untuk anak muda
- Meningkatkan daily active users melalui fitur AI dan tips harian
- Menjadi aplikasi pencatatan keuangan yang praktis dan modern

## User Goals
- Mengetahui sisa uang dengan cepat
- Memahami pola pengeluaran
- Mendapat rekomendasi hemat
- Mengurangi kebiasaan boros

---

# 6. Success Metrics

| Metric | Target |
|---|---|
| Daily Active Users | Tinggi |
| User Retention | >60% mingguan |
| Expense Entries per Day | Minimal 3 |
| AI Receipt Scan Usage | Tinggi |
| Tips Open Rate | >70% |

---

# 7. First-Time User Experience

## Hal yang harus berhasil dilakukan user sebelum menutup aplikasi pertama kali
✅ Melihat sisa uang mereka

## User Flow Pertama Kali
1. User register/login
2. Input saldo awal
3. Tambahkan pengeluaran pertama
4. Dashboard langsung menampilkan:
   - Sisa uang
   - Pengeluaran hari ini
   - Grafik sederhana

---

# 8. Core Features (Mandatory Features)

## 8.1 Smart Expense Tracking

Fitur mencatat pemasukan dan pengeluaran.

### User Can
- Tambah pemasukan
- Tambah pengeluaran
- Pilih kategori
- Tambah catatan

### Categories
- Makanan
- Transportasi
- Hiburan
- Belanja
- Pendidikan
- Lainnya

---

## 8.2 AI Receipt Scanner

Fitur scan struk otomatis menggunakan AI OCR.

### Function
- User foto struk
- AI membaca:
  - Nama toko
  - Total harga
  - Tanggal
  - Item belanja
- Data otomatis masuk ke pencatatan

### Technology
- OCR AI
- Firebase ML Kit / Google ML Kit

---

## 8.3 Financial Dashboard

Dashboard utama menampilkan kondisi keuangan user.

### Information
- Sisa uang
- Total pengeluaran hari ini
- Total pemasukan
- Pengeluaran terbesar

---

## 8.4 Expense Analytics Graph

Visualisasi pengeluaran dalam bentuk grafik.

### Graph Types
- Pie chart kategori pengeluaran
- Line chart pengeluaran mingguan
- Bar chart pemasukan vs pengeluaran

---

## 8.5 AI Saving Tips

AI memberikan saran hemat berdasarkan pola pengeluaran.

### Example
- “Pengeluaran makananmu naik 20% minggu ini.”
- “Kamu terlalu sering membeli kopi.”
- “Coba batasi transportasi online minggu ini.”

### AI Logic
Analisis kategori pengeluaran terbanyak lalu generate rekomendasi.

---

# 9. Additional Features (Optional)

## Wishlist Feature
Menyimpan target pembelian.

## Monthly Financial Report
Laporan bulanan otomatis.

## Spending Reminder
Notifikasi jika pengeluaran terlalu besar.

## Dark Mode
Tema gelap aplikasi.

---

# 10. AI Integration

## AI Features

### 1. OCR Receipt Recognition
Membaca struk otomatis.

### 2. Spending Pattern Analysis
Menganalisis kebiasaan pengeluaran.

### 3. Personalized Saving Tips
Memberikan rekomendasi hemat.

---

# 11. Technology Stack

## Frontend
- Java Android Native
- XML Layout

## Backend
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Firebase Cloud Messaging

## AI Integration
- Google ML Kit OCR
- AI Recommendation Engine

---

# 12. Database Design (Firebase Firestore)

## Collection: users

| Field | Type |
|---|---|
| uid | string |
| name | string |
| email | string |
| saldo | number |

---

## Collection: transactions

| Field | Type |
|---|---|
| transactionId | string |
| userId | string |
| type | string |
| category | string |
| amount | number |
| note | string |
| createdAt | timestamp |

---

## Collection: ai_tips

| Field | Type |
|---|---|
| tipId | string |
| userId | string |
| content | string |
| createdAt | timestamp |

---

# 13. User Flow

## Main Flow
Login/Register → Dashboard → Tambah Pengeluaran → AI Analisis → Grafik & Tips

---

# 14. UI Pages

| Page | Description |
|---|---|
| Splash Screen | Intro aplikasi |
| Login/Register | Authentication |
| Dashboard | Ringkasan keuangan |
| Add Transaction | Tambah transaksi |
| Receipt Scanner | Scan struk |
| Analytics | Grafik pengeluaran |
| AI Tips | Tips hemat |
| Profile | Profil user |

---

# 15. Competitive Advantage

## Mengapa lebih baik daripada catatan manual?
- Lebih cepat
- Praktis
- Ada grafik otomatis
- Ada AI recommendation
- Scan struk otomatis
- Data tersimpan aman di cloud

---

# 16. Daily Engagement Strategy

## Hal yang membuat user membuka aplikasi setiap hari
- Tips hemat harian
- Reminder pengeluaran
- Melihat sisa uang real-time
- Grafik pengeluaran yang terus update

---

# 17. Future Development

- Integrasi e-wallet
- AI chatbot finance assistant
- Budget planning otomatis
- Export PDF laporan keuangan
- Multi-device synchronization

---

# 18. Conclusion

MyUang adalah aplikasi finance tracker modern yang memanfaatkan AI untuk membantu pengguna mengelola uang dengan lebih disiplin, praktis, dan cerdas.

Dengan fitur scan struk otomatis, analisis pengeluaran, dan tips hemat personal, aplikasi ini memberikan pengalaman yang jauh lebih efektif dibanding pencatatan manual.