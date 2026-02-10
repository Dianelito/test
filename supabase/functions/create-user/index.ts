import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

Deno.serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { email, password, fullName, username, language } = await req.json();

    if (!email || !password || !username) {
      throw new Error("Missing required fields: email, password, username");
    }

    // Create Supabase client with Service Role Key (Admin Access)
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false,
        },
      }
    );

    // 1. Check if username exists (profile table)
    // We check this manually because the client-side check is just a hint,
    // and we need to enforce uniqueness before creating the auth user.
    const { data: existingUser } = await supabaseAdmin
      .from('profiles')
      .select('username')
      .eq('username', username)
      .maybeSingle();

    if (existingUser) {
      return new Response(JSON.stringify({ error: { message: 'Username already taken' } }), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 400,
      });
    }

    // 2. Create user (confirmed) via Admin API
    // This creates the user directly as confirmed, bypassing email verification logic entirely.
    // No email is sent by Supabase.
    const { data: { user }, error: createError } = await supabaseAdmin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: {
        full_name: fullName,
        username,
        language
      }
    });

    if (createError) throw createError;
    if (!user) throw new Error("User creation failed");

    // 3. Create profile via Admin Client (bypassing RLS if enabled, though RLS should be disabled for profiles)
    // We insert directly into the profiles table using the user.id
    const { error: profileError } = await supabaseAdmin
      .from('profiles')
      .insert([{
        id: user.id,
        username,
        email,
        full_name: fullName,
        status: 'online',
        language: language || 'en',
        last_seen: new Date().toISOString()
      }]);

    if (profileError) {
      console.error('Error creating profile:', profileError);
      // Attempt to clean up the user if profile creation fails to maintain consistency
      await supabaseAdmin.auth.admin.deleteUser(user.id);
      throw profileError;
    }

    // Return the created user object
    return new Response(JSON.stringify({ user }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 200,
    });

  } catch (error: any) {
    return new Response(JSON.stringify({ error: { message: error.message || error } }), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
      status: 400,
    });
  }
});
