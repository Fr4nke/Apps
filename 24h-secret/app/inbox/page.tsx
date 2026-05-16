'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

interface Conversation {
  secret_id: string
  secret_text: string
  other_user_id: string
  last_message: string
  last_message_at: string
  unread_count: number
}

export default function InboxPage() {
  const [convos, setConvos] = useState<Conversation[]>([])
  const [loading, setLoading] = useState(true)
  const [authed, setAuthed] = useState<boolean | null>(null)
  const router = useRouter()

  useEffect(() => {
    const supabase = getSupabaseBrowser()
    supabase.auth.getUser().then(async ({ data }) => {
      if (!data.user) { setAuthed(false); setLoading(false); return }
      setAuthed(true)
      const { data: rows } = await supabase.rpc('get_whisper_conversations')
      setConvos((rows as unknown as Conversation[] | null) ?? [])
      setLoading(false)
    })
  }, [])

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
        <p>Sign in to see your inbox.</p>
        <button onClick={() => router.push('/')} className="mt-4 text-sm text-violet-400 hover:text-violet-300">
          ← Back
        </button>
      </main>
    )
  }

  return (
    <main className="max-w-xl mx-auto px-4 py-8 space-y-6">
      <div className="flex items-center gap-3">
        <button onClick={() => router.push('/')} className="text-zinc-500 hover:text-zinc-300 text-sm">← Back</button>
        <h1 className="text-lg font-semibold">Inbox</h1>
      </div>

      {convos.length === 0 ? (
        <p className="text-center text-zinc-600 py-16">No whispers yet.</p>
      ) : (
        <div className="space-y-3">
          {convos.map((c) => (
            <Link
              key={`${c.secret_id}-${c.other_user_id}`}
              href={`/whispers/${c.secret_id}/${c.other_user_id}`}
              className="block bg-zinc-900 border border-zinc-800 rounded-2xl p-4 space-y-2 hover:border-zinc-700 transition-colors"
            >
              <p className="text-zinc-500 text-xs line-clamp-1">
                Re: <span className="text-zinc-400">{c.secret_text}</span>
              </p>
              <p className="text-zinc-200 text-sm line-clamp-2">{c.last_message}</p>
              <div className="flex items-center justify-between">
                <span className="text-zinc-600 text-xs">
                  {new Date(c.last_message_at).toLocaleString('en', { dateStyle: 'short', timeStyle: 'short' })}
                </span>
                {c.unread_count > 0 && (
                  <span className="bg-violet-600 text-white text-xs font-bold px-2 py-0.5 rounded-full">
                    {c.unread_count} new
                  </span>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  )
}
