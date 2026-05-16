# Apps repo (Fr4nke/24HS)

This repo holds **multiple sibling projects** for the "24h Secret" product. Pick the right one before editing:

| Folder | What | Stack | Read its own CLAUDE.md? |
|---|---|---|---|
| **`24h-secret-android/`** | Android client | Kotlin + Jetpack Compose | this file covers it |
| **`24h-secret/`** | Web client | Next.js 16 + React 19 + Tailwind v4 | yes — see `24h-secret/AGENTS.md` |
| `24h Secret/` | empty (legacy folder, can delete) | — | — |

Both clients hit the **same Supabase backend** (project `jghtqgsnevtzxhscfirg`). Schema, RLS, RPCs, and OAuth config below apply to both.

The `.github/workflows/build-apk.yml` only builds the Android APK — the Next.js app is deployed separately (check `24h-secret/` for deployment config).

---

# 24h Secret — Android app

Anonymous-confession app. Users post short "secrets" that disappear after 24 hours. Reactions (🙋 Me Too, 🤯 Wild, 🤨 Doubtful), geo-filtering, optional Google Sign-In for private "whispers" between users.

## Stack

- **Android (Kotlin + Jetpack Compose)** — minSdk 26, targetSdk 35, AGP via Compose BOM `2024.09.00`
- **Supabase** (PostgREST + Auth + Postgres RLS) — project `jghtqgsnevtzxhscfirg`
- **Google Sign-In** via legacy `play-services-auth:21.2.0` (`requestIdToken(webClientId)`)
- **GitHub Actions** — builds debug APK on every push to `main`, attaches to GitHub Release

## Repo layout

```
Apps/                                # this repo (origin: github.com/Fr4nke/24HS)
├── 24h-secret/                      # Next.js web client (its own CLAUDE.md/AGENTS.md)
├── 24h-secret-android/              # Android project
│   ├── app/
│   │   ├── build.gradle.kts         # explicit debug signingConfig → ~/.android/debug.keystore
│   │   └── src/main/java/no/secret24h/
│   │       ├── MainActivity.kt      # calls UserSession.init(ctx) on startup
│   │       ├── data/
│   │       │   ├── Api.kt           # PostgREST: getSecrets, postSecret, react
│   │       │   ├── AuthApi.kt       # Supabase /auth/v1/token (Google id_token grant)
│   │       │   ├── Config.kt        # SUPABASE_URL/KEY/GOOGLE_WEB_CLIENT_ID (overwritten in CI)
│   │       │   ├── MySecretsApi.kt  # GET secrets?user_id=eq.<uid>
│   │       │   ├── WhisperApi.kt    # whispers table + get_whisper_conversations RPC
│   │       │   ├── UserSession.kt   # SharedPreferences-backed, persists login
│   │       │   ├── LocationHelper.kt
│   │       │   └── SecretsViewModel.kt
│   │       └── ui/
│   │           ├── MainScreen.kt    # feed; combined sort+filter row with dropdowns
│   │           ├── ComposeBox.kt    # write-a-secret input
│   │           ├── SecretCard.kt    # one secret + reaction buttons + share
│   │           ├── AuthScreen.kt    # Google Sign-In flow
│   │           ├── InboxScreen.kt   # whisper conversations list
│   │           ├── WhisperScreen.kt # 1:1 whisper chat
│   │           ├── MySecretsScreen.kt
│   │           ├── ShareImageGenerator.kt
│   │           └── Theme.kt         # Smolder palette + EMOTION_COLORS
│   └── fix_db.sql                   # idempotent DB migration (whispers, RLS, RPC)
├── .github/workflows/build-apk.yml  # CI: restore keystore → assembleDebug → release
└── CLAUDE.md                        # this file
```

## Build & release

CI handles everything. **Just commit + push to `main`**:
```bash
git push   # triggers GitHub Actions, publishes APK to /releases/latest
```

Latest APK: <https://github.com/Fr4nke/24HS/releases/latest>

Manual trigger: <https://github.com/Fr4nke/24HS/actions/workflows/build-apk.yml> → "Run workflow"

### GitHub Actions secrets (set; do not regenerate)

