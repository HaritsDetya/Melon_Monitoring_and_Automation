import { serve } from "https://deno.land/std@0.190.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, x-admin-secret',
  'Access-Control-Allow-Methods': 'POST, OPTIONS, DELETE',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log('🔹 [EDGE] Starting delete-auth-account function')

    // Check admin secret
    const adminSecret = req.headers.get('x-admin-secret')
    const expectedSecret = Deno.env.get('ADMIN_DELETE_SECRET')

    if (!adminSecret || adminSecret !== expectedSecret) {
      console.error('❌ [EDGE] Invalid admin secret')
      return new Response(
        JSON.stringify({ error: 'Unauthorized: Invalid admin secret' }),
        {
          status: 401,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    // Parse request body
    let userId: string
    try {
      const body = await req.json()
      userId = body.user_id

      if (!userId || typeof userId !== 'string') {
        throw new Error('Missing user_id')
      }

      // Validate UUID format
      const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
      if (!uuidRegex.test(userId)) {
        throw new Error('Invalid user_id format')
      }
    } catch (parseError) {
      console.error('❌ [EDGE] Invalid request body:', parseError)
      return new Response(
        JSON.stringify({ error: 'Invalid request body. Expected { "user_id": "<uuid>" }' }),
        {
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    console.log(`🔹 [EDGE] Deleting user: ${userId}`)

    // Delete user using Supabase Admin API
    const supabaseUrl = Deno.env.get('SUPABASE_URL')
    const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')

    if (!supabaseUrl || !serviceRoleKey) {
      console.error('❌ [EDGE] Missing environment variables')
      return new Response(
        JSON.stringify({ error: 'Server configuration error' }),
        {
          status: 500,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    // ✅ OPTION A: Gunakan Database Function (Recommended)
    console.log('🔹 [EDGE] Using database function for deletion...')
    const dbFunctionResponse = await fetch(`${supabaseUrl}/rest/v1/rpc/delete_user_completely`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${serviceRoleKey}`,
        'Content-Type': 'application/json',
        'apiKey': serviceRoleKey,
        'Prefer': 'return=minimal'
      },
      body: JSON.stringify({ user_id: userId })
    })

    if (!dbFunctionResponse.ok) {
      const errorText = await dbFunctionResponse.text()
      console.error(`❌ [EDGE] Database function error: ${dbFunctionResponse.status} - ${errorText}`)

      // ✅ OPTION B: Fallback ke direct deletion jika function tidak ada
      console.log('🔹 [EDGE] Trying direct deletion as fallback...')
      const directDeleteSuccess = await tryDirectDeletion(userId, supabaseUrl, serviceRoleKey)

      if (!directDeleteSuccess) {
        return new Response(
          JSON.stringify({ error: 'Failed to delete user data from database' }),
          {
            status: 500,
            headers: { ...corsHeaders, 'Content-Type': 'application/json' }
          }
        )
      }
    }

    // Step 2: Delete user from Auth
    console.log('🔹 [EDGE] Step 2: Deleting user from Auth...')
    const authResponse = await fetch(`${supabaseUrl}/auth/v1/admin/users/${userId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${serviceRoleKey}`,
        'Content-Type': 'application/json',
        'apiKey': serviceRoleKey
      }
    })

    if (!authResponse.ok) {
      const errorText = await authResponse.text()
      console.error(`❌ [EDGE] Auth API error: ${authResponse.status} - ${errorText}`)

      let errorMessage = 'Failed to delete user from authentication'
      if (authResponse.status === 404) {
        errorMessage = 'User not found in authentication'
      } else if (authResponse.status === 401) {
        errorMessage = 'Invalid service role key'
      }

      return new Response(
        JSON.stringify({ error: errorMessage, details: errorText }),
        {
          status: authResponse.status,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    console.log(`✅ [EDGE] User ${userId} deleted successfully from Auth and database`)

    return new Response(
      JSON.stringify({
        success: true,
        message: 'User account and all data deleted successfully',
        user_id: userId
      }),
      {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )

  } catch (error) {
    console.error('❌ [EDGE] Unexpected error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error: ' + error.message }),
      {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )
  }
})

// Fallback function untuk direct deletion
async function tryDirectDeletion(userId: string, supabaseUrl: string, serviceRoleKey: string): Promise<boolean> {
  try {
    console.log('🔹 [EDGE] Trying direct deletion...')

    const tablesToDelete = [
      'sensor_data',
      'sensor_history',
      'sensor_readings',
      'control_devices',
      'automation_settings',
      'greenhouses',
      'user_profiles',
      'users'
    ]

    for (const table of tablesToDelete) {
      try {
        let query = ''
        if (table === 'users' || table === 'user_profiles') {
          query = `id=eq.${userId}`
        } else if (table === 'greenhouses') {
          query = `owner_id=eq.${userId}`
        } else {
          query = `greenhouse_id=in.(SELECT id FROM greenhouses WHERE owner_id=eq.${userId})`
        }

        const response = await fetch(`${supabaseUrl}/rest/v1/${table}?${query}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${serviceRoleKey}`,
            'Content-Type': 'application/json',
            'apiKey': serviceRoleKey,
            'Prefer': 'return=minimal'
          }
        })

        if (response.ok) {
          console.log(`✅ [EDGE] Deleted from ${table}`)
        } else if (response.status === 404) {
          console.log(`🔹 [EDGE] No data in ${table}`)
        } else {
          console.warn(`⚠️ [EDGE] Failed to delete from ${table}: ${response.status}`)
        }
      } catch (error) {
        console.warn(`⚠️ [EDGE] Error deleting from ${table}: ${error.message}`)
      }
    }

    return true
  } catch (error) {
    console.error('❌ [EDGE] Direct deletion error:', error)
    return false
  }
}
