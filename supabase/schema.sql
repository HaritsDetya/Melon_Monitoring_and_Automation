-- 1. Enable Extensions
CREATE EXTENSION IF NOT EXISTS "pg_net";

-- 2. Create Users Table
CREATE TABLE public.users (
  id uuid NOT NULL DEFAULT gen_random_uuid (),
  username text NOT NULL,
  email text NOT NULL,
  phone_number text NULL,
  created_at timestamp with time zone NULL DEFAULT now(),
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_email_key UNIQUE (email)
);

-- 3. Create Greenhouses Table
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

-- 4. Create Sensor Readings Table (Raw Data)
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

-- 5. Create Sensor History Table (Normalized Data)
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
CREATE INDEX IF NOT EXISTS idx_sensor_history_greenhouse_recorded ON public.sensor_history USING btree (greenhouse_id, recorded_at);

-- 6. Create IoT Devices Table
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

-- 7. Create Control Devices Table
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

-- 8. Create Automation Settings Table
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

CREATE INDEX IF NOT EXISTS idx_automation_greenhouse_id ON public.automation_settings USING btree (greenhouse_id);

-- =============================================
-- FUNCTIONS & TRIGGERS
-- =============================================

-- A. Function: Auto-create Default Data for New Greenhouse
CREATE OR REPLACE FUNCTION create_greenhouse_default_data()
RETURNS TRIGGER AS $$
BEGIN
    -- Create automation settings default
    INSERT INTO automation_settings (id, greenhouse_id, max_temperature, min_temperature, nutrient_droplets, updated_at)
    VALUES (gen_random_uuid(), NEW.id, 38.0, 25.0, 10, NOW());

    -- Create control devices default
    INSERT INTO control_devices (id, greenhouse_id, fan, pump, auto_mode, updated_at)
    VALUES (gen_random_uuid(), NEW.id, false, false, false, NOW());

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger A
CREATE TRIGGER after_greenhouse_created
AFTER INSERT ON greenhouses
FOR EACH ROW
EXECUTE FUNCTION create_greenhouse_default_data();


-- B. Function: Trigger Edge Function (Copy to History)
-- NOTE: Please replace [YOUR_SUPABASE_URL] and [YOUR_SERVICE_ROLE_KEY] with actual values in Supabase Dashboard
CREATE OR REPLACE FUNCTION trigger_copy_to_history()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM
    net.http_post(
      url := '[YOUR_SUPABASE_URL]/functions/v1/copy-to-history',
      headers := '{"Content-Type": "application/json", "Authorization": "Bearer [YOUR_SERVICE_ROLE_KEY]"}'::jsonb,
      body := json_build_object('record', NEW)::text
    );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger B
CREATE TRIGGER on_sensor_reading_inserted
  AFTER INSERT ON sensor_readings
  FOR EACH ROW
  EXECUTE FUNCTION trigger_copy_to_history();
