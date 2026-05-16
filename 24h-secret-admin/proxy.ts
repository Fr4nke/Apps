import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function proxy(req: NextRequest) {
  const auth = req.cookies.get('admin_auth')?.value
  if (auth !== process.env.ADMIN_PASSWORD) {
    return NextResponse.redirect(new URL('/', req.url))
  }
  return NextResponse.next()
}

export const config = { matcher: '/admin/:path*' }
