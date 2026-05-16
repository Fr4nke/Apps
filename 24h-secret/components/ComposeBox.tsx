'use client'

import { useState } from 'react'
import type { Mood, Secret } from '@/lib/supabase'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

const MOODS: { value: Mood; label: string; emoji: string }[] = [
  { value: 'relief',  label: 'Relief',  emoji: '😮' },
  { value: 'shame',   label: 'Shame',   emoji: '😔' },
  { value: 'pride',   label: 'Pride',   emoji: '😤' },
  { value: 'regret',  label: 'Regret',  emoji: '😞' },
  { value: 'longing', label: 'Longing', emoji: '💭' },
  { value: 'anger',   label: 'Anger',   emoji: '😡' },
  { value: 'fear',    label: 'Fear',    emoji: '😨' },
  { value: 'joy',     label: 'Joy',     emoji: '😄' },
  { value: 'other',   label: 'Other',   emoji: '🤔' },
]

interface Props {
  onPublished: (secret: Secret) => void
}

export default function ComposeBox({ onPublished }: Props) {
  const [text, setText] = useState('')
  const [mood, setMood] = useState<Mood>('other')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (text.trim().length < 5) return
    setLoading(true)
    setError('')

    try {
      const { data: { session } } = await getSupabaseBrowser().auth.getSession()
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (session?.access_token) headers['Authorization'] = `Bearer ${session.access_token}`
      const res = await fetch('/api/secrets', {
        method: 'POST',
        headers,
        body: JSON.stringify({ text: text.trim(), mood }),
      })
      const json = await res.json()
      if (!res.ok) throw new Error(json.error)
      setText('')
      onPublished(json)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Noe gikk galt')
    } finally {
      setLoading(false)
    }
  }

  const remaining = 280 - text.length
  const tooShort = text.trim().length < 5 && text.length > 0

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-4"
    >
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Share a secret anonymously... 🤫"
        maxLength={280}
        rows={3}
        className="w-full bg-transparent text-zinc-100 placeholder-zinc-600 resize-none outline-none text-base leading-relaxed"
      />

      <div className="flex items-center justify-between gap-2 flex-wrap">
        <div className="flex gap-2 flex-wrap">
          {MOODS.map((m) => (
            <button
              key={m.value}
              type="button"
              onClick={() => setMood(m.value)}
              className={`px-3 py-1 rounded-full text-sm transition-colors ${
                mood === m.value
                  ? 'bg-violet-600 text-white'
                  : 'bg-zinc-800 text-zinc-400 hover:bg-zinc-700'
              }`}
            >
              {m.emoji} {m.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3 ml-auto">
          <span
            className={`text-sm tabular-nums ${
              remaining < 20 ? 'text-red-400' : 'text-zinc-600'
            }`}
          >
            {remaining}
          </span>
          <button
            type="submit"
            disabled={loading || text.trim().length < 5}
            className="px-5 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed text-white text-sm font-medium transition-colors"
          >
            {loading ? 'Posting...' : 'Share secret'}
          </button>
        </div>
      </div>

      {tooShort && (
        <p className="text-xs text-zinc-500">At least 5 characters required</p>
      )}
      {error && <p className="text-xs text-red-400">{error}</p>}
    </form>
  )
}
