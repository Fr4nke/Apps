'use client'

import { useEffect, useRef } from 'react'

interface Props {
  onClose: () => void
}

export default function AboutModal({ onClose }: Props) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKey)
    return () => document.removeEventListener('keydown', handleKey)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center px-4 bg-black/70 backdrop-blur-sm"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onClose() }}
    >
      <div
        ref={ref}
        className="w-full max-w-md bg-zinc-900 border border-zinc-700 rounded-2xl shadow-2xl overflow-hidden"
      >
        <div className="flex items-center justify-between px-5 pt-5 pb-3 border-b border-zinc-800">
          <h2 className="text-base font-semibold text-zinc-100">About 24h Secret</h2>
          <button
            onClick={onClose}
            className="text-zinc-500 hover:text-zinc-200 transition-colors text-lg leading-none"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <div className="px-5 py-4 space-y-4 text-sm text-zinc-400 max-h-[70vh] overflow-y-auto">
          <p className="text-zinc-200">
            Share something you've never dared to say out loud. Anonymously. No trace. Gone in 24 hours.
          </p>

          <Section title="How it works">
            <Item>Write a secret — no account needed</Item>
            <Item>Pick a mood that fits how you feel</Item>
            <Item>It disappears automatically after 24 hours</Item>
          </Section>

          <Section title="Reactions">
            <Item>🙋 <strong className="text-zinc-300">Me Too</strong> — you relate</Item>
            <Item>🤯 <strong className="text-zinc-300">Wild</strong> — you're surprised</Item>
            <Item>🤨 <strong className="text-zinc-300">Doubtful</strong> — you're not buying it</Item>
            <Item>Tap again to undo a reaction</Item>
          </Section>

          <Section title="Comments">
            <Item>Reply to secrets — also anonymously</Item>
            <Item>Reply directly to a comment in threads</Item>
            <Item>Comments disappear when the secret expires</Item>
          </Section>

          <Section title="Whispers">
            <Item>Sign in to send private messages to the owner of a secret</Item>
            <Item>Only visible to you and the recipient</Item>
            <Item>Find them in your inbox</Item>
          </Section>

          <Section title="Privacy">
            <Item>Secrets posted without an account are fully anonymous</Item>
            <Item>Signed-in users appear only as an anonymous ID (#abc123)</Item>
            <Item>We do not store IP addresses</Item>
          </Section>

          <p className="text-zinc-600 text-xs pt-1">
            24h Secret · Everything fades. Nothing lasts.
          </p>
        </div>
      </div>
    </div>
  )
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">{title}</p>
      <ul className="space-y-1">{children}</ul>
    </div>
  )
}

function Item({ children }: { children: React.ReactNode }) {
  return (
    <li className="flex gap-2 text-zinc-400">
      <span className="text-zinc-700 mt-0.5 flex-shrink-0">–</span>
      <span>{children}</span>
    </li>
  )
}
