'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { use } from 'react'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

interface Whisper {
  id: string
  text: string
  created_at: string
  sender_id: string
  read_at: string | null
}

function timeAgo(iso: string) {
  const diff = Date.now() - new Date(iso).getTime()
  const m = Math.floor(diff / 60_000)
  if (m < 1) return 'just now'
  if (m < 60) return `${m}m ago`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

export default function WhisperThreadPage({
  params,
}: {
  params: Promise<{ secretId: string; otherId: string }>
}) {
  const { secretId, otherId } = use(params)
  const [whispers, setWhispers] = useState<Whisper[]>([])
  const [myId, setMyId] = useState<string | null>(null)
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [loading, setLoading] = useState(true)
  const bottomRef = useRef<HTMLDivElement>(null)
  const router = useRouter()

  useEffect(() => {
    const supabase = getSupabaseBrowser()
    supabase.auth.getUser().then(async ({ data }) => {
      if (!data.user) { router.push('/'); return }
      const userId = data.user.id
      setMyId(userId)

      const { data: rows } = await supabase
        .from('whispers')
        .select('id, text, created_at, sender_id, read_at')
        .eq('secret_id', secretId)
        .or(
          `and(sender_id.eq.${userId},receiver_id.eq.${otherId}),and(sender_id.eq.${otherId},receiver_id.eq.${userId})`
        )
        .order('created_at', { ascending: true })

      setWhispers((rows as Whisper[]) ?? [])
      setLoading(false)

      await supabase
        .from('whispers')
        .update({ read_at: new Date().toISOString() })
        .eq('secret_id', secretId)
        .eq('sender_id', otherId)
        .eq('receiver_id', userId)
        .is('read_at', null)
    })
  }, [secretId, otherId, router])

  useEffect(() => {
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
  }, [whispers])

  async function send() {
    if (!myId || text.trim().length < 1 || sending) return
    setSending(true)
    const supabase = getSupabaseBrowser()
    const { data, error } = await supabase
      .from('whispers')
      .insert({
        secret_id: secretId,
        sender_id: myId,
        receiver_id: otherId,
        text: text.trim(),
      })
      .select('id, text, created_at, sender_id, read_at')
      .single()
    if (!error && data) {
      setWhispers((prev) => [...prev, data as Whisper])
      setText('')
    }
    setSending(false)
  }

  function handleKey(e: React.KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  if (loading) {
    return (
      <main className="max-w-xl mx-auto px-4 py-12 flex justify-center">
        <div className="w-6 h-6 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </main>
    )
  }

  return (
    <main className="max-w-xl mx-auto px-4 py-8 flex flex-col min-h-screen">
      <div className="flex items-center gap-3 mb-6">
        <button
          onClick={() => router.push('/inbox')}
          className="text-zinc-500 hover:text-zinc-300 text-sm"
        >
          ← Inbox
        </button>
        <span className="text-zinc-600 text-xs font-mono">#{otherId.slice(-6)}</span>
      </div>

      <div className="flex-1 space-y-3 pb-4">
        {whispers.length === 0 && (
          <p className="text-center text-zinc-600 text-sm py-12">No messages yet. Say something.</p>
        )}
        {whispers.map((w) => {
          const mine = w.sender_id === myId
          return (
            <div key={w.id} className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[80%] rounded-2xl px-4 py-2.5 ${
                  mine
                    ? 'bg-violet-600 text-white rounded-br-sm'
                    : 'bg-zinc-800 text-zinc-100 rounded-bl-sm'
                }`}
              >
                <p className="text-sm leading-relaxed break-words">{w.text}</p>
                <p className={`text-[10px] mt-1 ${mine ? 'text-violet-300' : 'text-zinc-500'}`}>
                  {timeAgo(w.created_at)}
                </p>
              </div>
            </div>
          )
        })}
        <div ref={bottomRef} />
      </div>

      <div className="sticky bottom-4 bg-zinc-900 border border-zinc-700 rounded-2xl p-3 flex gap-2 items-end shadow-xl">
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={handleKey}
          placeholder="Whisper something... (Enter to send)"
          rows={1}
          maxLength={500}
          className="flex-1 bg-transparent text-zinc-100 placeholder-zinc-600 resize-none outline-none text-sm leading-relaxed"
        />
        <button
          onClick={send}
          disabled={sending || text.trim().length < 1}
          className="px-3 py-1.5 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 text-white text-sm font-medium transition-colors flex-shrink-0"
        >
          {sending ? '...' : 'Send'}
        </button>
      </div>
    </main>
  )
}
