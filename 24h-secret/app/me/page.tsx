'use client'

import { useState, useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

interface MySecret {
  id: string
  text: string
  mood: string
  expires_at: string
  created_at: string
  reaction_me_too: number
  reaction_wild: number
  reaction_doubtful: number
  comment_count: number
}

export default function MePage() {
  const [secrets, setSecrets] = useState<MySecret[]>([])
  const [loading, setLoading] = useState(true)
  const [authed, setAuthed] = useState<boolean | null>(null)
  const [republishing, setRepublishing] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const router = useRouter()

  useEffect(() => {
    const supabase = getSupabaseBrowser()
    supabase.auth.getUser().then(async ({ data }) => {
      if (!data.user) { setAuthed(false); setLoading(false); return }
      setAuthed(true)

      const { data: rows } = await supabase
        .from('secrets')
        .select('id, text, mood, expires_at, created_at, reaction_me_too, reaction_wild, reaction_doubtful')
        .eq('user_id', data.user.id)
        .order('created_at', { ascending: false })

      type DbSecret = Omit<MySecret, 'comment_count'>
      const secretRows = (rows ?? []) as DbSecret[]

      const countMap: Record<string, number> = {}
      if (secretRows.length > 0) {
        const ids = secretRows.map((s) => s.id)
        const { data: counts } = await supabase
          .from('comments')
          .select('secret_id')
          .in('secret_id', ids)
        if (counts) {
          for (const c of counts as { secret_id: string }[]) {
            countMap[c.secret_id] = (countMap[c.secret_id] || 0) + 1
          }
        }
      }

      setSecrets(secretRows.map((s) => ({ ...s, comment_count: countMap[s.id] || 0 })))
      setLoading(false)
    })
  }, [])

  async function republish(e: React.MouseEvent, id: string) {
    e.preventDefault()
    setRepublishing(id)
    const supabase = getSupabaseBrowser()
    await supabase.rpc('republish_secret', { p_secret_id: id })
    const newExpiry = new Date(Date.now() + 86_400_000).toISOString()
    setSecrets((prev) => prev.map((s) => s.id === id ? { ...s, expires_at: newExpiry } : s))
    setRepublishing(null)
  }

  async function deleteSecret(e: React.MouseEvent, id: string) {
    e.preventDefault()
    if (!confirm('Delete this secret? This cannot be undone.')) return
    setDeleting(id)
    const supabase = getSupabaseBrowser()
    await supabase.from('secrets').delete().eq('id', id)
    setSecrets((prev) => prev.filter((s) => s.id !== id))
    setDeleting(null)
  }

  if (loading) {
    return (
      <main className="max-w-xl mx-auto px-4 py-12 flex justify-center">
        <div className="w-6 h-6 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </main>
    )
  }

  if (!authed) {
    return (
      <main className="max-w-xl mx-auto px-4 py-12 text-center text-zinc-500">
        <p>Sign in to see your secrets.</p>
        <button onClick={() => router.push('/')} className="mt-4 text-sm text-violet-400 hover:text-violet-300">
          ← Back
        </button>
      </main>
    )
  }

  const now = new Date()
  const active = secrets.filter((s) => new Date(s.expires_at) >= now)
  const dead = secrets.filter((s) => new Date(s.expires_at) < now)

  function SecretCard({ s, expired }: { s: MySecret; expired: boolean }) {
    return (
      <div key={s.id} className={`relative bg-zinc-900 border rounded-2xl p-4 space-y-3 hover:border-zinc-700 transition-colors ${expired ? 'border-zinc-800/50 opacity-70' : 'border-zinc-800'}`}>
        <Link href={`/secrets/${s.id}`} className="absolute inset-0 rounded-2xl z-0" aria-label="View secret" />

        <Link href={`/secrets/${s.id}`} className="relative z-10 block text-zinc-100 text-sm leading-relaxed hover:text-zinc-200 transition-colors">
          {s.text}
        </Link>

        <div className="relative z-10 flex items-center justify-between gap-2 flex-wrap">
          <div className="flex items-center gap-3 text-xs text-zinc-600 flex-wrap">
            <span className="capitalize">{s.mood}</span>
            {!expired && (
              <span>Expires {new Date(s.expires_at).toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit' })}</span>
            )}
            <span>🙋{s.reaction_me_too} 🤯{s.reaction_wild} 🤨{s.reaction_doubtful}</span>
            {s.comment_count > 0 && <span>💬 {s.comment_count}</span>}
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={(e) => republish(e, s.id)}
              disabled={republishing === s.id}
              className="px-3 py-1 rounded-lg bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-xs transition-colors disabled:opacity-40"
            >
              {republishing === s.id ? '...' : expired ? 'Revive' : 'Extend 24h'}
            </button>
            <button
              onClick={(e) => deleteSecret(e, s.id)}
              disabled={deleting === s.id}
              className="px-3 py-1 rounded-lg bg-zinc-800 hover:bg-red-900/60 text-zinc-500 hover:text-red-400 text-xs transition-colors disabled:opacity-40"
            >
              {deleting === s.id ? '...' : 'Delete'}
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <main className="max-w-xl mx-auto px-4 py-8 space-y-8">
      <div className="flex items-center gap-3">
        <button onClick={() => router.push('/')} className="text-zinc-500 hover:text-zinc-300 text-sm">← Back</button>
        <h1 className="text-lg font-semibold">My Secrets</h1>
        <span className="text-sm text-zinc-600 ml-auto">{secrets.length} posted</span>
      </div>

      {secrets.length === 0 ? (
        <p className="text-center text-zinc-600 py-16">You haven&apos;t posted any secrets yet.</p>
      ) : (
        <>
          {active.length > 0 && (
            <div className="space-y-3">
              {active.map((s) => <SecretCard key={s.id} s={s} expired={false} />)}
            </div>
          )}

          {dead.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <span className="text-sm font-semibold text-zinc-500">Dead Secrets</span>
                <div className="flex-1 h-px bg-zinc-800" />
                <span className="text-xs text-zinc-700">{dead.length}</span>
              </div>
              {dead.map((s) => <SecretCard key={s.id} s={s} expired={true} />)}
            </div>
          )}
        </>
      )}
    </main>
  )
}
