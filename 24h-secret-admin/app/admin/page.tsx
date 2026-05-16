import { getAdminClient } from '@/lib/supabase-admin'
import { logout, deleteSecret, deleteWhisper } from './actions'
import DeleteButton from './DeleteButton'

type Tab = 'stats' | 'secrets' | 'whispers'

const now = new Date()
const ago = (days: number) => new Date(now.getTime() - days * 86400000).toISOString()

const FREE_TIER = {
  db_mb: 500,
  connections: 60,
  api_calls_month: 500_000,
  secrets_est_limit: 1_000_000,
}

async function fetchStats(db: ReturnType<typeof getAdminClient>) {
  const [
    { count: secretsTotal, error: dbError },
    { count: secretsMonth },
    { count: secretsWeek },
    { count: secretsDay },
    { count: whispersTotal },
    { count: whispersMonth },
    { count: whispersWeek },
    { count: whispersDay },
    { count: commentsTotal },
    { data: reactionData },
  ] = await Promise.all([
    db.from('secrets').select('*', { count: 'exact', head: true }),
    db.from('secrets').select('*', { count: 'exact', head: true }).gte('created_at', ago(30)),
    db.from('secrets').select('*', { count: 'exact', head: true }).gte('created_at', ago(7)),
    db.from('secrets').select('*', { count: 'exact', head: true }).gte('created_at', ago(1)),
    db.from('whispers').select('*', { count: 'exact', head: true }),
    db.from('whispers').select('*', { count: 'exact', head: true }).gte('created_at', ago(30)),
    db.from('whispers').select('*', { count: 'exact', head: true }).gte('created_at', ago(7)),
    db.from('whispers').select('*', { count: 'exact', head: true }).gte('created_at', ago(1)),
    db.from('comments').select('*', { count: 'exact', head: true }),
    db.from('secrets').select('total_reactions'),
  ])

  if (dbError) throw new Error(`Supabase feil (${dbError.code}): ${dbError.message || dbError.hint || JSON.stringify(dbError)}`)

  const totalReactions = (reactionData ?? []).reduce((s: number, r: { total_reactions: number }) => s + (r.total_reactions ?? 0), 0)

  const { data: usersData } = await db.auth.admin.listUsers({ perPage: 1 })
  const usersTotal = usersData?.total ?? 0
  const { data: allUsers } = await db.auth.admin.listUsers({ perPage: 1000 })
  const users = allUsers?.users ?? []
  const usersMonth = users.filter(u => u.created_at >= ago(30)).length
  const usersWeek = users.filter(u => u.created_at >= ago(7)).length
  const usersDay = users.filter(u => u.created_at >= ago(1)).length

  // Try to get real DB stats via RPC (requires db_stats.sql to be run first)
  let dbSizeBytes: number | null = null
  let activeConnections: number | null = null
  try {
    const { data } = await db.rpc('get_db_stats')
    if (data) {
      dbSizeBytes = (data as { db_size_bytes: number }).db_size_bytes
      activeConnections = (data as { active_connections: number }).active_connections
    }
  } catch { /* function not set up yet */ }

  return {
    secrets: { total: secretsTotal ?? 0, month: secretsMonth ?? 0, week: secretsWeek ?? 0, day: secretsDay ?? 0 },
    whispers: { total: whispersTotal ?? 0, month: whispersMonth ?? 0, week: whispersWeek ?? 0, day: whispersDay ?? 0 },
    comments: { total: commentsTotal ?? 0 },
    users: { total: usersTotal, month: usersMonth, week: usersWeek, day: usersDay },
    totalReactions,
    dbSizeBytes,
    activeConnections,
  }
}

