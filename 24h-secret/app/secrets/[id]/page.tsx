'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { use } from 'react'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

interface Secret {
  id: string
  text: string
  mood: string
  expires_at: string
  reaction_me_too: number
  reaction_wild: number
  reaction_doubtful: number
  user_id: string | null
}

interface Comment {
  id: string
  text: string
  created_at: string
  parent_id: string | null
  user_id: string | null
}

const MOOD_COLORS: Record<string, string> = {
  relief: 'bg-blue-900/40 text-blue-300',
  shame: 'bg-red-900/40 text-red-300',
  pride: 'bg-yellow-900/40 text-yellow-300',
  regret: 'bg-orange-900/40 text-orange-300',
  longing: 'bg-purple-900/40 text-purple-300',
  anger: 'bg-red-900/60 text-red-200',
  fear: 'bg-indigo-900/40 text-indigo-300',
  joy: 'bg-green-900/40 text-green-300',
  other: 'bg-zinc-800 text-zinc-400',
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

function CommentBubble({ children }: { children: React.ReactNode }) {
  return (
    <div className="w-7 h-7 rounded-full bg-zinc-800 flex items-center justify-center text-xs flex-shrink-0">
      🤫
    </div>
  )
}

function commentHandle(comment: Comment, currentUserId: string | null) {
  if (!comment.user_id) return 'Anonymous'
  if (comment.user_id === currentUserId) return 'You'
  return `#${comment.user_id.slice(-6)}`
}

interface CommentRowProps {
  comment: Comment
  replies: Comment[]
  expired: boolean
  currentUserId: string | null
  ownerUserId: string | null
  onReply: (id: string) => void
  replyingTo: string | null
  replyText: string
  setReplyText: (t: string) => void
  onSubmitReply: (parentId: string) => void
  posting: boolean
}

function CommentRow({ comment, replies, expired, currentUserId, ownerUserId, onReply, replyingTo, replyText, setReplyText, onSubmitReply, posting }: CommentRowProps) {
  const handle = commentHandle(comment, currentUserId)
  const isOwn = comment.user_id && comment.user_id === currentUserId
  const isOwner = comment.user_id && comment.user_id === ownerUserId

  return (
    <div>
      <div className="flex gap-2.5 py-3">
        <CommentBubble>{null}</CommentBubble>
        <div className="flex-1 min-w-0">
          <div className="flex items-baseline gap-2 mb-0.5">
            <span className={`text-xs font-medium ${isOwn ? 'text-violet-400' : isOwner ? 'text-amber-400' : 'text-zinc-500'}`}>{handle}</span>
            {isOwner && <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-900/40 text-amber-400 font-medium">owner</span>}
            <span className="text-xs text-zinc-700">{timeAgo(comment.created_at)}</span>
          </div>
          <p className="text-sm text-zinc-200 leading-relaxed break-words">{comment.text}</p>
          {!expired && (
            <button
              onClick={() => onReply(replyingTo === comment.id ? '' : comment.id)}
              className="mt-1 text-xs text-zinc-600 hover:text-zinc-400 transition-colors"
            >
              {replyingTo === comment.id ? 'Cancel' : 'Reply'}
            </button>
          )}
        </div>
      </div>

      {replyingTo === comment.id && (
        <div className="ml-9 mb-2 flex gap-2 items-start">
          <CommentBubble>{null}</CommentBubble>
          <div className="flex-1 bg-zinc-800/60 rounded-xl px-3 py-2 space-y-2">
            <textarea
              autoFocus
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              placeholder="Write a reply..."
              rows={2}
              maxLength={500}
              className="w-full bg-transparent text-zinc-100 placeholder-zinc-600 resize-none outline-none text-sm leading-relaxed"
            />
            <div className="flex justify-end gap-2">
              <span className="text-xs text-zinc-700 self-center">{500 - replyText.length}</span>
              <button
                onClick={() => onSubmitReply(comment.id)}
                disabled={posting || replyText.trim().length < 1}
                className="px-3 py-1 rounded-lg bg-violet-600 hover:bg-violet-500 disabled:opacity-40 text-white text-xs font-medium transition-colors"
              >
                {posting ? '...' : 'Reply'}
              </button>
            </div>
          </div>
        </div>
      )}

      {replies.length > 0 && (
        <div className="ml-9 border-l border-zinc-800 pl-3">
          {replies.map((reply) => {
            const rHandle = commentHandle(reply, currentUserId)
            const rOwn = reply.user_id && reply.user_id === currentUserId
            const rIsOwner = reply.user_id && reply.user_id === ownerUserId
            return (
              <div key={reply.id} className="flex gap-2.5 py-2.5">
                <CommentBubble>{null}</CommentBubble>
                <div className="flex-1 min-w-0">
                  <div className="flex items-baseline gap-2 mb-0.5">
                    <span className={`text-xs font-medium ${rOwn ? 'text-violet-400' : rIsOwner ? 'text-amber-400' : 'text-zinc-500'}`}>{rHandle}</span>
                    {rIsOwner && <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-amber-900/40 text-amber-400 font-medium">owner</span>}
                    <span className="text-xs text-zinc-700">{timeAgo(reply.created_at)}</span>
                  </div>
                  <p className="text-sm text-zinc-300 leading-relaxed break-words">{reply.text}</p>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

export default function SecretDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params)
  const router = useRouter()
  const [secret, setSecret] = useState<Secret | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [loading, setLoading] = useState(true)
  const [text, setText] = useState('')
  const [posting, setPosting] = useState(false)
  const [error, setError] = useState('')
  const [replyingTo, setReplyingTo] = useState<string | null>(null)
  const [replyText, setReplyText] = useState('')
  const [currentUserId, setCurrentUserId] = useState<string | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    getSupabaseBrowser().auth.getUser().then(({ data }) => {
      setCurrentUserId(data.user?.id ?? null)
    })
  }, [])

  useEffect(() => {
    async function load() {
      const [sRes, cRes] = await Promise.all([
        fetch(`/api/secrets/${id}`),
        fetch(`/api/comments?secret_id=${id}`),
      ])
      if (sRes.ok) setSecret(await sRes.json())
      if (cRes.ok) {
        const data = await cRes.json()
        setComments(Array.isArray(data) ? data : [])
      }
      setLoading(false)
    }
    load()
  }, [id])

  async function getAuthHeaders(): Promise<Record<string, string>> {
    const { data: { session } } = await getSupabaseBrowser().auth.getSession()
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    if (session?.access_token) headers['Authorization'] = `Bearer ${session.access_token}`
    return headers
  }

  async function postComment(e: React.FormEvent) {
    e.preventDefault()
    if (text.trim().length < 1) return
    setPosting(true)
    setError('')
    try {
      const res = await fetch(`/api/comments?secret_id=${id}`, {
        method: 'POST',
        headers: await getAuthHeaders(),
        body: JSON.stringify({ secret_id: id, text }),
      })
      const json = await res.json()
      if (!res.ok) { setError(json.error ?? 'Failed to post comment'); return }
      setComments((prev) => [...prev, json])
      setText('')
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 100)
    } catch {
      setError('Something went wrong. Please try again.')
    } finally {
      setPosting(false)
    }
  }

  async function submitReply(parentId: string) {
    if (replyText.trim().length < 1) return
    setPosting(true)
    try {
      const res = await fetch(`/api/comments?secret_id=${id}`, {
        method: 'POST',
        headers: await getAuthHeaders(),
        body: JSON.stringify({ secret_id: id, text: replyText, parent_id: parentId }),
      })
      const json = await res.json()
      if (res.ok) {
        setComments((prev) => [...prev, json])
        setReplyText('')
        setReplyingTo(null)
      }
    } catch { /* silent */ } finally {
      setPosting(false)
    }
  }

  function handleReply(commentId: string) {
    if (replyingTo === commentId) {
      setReplyingTo(null)
      setReplyText('')
    } else {
      setReplyingTo(commentId)
      setReplyText('')
    }
  }

  if (loading) {
    return (
      <main className="max-w-xl mx-auto px-4 py-12 flex justify-center">
        <div className="w-6 h-6 border-2 border-violet-500 border-t-transparent rounded-full animate-spin" />
      </main>
    )
  }

  if (!secret) {
    return (
      <main className="max-w-xl mx-auto px-4 py-12 text-center text-zinc-500">
        <p>Secret not found or expired.</p>
        <button onClick={() => router.push('/')} className="mt-4 text-sm text-violet-400 hover:text-violet-300">← Back</button>
      </main>
    )
  }

  const expired = new Date(secret.expires_at) < new Date()
  const moodColor = MOOD_COLORS[secret.mood] ?? MOOD_COLORS['other']
  const topLevel = comments.filter((c) => !c.parent_id)
  const repliesFor = (cid: string) => comments.filter((c) => c.parent_id === cid)
  const totalCount = comments.length

  return (
    <main className="max-w-xl mx-auto px-4 py-8 space-y-6">
      <button onClick={() => router.back()} className="text-zinc-500 hover:text-zinc-300 text-sm flex items-center gap-1">
        ← Back
      </button>

      {/* Secret card */}
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-5 space-y-3 relative">
        {secret.user_id && (
          <span className="absolute top-3 right-4 text-[10px] text-zinc-700 font-mono">
            #{secret.user_id.slice(-6)}
          </span>
        )}
        <p className="text-zinc-100 text-base leading-relaxed whitespace-pre-wrap break-words pr-14">{secret.text}</p>
        <div className="flex items-center gap-3 flex-wrap">
          <span className={`text-xs px-2 py-0.5 rounded-full ${moodColor}`}>{secret.mood}</span>
          <span className="text-xs text-zinc-600">
            {expired ? '⏳ Expired' : `⏳ Expires ${new Date(secret.expires_at).toLocaleTimeString('en', { hour: '2-digit', minute: '2-digit' })}`}
          </span>
          <span className="text-xs text-zinc-600 ml-auto">🙋 {secret.reaction_me_too} · 🤯 {secret.reaction_wild} · 🤨 {secret.reaction_doubtful}</span>
        </div>
      </div>

      {/* Comments header */}
      <div className="flex items-center gap-2">
        <span className="text-sm font-semibold text-zinc-300">{totalCount} {totalCount === 1 ? 'comment' : 'comments'}</span>
        <div className="flex-1 h-px bg-zinc-800" />
      </div>

      {/* Threaded comments */}
      <div className="divide-y divide-zinc-800/60">
        {topLevel.length === 0 ? (
          <p className="text-zinc-600 text-sm text-center py-6">No comments yet. Be the first.</p>
        ) : (
          topLevel.map((c) => (
            <CommentRow
              key={c.id}
              comment={c}
              replies={repliesFor(c.id)}
              expired={expired}
              currentUserId={currentUserId}
              ownerUserId={secret.user_id}
              onReply={handleReply}
              replyingTo={replyingTo}
              replyText={replyText}
              setReplyText={setReplyText}
              onSubmitReply={submitReply}
              posting={posting}
            />
          ))
        )}
        <div ref={bottomRef} />
      </div>

      {/* New top-level comment input */}
      {!expired && (
        <form onSubmit={postComment} className="sticky bottom-4">
          <div className="bg-zinc-900 border border-zinc-700 rounded-2xl p-4 space-y-3 shadow-xl">
            <textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Write a comment..."
              rows={2}
              maxLength={500}
              className="w-full bg-transparent text-zinc-100 placeholder-zinc-600 resize-none outline-none text-sm leading-relaxed"
            />
            {error && <p className="text-xs text-red-400">{error}</p>}
            <div className="flex items-center justify-between">
              <span className="text-xs text-zinc-700">{500 - text.length}</span>
              <button
                type="submit"
                disabled={posting || text.trim().length < 1}
                className="px-4 py-1.5 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 text-white text-sm font-medium transition-colors"
              >
                {posting ? 'Posting...' : 'Comment'}
              </button>
            </div>
          </div>
        </form>
      )}
    </main>
  )
}
