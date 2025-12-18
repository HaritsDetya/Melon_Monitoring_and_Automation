# 🗄️ **Supabase Database Documentation**

![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue)
![RLS](https://img.shields.io/badge/Row%20Level%20Security-Enabled-green)
![Triggers](https://img.shields.io/badge/Triggers-2%20active-orange)
![Functions](https://img.shields.io/badge/Functions-6%20defined-yellow)
![Edge Functions](https://img.shields.io/badge/Edge%20Functions-2%20deployed-purple)

> **Dokumentasi teknis lengkap untuk database Supabase - Greenhouse Monitoring System**

## 📋 **Quick Navigation**

- [🔙 Kembali ke README](README.md)
- [🏗️ Schema Overview](#-schema-overview)
- [📊 Tabel Structure](#-tabel-structure)
- [🔗 Relationships](#-relationships-erd)
- [⚙️ Functions](#-database-functions)
- [🔐 RLS Policies](#-row-level-security-policies)
- [🎯 Triggers](#-database-triggers)
- [🌐 Edge Functions](#-edge-functions)
- [🚀 Setup Guide](#-setup-guide)

## 🎯 **Database Overview**

### Spesifikasi Teknis
```yaml
Database: PostgreSQL 15+
Total Tables: 7
Total Rows: ~10,000 (estimated)
Storage: ~500MB (initial)
Backup: Daily automatic
Region: Singapore (nearest to Indonesia)
Performance: 2GB RAM, 1 vCPU (Supabase Free Tier)
Extensions: pg_net, pg_cron, pg_stat_statements
```

### Data Flow Architecture

```flowchart TD
A[IoT Device] -->|HTTP POST| B[sensor_readings]
B -->|Trigger| C[Edge Function]
C --> D{sensor_history}
D --> E[TEMPERATURE]
D --> F[HUMIDITY]
D --> G[WATER_TEMP]
D --> H[PH]
D --> I[TDS]

    J[Mobile App] -->|Query| K[All Tables]
    J -->|Subscribe| L[Realtime Changes]
    
    M[Admin Panel] -->|Manage| N[users, greenhouses]
```

## 📊 **Tabel Structure**

1. `users` - Tabel Pengguna

   **Purpose**: Menyimpan data profil pengguna (link dengan Supabase Auth)
   
   ```sql
   CREATE TABLE public.users (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     username TEXT NOT NULL,
     email TEXT NOT NULL UNIQUE,
     phone_number TEXT NULL,
     created_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```
   
   **Field Details**:
   
   |    Column    |    	Type     | Required |      	Default      |             	Description             |
   |:------------:|:------------:|:--------:|:------------------:|:------------------------------------:|
   |      id      |     UUID     |    	✅    | 	gen_random_uuid() | 	Primary key, sync dengan auth.users |
   |   username   |     TEXT     |    	✅    |         	-         |       	Nama tampilan pengguna        |
   |    email     |     TEXT     |    	✅    |         	-         |       	Email unik untuk login        |
   | phone_number |    TEXT	     |    ❌     |       	NULL        |       	Nomor telepon opsional        |
   |  created_at  | TIMESTAMPTZ  |    ❌     |       	NOW()       |        	Timestamp registrasi         |
   
   **Indexes**:
   
   ```sql
   CREATE INDEX idx_users_email ON users(email);
   CREATE INDEX idx_users_created_at ON users(created_at DESC);
   ```
   
   > **Note**: Tabel ini HARUS disinkronisasi dengan auth.users. Saat user register di Auth, trigger harus membuat row di tabel ini.

2. `greenhouses` - Data Greenhouse

   **Purpose**: Setiap greenhouse yang dimiliki/dikelola user
   
   ```sql
   CREATE TABLE public.greenhouses (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     name TEXT NOT NULL,
     location TEXT NOT NULL,
     owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
     description TEXT NULL,
     created_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```
   
   **Field Details**:
   
   |    Column     |     	Type     |  	Required  |     	Constraint     |                	Description                 |
   |:-------------:|:-------------:|:-----------:|:-------------------:|:-------------------------------------------:|
   |      id       |     	UUID     |     	✅      |    	PRIMARY KEY     |            	Unique greenhouse ID            |
   |     name      |     	TEXT     |     	✅      |         	-          | 	Nama greenhouse (contoh: "Kebun Melon A")  |
   |   location    |     	TEXT     |     	✅      |         	-          |            	Alamat/lokasi fisik             |
   |   owner_id    |     	UUID     |     	❌      |    	FOREIGN KEY     |             	Pemilik greenhouse             |
   |  description  |     	TEXT     |     	❌      |         	-          |             	Deskripsi tambahan             |
   |  created_at   | 	TIMESTAMPTZ  |     	❌      |   	DEFAULT NOW()    |              	Waktu pembuatan               |

   **Business Rules**:

   * Satu user bisa memiliki banyak greenhouse
   * Saat user dihapus, semua greenhouse miliknya ikut terhapus (CASCADE)
   * Nama greenhouse harus unik per user (validasi di aplikasi)

3. `sensor_readings` - Pembacaan Sensor Terkini

   **Purpose**: Menyimpan data sensor terbaru untuk fast query
   
   ```sql
   CREATE TABLE public.sensor_readings (
   id uuid NOT NULL DEFAULT gen_random_uuid(),
   greenhouse_id uuid NULL,
   temperature double precision NULL,
   humidity double precision NULL,
   water_temp double precision NULL,
   ph double precision NULL,
   tds double precision NULL,
   recorded_at timestamp with time zone NULL DEFAULT now(),
   device_id character varying(255) NULL,
   CONSTRAINT sensor_readings_pkey1 PRIMARY KEY (id),
   CONSTRAINT fk_sensor_device FOREIGN KEY (device_id) REFERENCES iot_devices (device_id) ON DELETE SET NULL,
   CONSTRAINT sensor_readings_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE
   );
   
   -- Indexes
   CREATE INDEX IF NOT EXISTS idx_sensor_readings_device_id ON public.sensor_readings USING btree (device_id);
   CREATE INDEX IF NOT EXISTS idx_greenhouse_id_readings ON public.sensor_readings USING btree (greenhouse_id);
   ```
   
   **Sensor Specifications**:
   
   |  Parameter   |   	Range    |    	Unit     |     	Sensor Type     | 	Accuracy  |
   |:------------:|:-----------:|:------------:|:--------------------:|:----------:|
   | temperature  | 	-40 to 80  |     	°C      |        	DHT22        |  	±0.5°C   |
   |  humidity	   |   0-100	    |      %	      |        DHT22	        |    ±2%     |
   |  water_temp  |   	0-100    |     	°C      |       	DS18B20       |  	±0.5°C   |
   |     ph	      |    0-14	    |     pH	      |      PH-4502C	       |    ±0.1    |
   |     tds      |   	0-1000   |     	ppm     |      	TDS Meter      |    	±5%    |
   
   **Performance Optimizations**:

   ```sql
   -- Index untuk query berdasarkan greenhouse
   CREATE INDEX idx_greenhouse_id_readings ON sensor_readings(greenhouse_id);
   
   -- Index untuk query data terbaru
   CREATE INDEX idx_readings_recorded ON sensor_readings(recorded_at DESC);
   
   -- Retention policy (opsional)
   -- DELETE FROM sensor_readings WHERE recorded_at < NOW() - INTERVAL '7 days';
   ```

4. `sensor_history` - Riwayat Time-series

   **Purpose**: Penyimpanan optimasi untuk data historis (grafik, analytics)
   
   ```sql
   CREATE TABLE public.sensor_history (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     greenhouse_id UUID REFERENCES greenhouses(id) ON DELETE CASCADE,
     sensor_type TEXT NOT NULL CHECK (
       sensor_type IN ('TEMPERATURE', 'HUMIDITY', 'WATER_TEMPERATURE', 'PH', 'TDS')
     ),
     value DOUBLE PRECISION NOT NULL,
     recorded_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```
   
   **Table Partitioning Strategy (Recommended untuk production)**:
   
   ```sql
   -- Create monthly partitions untuk performa
   CREATE TABLE sensor_history_y2024m11 PARTITION OF sensor_history
   FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');
   
   -- Index untuk setiap partition
   CREATE INDEX idx_history_y2024m11_greenhouse ON sensor_history_y2024m11(greenhouse_id);
   CREATE INDEX idx_history_y2024m11_type ON sensor_history_y2024m11(sensor_type);
   CREATE INDEX idx_history_y2024m11_time ON sensor_history_y2024m11(recorded_at);
   ```
   
   **Indexes for Analytics Queries**:
   
   ```sql
   -- Query: Get temperature history for last 24 hours
   CREATE INDEX idx_sensor_history_greenhouse_type ON sensor_history(greenhouse_id, sensor_type);
   
   -- Query: Get all sensors at specific time range  
   CREATE INDEX idx_sensor_history_recorded_at ON sensor_history(recorded_at);
   
   -- Query: Dashboard aggregated data
   CREATE INDEX idx_sensor_history_greenhouse_recorded ON sensor_history(greenhouse_id, recorded_at);
   ```

5. `iot_devices` - Manajemen Perangkat IoT

   **Purpose**: Registry dan pairing perangkat IoT
   
   ```sql
   CREATE TABLE public.iot_devices (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     device_id VARCHAR(50) UNIQUE NOT NULL,      -- MAC address atau ID unik
     serial_number VARCHAR(100) UNIQUE NOT NULL, -- Serial number hardware
     device_name VARCHAR(100) NULL,              -- Nama custom
     device_type VARCHAR(50) DEFAULT 'HYDROPONIC_SENSOR',
     greenhouse_id UUID REFERENCES greenhouses(id),
     pairing_code VARCHAR(6) NOT NULL,           -- Kode 6 digit untuk pairing
     is_paired BOOLEAN DEFAULT FALSE,
     paired_at TIMESTAMPTZ NULL,
     created_at TIMESTAMPTZ DEFAULT NOW(),
     last_seen TIMESTAMPTZ NULL,                 -- Last heartbeat
     firmware_version VARCHAR(20) NULL,
     encryption_key VARCHAR(100) NULL            -- Untuk komunikasi encrypted
   );
   ```
   
   **Device Pairing Flow**:
   
   ```text
   1. Device dibuat di database dengan is_paired = false
   2. Device menampilkan pairing_code di LCD/serial
   3. User masukkkan kode di aplikasi
   4. Aplikasi call API untuk pair device dengan greenhouse
   5. System update: is_paired = true, paired_at = NOW(), greenhouse_id = [id]
   6. Device bisa mulai mengirim data
   ```

   **Security Notes**:

   * encryption_key digunakan untuk encrypt data antara device-server
   * last_seen untuk monitoring device health
   * pairing_code expired setelah 10 menit (validasi di aplikasi)

6. `control_devices` - Status Kontrol Aktuator

   **Purpose**: State management untuk perangkat kontrol (relay)
   
   ```sql
   CREATE TABLE public.control_devices (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     greenhouse_id UUID UNIQUE REFERENCES greenhouses(id) ON DELETE CASCADE,
     fan BOOLEAN DEFAULT FALSE,      -- Kipas ON/OFF
     pump BOOLEAN DEFAULT FALSE,     -- Pompa air ON/OFF
     auto_mode BOOLEAN DEFAULT FALSE, -- Mode otomatis ON/OFF
     updated_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```
   
   **Relay Specifications**:
   
   | Device | 	GPIO Pin | 	Current Rating |        	Purpose        |
   |:------:|:---------:|:---------------:|:----------------------:|
   |  Fan   |  	GPIO12  |      	10A       |    	Sirkulasi udara    |
   |  Pump  |  	GPIO13  |       	5A       | 	Sirkulasi air nutrisi |
   
   **State Transition Logic**:
   
   ```sql
   -- When auto_mode = TRUE:
   -- - Fan ON when temperature > max_temperature
   -- - Pump ON when TDS < threshold
   -- - Both OFF when conditions normal
   
   -- When auto_mode = FALSE:
   -- - Manual control via app
   -- - States persist until changed
   ```

7. `automation_settings` - Pengaturan Otomatisasi

   **Purpose**: Configuration untuk logic otomatis
   
   ```sql
   CREATE TABLE public.automation_settings (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     greenhouse_id UUID UNIQUE REFERENCES greenhouses(id) ON DELETE CASCADE,
     max_temperature DOUBLE PRECISION DEFAULT 38.0,   -- °C
     min_temperature DOUBLE PRECISION DEFAULT 25.0,   -- °C  
     nutrient_droplets INTEGER DEFAULT 10,            -- ml per hour
     updated_at TIMESTAMPTZ DEFAULT NOW()
   );
   ```
   
   **Default Values (Optimal for Hydroponics)**:
   
   |     Parameter      | 	Default  |         	Min         | 	Max |  	Unit  |          	Description           |
   |:------------------:|:---------:|:--------------------:|:----:|:-------:|:-------------------------------:|
   |  max_temperature	  |   38.0	   |         20	          | 45	  |   °C	   | Fan akan ON jika suhu melebihi  |
   |  min_temperature   |   	25.0   |         	15          | 	35  |   	°C   | 	Fan akan OFF jika suhu dibawah |
   | nutrient_droplets	 |    10	    |          1	          | 50	  | ml/hour |     	Rate pemberian nutrisi     |

## 🔗 **Relationships (ERD)**
   
   ```mermaid
    erDiagram
       users ||--o{ greenhouses : owns
       users ||--o{ greenhouse_members : "member of"
       
       greenhouses ||--o{ sensor_readings : "has current"
       greenhouses ||--o{ sensor_history : "has history"
       greenhouses ||--|| iot_devices : "contains"
       greenhouses ||--|| control_devices : "controls"
       greenhouses ||--|| automation_settings : "configures"
       
       sensor_readings ||--o{ sensor_history : "archived to"
       
       users {
           uuid id PK
           string username
           string email UK
           timestamp created_at
       }
       
       greenhouses {
           uuid id PK
           string name
           uuid owner_id FK
           timestamp created_at
       }
       
       sensor_readings {
           uuid id PK
           uuid greenhouse_id FK
           float temperature
           float humidity
           float water_temp
           float ph
           float tds
           timestamp recorded_at
       }
       
       sensor_history {
           uuid id PK
           uuid greenhouse_id FK
           string sensor_type
           float value
           timestamp recorded_at
       }
       
       iot_devices {
           uuid id PK
           string device_id UK
           string serial_number UK
           uuid greenhouse_id FK
           string pairing_code
           boolean is_paired
           timestamp paired_at
       }
       
       control_devices {
           uuid id PK
           uuid greenhouse_id FK
           boolean fan
           boolean pump
           boolean auto_mode
           timestamp updated_at
       }
       
       automation_settings {
           uuid id PK
           uuid greenhouse_id FK
           float max_temperature
           float min_temperature
           integer nutrient_droplets
           timestamp updated_at
       }
   ```
   
   **Cardinality Rules**:
   
   * 1 User → N Greenhouses (One-to-Many)
     * 1 Greenhouse → 1 Control Device (One-to-One)
     * 1 Greenhouse → 1 Automation Settings (One-to-One)
     * 1 Greenhouse → N IoT Devices (One-to-Many)
     * 1 Greenhouse → N Sensor Readings (One-to-Many)
     * 1 Greenhouse → N Sensor History (One-to-Many)

## ⚙️ **Database Functions**

### 1. `create_greenhouse_default_data()`

**Type:** Trigger Function  
**Called By:** `after_greenhouse_created` trigger
    
```sql
CREATE OR REPLACE FUNCTION create_greenhouse_default_data()
   RETURNS TRIGGER AS $$
   BEGIN
       -- 1. Create default automation settings
       INSERT INTO automation_settings (
           id, greenhouse_id, max_temperature, 
           min_temperature, nutrient_droplets, updated_at
       ) VALUES (
           gen_random_uuid(), NEW.id, 38.0, 25.0, 10, NOW()
       );
   
       -- 2. Create default control device state
       INSERT INTO control_devices (
           id, greenhouse_id, fan, pump, auto_mode, updated_at
       ) VALUES (
           gen_random_uuid(), NEW.id, FALSE, FALSE, FALSE, NOW()
       );
   
       RETURN NEW;
   END;
   $$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 2. `copy_to_history_native()`

**Type**: Trigger Function
**Called By**: `on_sensor_reading_inserted_native` trigger
   
```sql
CREATE OR REPLACE FUNCTION copy_to_history_native()
RETURNS TRIGGER AS $$
BEGIN
  -- Cek dulu: Pastikan greenhouse_id TIDAK NULL agar tidak error constraint
  IF NEW.greenhouse_id IS NULL THEN
     -- Opsional: Anda bisa raise warning atau biarkan saja (data history tidak tercatat)
     RETURN NEW; 
  END IF;

  -- 1. Insert Temperature
  IF NEW.temperature IS NOT NULL THEN
    INSERT INTO sensor_history (greenhouse_id, sensor_type, value, recorded_at)
    VALUES (NEW.greenhouse_id, 'TEMPERATURE', NEW.temperature, NEW.recorded_at);
  END IF;

  -- 2. Insert Humidity
  IF NEW.humidity IS NOT NULL THEN
    INSERT INTO sensor_history (greenhouse_id, sensor_type, value, recorded_at)
    VALUES (NEW.greenhouse_id, 'HUMIDITY', NEW.humidity, NEW.recorded_at);
  END IF;

  -- 3. Insert Water Temp
  IF NEW.water_temp IS NOT NULL THEN
    INSERT INTO sensor_history (greenhouse_id, sensor_type, value, recorded_at)
    VALUES (NEW.greenhouse_id, 'WATER_TEMPERATURE', NEW.water_temp, NEW.recorded_at);
  END IF;

  -- 4. Insert pH
  IF NEW.ph IS NOT NULL THEN
    INSERT INTO sensor_history (greenhouse_id, sensor_type, value, recorded_at)
    VALUES (NEW.greenhouse_id, 'PH', NEW.ph, NEW.recorded_at);
  END IF;

  -- 5. Insert TDS
  IF NEW.tds IS NOT NULL THEN
    INSERT INTO sensor_history (greenhouse_id, sensor_type, value, recorded_at)
    VALUES (NEW.greenhouse_id, 'TDS', NEW.tds, NEW.recorded_at);
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### 3. `delete_user_completely(user_id UUID)`

**Type**: Stored Procedure
**Purpose**: Delete cascade semua data user
   
   ```sql
   CREATE OR REPLACE FUNCTION delete_user_completely(user_id UUID)
   RETURNS VOID AS $$
   BEGIN
       -- Delete dalam urutan yang benar (child ke parent)
       DELETE FROM sensor_readings 
       WHERE greenhouse_id IN (
           SELECT id FROM greenhouses WHERE owner_id = user_id
       );
       
       DELETE FROM sensor_history 
       WHERE greenhouse_id IN (
           SELECT id FROM greenhouses WHERE owner_id = user_id
       );
       
       DELETE FROM control_devices 
       WHERE greenhouse_id IN (
           SELECT id FROM greenhouses WHERE owner_id = user_id
       );
       
       DELETE FROM automation_settings 
       WHERE greenhouse_id IN (
           SELECT id FROM greenhouses WHERE owner_id = user_id
       );
       
       DELETE FROM iot_devices 
       WHERE greenhouse_id IN (
           SELECT id FROM greenhouses WHERE owner_id = user_id
       );
       
       DELETE FROM greenhouses WHERE owner_id = user_id;
       DELETE FROM users WHERE id = user_id;
   END;
   $$ LANGUAGE plpgsql SECURITY DEFINER;
   ```
   
   **Execution**:
   
   ```sql
   -- Call dari Edge Function
   SELECT delete_user_completely('user-uuid-here');
   
   -- Atau via REST API
   POST /rest/v1/rpc/delete_user_completely
   {
       "user_id": "uuid-here"
   }
   ```

## 🔐 **Row Level Security Policies**

**Activation & Principles**
```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE greenhouses ENABLE ROW LEVEL SECURITY;
-- ... repeat for all tables

-- Principles:
-- 1. Users can only access their own data
-- 2. Service role has full access
-- 3. Public access only for specific operations
```

**Users Table Policies**
```sql
-- Policy 1: Users can view their own profile
CREATE POLICY "Users can view own profile" ON users
FOR SELECT USING (auth.uid() = id);

-- Policy 2: Users can insert their own profile  
CREATE POLICY "Users can insert own profile" ON users
FOR INSERT WITH CHECK (auth.uid() = id);

-- Policy 3: Users can update their own profile
CREATE POLICY "Users can update own profile" ON users
FOR UPDATE USING (auth.uid() = id);

-- Policy 4: Users can delete their own profile
CREATE POLICY "Users can delete own profile" ON users
FOR DELETE USING (auth.uid() = id);

-- Policy 5: Service role full access
CREATE POLICY "Service role full access" ON users
FOR ALL USING (auth.role() = 'service_role');
```

**Greenhouses Table Policies**
```sql
-- View: Only owner can see their greenhouses
CREATE POLICY "Users can view own greenhouses" ON greenhouses
FOR SELECT USING (auth.uid() = owner_id);

-- Insert: Users can create greenhouses for themselves
CREATE POLICY "Users can insert own greenhouses" ON greenhouses
FOR INSERT WITH CHECK (auth.uid() = owner_id);

-- Update: Only owner can update
CREATE POLICY "Users can update own greenhouses" ON greenhouses
FOR UPDATE USING (auth.uid() = owner_id);

-- Delete: Only owner can delete  
CREATE POLICY "Users can delete own greenhouses" ON greenhouses
FOR DELETE USING (auth.uid() = owner_id);

-- Service role access
CREATE POLICY "Service role full access" ON greenhouses
FOR ALL USING (auth.role() = 'service_role');
```

**Sensor Readings Policies**
```sql
-- Users can view readings from their greenhouses
CREATE POLICY "Users can view own greenhouse readings" ON sensor_readings
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = sensor_readings.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);

-- IoT devices can insert readings (via service role)
CREATE POLICY "IoT devices can insert readings" ON sensor_readings
FOR INSERT WITH CHECK (auth.role() = 'service_role');

-- Service role full access
CREATE POLICY "Service role full access" ON sensor_readings
FOR ALL USING (auth.role() = 'service_role');
```

**Sensor History Policies**
```sql
-- Similar to sensor_readings
CREATE POLICY "Users can view own greenhouse history" ON sensor_history
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = sensor_history.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);

CREATE POLICY "Service role full access" ON sensor_history
FOR ALL USING (auth.role() = 'service_role');
```

**Control Devices Policies**
```sql
-- Users can view control devices for their greenhouses
CREATE POLICY "Users can view own greenhouse devices" ON control_devices
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = control_devices.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);

-- Users can update control devices for their greenhouses
CREATE POLICY "Users can update own greenhouse devices" ON control_devices
FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = control_devices.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);
```

**Automation Settings Policies**
```sql
-- Similar pattern to control_devices
CREATE POLICY "Users can view own greenhouse settings" ON automation_settings
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = automation_settings.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);

CREATE POLICY "Users can update own greenhouse settings" ON automation_settings
FOR UPDATE USING (
    EXISTS (
        SELECT 1 FROM greenhouses 
        WHERE greenhouses.id = automation_settings.greenhouse_id 
        AND greenhouses.owner_id = auth.uid()
    )
);
```

## 🎯 **Database Triggers**

1. `after_greenhouse_created`

   **Table**: `greenhouses`
   **Timing**: `AFTER INSERT`
   **Function**: `create_greenhouse_default_data()`
   
   ```sql
   CREATE TRIGGER after_greenhouse_created
   AFTER INSERT ON greenhouses
   FOR EACH ROW
   EXECUTE FUNCTION create_greenhouse_default_data();
   ```
   
   **Purpose**: Otomatis membuat default settings saat greenhouse baru dibuat

2. `on_sensor_reading_inserted`

   **Table**: `sensor_readings`
   **Timing**: `AFTER INSERT`
   **Function**: `trigger_copy_to_history()`
   
   ```sql
   CREATE TRIGGER on_sensor_reading_inserted
   AFTER INSERT ON sensor_readings
   FOR EACH ROW
   EXECUTE FUNCTION trigger_copy_to_history();
   ```
   
   **Purpose**: Archive data ke tabel history via Edge Function

## 🌐 **Edge Functions**

1. `copy-to-history`

   **Endpoint**: `https://[project].supabase.co/functions/v1/copy-to-history`
   **Trigger**: Database trigger `on_sensor_reading_inserted`
   
   ```typescript
   // Function: Memisahkan data sensor menjadi multiple rows di history
   // Input: { record: { greenhouse_id, temperature, humidity, ... } }
   // Output: 5 rows inserted ke sensor_history (satu per sensor type)
   ```
   
   **Deployment**:
   
   ```bash
   supabase functions deploy copy-to-history --project-ref your-project-ref
   ```
   
   **Environment Variables**:
   
   ```bash
   # Set in Supabase Dashboard → Settings → API → Edge Functions
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
   ```

2. `delete-auth-account`

   **Endpoint**: `https://[project].supabase.co/functions/v1/delete-auth-account`
   **Headers**: `x-admin-secret: [secret]`
   
   ```typescript
   // Function: Delete user dari Auth dan semua data terkait
   // Steps:
   // 1. Validate admin secret
   // 2. Call database function delete_user_completely()
   // 3. Delete from Supabase Auth
   // 4. Return success/failure
   ```
   
   **Security Considerations**:
   * Require admin secret header
   * Validate UUID format
   * Comprehensive error handling
   * Log all deletion attempts

## 🚀 **Setup Guide**

### **Step 1: Database Initialization**

```sql
-- 1. Run all CREATE TABLE statements
-- 2. Create indexes for performance
-- 3. Enable RLS on all tables
-- 4. Create RLS policies
-- 5. Create functions
-- 6. Create triggers
-- 7. Insert initial data if needed
```

### **Step 2: Supabase Configuration**

1. **Enable Extensions**:

   ```sql
   CREATE EXTENSION IF NOT EXISTS "pg_net";
   CREATE EXTENSION IF NOT EXISTS "pg_cron";
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   ```

2. **Set Service Role Key for Triggers**:

   ```sql
   ALTER DATABASE postgres 
   SET app.settings.service_role_key TO 'your-service-role-key';
   ```

3. **Configure Realtime**:

   * Go to Database → Replication
   * Enable for: `sensor_readings`, `control_devices`, `sensor_history`

### **Step 3: Deploy Edge Functions**

```bash
# Install Supabase CLI
npm install -g supabase

# Login
supabase login

# Deploy functions
supabase functions deploy copy-to-history
supabase functions deploy delete-auth-account

# Set secrets
supabase secrets set ADMIN_DELETE_SECRET=your-secret-key
supabase secrets set SUPABASE_URL=https://your-project.supabase.co
supabase secrets set SUPABASE_SERVICE_ROLE_KEY=your-key
```

### **Step 4: Authentication Setup**

1. Go to Authentication → Settings
2. Enable Email provider
3. Configure Site URL: `your-app://login-callback`
4. Setup email templates
5. Configure OAuth if needed

### **Step 5: Monitoring & Maintenance**

```sql
-- Daily maintenance tasks (pg_cron)
SELECT cron.schedule('cleanup-old-data', '0 2 * * *', $$
    DELETE FROM sensor_history 
    WHERE recorded_at < NOW() - INTERVAL '6 months';
$$);

-- Health check query
SELECT 
    table_name,
    pg_size_pretty(pg_total_relation_size(quote_ident(table_name))) as size,
    (SELECT count(*) FROM quote_ident(table_name)) as row_count
FROM information_schema.tables 
WHERE table_schema = 'public'
ORDER BY pg_total_relation_size(quote_ident(table_name)) DESC;
```

## 📈 **Performance Optimization**

### **Indexing Strategy**

```sql
-- Composite indexes for common queries
CREATE INDEX idx_sensor_history_composite ON sensor_history 
(greenhouse_id, sensor_type, recorded_at DESC);

-- Partial indexes for active data
CREATE INDEX idx_active_readings ON sensor_readings (recorded_at DESC) 
WHERE recorded_at > NOW() - INTERVAL '7 days';

-- BRIN indexes for time-series
CREATE INDEX idx_history_time_brin ON sensor_history 
USING BRIN (recorded_at);
```

### **Query Optimization Examples**

```sql
-- Get latest readings for dashboard
EXPLAIN ANALYZE
SELECT * FROM sensor_readings
WHERE greenhouse_id = 'uuid-here'
ORDER BY recorded_at DESC
LIMIT 1;

-- Get 24-hour history for chart
EXPLAIN ANALYZE
SELECT sensor_type, value, recorded_at
FROM sensor_history
WHERE greenhouse_id = 'uuid-here'
AND recorded_at > NOW() - INTERVAL '24 hours'
ORDER BY recorded_at;
```

## 🐛 **Troubleshooting Guide**

### **Common Issues & Solutions**

1. **RLS Policy Violation**

   **Error**: `new row violates row-level security policy`
   
   **Solution**:
   ```sql
   -- Temporarily disable RLS for debugging
   ALTER TABLE table_name DISABLE ROW LEVEL SECURITY;
   
   -- Check current policies
   SELECT * FROM pg_policies 
   WHERE tablename = 'table_name';
   
   -- Test with service role
   SET ROLE service_role;
   SELECT * FROM table_name;
   RESET ROLE;
   ```

2. **Trigger Not Firing**

   **Debug Steps**:
   ```sql
   -- Check trigger status
   SELECT 
       tgname as trigger_name,
       tgisinternal as is_internal,
       tgenabled as enabled
   FROM pg_trigger 
   WHERE tgrelid = 'sensor_readings'::regclass;
   
   -- Manual test
   INSERT INTO sensor_readings (...) VALUES (...);
   SELECT * FROM sensor_history ORDER BY recorded_at DESC LIMIT 5;
   ```

3. **Edge Function Timeout**

   **Solution**:
   * Increase timeout in function config
   * Optimize database queries
   * Implement batching for large operations

4. **Real-time Not Working**

   **Checklist**:
   * Table added to replication in Supabase dashboard
   * Client subscribed correctly
   * WebSocket connection established
   * No firewall blocking WebSocket

## 🔧 **Maintenance Scripts**

### **Database Health Check**

```sql
-- Connection count
SELECT count(*) FROM pg_stat_activity;

-- Table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;

-- Index usage statistics
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as scans,
    idx_tup_read as rows_read,
    idx_tup_fetch as rows_fetched
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### **Data Retention Management**

```sql
-- Archive old data (keep 6 months)
CREATE TABLE sensor_history_archive 
AS SELECT * FROM sensor_history 
WHERE recorded_at < NOW() - INTERVAL '6 months';

-- Delete archived data
DELETE FROM sensor_history 
WHERE recorded_at < NOW() - INTERVAL '6 months';

-- Vacuum to reclaim space
VACUUM ANALYZE sensor_history;
```

## 📊 **Monitoring & Analytics**

### **Key Metrics to Monitor**

```sql
-- Sensor data volume
SELECT 
    DATE(recorded_at) as date,
    COUNT(*) as readings,
    AVG(temperature) as avg_temp,
    AVG(humidity) as avg_humidity
FROM sensor_readings
WHERE recorded_at > NOW() - INTERVAL '7 days'
GROUP BY DATE(recorded_at)
ORDER BY date DESC;

-- Active users/devices
SELECT 
    DATE(created_at) as date,
    COUNT(DISTINCT owner_id) as active_users,
    COUNT(*) as new_greenhouses
FROM greenhouses
WHERE created_at > NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

## 🚨 **Backup & Recovery**

### **Backup Strategy**

1. Automatic Backups: Supabase daily backups
2. Point-in-Time Recovery: Enabled in Supabase
3. Export Data:
    ```bash
    # Using pg_dump
    pg_dump -h db-host -U postgres -d dbname > backup.sql
    
    # Using Supabase CLI
    supabase db dump --local
   ```
   
### **Disaster Recovery**

```sql
-- Restore from backup
psql -h db-host -U postgres -d dbname < backup.sql

-- Verify recovery
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM sensor_readings;
```

## 📚 **References & Resources**

### **Supabase Documentation**

* [Supabase Docs]()
* [PostgreSQL RLS Guide]()
* [Edge Functions]()
* [Realtime Subscriptions]()

### **PostgreSQL Resources**

* [PostgreSQL Official Docs]()
* [Indexing Strategies]()
* [Performance Tuning]()

### **IoT Integration**

* [REST API Design]()
* [MQTT Protocol]()
* [ESP32 Programming]()

## 📞 **Support**

For database-related issues:
1. Check the [Supabase Status Page]()
2. Review [PostgreSQL Logs]()

___
**Database Version**: 1.0.0
**Last Updated**: December 2025
