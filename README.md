# 🌱 Greenhouse Monitoring App

![Language](https://img.shields.io/badge/Language-Kotlin-purple) ![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green) ![Backend](https://img.shields.io/badge/Backend-Supabase-emerald) ![Status](https://img.shields.io/badge/Status-Development-orange)

Aplikasi Android berbasis **Jetpack Compose** yang terintegrasi dengan **Supabase** serta sistem IoT untuk monitoring kondisi greenhouse dan kontrol perangkat secara *real-time*.

## 📱 Screenshots

| Login / Auth | Dashboard Monitoring | Kontrol Perangkat | Detail History |
|:---:|:---:|:---:|:---:|
| ![Login](docs/login_preview.png) | ![Dashboard](docs/dashboard_preview.png) | ![Control](docs/control_preview.png) | ![History](docs/history_preview.png) |
> *Catatan: Screenshot aplikasi.*

## 📌 Deskripsi Proyek

Greenhouse Monitoring App dibangun untuk mempermudah petani hidroponik dalam memantau parameter vital tanaman dari jarak jauh. Aplikasi ini mengadopsi arsitektur modern (MVVM) dengan pendekatan UI deklaratif (Compose).

Sistem ini menjembatani komunikasi antara:
1.  **Pengguna (Android App)**: Interface pemantauan dan kontrol.
2.  **Server (Supabase)**: Pusat autentikasi, database, dan real-time engine.
3.  **Hardware (IoT)**: Sensor fisik dan aktuator di lapangan.

## 🚀 Fitur Utama

### 🔐 Autentikasi Pengguna
* **Sign Up & Login**: Menggunakan Supabase Auth (Email/Password).
* **Deep Link Reset Password**: Integrasi email untuk reset password yang aman.
* **Manajemen Akun**: Update password, logout, dan penghapusan akun.

### 📊 Dashboard Monitoring (Real-time)
Menampilkan data sensor terkini:
* 🌡️ **Lingkungan**: Suhu Udara & Kelembapan.
* 💧 **Kualitas Air**: Suhu Air, pH, dan TDS (Total Dissolved Solids).
* 📈 **History**: Grafik riwayat data sensor untuk analisis tren.

### 🔧 Kontrol Perangkat (Actuators)
Kontrol jarak jauh untuk perangkat keras:
* ✅ **Blower**: Mengatur sirkulasi udara.
* ✅ **Pompa Air**: Mengatur irigasi/sirkulasi air.

## 🏗️ Arsitektur Sistem

Aplikasi ini menggunakan pola **MVVM (Model-View-ViewModel)** dan **Clean Architecture** sederhana.

### Alur Data
```mermaid
graph TD
    User[📱 Android App] <-->|Auth & Data| SB[🔥 Supabase]
    SB <-->|Realtime MQTT/Rest| IoT[🤖 IoT Microcontroller]
    
    subgraph "Supabase Backend"
        Auth[Auth]
        DB[(PostgreSQL)]
        Realtime[Realtime Engine]
    end
    
    subgraph "IoT System"
        Sensors[Sensors: pH, TDS, Temp]
        Actuators[Relay: Pump, Blower]
    end
```

### Komponen Teknis
* Android Client:
    * UI: Jetpack Compose
    * State Management: ViewModel & StateFlow
    * Network: Ktor / Supabase-kt
    * DI: Hilt (Dagger)
* Supabase Backend:
    * Auth: Mengelola sesi pengguna.
    * Database: Menyimpan data users, devices, dan log sensor.
    * Realtime: Broadcast perubahan data sensor ke aplikasi instan.
