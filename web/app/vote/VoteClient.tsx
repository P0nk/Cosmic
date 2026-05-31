'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import Sprite from '@/components/maple/Sprite'
import SectionBanner from '@/components/maple/SectionBanner'

/* ── Countdown hook ── */
function useCountdown(targetMs: number) {
  const [now, setNow] = useState(Date.now())
  useEffect(() => {
    const i = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(i)
  }, [])
  return Math.max(0, targetMs - now)
}

function fmt(ms: number) {
  if (ms <= 0) return 'READY'
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${String(h).padStart(2, '0')}h ${String(m).padStart(2, '0')}m ${String(sec).padStart(2, '0')}s`
}

/* ── Types ── */
export type Site = {
  id: string
  name: string
  logo: string
  cdHours: number
  url: string
  urlNoUser: string
  desc: string
  reward: string
}

export type TopVoter = { name: string; votepoints: number }

/* ── Vote Card ── */
function VoteCard({
  site,
  nextAvailable,
  onVote,
  username,
}: {
  site: Site
  nextAvailable: number
  onVote: (site: Site) => void
  username: string | null
}) {
  const remaining = useCountdown(nextAvailable)
  const ready = remaining === 0
  const canVote = ready && !!username

  return (
    <div className="relative">
      <div className="ms-pixel-panel flex flex-col overflow-hidden">
        {/* Title bar */}
        <div
          style={{
            background: 'linear-gradient(to bottom, #6a4830 0%, #4a3220 50%, #3a2418 100%)',
            borderBottom: '3px solid #2a1810',
            padding: '10px 14px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontFamily: 'var(--ms-font-d)',
            fontSize: '11px',
            color: '#ffd96b',
            letterSpacing: '1px',
            textShadow: '2px 2px 0 #1a0a04',
          }}
        >
          <span>🗳️ {site.name.toUpperCase()}</span>
          <span
            style={{
              background: '#c64b1b',
              color: '#fff',
              fontSize: '9px',
              padding: '3px 8px',
              border: '2px solid #1a0a04',
              boxShadow: '2px 2px 0 rgba(0,0,0,0.4)',
              letterSpacing: '1px',
            }}
          >
            {site.reward}
          </span>
        </div>

        {/* Body */}
        <div
          className="flex items-center gap-4 p-4"
          style={{ background: 'var(--ms-npc-bg)' }}
        >
          {/* Logo box */}
          <div
            className="ms-pixel-panel flex items-center justify-center shrink-0"
            style={{ width: 72, height: 72, background: '#f0dfb0' }}
          >
            <span
              style={{
                fontFamily: 'var(--ms-font-d)',
                fontSize: '16px',
                color: '#2a1810',
                textShadow: '1px 1px 0 #fff5b0',
              }}
            >
              {site.logo}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <div
              style={{
                fontFamily: 'var(--ms-font-d)',
                fontSize: '11px',
                color: '#c64b1b',
                letterSpacing: '1px',
                marginBottom: 6,
                lineHeight: 1.3,
              }}
            >
              VOTE FOR SHINYMS
            </div>
            <div
              style={{
                fontFamily: 'var(--ms-font-b)',
                fontSize: '20px',
                color: '#4a3220',
                lineHeight: 1.25,
              }}
            >
              {site.desc}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div
          className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3"
          style={{
            background: '#f0dfb0',
            borderTop: '2px dashed var(--ms-slot-shadow)',
          }}
        >
          <span
            className="text-center sm:text-left"
            style={{
              fontFamily: 'var(--ms-font-d)',
              fontSize: '10px',
              color: ready ? '#2e7a18' : '#c64b1b',
              letterSpacing: '1px',
              textShadow: ready ? '1px 1px 0 #b4f098' : '1px 1px 0 #ffd0d0',
              whiteSpace: 'nowrap',
            }}
          >
            {ready ? '✓ READY' : `NEXT IN ${fmt(remaining)}`}
          </span>

          {canVote ? (
            <a
              href={site.url}
              target="_blank"
              rel="noopener noreferrer"
              className="ms-btn ms-btn-sm ms-btn-green justify-center"
              onClick={() => onVote(site)}
            >
              ▶ VOTE NOW
            </a>
          ) : username ? (
            <button
              className="ms-btn ms-btn-sm justify-center"
              disabled
              style={{ opacity: 0.5, cursor: 'not-allowed' }}
            >
              ⏳ COOLDOWN
            </button>
          ) : (
            <Link
              href="/login?callbackUrl=%2Fvote"
              className="ms-btn ms-btn-sm justify-center"
              style={{ opacity: 0.6 }}
            >
              🔒 SIGN IN TO VOTE
            </Link>
          )}
        </div>
      </div>
    </div>
  )
}

/* ── Main Client Component ── */
export default function VoteClient({
  sites,
  topVoters,
  username,
}: {
  sites: Site[]
  topVoters: TopVoter[]
  username: string | null
}) {
  const [cooldowns, setCooldowns] = useState<Record<string, number>>({})
  const [toast, setToast] = useState<{ site: string; reward: string } | null>(null)

  useEffect(() => {
    try {
      const raw = localStorage.getItem('shinyms.vote-cd')
      if (raw) setCooldowns(JSON.parse(raw))
    } catch (e) {
      /* ignore */
    }
  }, [])

  const handleVote = useCallback(
    (site: Site) => {
      const until = Date.now() + site.cdHours * 3600 * 1000
      const next = { ...cooldowns, [site.id]: until }
      setCooldowns(next)
      setToast({ site: site.name, reward: site.reward })
      try {
        localStorage.setItem('shinyms.vote-cd', JSON.stringify(next))
      } catch (e) {
        /* ignore */
      }
      setTimeout(() => setToast(null), 4000)
    },
    [cooldowns]
  )

  return (
    <div className="relative overflow-hidden">
      {/* Floating sprite decorations */}
      <div className="pointer-events-none absolute inset-0 z-10" aria-hidden="true">
        <div className="absolute" style={{ top: 10, left: '6%' }}>
          <Sprite src="/maple/avatars/nightlord.png" alt="" height={72} anim="bob" grounded={false} />
        </div>
        <div className="absolute" style={{ top: 90, right: '8%' }}>
          <Sprite
            src="/maple/mobs/orange-mushroom.gif"
            alt=""
            height={52}
            anim="bob"
            delay={400}
            grounded={false}
          />
        </div>
        <div className="absolute" style={{ top: 160, left: '18%' }}>
          <Sprite src="/maple/mobs/snail.gif" alt="" height={40} anim="hop" delay={800} grounded={false} />
        </div>
        <div className="absolute hidden lg:block" style={{ top: 50, right: '22%' }}>
          <Sprite
            src="/maple/mobs/blue-snail.gif"
            alt=""
            height={36}
            anim="sway"
            delay={1200}
            grounded={false}
          />
        </div>
        <div className="absolute hidden md:block" style={{ bottom: 100, left: '4%' }}>
          <Sprite src="/maple/mobs/slime.gif" alt="" height={44} anim="bob" delay={600} grounded={false} />
        </div>
        <div className="absolute hidden md:block" style={{ bottom: 80, right: '6%' }}>
          <Sprite src="/maple/mobs/pig.gif" alt="" height={40} anim="hop" delay={1000} grounded={false} />
        </div>
        <span className="ms-sparkle" style={{ top: 60, left: '40%' }}>
          ✦
        </span>
        <span className="ms-sparkle" style={{ top: 220, right: '15%', animationDelay: '0.8s' }}>
          ✦
        </span>
        <span className="ms-sparkle" style={{ bottom: 160, left: '35%', animationDelay: '1.2s', fontSize: 12 }}>
          ✦
        </span>
      </div>

      {/* Hero */}
      <section className="relative z-20 px-4 pt-10 pb-6 sm:pt-14 sm:pb-8">
        <div className="max-w-6xl mx-auto text-center">
          <SectionBanner>SUPPORT · VOTE</SectionBanner>
          <h1 className="ms-hero-title">
            Vote for ShinyMS,
            <br className="hidden sm:block" /> earn free NX
          </h1>
          <div className="mt-4 mb-6 inline-block">
            <span className="ms-hero-sub">
              Click a site, pass the captcha — we credit Vote Points to your account automatically.
            </span>
          </div>
        </div>
      </section>

      {/* Content */}
      <section className="relative z-20 px-4 pb-10 sm:pb-14">
        <div className="max-w-6xl mx-auto">
          {/* Status banners */}
          {username ? (
            <div className="ms-npc-box mb-6">
              <div className="ms-npc-title">✅ SIGNED IN</div>
              <div className="ms-npc-body">
                <div className="ms-npc-text">
                  <span className="ms-npc-name">— {username} —</span>
                  Voting as <strong>{username}</strong> — Vote Points are credited to your account automatically.
                </div>
              </div>
            </div>
          ) : (
            <div className="ms-npc-box mb-6">
              <div className="ms-npc-title">⚠️ NOTICE</div>
              <div className="ms-npc-body">
                <div className="ms-npc-text">
                  <span className="ms-npc-name">— Voting Clerk —</span>
                  <strong>Sign in before voting</strong> so your Vote Points are credited to the right account.{' '}
                  <Link
                    href="/login?callbackUrl=%2Fvote"
                    className="underline"
                    style={{ color: '#c64b1b', fontWeight: 700 }}
                  >
                    Sign in →
                  </Link>
                </div>
              </div>
            </div>
          )}

          {toast && (
            <div className="ms-npc-box mb-6">
              <div className="ms-npc-title">🎁 REWARD</div>
              <div className="ms-npc-body">
                <div className="ms-npc-text">
                  <span className="ms-npc-name">— System —</span>
                  Thanks for voting on <strong>{toast.site}</strong>! {toast.reward} credited.
                </div>
              </div>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 lg:gap-8">
            {/* Vote sites grid */}
            <div className="lg:col-span-2 grid grid-cols-1 md:grid-cols-2 gap-5">
              {sites.map((site) => (
                <VoteCard
                  key={site.id}
                  site={site}
                  nextAvailable={cooldowns[site.id] || 0}
                  onVote={handleVote}
                  username={username}
                />
              ))}
            </div>

            {/* Sidebar */}
            <div className="flex flex-col gap-5">
              {/* Top voters */}
              {topVoters.length > 0 && (
                <div className="ms-pixel-panel">
                  <div
                    style={{
                      background: 'linear-gradient(to bottom, #6a4830 0%, #4a3220 50%, #3a2418 100%)',
                      borderBottom: '3px solid #2a1810',
                      padding: '10px 14px',
                      fontFamily: 'var(--ms-font-d)',
                      fontSize: '11px',
                      color: '#ffd96b',
                      letterSpacing: '1px',
                      textShadow: '2px 2px 0 #1a0a04',
                    }}
                  >
                    🏆 TOP VOTERS
                  </div>
                  <div className="p-3 flex flex-col gap-1" style={{ background: 'var(--ms-npc-bg)' }}>
                    {topVoters.map((voter, i) => (
                      <div
                        key={voter.name}
                        className="flex items-center justify-between px-2 py-2"
                        style={{
                          borderBottom:
                            i < topVoters.length - 1 ? '1px dashed var(--ms-slot-shadow)' : undefined,
                        }}
                      >
                        <div className="flex items-center gap-2">
                          <span
                            style={{
                              fontFamily: 'var(--ms-font-d)',
                              fontSize: '9px',
                              color: i < 3 ? '#c64b1b' : '#8a6f3c',
                              minWidth: 20,
                              textAlign: 'right',
                            }}
                          >
                            {i + 1}
                          </span>
                          <span
                            style={{
                              fontFamily: 'var(--ms-font-b)',
                              fontSize: '20px',
                              color: '#2a1810',
                            }}
                          >
                            {voter.name}
                          </span>
                        </div>
                        <span
                          style={{
                            fontFamily: 'var(--ms-font-d)',
                            fontSize: '9px',
                            color: '#2e7a18',
                          }}
                        >
                          {voter.votepoints} pts
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Daily limits */}
              <div className="ms-step-card">
                <div className="ms-step-num">!</div>
                <div className="ms-step-title">DAILY LIMITS</div>
                <div className="ms-step-body">
                  <p className="mb-2">Earn up to 2 Vote Points per day.</p>
                  <p>Vote on both sites daily for maximum rewards. Points never expire.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* How voting works */}
      <section
        className="relative z-20 px-4 py-10 sm:py-14"
        style={{
          background: 'rgba(255, 248, 216, 0.4)',
          borderTop: '2px solid #c8a868',
          borderBottom: '2px solid #c8a868',
        }}
      >
        <div className="max-w-6xl mx-auto">
          <SectionBanner>HOW VOTING WORKS</SectionBanner>
          <h2 className="ms-section-title">Three quick steps</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
            {[
              {
                t: 'Click a vote site',
                b: 'Opens in a new tab. Pick any of the available sites.',
              },
              {
                t: 'Pass the captcha',
                b: 'Takes about five seconds on the voting site.',
              },
              {
                t: 'Get credited',
                b: 'Vote Points land in your account automatically.',
              },
            ].map((s, i) => (
              <div key={i} className="ms-step-card">
                <div className="ms-step-num">{i + 1}</div>
                <div className="ms-step-title">{s.t}</div>
                <div className="ms-step-body">{s.b}</div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
