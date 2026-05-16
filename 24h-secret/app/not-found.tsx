import Link from 'next/link'

export default function NotFound() {
  return (
    <main className="max-w-xl mx-auto px-4 py-24 text-center space-y-4">
      <p className="text-5xl">🤫</p>
      <h1 className="text-xl font-semibold text-zinc-200">Nothing to see here</h1>
      <p className="text-zinc-500 text-sm">This secret may have expired or never existed.</p>
      <Link href="/" className="inline-block mt-4 text-sm text-violet-400 hover:text-violet-300">
        ← Back to feed
      </Link>
    </main>
  )
}
