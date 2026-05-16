'use client'

import { useState, useEffect, useRef } from 'react'
import { useRouter } from 'next/navigation'
import type { User } from '@supabase/supabase-js'
import { getSupabaseBrowser } from '@/lib/supabase-browser'

function PersonIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" />
    </svg>
  )
}

function InboxIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="2" y="4" width="20" height="16" rx="2" />
      <path d="M2 9l10 6 10-6" />
    </svg>
  )
}

function SignOutIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <polyline points="16 17 21 12 16 7" />
      <line x1="21" y1="12" x2="9" y2="12" />
    </svg>
  )
}

function GoogleIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>
  )
}

function SignInDropdown({ onClose }: { onClose: () => void }) {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [loading, setLoading] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) onClose()
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [onClose])

  async function signInGoogle() {
    const supabase = getSupabaseBrowser()
    await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: `${window.location.origin}/auth/callback` },
    })
  }

  async function sendMagicLink(e: React.FormEvent) {
    e.preventDefault()
    if (!email.trim()) return
    setLoading(true)
    const supabase = getSupabaseBrowser()
    await supabase.auth.signInWithOtp({
      email: email.trim(),
      options: { emailRedirectTo: window.location.origin },
    })
    setSent(true)
    setLoading(false)
  }

  return (
    <div
      ref={ref}
      className="absolute right-0 top-11 z-50 w-64 bg-zinc-900 border border-zinc-700 rounded-2xl shadow-2xl p-4 space-y-3"
    >
      {sent ? (
        <div className="text-center py-2 space-y-1">
          <p className="text-zinc-100 text-sm font-medium">Check your email ✉️</p>
          <p className="text-zinc-500 text-xs">We sent a magic link to <span className="text-zinc-300">{email}</span></p>
        </div>
      ) : (
        <>
          <button
            onClick={signInGoogle}
            className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-white text-zinc-900 text-sm font-medium hover:bg-zinc-100 transition-colors"
          >
            <GoogleIcon />
            Sign in with Google
          </button>

          <div className="flex items-center gap-2">
            <div className="flex-1 h-px bg-zinc-800" />
            <span className="text-xs text-zinc-600">or</span>
            <div className="flex-1 h-px bg-zinc-800" />
          </div>

          <form onSubmit={sendMagicLink} className="space-y-2">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="your@email.com"
              className="w-full bg-zinc-800 text-zinc-100 placeholder-zinc-600 text-sm px-3 py-2 rounded-xl outline-none border border-transparent focus:border-zinc-600 transition-colors"
            />
            <button
              type="submit"
              disabled={loading || !email.trim()}
              className="w-full px-4 py-2 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 text-white text-sm font-medium transition-colors"
            >
              {loading ? 'Sending...' : 'Send magic link'}
            </button>
          </form>
        </>
      )}
    </div>
  )
}

export default function AuthHeader() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [showSignIn, setShowSignIn] = useState(false)
  const [unreadCount, setUnreadCount] = useState(0)
  const router = useRouter()

  async function fetchUnread(userId: string) {
    const supabase = getSupabaseBrowser()
    const { count } = await supabase
      .from('whispers')
      .select('*', { count: 'exact', head: true })
      .eq('receiver_id', userId)
      .is('read_at', null)
    setUnreadCount(count ?? 0)
  }

  useEffect(() => {
    const supabase = getSupabaseBrowser()
    supabase.auth.getUser().then(({ data }) => {
      setUser(data.user)
      setLoading(false)
      if (data.user) fetchUnread(data.user.id)
    })
    const { data: { subscription } } = supabase.auth.onAuthStateChange((_, session) => {
      setUser(session?.user ?? null)
      if (session?.user) fetchUnread(session.user.id)
      else setUnreadCount(0)
    })
    return () => subscription.unsubscribe()
  }, [])

  async function signOut() {
    const supabase = getSupabaseBrowser()
    await supabase.auth.signOut()
    router.push('/')
    router.refresh()
  }

  if (loading) {
    return (
      <div className="flex items-center gap-1">
        <div className="w-8 h-8 rounded-full bg-zinc-800 animate-pulse" />
        <div className="w-8 h-8 rounded-full bg-zinc-800 animate-pulse" />
      </div>
    )
  }

  const btnClass = 'w-9 h-9 flex items-center justify-center rounded-full transition-colors'

  if (user) {
    return (
      <div className="flex items-center gap-1">
        <button onClick={() => router.push('/me')} title="My secrets" className={`${btnClass} text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800`}>
          <PersonIcon />
        </button>
        <button onClick={() => router.push('/inbox')} title="Inbox" className={`${btnClass} relative text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800`}>
          <InboxIcon />
          {unreadCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[16px] h-4 bg-violet-600 text-white text-[10px] font-bold rounded-full flex items-center justify-center px-1 leading-none">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </button>
        <button onClick={signOut} title="Sign out" className={`${btnClass} text-zinc-600 hover:text-red-400 hover:bg-zinc-800`}>
          <SignOutIcon />
        </button>
      </div>
    )
  }

  return (
    <div className="relative flex items-center gap-1">
      <button
        onClick={() => setShowSignIn((v) => !v)}
        title="Sign in"
        className={`${btnClass} ${showSignIn ? 'text-zinc-100 bg-zinc-800' : 'text-zinc-500 hover:text-zinc-100 hover:bg-zinc-800'}`}
      >
        <PersonIcon />
      </button>
      <button
        onClick={() => setShowSignIn((v) => !v)}
        title="Sign in to access inbox"
        className={`${btnClass} text-zinc-500 hover:text-zinc-100 hover:bg-zinc-800`}
      >
        <InboxIcon />
      </button>
      {showSignIn && <SignInDropdown onClose={() => setShowSignIn(false)} />}
    </div>
  )
}
