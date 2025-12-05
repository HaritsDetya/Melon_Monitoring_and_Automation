import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight request
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Initialize Supabase Client
    // Ensure these ENV variables are set in your Supabase Dashboard -> Edge Functions -> Secrets
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const { record } = await req.json()

    // Prepare inserts for sensor_history (Normalization)
    const inserts = []

    // Helper function to push insert promise
    const addInsert = (type, value) => {
        if (value !== null && value !== undefined) {
            inserts.push(
                supabaseClient.from('sensor_history').insert({
                    greenhouse_id: record.greenhouse_id,
                    sensor_type: type,
                    value: value,
                    recorded_at: record.recorded_at
                })
            )
        }
    }

    addInsert('TEMPERATURE', record.temperature)
    addInsert('HUMIDITY', record.humidity)
    addInsert('WATER_TEMPERATURE', record.water_temp)
    addInsert('PH', record.ph)
    addInsert('TDS', record.tds)

    // Execute all inserts concurrently
    if (inserts.length > 0) {
        await Promise.all(inserts)
    }

    return new Response(
      JSON.stringify({ success: true, message: `Processed ${inserts.length} sensor values` }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200,
      }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500,
      }
    )
  }
})
