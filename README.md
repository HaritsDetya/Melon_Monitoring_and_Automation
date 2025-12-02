# 🌱 Greenhouse Monitoring App
Aplikasi Android berbasis Jetpack Compose yang terintegrasi dengan Supabase serta sistem IoT sensor & device untuk monitoring dan kontrol greenhouse secara real-time.

## 📌 Deskripsi Proyek

Greenhouse Monitoring App adalah aplikasi Android yang memungkinkan pengguna melakukan monitoring sensor dan kontrol perangkat IoT pada greenhouse. Integrasi dengan Supabase digunakan untuk autentikasi, database, serta storage, sementara data sensor dikirimkan melalui perangkat IoT yang terhubung ke server backend.

Aplikasi dibangun dengan Jetpack Compose dan mengadopsi pendekatan UI modern serta state-driven.
## 🚀 Fitur Utama

### 🔐 Autentikasi
- Login
- Registrasi akun
- Reset password (via dialog + halaman khusus melalui deeplink email)
- Logout
- Delete account
- Update password

### 📊 Dashboard Monitoring
- Menampilkan daftar greenhouse yang terdaftar
- Data sensor sekilas untuk setiap greenhouse:
    - Suhu udara
    - Kelembapan
    - Suhu air
    - pH air
    - TDS
- Detail greenhouse:
    - Data sensor lengkap
    - History sensor

### 🔧 Kontrol Perangkat
- Mengatur dan mengendalikan:
    - Blower
    - Pompa air

### 👤 Profil Pengguna
- Menampilkan data akun
- Akses ke:
    - Ganti password
    - Logout
    - Delete account

## 🏗️ Arsitektur Sistem
Aplikasi terdiri dari tiga komponen utama yang saling terintegrasi untuk menyediakan sistem monitoring dan kontrol greenhouse secara real-time.
- Android App (Jetpack Compose)

    Aplikasi Android sebagai sisi client yang bertanggung jawab pada UI dan interaksi pengguna.
    - UI berbasis Jetpack Compose
    - ViewModel untuk pengelolaan state
    - Navigasi Compose
    - Supabase Client
    - Realtime Sensor Listener (menerima update data sensor melalui Supabase Realtime)

- Supabase Backend

    Menjadi pusat data dan autentikasi untuk aplikasi serta jembatan antara IoT dan aplikasi pengguna.

    - Authentication (login, register, reset password, dll)
    - Database:
        - sensors
        - users
        - greenhouse
        - devices
    - REST API untuk akses data
    - Realtime Channel untuk streaming data sensor dan kontrol device

- IoT Devices (Hardware)
    
    Perangkat fisik dalam greenhouse yang berfungsi sebagai sensor dan aktuator.
    - Mengirimkan data sensor ke Supabase:
        - Suhu udara
        - Kelembapan
        - Suhu air
        - pH
        - TDS
    - Menerima perintah kendali dari aplikasi:
        - Blower
        - Pompa air

### Diagram Sederhana

``` bash
[IoT Sensors] ---> Supabase DB ---> Android App (Dashboard)
[Android App] ---> Device Control ---> [IoT Devices]
```

## 🛠️ Teknologi yang Digunakan

- Kotlin + Jetpack Compose
- Supabase:
    - Auth
    - Postgres Database
    - Realtime
- Android Architecture Components
    - ViewModel
    - StateFlow / LiveData
- Ktor / Retrofit
- IoT Microcontroller


## 📥 Instalasi & Setup

1. Clone repository:

```bash
  git clone https://github.com/HaritsDetya/Melon_Monitoring_and_Automation.git
```
2. Buka di Android Studio (Hedgehog atau yang lebih baru)
3. Pastikan Anda menggunakan:
    - Compose Compiler terbaru
    - Min SDK 24
4. Tambahkan dependencies Supabase di build.gradle.
    
