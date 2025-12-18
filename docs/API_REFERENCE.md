# 🌐 API Reference - Greenhouse Monitoring System

![API Status](https://img.shields.io/badge/API-Online-brightgreen)
![Authentication](https://img.shields.io/badge/Auth-JWT%20Bearer-blue)
![Rate Limit](https://img.shields.io/badge/Rate%20Limit-100%2Fhour-orange)

> **Dokumentasi lengkap REST API untuk integrasi dengan Greenhouse Monitoring System**

## 📋 Daftar Isi
- [Authentication](#-authentication)
- [Users API](#-users-api)
- [Greenhouses API](#-greenhouses-api)
- [Sensors API](#-sensors-api)
- [Devices API](#-devices-api)
- [Control API](#-control-api)
- [WebSocket (Realtime)](#-websocket-realtime)
- [Error Codes](#-error-codes)

## 🔐 Authentication

### Base URL
`https://[project-id].supabase.co/rest/v1/`


### Authentication Headers
```http
Authorization: Bearer [JWT_TOKEN]
apikey: [ANON_KEY]
```

### **Login Endpoint**

**POST** `/auth/v1/token?grant_type=password`
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIs...",
  "token_type": "bearer",
  "expires_in": 3600,
  "refresh_token": "yfGzL...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  }
}
```

### **Refresh Token**

**POST** `/auth/v1/token?grant_type=refresh_token`
```json
{
  "refresh_token": "[REFRESH_TOKEN]"
}
```

### **Logout**

**POST** `/auth/v1/logout`

## 👤 **Users API**

### **Get Current User Profile**

**GET** `/users?select=*&id=eq.[USER_ID]`

**Response**:
```json
[
  {
    "id": "uuid",
    "username": "John Doe",
    "email": "john@example.com",
    "phone_number": "+628123456789",
    "created_at": "2024-01-01T00:00:00Z"
  }
]
```

### **Update User Profile**

**PATCH** `/users?id=eq.[USER_ID]`

```json
{
  "username": "New Name",
  "phone_number": "+628987654321"
}
```

**Headers**:
```http
x-admin-secret: [ADMIN_DELETE_SECRET]
Authorization: Bearer [SERVICE_ROLE_KEY]
```

## 🏠 **Greenhouses API**

### **List User's Greenhouses**

**GET** `/greenhouses?select=*&owner_id=eq.[USER_ID]&order=created_at.desc`

**Response**:
```json
[
  {
    "id": "greenhouse-uuid",
    "name": "Kebun Melon A",
    "location": "Bandung, Jawa Barat",
    "owner_id": "user-uuid",
    "description": "Greenhouse untuk melon hidroponik",
    "created_at": "2024-01-01T00:00:00Z"
  }
]
```

### **Create New Greenhouse**

**POST** `/greenhouses`
```json
{
  "name": "Greenhouse Baru",
  "location": "Jakarta",
  "description": "Deskripsi greenhouse"
}
```

**Note**: `owner_id` akan otomatis diisi dengan ID user yang login

### **Get Greenhouse Details**

**GET** `/greenhouses?select=*&id=eq.[GREENHOUSE_ID]`

### **Update Greenhouse**

**PATCH** `/greenhouses?id=eq.[GREENHOUSE_ID]`

```json
{
  "name": "Nama Baru",
  "description": "Deskripsi diperbarui"
}
```

### **Delete Greenhouse**

**DELETE** `/greenhouses?id=eq.[GREENHOUSE_ID]`

## 📡 **Sensors API**

### **Get Latest Sensor Readings**

**GET** `/sensor_readings?select=*&greenhouse_id=eq.[GREENHOUSE_ID]&order=recorded_at.desc&limit=1`

**Response**:

```json
[
  {
    "id": "reading-uuid",
    "greenhouse_id": "greenhouse-uuid",
    "temperature": 28.5,
    "humidity": 65.2,
    "water_temp": 26.8,
    "ph": 6.5,
    "tds": 450,
    "recorded_at": "2024-01-01T12:00:00Z"
  }
]
```

### **Get Historical Sensor Data**

**GET** `/sensor_history?select=*&greenhouse_id=eq.[GREENHOUSE_ID]&sensor_type=eq.TEMPERATURE&recorded_at=gte.2024-01-01T00:00:00Z&recorded_at=lte.2024-01-02T00:00:00Z&order=recorded_at.asc`

**Query Parameters**:
* `sensor_type`: TEMPERATURE, HUMIDITY, WATER_TEMPERATURE, PH, TDS
* `recorded_at.gte`: Start datetime (ISO format)
* `recorded_at.lte`: End datetime (ISO format)
* `limit`: Jumlah data (default: 1000)

### **Post Sensor Data (IoT Device)**

**POST** `/sensor_readings`

```json
{
  "greenhouse_id": "greenhouse-uuid",
  "temperature": 28.5,
  "humidity": 65.2,
  "water_temp": 26.8,
  "ph": 6.5,
  "tds": 450
}
```

**Headers untuk IoT Device**:

```http
Authorization: Bearer [SERVICE_ROLE_KEY]
apikey: [ANON_KEY]
```

## 📱 **Devices API**

### **List IoT Devices**

**GET** `/iot_devices?select=*&greenhouse_id=eq.[GREENHOUSE_ID]`

**Response**:
```json
[
  {
    "id": "device-uuid",
    "device_id": "ESP32_001",
    "device_name": "Sensor Utama",
    "device_type": "HYDROPONIC_SENSOR",
    "greenhouse_id": "greenhouse-uuid",
    "is_paired": true,
    "paired_at": "2024-01-01T10:00:00Z",
    "last_seen": "2024-01-01T12:00:00Z",
    "firmware_version": "1.2.0"
  }
]
```

### **Pair New Device**

**POST** `/iot_devices`
```json
{
  "device_id": "ESP32_002",
  "serial_number": "SN123456789",
  "device_name": "Sensor Tambahan",
  "pairing_code": "ABC123",
  "device_type": "HYDROPONIC_SENSOR"
}
```

### **Update Device Pairing**

**PATCH** `/iot_devices?device_id=eq.[DEVICE_ID]`

```json
{
  "is_paired": true,
  "paired_at": "2024-01-01T10:00:00Z",
  "greenhouse_id": "greenhouse-uuid"
}
```

### **Send Device Heartbeat**

**PATCH** `/iot_devices?device_id=eq.[DEVICE_ID]`
```json
{
  "last_seen": "2024-01-01T12:00:00Z"
}
```

## 🎛️ **Control API**

### **Get Control Device State**

**GET** `/control_devices?select=*&greenhouse_id=eq.[GREENHOUSE_ID]`

**Response**:
```json
[
  {
    "id": "control-uuid",
    "greenhouse_id": "greenhouse-uuid",
    "fan": false,
    "pump": true,
    "auto_mode": false,
    "updated_at": "2024-01-01T11:30:00Z"
  }
]
```

### **Control Fan**

**PATCH** `/control_devices?greenhouse_id=eq.[GREENHOUSE_ID]`

```json
{
  "fan": true,
  "updated_at": "2024-01-01T12:00:00Z"
}
```

### **Control Pump**

**PATCH** `/control_devices?greenhouse_id=eq.[GREENHOUSE_ID]`
```json
{
  "pump": false,
  "updated_at": "2024-01-01T12:00:00Z"
}
```

### **Toggle Auto Mode**

**PATCH** `/control_devices?greenhouse_id=eq.[GREENHOUSE_ID]`
```json
{
  "auto_mode": true,
  "updated_at": "2024-01-01T12:00:00Z"
}
```

## ⚙️ **Automation Settings API**

### **Get Automation Settings**

**GET** `/automation_settings?select=*&greenhouse_id=eq.[GREENHOUSE_ID]`

**Response**:
```json
[
  {
    "id": "settings-uuid",
    "greenhouse_id": "greenhouse-uuid",
    "max_temperature": 38.0,
    "min_temperature": 25.0,
    "nutrient_droplets": 10,
    "updated_at": "2024-01-01T10:00:00Z"
  }
]
```

### **Update Automation Settings**

**PATCH** `/automation_settings?greenhouse_id=eq.[GREENHOUSE_ID]`
```json
{
  "max_temperature": 35.0,
  "min_temperature": 22.0,
  "nutrient_droplets": 8,
  "updated_at": "2024-01-01T12:00:00Z"
}
```

## 🔄 **WebSocket (Realtime)**

### **Connection Setup**
```javascript
const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// Subscribe to sensor readings
const channel = supabase
  .channel('sensor-updates')
  .on(
    'postgres_changes',
    {
      event: 'INSERT',
      schema: 'public',
      table: 'sensor_readings',
      filter: `greenhouse_id=eq.${greenhouseId}`
    },
    (payload) => {
      console.log('New sensor reading:', payload.new);
    }
  )
  .subscribe();
```

### **Available Subscriptions**

1. **Sensor Readings Updates**

    ```javascript
    supabase
      .channel('sensor-channel')
      .on(
        'postgres_changes',
        {
          event: 'INSERT',
          schema: 'public',
          table: 'sensor_readings'
        },
        handleNewReading
      )
      .subscribe();
    ```

2. **Control Devices Updates**

    ```javascript
    supabase
      .channel('control-channel')
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'control_devices'
        },
        handleControlUpdate
      )
      .subscribe();
    ```

3. **System Events**

    ```javascript
    supabase
      .channel('system-channel')
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public'
        },
        handleSystemEvent
      )
      .subscribe();
    ```
   
## 🚨 **Error Codes**

### **HTTP Status Codes**

| Code	  |      Description	      |              Possible Causes               |
|:------:|:----------------------:|:------------------------------------------:|
|  200   |          	OK           |            	Request successful             |
|  201   |        	Created        |       	Resource created successfully       |
|  400	  |          Bad           | Request	Invalid request body or parameters |
|  401   |     	Unauthorized      |     	Missing or invalid authentication     |
|  403   |       	Forbidden       |         	Insufficient permissions          |
|  404   |       	Not Found       |          	Resource does not exist          |
|  409   |       	Conflict        |    	Resource conflict (e.g., duplicate)    |
|  422   | 	Unprocessable Entity  |             	Validation error              |
|  429   |  	Too Many Requests	   |            Rate limit exceeded             |
|  500   | 	Internal Server Error |             	Server-side error             |

### **Common Error Responses**

```json
{
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid",
  "statusCode": 401
}
```

```json
{
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "statusCode": 403
}
```

```json
{
  "error": "Validation Error",
  "details": {
    "temperature": "Must be between -40 and 80"
  },
  "statusCode": 422
}
```

## 📊 **Rate Limiting**

### **Limits per API Key**

|     Tier     | 	Requests per Hour  |  	Burst  |
|:------------:|:-------------------:|:--------:|
|     Free     |        	100	        |    10    |
|     Pro      |      	10,000	       |   100    |
|  Enterprise  |      	100,000       |  	1000   |

### **Rate Limit Headers**

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 3600
```

## 🔒 **Security**

### **API Keys**

1. **ANON_KEY**: Untuk client-side applications
2. **SERVICE_ROLE_KEY**: Untuk server-side applications (full access)
3. **ADMIN_SECRET**: Untuk admin operations

### **Best Practices**

1. Never expose SERVICE_ROLE_KEY in client code
2. Use RLS policies for data isolation
3. Validate all input data
4. Use HTTPS for all requests
5. Rotate API keys regularly

## 🧪 **Testing API**

### **Using cURL**

```bash
# Test authentication
curl -X POST https://[project].supabase.co/auth/v1/token \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# Test sensor data retrieval
curl -X GET "https://[project].supabase.co/rest/v1/sensor_readings?select=*&limit=1" \
  -H "Authorization: Bearer [JWT_TOKEN]" \
  -H "apikey: [ANON_KEY]"
```

### **Using Postman**

1. Import Postman collection dari docs/postman_collection.json
2. Set environment variables:
   * supabase_url
   * anon_key
   * jwt_token
3. Run requests untuk testing

## 📈 **Monitoring & Analytics**

### **API Metrics Endpoint**

**GET** `/rest/v1/rpc/get_api_metrics`

**Response**:
```json
{
  "total_requests": 1250,
  "success_rate": 98.4,
  "average_response_time": 145,
  "endpoints": [
    {
      "endpoint": "/sensor_readings",
      "requests": 450,
      "avg_response_time": 120
    }
  ]
}
```

### **Health Check**

**GET** `/health`

**Response**:
```json
{
  "status": "healthy",
  "timestamp": "2024-01-01T12:00:00Z",
  "database": "connected",
  "realtime": "connected",
  "storage": "connected"
}
```

## 📝 **Contoh Implementasi**

### **Kotlin (Android)**

```kotlin
class SupabaseApiClient {
    private val client = SupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    )
    
    suspend fun getGreenhouses(userId: String): List<Greenhouse> {
        return client.from("greenhouses")
            .select()
            .eq("owner_id", userId)
            .execute()
            .decodeList()
    }
    
    suspend fun postSensorReading(reading: SensorReading) {
        client.from("sensor_readings")
            .insert(reading)
            .execute()
    }
}
```

### **Python (IoT Device)**

```python
import requests
import json

class IoTDevice:
    def __init__(self, api_url, api_key):
        self.api_url = api_url
        self.headers = {
            'Authorization': f'Bearer {api_key}',
            'apikey': api_key,
            'Content-Type': 'application/json'
        }
    
    def send_sensor_data(self, greenhouse_id, data):
        payload = {
            'greenhouse_id': greenhouse_id,
            **data
        }
        
        response = requests.post(
            f'{self.api_url}/sensor_readings',
            headers=self.headers,
            json=payload
        )
        
        return response.status_code == 201
```

### **JavaScript (Web Dashboard)**

```javascript
async function fetchGreenhouseData(greenhouseId) {
  const response = await fetch(
    `${supabaseUrl}/rest/v1/sensor_readings?greenhouse_id=eq.${greenhouseId}&order=recorded_at.desc&limit=1`,
    {
      headers: {
        'Authorization': `Bearer ${accessToken}`,
        'apikey': anonKey
      }
    }
  );
  
  return await response.json();
}
```

## 🔗 **Related Resources**

* [Supabase JavaScript Client](https://supabase.com/docs/reference/javascript/introduction)
* [PostgREST API Reference](https://postgrest.org/en/stable/references/api.html)
* [REST API Best Practices](https://restfulapi.net/)

___
**API Version**: v1.0.0
**Last Updated**: December 2025
