# 🌿 Melon Monitoring and Automation

A comprehensive Android application for monitoring and automating greenhouse environments, built with modern Android development practices using Jetpack Compose and Supabase.

## ✨ Features

### 🏠 Greenhouse Management
- **Multi-Greenhouse Support** - Manage multiple greenhouse locations
- **Real-time Monitoring** - Live data from various sensors
- **User-specific Access** - Each user can manage their own greenhouses

### 📊 Sensor Monitoring
- **Temperature Tracking** - Real-time temperature monitoring with historical charts
- **Interactive Charts** - 24-hour historical data visualization
- **Sensor History** - Track sensor data over time with different time ranges

### ⚡ Smart Device Control
- **Blower/Fan Control** - Automated temperature control with manual override
- **Pump Management** - Control main pump and nutrient pump systems
- **Auto Mode** - Intelligent automation based on temperature thresholds
- **Real-time Device Sync** - Instant device status synchronization across devices

### 🔐 Secure Authentication
- **Email-based Login** - Secure authentication with Supabase Auth
- **User Profiles** - Personalized accounts with profile management
- **Session Management** - Automatic login with secure session handling

## 🛠 Tech Stack

### Frontend
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern declarative UI toolkit
- **Material Design 3** - Latest Material Design components
- **Android Architecture Components** - ViewModel, StateFlow, Coroutines

### Backend & Database
- **Supabase** - Backend-as-a-Service with PostgreSQL
- **PostgreSQL** - Relational database with Row Level Security
- **Supabase Auth** - Secure authentication service
- **Supabase Realtime** - Real-time subscriptions for live updates

### Architecture & Patterns
- **MVVM Architecture** - Model-View-ViewModel pattern
- **Repository Pattern** - Data abstraction layer
- **Dependency Injection** - Hilt for dependency management
- **Coroutines & Flow** - Asynchronous programming

## 🚀 Installation & Setup

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 21+
- Kotlin 1.9.0+

### Step 1: Clone the Repository
```bash
git clone https://github.com/your-username/melon-monitoring-android.git
cd melon-monitoring-android
