import { getSupabase } from '@/lib/supabase'
import { getUserFromRequest } from '@/lib/auth-server'
import { rateLimit, getClientIP } from '@/lib/rate-limit'
import { NextResponse } from 'next/server'

export const dynamic = 'force-dynamic'

export async function POST(req: Request) {
  const ip = getClientIP(req)
  if (!rateLimit(ip, 10, 60 * 60 * 1000)) {
    return NextResponse.json({ error: 'Too many requests. Try again later.' }, { status: 429 })
  }

  const { text, mood } = await req.json()

  if (!text || text.length < 5 || text.length > 280) {
    return NextResponse.json({ error: 'Text must be between 5 and 280 characters' }, { status: 400 })
  }

  const user = await getUserFromRequest(req)

  const supabase = getSupabase()
  const { data, error } = await supabase
    .from('secrets')
    .insert({ text, mood: mood ?? 'other', user_id: user?.id ?? null })
    .select()
    .single()

  if (error) return NextResponse.json({ error: error.message }, { status: 500 })
  return NextResponse.json(data)
}

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url)
  const sort = searchParams.get('sort') ?? 'recent'
  const orderCol = sort === 'top' ? 'total_reactions' : 'created_at'

  const supabase = getSupabase()
  const { data, error } = await supabase
    .from('secrets')
    .select('id, text, mood, expires_at, reaction_me_too, reaction_wild, reaction_doubtful, user_id')
    .gt('expires_at', new Date().toISOString())
    .order(orderCol, { ascending: false })
    .limit(50)

  if (error) return NextResponse.json({ error: error.message }, { status: 500 })

  const rows = data ?? []

  let countMap: Record<string, number> = {}
  try {
    const ids = rows.map((s) => s.id)
    if (ids.length > 0) {
      const { data: counts } = await supabase
        .from('comments')
        .select('secret_id')
        .in('secret_id', ids)
      if (counts) {
        for (const c of counts) {
          countMap[c.secret_id] = (countMap[c.secret_id] || 0) + 1
        }
      }
    }
  } catch { /* comments table may not exist yet */ }

  return NextResponse.json(rows.map((s) => ({ ...s, comment_count: countMap[s.id] || 0 })))
}
