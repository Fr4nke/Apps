import { getSupabase } from './supabase'

export async function getUserFromRequest(req: Request) {
  const auth = req.headers.get('Authorization')
  if (!auth?.startsWith('Bearer ')) return null
  const token = auth.slice(7)
  const { data: { user } } = await getSupabase().auth.getUser(token)
  return user ?? null
}
