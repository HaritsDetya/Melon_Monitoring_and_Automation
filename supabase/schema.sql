-- =========================================================
-- SCHEMA DATABASE & SECURITY POLICIES
-- Project: Greenhouse Monitoring App
-- =========================================================

-- 1. Enable Extensions
CREATE EXTENSION IF NOT EXISTS "pg_net";

-- 2. TABLE STRUCTURES

-- A. Users (Managed by Supabase Auth, extending profile info)
CREATE TABLE public.users (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  username text NOT NULL,
  email text NOT NULL,
  phone_number text NULL,
  created_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_email_key UNIQUE (email)
);

-- B. Greenhouses
CREATE TABLE public.greenhouses (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  name text NOT NULL,
  location text NOT NULL,
  owner_id uuid NULL,
  description text NULL,
  created_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT greenhouses_pkey PRIMARY KEY (id),
  CONSTRAINT greenhouses_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

-- C. Sensor Readings (Raw Data)
CREATE TABLE public.sensor_readings (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  greenhouse_id uuid NULL,
  temperature double precision NULL,
  humidity double precision NULL,
  water_temp double precision NULL,
  ph double precision NULL,
  tds double precision NULL,
  recorded_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT sensor_readings_pkey PRIMARY KEY (id),
  CONSTRAINT sensor_readings_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_greenhouse_id_readings ON public.sensor_readings USING btree (greenhouse_id);

-- D. Sensor History (Normalized Data for Graphs)
CREATE TABLE public.sensor_history (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  greenhouse_id uuid NULL,
  sensor_type text NOT NULL,
  value double precision NOT NULL,
  recorded_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT sensor_history_pkey PRIMARY KEY (id),
  CONSTRAINT sensor_history_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE,
  CONSTRAINT sensor_history_sensor_type_check CHECK (
    (sensor_type = ANY (ARRAY['TEMPERATURE'::text, 'HUMIDITY'::text, 'WATER_TEMPERATURE'::text, 'PH'::text, 'TDS'::text]))
  )
);
CREATE INDEX IF NOT EXISTS idx_sensor_history_greenhouse_type ON public.sensor_history USING btree (greenhouse_id, sensor_type);
CREATE INDEX IF NOT EXISTS idx_sensor_history_recorded_at ON public.sensor_history USING btree (recorded_at);

-- E. Control Devices (Actuators)
CREATE TABLE public.control_devices (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  greenhouse_id uuid NULL,
  fan boolean NULL DEFAULT false,
  pump boolean NULL DEFAULT false,
  auto_mode boolean NULL DEFAULT false,
  updated_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT control_devices_pkey PRIMARY KEY (id),
  CONSTRAINT control_devices_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE
);

-- F. Automation Settings
CREATE TABLE public.automation_settings (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  greenhouse_id uuid NULL,
  max_temperature double precision NULL DEFAULT 38.0,
  min_temperature double precision NULL DEFAULT 25.0,
  nutrient_droplets integer NULL DEFAULT 10,
  updated_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT automation_settings_pkey PRIMARY KEY (id),
  CONSTRAINT automation_settings_greenhouse_id_key UNIQUE (greenhouse_id),
  CONSTRAINT automation_settings_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id) ON DELETE CASCADE
);

-- G. IoT Devices
CREATE TABLE public.iot_devices (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  device_id character varying(50) NOT NULL,
  serial_number character varying(100) NOT NULL,
  device_name character varying(100) NULL,
  device_type character varying(50) NULL DEFAULT 'HYDROPONIC_SENSOR'::character varying,
  greenhouse_id uuid NULL,
  pairing_code character varying(6) NOT NULL,
  is_paired boolean NULL DEFAULT false,
  paired_at timestamp with time zone NULL,
  created_at timestamp with time zone NULL DEFAULT now(),
  last_seen timestamp with time zone NULL,
  firmware_version character varying(20) NULL,
  encryption_key character varying(100) NULL,
  CONSTRAINT iot_devices_pkey PRIMARY KEY (id),
  CONSTRAINT iot_devices_device_id_key UNIQUE (device_id),
  CONSTRAINT iot_devices_serial_number_key UNIQUE (serial_number),
  CONSTRAINT iot_devices_greenhouse_id_fkey FOREIGN KEY (greenhouse_id) REFERENCES greenhouses (id)
);


-- =============================================
-- 3. FUNCTIONS & TRIGGERS
-- =============================================

-- Auto-create Default Data Trigger
CREATE OR REPLACE FUNCTION create_greenhouse_default_data()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO automation_settings (id, greenhouse_id, max_temperature, min_temperature, nutrient_droplets, updated_at)
    VALUES (gen_random_uuid(), NEW.id, 38.0, 25.0, 10, NOW());

    INSERT INTO control_devices (id, greenhouse_id, fan, pump, auto_mode, updated_at)
    VALUES (gen_random_uuid(), NEW.id, false, false, false, NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER after_greenhouse_created
AFTER INSERT ON greenhouses
FOR EACH ROW
EXECUTE FUNCTION create_greenhouse_default_data();

-- Edge Function Trigger (Please replace URL and Key manually in dashboard or use env vars if possible via Vault)
CREATE OR REPLACE FUNCTION trigger_copy_to_history()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM
    net.http_post(
      url := 'YOUR_SUPABASE_FUNCTION_URL/copy-to-history',
      headers := '{"Content-Type": "application/json", "Authorization": "Bearer YOUR_SERVICE_ROLE_KEY"}'::jsonb,
      body := json_build_object('record', NEW)::text
    );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_sensor_reading_inserted
  AFTER INSERT ON sensor_readings
  FOR EACH ROW
  EXECUTE FUNCTION trigger_copy_to_history();


-- =============================================
-- 4. ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================

-- Enable RLS on all tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.greenhouses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sensor_readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sensor_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.iot_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.control_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.automation_settings ENABLE ROW LEVEL SECURITY;

-- ----------------------------
-- Policies for: USERS
-- ----------------------------
CREATE POLICY "Users can view own profile" ON public.users
FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.users
FOR UPDATE USING (auth.uid() = id);

-- ----------------------------
-- Policies for: GREENHOUSES
-- ----------------------------
CREATE POLICY "Users can view own greenhouses" ON public.greenhouses
FOR SELECT USING (auth.uid() = owner_id);

CREATE POLICY "Users can insert own greenhouses" ON public.greenhouses
FOR INSERT WITH CHECK (auth.uid() = owner_id);

CREATE POLICY "Users can update own greenhouses" ON public.greenhouses
FOR UPDATE USING (auth.uid() = owner_id);

CREATE POLICY "Users can delete own greenhouses" ON public.greenhouses
FOR DELETE USING (auth.uid() = owner_id);

-- ----------------------------
-- Policies for: SENSOR READINGS
-- ----------------------------
-- Menggunakan logic EXISTS Anda yang sudah benar
CREATE POLICY "Users can view own greenhouse readings" ON public.sensor_readings
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = sensor_readings.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

-- Note: Kita asumsikan IoT insert menggunakan Service Role Key, jadi tidak butuh policy insert khusus user.
-- Jika IoT menggunakan Auth User, barulah tambahkan policy insert.

-- ----------------------------
-- Policies for: SENSOR HISTORY
-- ----------------------------
CREATE POLICY "Users can view own greenhouse history" ON public.sensor_history
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = sensor_history.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

-- ----------------------------
-- Policies for: CONTROL DEVICES
-- ----------------------------
CREATE POLICY "Users can view own greenhouse devices" ON public.control_devices
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = control_devices.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

CREATE POLICY "Users can update own greenhouse devices" ON public.control_devices
FOR UPDATE USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = control_devices.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

-- ----------------------------
-- Policies for: AUTOMATION SETTINGS
-- ----------------------------
CREATE POLICY "Users can view own greenhouse settings" ON public.automation_settings
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = automation_settings.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

CREATE POLICY "Users can update own greenhouse settings" ON public.automation_settings
FOR UPDATE USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = automation_settings.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

-- ----------------------------
-- Policies for: IOT DEVICES
-- ----------------------------
CREATE POLICY "Users can view own iot devices" ON public.iot_devices
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM greenhouses
    WHERE greenhouses.id = iot_devices.greenhouse_id
    AND greenhouses.owner_id = auth.uid()
  )
);

-- NOTE: Service Role (Admin/Backend) automatically bypasses RLS in Supabase.
-- Explicit policies for 'service_role' are generally not needed unless you are
-- using a custom role setup. But if you want to be explicit, you can add:
-- CREATE POLICY "Service Role Full Access" ON [table_name] TO service_role USING (true) WITH CHECK (true);
