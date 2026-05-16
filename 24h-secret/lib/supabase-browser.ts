import { createClient, SupabaseClient } from '@supabase/supabase-js'

// Loose schema so insert/update/rpc don't infer `never`.
// Replace with generated types via `supabase gen types typescript` when desired.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type LooseDb = any

let _client: SupabaseClient<LooseDb> | null = null

export function getSupabaseBrowser(): SupabaseClient<LooseDb> {
  if (!_client) {
    _client = createClient<LooseDb>(
      process.env.NEXT_PUBLIC_SUPABASE_URL!,
      process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
    )
  }
  return _client
}