export default async function AdminPage({
  searchParams,
}: {
  searchParams: Promise<{ tab?: string }>
}) {
  const { tab = 'stats' } = await searchParams as { tab?: Tab }
  const db = getAdminClient()

  const [stats, secretsResult, whispersResult, usersListResult] = await Promise.all([
    tab === 'stats' ? fetchStats(db) : null,
    tab === 'secrets'
      ? db.from('secrets')
          .select('id,text,mood,expires_at,reaction_me_too,reaction_wild,reaction_doubtful,total_reactions,user_id,created_at')
          .order('created_at', { ascending: false })
          .limit(200)
      : { data: [] },
    tab === 'whispers'
      ? db.from('whispers')
          .select('id,secret_id,sender_id,receiver_id,text,created_at,read_at')
          .order('created_at', { ascending: false })
          .limit(200)
      : { data: [] },
    tab === 'secrets' || tab === 'whispers'
      ? db.auth.admin.listUsers({ perPage: 1000 })
      : { data: { users: [] } },
  ])

  const secrets = (secretsResult as { data: unknown[] }).data ?? []
  const whispers = (whispersResult as { data: unknown[] }).data ?? []
  const userEmailMap = new Map<string, string>(
    ((usersListResult as { data: { users: { id: string; email?: string }[] } }).data?.users ?? [])
      .map(u => [u.id, u.email ?? ''])
  )
  const nowIso = now.toISOString()

  return (
    <div style={{ minHeight: '100vh' }}>
      {/* Header */}
      <header style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: '14px 24px',
        borderBottom: '1px solid rgba(255,232,220,0.08)',
        background: '#1a0a0d',
        position: 'sticky',
        top: 0,
        zIndex: 10,
      }}>
        <h1 style={{ margin: 0, fontSize: 17, color: '#FF7A4D', fontWeight: 600 }}>
          🔐 24h Secret Admin
        </h1>
        <form action={logout}>
          <button type="submit" style={{
            background: 'none',
            border: '1px solid rgba(255,232,220,0.15)',
            color: '#9a7070',
            borderRadius: 6,
            padding: '5px 12px',
            cursor: 'pointer',
            fontSize: 12,
          }}>
            Logout
          </button>
        </form>
      </header>

      {/* Tabs */}
      <nav style={{ display: 'flex', gap: 6, padding: '12px 24px', borderBottom: '1px solid rgba(255,232,220,0.08)' }}>
        {(['stats', 'secrets', 'whispers'] as Tab[]).map(t => (
          <a key={t} href={`/admin?tab=${t}`} style={{
            padding: '5px 14px',
            borderRadius: 100,
            fontSize: 13,
            textDecoration: 'none',
            color: tab === t ? 'white' : '#9a7070',
            background: tab === t ? '#FF7A4D' : 'transparent',
            border: tab === t ? 'none' : '1px solid rgba(255,232,220,0.1)',
          }}>
            {t.charAt(0).toUpperCase() + t.slice(1)}
          </a>
        ))}
      </nav>

      {/* Content */}
      <main style={{ padding: 24 }}>

        {/* ── Stats ── */}
        {tab === 'stats' && stats && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 32 }}>

            {/* Database health */}
            <div>
              <h2 style={{ margin: '0 0 12px', fontSize: 12, color: '#9a7070', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 500 }}>
                🗄️ Supabase Free Tier
              </h2>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, background: '#1a0a0d', border: '1px solid rgba(255,232,220,0.08)', borderRadius: 12, padding: '18px 20px' }}>
                {stats.dbSizeBytes !== null ? (
                  <DbBar
                    label="Databasestørrelse"
                    value={Math.round(stats.dbSizeBytes / 1024 / 1024)}
                    limit={FREE_TIER.db_mb}
                    unit="MB"
                    warn="Oppgrader til Supabase Pro ($25/mnd) for å fjerne grensen"
                  />
                ) : (
                  <div style={{ fontSize: 12, color: '#9a7070' }}>
                    ⚠️ Databasestørrelse ikke tilgjengelig — kjør <code style={{ color: '#FF7A4D' }}>db_stats.sql</code> i Supabase SQL Editor for å aktivere
                  </div>
                )}
                {stats.activeConnections !== null && (
                  <DbBar
                    label="Tilkoblinger"
                    value={stats.activeConnections}
                    limit={FREE_TIER.connections}
                    unit=""
                    warn="Nær maks — vurder connection pooling eller Pro-plan"
                  />
                )}
                <DbBar
                  label="Hemmeligheter (est. mot 500 MB)"
                  value={stats.secrets.total}
                  limit={FREE_TIER.secrets_est_limit}
                  unit=""
                  warn="Nærmer seg estimert kapasitet på Free tier"
                  formatValue={(v) => v.toLocaleString('no')}
                  formatLimit={(l) => `${(l / 1_000_000).toFixed(1)}M`}
                />
                <div style={{ marginTop: 8, paddingTop: 10, borderTop: '1px solid rgba(255,232,220,0.06)', fontSize: 11, color: '#9a7070', lineHeight: 1.6 }}>
                  <span style={{ color: '#facc15' }}>⏸</span> Free tier pauser etter <strong style={{ color: '#ffe8dc' }}>7 dager</strong> uten aktivitet — første besøk tar 20–30 sek å vekke opp.
                  {' '}API-kall (500 000/mnd) vises i{' '}
                  <a href="https://supabase.com/dashboard/project/jghtqgsnevtzxhscfirg/reports" target="_blank" rel="noreferrer" style={{ color: '#FF7A4D' }}>Supabase dashboard → Reports</a>.
                </div>
              </div>
            </div>

            <StatGroup label="Users" rows={[
              ['Total', stats.users.total],
              ['Last 30 days', stats.users.month],
              ['Last 7 days', stats.users.week],
              ['Last 24h', stats.users.day],
            ]} icon="👤" />
            <StatGroup label="Secrets" rows={[
              ['Total', stats.secrets.total],
              ['Last 30 days', stats.secrets.month],
              ['Last 7 days', stats.secrets.week],
              ['Last 24h', stats.secrets.day],
            ]} icon="🤫" />
            <StatGroup label="Whispers" rows={[
              ['Total', stats.whispers.total],
              ['Last 30 days', stats.whispers.month],
              ['Last 7 days', stats.whispers.week],
              ['Last 24h', stats.whispers.day],
            ]} icon="💬" />
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: 16 }}>
              <StatCard label="Total Reactions" value={stats.totalReactions} icon="⚡" />
              <StatCard label="Comments" value={stats.comments.total} icon="🗨️" />
            </div>
          </div>
        )}

        {/* ── Secrets ── */}
        {tab === 'secrets' && (
          <>
            <p style={{ color: '#9a7070', margin: '0 0 16px', fontSize: 13 }}>
              Showing {secrets.length} secrets (newest first)
            </p>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr>
                    {['ID', 'Text', 'Mood', 'Posted', 'Expires', 'User', 'Email', '🙋🤯🤨', ''].map(h => (
                      <th key={h} style={{ textAlign: 'left', padding: '8px 12px', color: '#9a7070', fontWeight: 500, borderBottom: '1px solid rgba(255,232,220,0.08)', whiteSpace: 'nowrap', fontSize: 11 }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(secrets as Record<string, unknown>[]).map(s => {
                    const expired = (s.expires_at as string) < nowIso
                    return (
                      <tr key={s.id as string} style={{ borderBottom: '1px solid rgba(255,232,220,0.04)', opacity: expired ? 0.5 : 1 }}>
                        <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{(s.id as string).slice(0, 8)}</td>
                        <td style={{ padding: '7px 12px', maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={s.text as string}>{s.text as string}</td>
                        <td style={{ padding: '7px 12px', color: '#9a7070' }}>{s.mood as string}</td>
                        <td style={{ padding: '7px 12px', color: '#9a7070', whiteSpace: 'nowrap', fontSize: 11 }}>{new Date(s.created_at as string).toLocaleDateString('no')}</td>
                        <td style={{ padding: '7px 12px', color: expired ? '#ff5f5f' : '#9a7070', whiteSpace: 'nowrap', fontSize: 11 }}>{expired ? 'Expired' : new Date(s.expires_at as string).toLocaleDateString('no')}</td>
                        <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{s.user_id ? (s.user_id as string).slice(0, 8) : '—'}</td>
                        <td style={{ padding: '7px 12px', color: '#9a7070', fontSize: 11 }}>{s.user_id ? (userEmailMap.get(s.user_id as string) ?? '—') : '—'}</td>
                        <td style={{ padding: '7px 12px', color: '#9a7070', whiteSpace: 'nowrap' }}>{s.reaction_me_too as number} · {s.reaction_wild as number} · {s.reaction_doubtful as number}</td>
                        <td style={{ padding: '7px 12px' }}>
                          <DeleteButton id={s.id as string} action={deleteSecret} />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* ── Whispers ── */}
        {tab === 'whispers' && (
          <>
            <p style={{ color: '#9a7070', margin: '0 0 16px', fontSize: 13 }}>
              Showing {whispers.length} whispers (newest first)
            </p>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                <thead>
                  <tr>
                    {['ID', 'Secret', 'From', 'To', 'Message', 'Sent', 'Read', ''].map(h => (
                      <th key={h} style={{ textAlign: 'left', padding: '8px 12px', color: '#9a7070', fontWeight: 500, borderBottom: '1px solid rgba(255,232,220,0.08)', whiteSpace: 'nowrap', fontSize: 11 }}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {(whispers as Record<string, unknown>[]).map(w => (
                    <tr key={w.id as string} style={{ borderBottom: '1px solid rgba(255,232,220,0.04)' }}>
                      <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{(w.id as string).slice(0, 8)}</td>
                      <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{(w.secret_id as string).slice(0, 8)}</td>
                      <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{(w.sender_id as string).slice(0, 8)}</td>
                      <td style={{ padding: '7px 12px', color: '#9a7070', fontFamily: 'monospace', fontSize: 11 }}>{(w.receiver_id as string).slice(0, 8)}</td>
                      <td style={{ padding: '7px 12px', maxWidth: 240, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={w.text as string}>{w.text as string}</td>
                      <td style={{ padding: '7px 12px', color: '#9a7070', whiteSpace: 'nowrap', fontSize: 11 }}>{new Date(w.created_at as string).toLocaleDateString('no')}</td>
                      <td style={{ padding: '7px 12px', color: '#9a7070', fontSize: 11 }}>{w.read_at ? '✓' : '—'}</td>
                      <td style={{ padding: '7px 12px' }}>
                        <DeleteButton id={w.id as string} action={deleteWhisper} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </main>
    </div>
  )
}

function StatCard({ label, value, icon }: { label: string; value: number; icon: string }) {
  return (
    <div style={{
      background: '#1a0a0d',
      border: '1px solid rgba(255,232,220,0.08)',
      borderRadius: 12,
      padding: '18px 20px',
    }}>
      <div style={{ fontSize: 26, fontWeight: 700, color: '#FF7A4D' }}>{value.toLocaleString()}</div>
      <div style={{ fontSize: 12, color: '#9a7070', marginTop: 4 }}>{icon} {label}</div>
    </div>
  )
}

function DbBar({
  label, value, limit, unit, warn, formatValue, formatLimit,
}: {
  label: string
  value: number
  limit: number
  unit: string
  warn: string
  formatValue?: (v: number) => string
  formatLimit?: (l: number) => string
}) {
  const pct = Math.min(100, Math.round((value / limit) * 100))
  const color = pct >= 90 ? '#f87171' : pct >= 70 ? '#facc15' : '#4ade80'
  const fv = formatValue ?? ((v) => `${v.toLocaleString('no')}${unit ? ' ' + unit : ''}`)
  const fl = formatLimit ?? ((l) => `${l.toLocaleString('no')}${unit ? ' ' + unit : ''}`)
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 5, fontSize: 12 }}>
        <span style={{ color: '#ffe8dc' }}>{label}</span>
        <span style={{ color, fontWeight: 600 }}>{fv(value)} / {fl(limit)} ({pct}%)</span>
      </div>
      <div style={{ background: 'rgba(255,255,255,0.06)', borderRadius: 100, height: 6, overflow: 'hidden' }}>
        <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 100, transition: 'width 0.3s' }} />
      </div>
      {pct >= 70 && (
        <div style={{ marginTop: 4, fontSize: 11, color: pct >= 90 ? '#f87171' : '#facc15' }}>
          {pct >= 90 ? '🚨' : '⚠️'} {warn}
        </div>
      )}
    </div>
  )
}

function StatGroup({ label, rows, icon }: { label: string; rows: [string, number][]; icon: string }) {
  return (
    <div>
      <h2 style={{ margin: '0 0 12px', fontSize: 12, color: '#9a7070', textTransform: 'uppercase', letterSpacing: 1, fontWeight: 500 }}>
        {icon} {label}
      </h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        {rows.map(([period, value]) => (
          <div key={period} style={{
            background: '#1a0a0d',
            border: '1px solid rgba(255,232,220,0.08)',
            borderRadius: 12,
            padding: '16px 18px',
          }}>
            <div style={{ fontSize: 24, fontWeight: 700, color: '#FF7A4D' }}>{value.toLocaleString()}</div>
            <div style={{ fontSize: 11, color: '#9a7070', marginTop: 4 }}>{period}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
