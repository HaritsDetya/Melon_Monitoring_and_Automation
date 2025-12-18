# 🚀 **Deployment Guide**

![Deployment](https://img.shields.io/badge/Deployment-Android%20Play%20Store-blue)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-green)
![Environment](https://img.shields.io/badge/Environment-3%20Stages-orange)

> **Panduan lengkap untuk deployment aplikasi Android ke berbagai environment**

## 📋 **Daftar Isi**
- [Prerequisites](#-prerequisites)
- [Environment Configuration](#-environment-configuration)
- [Build Configuration](#-build-configuration)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Play Store Deployment](#-play-store-deployment)
- [Monitoring](#-monitoring)
- [Rollback Procedure](#-rollback-procedure)
- [Troubleshooting](#-troubleshooting)

## 🛠️ **Prerequisites**

### **Tools Required**
1. **Android Studio** - Hedgehog atau versi lebih baru
2. **JDK 17** - Java Development Kit
3. **Git** - Version control system
4. **GitHub Account** - Untuk repository dan CI/CD
5. **Google Play Console Account** - Untuk publish aplikasi
6. **Supabase Account** - Untuk backend services

### **Development Environment**
```bash
# Verifikasi setup
java -version        # Should be JDK 17+
adb version          # Android Debug Bridge
git --version        # Git version
```

### **Keystore Setup**
```bash
# Generate keystore untuk signing
keytool -genkey -v -keystore greenhouse.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias greenhouse-key
```

## 🔧 **Environment Configuration**

### **Environment Variables**
Buat file `local.properties` di root project:
```properties
# Development
SUPABASE_URL=https://dev-project.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

# Production (jangan commit ke repository)
# SUPABASE_URL=https://prod-project.supabase.co
# SUPABASE_ANON_KEY=prod-key-here
```

### **Build Variants**
```text
┌─────────────────────────────────────┐
│          Build Variants             │
├─────────────┬───────────────────────┤
│   Flavor    │       Build Type      │
├─────────────┼───────────────────────┤
│   dev       │       debug           │
│   dev       │       release         │
│   staging   │       debug           │
│   staging   │       release         │
│   prod      │       debug           │
│   prod      │       release         │
└─────────────┴───────────────────────┘
```

### **Gradle Configuration**
```groovy
// app/build.gradle.kts
android {
    defaultConfig {
        applicationId "com.greenhouse.monitoring"
        versionCode 1
        versionName "1.0.0"
    }
    
    signingConfigs {
        create("release") {
            storeFile = file("../keystore/greenhouse.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Greenhouse Dev")
        }
        
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            resValue("string", "app_name", "Greenhouse Staging")
        }
        
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Greenhouse Monitoring")
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

## 🏗️ **Build Configuration**

### **Build Scripts**
```bash
# Build development debug
./gradlew assembleDevDebug

# Build staging release
./gradlew assembleStagingRelease

# Build production release
./gradlew assembleProdRelease

# Run tests
./gradlew test

# Generate APK/AAB
./gradlew bundleProdRelease  # AAB for Play Store
./gradlew assembleProdRelease  # APK for manual install
```

### **Proguard Rules**
```proguard
# app/proguard-rules.pro
-keep class com.greenhouse.monitoring.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class kotlin.coroutines.** { *; }

# Supabase
-keep class io.github.jan-tennert.supabase.** { *; }

# Hilt
-keep class * extends dagger.hilt.internal.aggregatedroot.codegen.** { *; }
```

### **Version Management**
```kotlin
// version.gradle.kts
object Versions {
    const val versionMajor = 1
    const val versionMinor = 0
    const val versionPatch = 0
    const val versionBuild = 1  // Increment for each build
    
    fun getVersionCode(): Int {
        return versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
    }
    
    fun getVersionName(): String {
        return "$versionMajor.$versionMinor.$versionPatch"
    }
}
```

## 🔄 **CI/CD Pipeline**

### **GitHub Actions Workflow**
```yaml
# .github/workflows/android-ci-cd.yml
name: Android CI/CD

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v2
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: app/build/reports/tests/

  build-dev:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Setup JDK 17
        uses: actions/setup-java@v3
      
      - name: Build APK
        run: ./gradlew assembleDevRelease
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-dev-release
          path: app/build/outputs/apk/dev/release/

  build-prod:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    
    env:
      STORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
      KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
      KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
    
    steps:
      - name: Checkout
        uses: actions/checkout@v3
      
      - name: Setup JDK 17
        uses: actions/setup-java@v3
      
      - name: Build AAB
        run: ./gradlew bundleProdRelease
      
      - name: Upload AAB
        uses: actions/upload-artifact@v3
        with:
          name: app-prod-release
          path: app/build/outputs/bundle/prodRelease/
```

### **Secrets Management**
```bash
# GitHub Secrets yang diperlukan
STORE_PASSWORD           # Keystore password
KEY_ALIAS                # Key alias
KEY_PASSWORD             # Key password
SUPABASE_URL_PROD        # Production Supabase URL
SUPABASE_ANON_KEY_PROD   # Production Supabase key
FIREBASE_SERVICE_ACCOUNT # Firebase service account JSON
```

## 📱 **Play Store Deployment**

### **Prerequisites**
1. Google Play Console Account - Sudah terdaftar
2. Developer Account - Sudah membayar fee $25
3. App Signing - Sudah setup app signing by Google Play
4. Store Listing - Assets siap (icon, screenshots, description)

### **Release Process**
```text
1. Prepare Release
   ├── Update versionCode dan versionName
   ├── Update changelog
   ├── Run semua tests
   └── Build AAB file

2. Internal Testing
   ├── Upload ke Internal Testing track
   ├── Add testers
   └── Monitor crash reports

3. Closed Testing
   ├── Promote ke Closed Testing
   ├── Add beta testers
   └── Collect feedback

4. Open Testing
   ├── Promote ke Open Testing
   ├── Monitor analytics
   └── Fix critical bugs

5. Production Release
   ├── Promote ke Production
   ├── Rollout bertahap (10% → 50% → 100%)
   └── Monitor performance
```

### **AAB Upload Process**
```bash
# Generate signed AAB
./gradlew bundleProdRelease

# Upload ke Play Console
# 1. Buka Google Play Console
# 2. Pilih aplikasi
# 3. Production → Create new release
# 4. Upload AAB file
# 5. Isi release notes
# 6. Review dan publish
```

### **Release Notes Template**
```markdown
## What's New in v1.2.0

### 🚀 New Features
- Added real-time sensor charts
- Improved greenhouse management
- Added dark mode support

### 🐛 Bug Fixes
- Fixed crash on login screen
- Improved data synchronization
- Fixed notification issues

### ⚡ Performance Improvements
- Reduced app size by 15%
- Improved loading times
- Optimized battery usage

### 📱 Compatibility
- Android 8.0+ (API 24)
- Tablet support improved
- Better accessibility features
```

## 📊 **Monitoring**

### **Crash Reporting**
```kotlin
// Firebase Crashlytics setup
class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            setCustomKey("version", BuildConfig.VERSION_NAME)
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        }
    }
}
```

### **Analytics Setup**
```kotlin
// Firebase Analytics
class AnalyticsManager {
    
    fun logScreenView(screenName: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }
    
    fun logSensorReading(sensorType: String, value: Double) {
        Firebase.analytics.logEvent("sensor_reading") {
            param("sensor_type", sensorType)
            param("value", value.toString())
        }
    }
}
```

### **Performance Monitoring**
```kotlin
// Firebase Performance
class PerformanceTracker {
    
    fun trackScreenLoad(screenName: String) {
        val trace = Firebase.performance.newTrace("screen_load_$screenName")
        trace.start()
        
        // ... screen loading logic
        
        trace.stop()
    }
    
    fun trackApiCall(endpoint: String) {
        val trace = Firebase.performance.newTrace("api_$endpoint")
        trace.start()
        
        // ... API call
        
        trace.putAttribute("status", "success")
        trace.stop()
    }
}
```

## 🔙 **Rollback Procedure**

### **Emergency Rollback Steps**
```text
1. Identify Issue
   ├── Monitor crash reports
   ├── Check user feedback
   └── Review analytics

2. Decision Making
   ├── Severity assessment
   ├── Impact analysis
   └── Rollback decision

3. Execute Rollback
   ├── Play Console → Production
   ├── Halt rollout (if in progress)
   ├── Revert to previous version
   └── Notify users

4. Post Mortem
   ├── Root cause analysis
   ├── Fix issue
   ├── Test thoroughly
   └── Schedule new release
```

### **Rollback Checklist**
* Backup current database state
* Notify stakeholders
* Update support team
* Prepare communication for users
* Schedule downtime if needed
* Document rollback process

## 🐛 **Troubleshooting**

### **Common Build Issues**
```bash
# Error: Keystore not found
# Solution: Set correct keystore path in signingConfigs

# Error: Minimum SDK version mismatch
# Solution: Update minSdkVersion in build.gradle

# Error: Dependency conflict
# Solution: Use ./gradlew app:dependencies to check
```

### **Play Store Rejection Issues**
|              Issue               |                	Solution                 |
|:--------------------------------:|:----------------------------------------:|
|      Privacy Policy missing      |         	Add privacy policy link         |
| App not compliant with policies	 |        Review Play Store policies        |
|         Poor performance         |   	Optimize app size and battery usage   |
|    Security vulnerabilities	     | Update dependencies, fix security issues |

### **Network Issues**
```kotlin
// Network connectivity check
class NetworkMonitor {
    
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
        
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
```

## 📝 **Release Checklist**

### **Pre-Release Checklist**
* All tests passing
* Code review completed
* Version numbers updated
* Changelog updated
* Documentation updated
* Backend compatibility verified
* Database migrations tested

### **Post-Release Checklist**
* Monitor crash reports
* Check analytics
* Verify user feedback
* Update support documentation
* Schedule next release planning

___
**Deployment Version**: 1.0.0
**Last Updated**: December 2025
