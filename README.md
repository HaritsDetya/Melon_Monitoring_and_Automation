# 🌱 Greenhouse Monitoring App

![Language](https://img.shields.io/badge/Language-Kotlin-purple) 
![UI](https://img.shields.io/badge/UI-Jetpack_Compose-green) 
![Backend](https://img.shields.io/badge/Backend-Supabase-emerald)
![Platform](https://img.shields.io/badge/Platform-Android-blue)
![Status](https://img.shields.io/badge/Status-Development-orange)

> **Aplikasi Android untuk monitoring dan kontrol greenhouse hidroponik secara real-time**

## 📱 Screenshots

|             Login / Auth             |             Dashboard Monitoring             |             Kontrol Perangkat            |              Detail History              |
|:------------------------------------:|:--------------------------------------------:|:----------------------------------------:|:----------------------------------------:|
| ![Login](docs/screenshots/login.png) | ![Dashboard](docs/screenshots/dashboard.png) | ![Control](docs/screenshots/control.png) | ![History](docs/screenshots/history.png) |

## 📌 Deskripsi Proyek

Greenhouse Monitoring App dibangun untuk mempermudah petani hidroponik dalam memantau parameter vital tanaman dari jarak jauh. Aplikasi ini mengadopsi arsitektur modern (MVVM) dengan pendekatan UI deklaratif (Compose).

Sistem ini menjembatani komunikasi antara:
1.  **Pengguna (Android App)**: Interface pemantauan dan kontrol.
2.  **Server (Supabase)**: Pusat autentikasi, database, dan real-time engine.
3.  **Hardware (IoT)**: Sensor fisik dan aktuator di lapangan.

## 🎯 Fitur Utama

### 🔐 **Autentikasi & Keamanan**
- ✅ Registrasi pengguna baru dengan email/password
- ✅ Login dengan kredensial aman
- ✅ Reset password via email
- ✅ Update profil pengguna
- ✅ Penghapusan akun beserta semua data terkait

### 📊 **Monitoring Real-time**
- 🌡️ **Suhu Udara**: Monitoring suhu lingkungan greenhouse
- 💧 **Kelembapan**: Tingkat kelembapan udara
- 🔋 **pH Air**: Tingkat keasaman air hidroponik
- ⚡ **TDS**: Total Dissolved Solids (nutrisi air)
- 🌊 **Suhu Air**: Suhu air dalam sistem hidroponik
- 📈 **Grafik History**: Visualisasi data historis 24 jam/7 hari/30 hari

### 🔧 **Kontrol Perangkat**
- 🌀 **Kipas (Fan)**: Kontrol sirkulasi udara ON/OFF
- 💦 **Pompa Air**: Kontrol irigasi air ON/OFF
- 🤖 **Mode Otomatis**: Sistem berjalan otomatis berdasarkan setting

## 🏗️ **Arsitektur Teknis**

Aplikasi ini menggunakan pola **MVVM (Model-View-ViewModel)** dan **Clean Architecture** sederhana.

### Tech Stack
```yaml
Frontend:
  Language: Kotlin
  UI Framework: Jetpack Compose
  Architecture: MVVM + Clean Architecture
  DI: Dagger Hilt
  Async: Coroutines + Flow
  Navigation: Jetpack Navigation Compose
  Database Local: Room Database

Backend:
  Platform: Supabase
  Database: PostgreSQL
  Auth: Supabase Auth
  Realtime: Supabase Realtime
  Storage: Supabase Storage
  Functions: Edge Functions

IoT Integration:
  Protocol: HTTP REST + MQTT
  Microcontroller: ESP32/Arduino
  Sensors: DHT22, pH Sensor, TDS Sensor
  Actuators: Relay Module
```

### Diagram Arsitektur
```graph TB
    subgraph "Mobile Application"
        UI[UI Layer - Compose]
        VM[ViewModel Layer]
        REPO[Repository Layer]
        LOCAL[Local Database]
    end
    
    subgraph "Supabase Backend"
        AUTH[Authentication]
        DB[(PostgreSQL Database)]
        RT[Realtime Engine]
        FUNC[Edge Functions]
        STORAGE[File Storage]
    end
    
    subgraph "IoT Ecosystem"
        DEVICE[IoT Device]
        SENSORS[Physical Sensors]
        ACTUATORS[Relay Controls]
    end
    
    UI --> VM
    VM --> REPO
    REPO --> LOCAL
    REPO -->|HTTP/REST| DB
    REPO -->|WebSocket| RT
    REPO -->|Auth| AUTH
    DB -->|Triggers| FUNC
    DEVICE -->|HTTP POST| DB
    DB -->|Realtime| DEVICE
    DEVICE --> SENSORS
    DEVICE --> ACTUATORS
```

## 🚀 **Quick Start**
### Prerequisites
- Android Studio Hedgehog (2023.1.1) atau versi lebih baru
- JDK 17 atau lebih baru
- Akun Supabase (gratis)
- Perangkat Android atau Emulator (API 24+)

### Langkah 1: Clone Repository
``` bash
git clone https://github.com/HaritsDetya/Melon_Monitoring_and_Automation.git
cd Melon_Monitoring_and_Automation
```

### Langkah 2: Setup Supabase
1. Buat project baru di supabase.com
2. Copy URL dan Anon Key dari Project Settings → API
3. Buat file local.properties di root project:
    ```properties
    # Supabase Configuration
    SUPABASE_URL=https://your-project.supabase.co
    SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    
    # Optional: Untuk Edge Functions
    ADMIN_DELETE_SECRET=your-secret-for-delete-function
    ```
### Langkah 3: Setup Database
Jalankan script SQL berikut di Supabase SQL Editor:
```sql
-- 1. Jalankan semua CREATE TABLE statements
-- 2. Jalankan semua CREATE FUNCTION statements  
-- 3. Enable RLS dan setup policies
-- 4. Deploy Edge Functions
```

### Langkah 4: Build & Run
1. Buka project di Android Studio
2. Tunggu Gradle sync selesai
3. Pilih device/emulator
4. Klik Run 'app' (Shift + F10)

## 📁 **Project Structure**
```text
app/
├── src/main/
│   ├── java/com/yourpackage/
│   │   ├── data/                   # Data layer
│   │   │   ├── local/             # Room database
│   │   │   ├── remote/            # Supabase API
│   │   │   └── repository/        # Repository implementations
│   │   ├── domain/                # Domain layer
│   │   │   ├── model/             # Business models
│   │   │   ├── repository/        # Repository interfaces
│   │   │   └── usecase/           # Use cases
│   │   ├── presentation/          # Presentation layer
│   │   │   ├── screen/           # Composable screens
│   │   │   ├── component/        # Reusable components
│   │   │   ├── viewmodel/        # ViewModels
│   │   │   └── theme/            # UI theming
│   │   └── di/                   # Dependency injection
│   └── res/                      # Resources
├── build.gradle.kts             # Module build config
└── proguard-rules.pro          # Proguard rules

docs/                           # Dokumentasi
├── screenshots/               # App screenshots
├── diagrams/                  # Architecture diagrams
└── api/                       # API documentation

supabase/                      # Backend configuration
├── migrations/               # Database migrations
├── functions/               # Edge functions
└── seeds/                   # Seed data
```

## 📚 **Dokumentasi Terkait**

|          Dokumen           |                        Deskripsi                        |        Target Pembaca        |
|:--------------------------:|:-------------------------------------------------------:|:----------------------------:|
| [DATABASE.md](DATABASE.md) | 	Dokumentasi teknis database Supabase lengkap           | Backend Dev, Database Admin  |
|      API_REFERENCE.md      |           API endpoints dan payload examples            |  Mobile Dev, IoT Developer   |
|      ARCHITECTURE.md       |               	Diagram arsitektur detail             |     Tech Lead, Architect     |
|       DEPLOYMENT.md        |            Panduan deployment ke Play Store             |   DevOps, Release Manager    |
|         TESTING.md         |                 	Panduan testing dan QA                 |     QA Engineer, Tester      |

## 🤝 **Kontribusi**
Cara Berkontribusi
1. Fork repository ini
2. Buat branch untuk fitur baru:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. Commit perubahan:
   ```bash
   git commit -m 'feat: add amazing feature'
   ```
4. Push ke branch :
   ```bash
   git push origin feature/amazing-feature
   ```
5. Buat Pull Request.

## **Coding Standards**
- Gunakan Kotlin dengan style official
- Architecture: Ikuti MVVM + Clean Architecture
- Documentation: Update README jika ada perubahan besar

## **Issue Labels**
- `bug` - Sesuatu tidak berfungsi
- `enhancement` - Perbaikan fitur yang ada
- `feature` - Fitur baru
- `documentation` - Perbaikan dokumentasi
- `help wanted` - Butuh bantuan komunitas

## 📄 **Lisensi**
```text
MIT License

Copyright (c) 2024 Harits

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONTRACT WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📊 Project Stats:
- 📱 **Version**: 1.0.0 (Beta)
- 🏗️ **Architecture**: MVVM + Clean Architecture
- 🔄 **Last Updated**: December 2025
- 👥 **Contributors**: 1
