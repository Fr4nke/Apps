import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: '24h Secret — Share anonymously, gone in 24h',
  description: 'Share secrets anonymously. No account. No trace. Gone after 24 hours.',
  openGraph: {
    title: '24h Secret',
    description: 'A world of secrets. Gone in 24h.',
    type: 'website',
    images: [{ url: '/logo.png', width: 512, height: 512 }],
  },
  twitter: {
    card: 'summary',
    title: '24h Secret',
    description: 'A world of secrets. Gone in 24h.',
    images: ['/logo.png'],
  },
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body suppressHydrationWarning className="min-h-screen bg-zinc-950 text-zinc-100 antialiased">
        {children}
      </body>
    </html>
  )
}
