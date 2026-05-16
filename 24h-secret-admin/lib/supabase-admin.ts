import { createClient } from '@supabase/supabase-js'

export function getAdminClient() {
  const url = process.env.SUPABASE_URL ?? process.env.NEXT_PUBLIC_SUPABASE_URL
  const key = process.env.SUPABASE_SERVICE_ROLE_KEY
  if (!url || !key) throw new Error(`Missing env vars — SUPABASE_URL=${url ? 'ok' : 'MISSING'}, SUPABASE_SERVICE_ROLE_KEY=${key ? 'ok' : 'MISSING'}`)
  return createClient(url, key, { auth: { persistSession: false, autoRefreshToken: false } })
}