| Secret | What |
|---|---|
| `SUPABASE_URL` | `https://jghtqgsnevtzxhscfirg.supabase.co` |
| `SUPABASE_ANON_KEY` | Supabase anon key |
| `GOOGLE_WEB_CLIENT_ID` | `534999979814-272pdtj98nv4cp0ohb03pgm3vjp088jr.apps.googleusercontent.com` |
| `DEBUG_KEYSTORE_B64` | base64 of `~/.android/debug.keystore` — **stable, do not change** |

If `DEBUG_KEYSTORE_B64` is ever regenerated, the APK's SHA-1 changes and Google Sign-In breaks with **code 10**. Set via GitHub API (libsodium-encrypted), never via the web form — the textarea silently corrupts long binary blobs. See `set_secret.js` pattern in past transcripts.

### Signing fingerprint

```
SHA-1: 8C:A1:53:53:60:B7:13:FC:23:3B:32:52:BA:BA:2F:72:02:E8:2E:C4
```

Registered on the Android OAuth client at:
<https://console.cloud.google.com/auth/clients/534999979814-ejhjle41cmgo20acd479dk5qdrg7odb8.apps.googleusercontent.com?project=h-secret-496313>

Package name: `no.secret24h`

## Supabase

Project: `jghtqgsnevtzxhscfirg` ("24h Secret")
SQL editor: <https://supabase.com/dashboard/project/jghtqgsnevtzxhscfirg/sql/new>

### Tables

- **`secrets`** — `id, text, mood, expires_at, reaction_*, total_reactions (GENERATED), user_id?, lat?, lng?`
  - `mood IN ('relief','shame','pride','regret','longing','anger','fear','joy','other')` — English only
  - `text` 5–280 chars
  - RLS: anyone reads; insert allowed if `user_id IS NULL OR = auth.uid()`; owner update/delete
- **`whispers`** — `id, secret_id, sender_id, receiver_id, text, read_at, created_at`
  - RLS: visible to sender or receiver; insert by sender; receiver marks read

### RPCs

- `secrets_near(user_lat, user_lng, radius_km, mood_filter?)` — distance-filtered feed
- `increment_reaction(secret_id, col_name)` — atomic reaction bump
- `get_whisper_conversations()` — inbox view, returns `{secret_id, secret_text, other_user_id, last_message, last_message_at, unread_count}`
- `republish_secret(p_secret_id)` — resets expiry to now()+24h

The full idempotent migration lives in `24h-secret-android/fix_db.sql`.

## UI conventions

- Smolder dark palette (`Theme.kt`): backgrounds `#3A0F15 → #120608`, accent `#FF7A4D`, text `#FFE8DC`
- Filter row: one horizontal-scroll row with `[Latest] [Top 24h+] [Distance ▾] [Mood ▾*] [Reactions ▾*]` (*only in Top mode)
- Reactions are flat `Column(emoji+count, label)` — see `FlatReactionButton`
- Glow-behind blur on cards via `Modifier.glowBehind`

## Common pitfalls

- **DB constraint `secrets_mood_check`** used to allow Norwegian mood names; now English only. If you see code 23514 on post, re-run `fix_db.sql`.
- **Google Sign-In code 10** = SHA-1 mismatch between APK and Google Cloud Android client. Verify with: `apksigner verify --print-certs app.apk | grep SHA-1`.
- **Inbox "Error: 404"** = `get_whisper_conversations` RPC missing → run `fix_db.sql`.
- **Compose-BOM 2024.09.00 + AGP 8.x** generates a fresh debug keystore per build unless `signingConfigs.debug` is explicitly set in `build.gradle.kts`. We set it explicitly.
- **Worktree confusion**: do NOT work from `C:\Users\Frank\Documents\PT\.claude\worktrees\…`. That's a sibling repo (`PT`). Run `claude` from `C:\Users\Frank\Documents\Apps\` directly.

## Resuming a session

```bash
cd C:\Users\Frank\Documents\Apps
claude --resume   # pick the most recent 24h-secret session
```

## Common commands

```bash
# Check latest build
curl -sH "Authorization: Bearer $GH_TOKEN" \
  "https://api.github.com/repos/Fr4nke/24HS/actions/runs?per_page=1" | jq

# Verify APK signing
"$ANDROID_HOME/build-tools/35.0.0/apksigner.bat" verify --print-certs app.apk

# Local APK SHA-1 from keystore
keytool -list -v -keystore ~/.android/debug.keystore -storepass android | grep SHA1
```
