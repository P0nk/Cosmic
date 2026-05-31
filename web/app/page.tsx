import Link from 'next/link'
import Image from 'next/image'
import { prisma } from '@/lib/prisma'
import ServerStatusWidget from '@/components/ui/ServerStatusWidget'
import ServerClock from '@/components/ui/ServerClock'
import HomeAnnouncements from '@/components/posts/HomeAnnouncements'
import HeroScene from '@/components/maple/HeroScene'
import Sprite from '@/components/maple/Sprite'
import SmegaTicker from '@/components/maple/SmegaTicker'
import NewsletterSignup from '@/components/NewsletterSignup'
import NpcBox from '@/components/maple/NpcBox'
import StatBar from '@/components/maple/StatBar'
import SectionBanner from '@/components/maple/SectionBanner'
import CharAvatar from '@/components/maple/CharAvatar'
import InvPanel from '@/components/maple/InvPanel'
import JobPicker from '@/components/maple/JobPicker'
import { recentSmegas } from '@/lib/community'
import { getSocialLinks, getServerRates } from '@/lib/settings'
import type { Metadata } from 'next'

// Render fresh on every request. The homepage shows live announcements +
// server status; as a static/ISR page its prefetched snapshot lingered in the
// client Router Cache (static staleTime = 5 min), so logged-in users soft-
// navigating to "/" saw a stale (sometimes empty) announcements list until a
// hard refresh. Dynamic rendering gives the client cache a 0s staleTime, so
// navigations and refreshes always reflect current posts.
export const dynamic = 'force-dynamic'

export const metadata: Metadata = {
  alternates: { canonical: 'https://shinyms.com' },
}

async function buildJsonLd(exp: number, meso: number, drop: number) {
  const social = await getSocialLinks()
  return {
    '@context': 'https://schema.org',
    '@type': 'VideoGame',
    name: 'ShinyMS',
    url: 'https://shinyms.com',
    description: `ShinyMS is a free MapleStory v83 private server with ${exp}× EXP, ${meso}× Meso & ${drop}× Drop rates, playable instantly in your browser with no download.`,
    genre: ['MMORPG', 'Role-playing game'],
    gamePlatform: ['PC', 'Browser'],
    applicationCategory: 'Game',
    operatingSystem: ['Windows', 'macOS', 'Linux'],
    inLanguage: 'en',
    offers: { '@type': 'Offer', price: '0', priceCurrency: 'USD', availability: 'https://schema.org/InStock' },
    publisher: {
      '@type': 'Organization',
      name: 'ShinyMS',
      url: 'https://shinyms.com',
      sameAs: social.discord ? [social.discord] : [],
    },
  }
}

function buildFaqLd(exp: number, meso: number, drop: number) {
  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: [
      {
        '@type': 'Question',
        name: 'Is ShinyMS free to play?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'Yes. ShinyMS is completely free to play with no pay-to-win mechanics. Everything is earned in-game — there is no cash shop power.',
        },
      },
      {
        '@type': 'Question',
        name: 'Do I need to download anything to play ShinyMS?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'No download or installer is required. You can play MapleStory v83 instantly in your browser on Windows, macOS, or Linux — just create a free account and click Play.',
        },
      },
      {
        '@type': 'Question',
        name: 'What are the rates on ShinyMS?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: `ShinyMS runs balanced rates of ${exp}× EXP, ${meso}× Meso, and ${drop}× Drop on the classic GMS v83 version of MapleStory.`,
        },
      },
      {
        '@type': 'Question',
        name: 'What version of MapleStory does ShinyMS run?',
        acceptedAnswer: {
          '@type': 'Answer',
          text: 'ShinyMS runs the nostalgic GMS v83 version, preserving the classic towns, jobs, party quests, and bosses from the golden era of MapleStory.',
        },
      },
    ],
  }
}

async function getPosts() {
  return prisma.cmsPost.findMany({
    orderBy: [{ pinned: 'desc' }, { createdAt: 'desc' }],
    include: { author: { select: { name: true } } },
    take: 10,
  }).catch(() => [])
}

const MONSTERS = [
  { src: '/maple/mobs/snail.gif', name: 'Snail', h: 44, anim: 'bob' as const },
  { src: '/maple/mobs/orange-mushroom.gif', name: 'Orange Mushroom', h: 64, anim: 'hop' as const },
  { src: '/maple/mobs/slime.gif', name: 'Slime', h: 56, anim: 'hop' as const },
  { src: '/maple/mobs/pig.gif', name: 'Pig', h: 60, anim: 'bob' as const },
  { src: '/maple/mobs/ribbon-pig.gif', name: 'Ribbon Pig', h: 60, anim: 'bob' as const },
  { src: '/maple/mobs/stump.gif', name: 'Stump', h: 58, anim: 'sway' as const },
  { src: '/maple/mobs/mano.gif', name: 'Mano', h: 76, anim: 'hop' as const },
  { src: '/maple/mobs/mushmom.gif', name: 'Mushmom', h: 80, anim: 'bob' as const },
]

const BOSSES = [
  { src: '/maple/mobs/king-slime.gif', name: 'King Slime', h: 84, hp: 68, label: 'Party Boss' },
  { src: '/maple/mobs/balrog.gif', name: 'Crimson Balrog', h: 96, hp: 82, label: 'Lvl 105' },
  { src: '/maple/mobs/zakum.gif', name: 'Zakum', h: 120, hp: 95, label: 'Lvl 140' },
]

const STEPS = [
  { n: '1', title: 'Create a free account', desc: 'Pick a username and password. No email hoops — about thirty seconds.' },
  { n: '2', title: 'Hit Play Now', desc: 'Launch straight in your browser. Nothing to install, nothing to configure.' },
  { n: '3', title: 'Start your adventure', desc: 'Choose your class, leave Maple Island, and dive back into the world you remember.' },
]

const TESTIMONIALS = [
  {
    quote: 'I have not played MapleStory since high school. Being able to just click a link and be back in Henesys — no launcher, no patching — genuinely got me emotional.',
    name: 'Reqei',
    detail: 'Night Lord · Lv. 142',
    cls: 'nightlord',
  },
  {
    quote: 'The rates feel exactly right. Fast enough that I can actually progress after work, slow enough that hitting a new level still means something.',
    name: 'Aelith',
    detail: 'Bishop · Lv. 120',
    cls: 'bishop',
  },
  {
    quote: 'Playing on my Mac with zero setup was the selling point. The community is small but kind, and the events keep me logging in every week.',
    name: 'Borin',
    detail: 'Hero · Lv. 155',
    cls: 'hero',
  },
]

const OFFERS = [
  { title: 'Jobs', desc: 'Every classic v83 class' },
  { title: 'Party Quests', desc: 'Run the classic PQs' },
  { title: 'Commands', desc: 'Handy @ shortcuts' },
  { title: 'Rebirths', desc: 'Reset and grow stronger' },
  { title: 'Free Market', desc: 'Trade with other players' },
  { title: 'Bossing', desc: 'Zakum, Horntail & more' },
  { title: 'Henesys Hub', desc: 'Hang out with the community' },
  { title: 'NX Items', desc: 'Cosmetics & gear' },
  { title: 'Teleporter', desc: 'Fast travel anywhere' },
  { title: 'Voting Rewards', desc: 'Earn NX by voting' },
  { title: 'Pets', desc: 'Hatch, raise & equip pets' },
  { title: 'Scrolling', desc: 'Enhance your gear' },
  { title: 'Gachapon', desc: 'Spin tickets for rare prizes' },
  { title: 'Alliance', desc: 'Unite guilds under one banner' },
  { title: 'Monster Book', desc: 'Collect cards from every mob' },
  { title: 'Marriage', desc: 'Wed and quest together' },
]

export default async function HomePage() {
  const [posts, smegas, rates] = await Promise.all([getPosts(), recentSmegas(15), getServerRates()])
  const { expRate: exp, mesoRate: meso, dropRate: drop } = rates
  const rateMax = Math.max(exp, meso, drop, 10)
  const [jsonLd, faqLd] = await Promise.all([buildJsonLd(exp, meso, drop), Promise.resolve(buildFaqLd(exp, meso, drop))])

  return (
    <div>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(faqLd) }} />

      {/* ── Hero ── */}
      <section
        className="relative overflow-hidden"
        style={{ background: 'linear-gradient(180deg, #2f74bd 0%, #4a8fce 45%, #79b4e2 78%, #a7d2ef 100%)' }}
      >
        <HeroScene />
        <div className="relative max-w-6xl mx-auto px-4 sm:px-6 pt-14 sm:pt-20 pb-28 sm:pb-36">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-14 items-center">
            {/* Left: title + stat bars + CTAs */}
            <div>
              <div className="flex items-center gap-3 mb-6 flex-wrap">
                <ServerStatusWidget />
                <ServerClock />
              </div>

              <h1 className="ms-hero-title mb-4">
                Welcome to<br />ShinyMS
              </h1>
              <p className="ms-hero-sub mb-6">
                Free MapleStory v83 private server. No download. No pay-to-win.
              </p>

              <div className="flex flex-col gap-2 mb-6 max-w-xs">
                <StatBar kind="hp" label="HP" value={exp} max={rateMax} displayValue={`${exp}× EXP`} />
                <StatBar kind="mp" label="MP" value={meso} max={rateMax} displayValue={`${meso}× Meso`} />
                <StatBar kind="exp" label="EXP" value={drop} max={rateMax} displayValue={`${drop}× Drop`} />
              </div>

              <div className="flex flex-wrap gap-3">
                <a href="https://play.shinyms.com" className="ms-btn ms-btn-green">
                  ▶ PLAY NOW
                </a>
                <Link href="/register" className="ms-btn">
                  CREATE ACCOUNT
                </Link>
              </div>
            </div>

            {/* Right: NPC dialogue box */}
            <div>
              <NpcBox
                title="Maple Admin"
                npcName="Maple Admin"
                npcCls="gm"
                actions={
                  <a href="https://play.shinyms.com" className="ms-btn ms-btn-sm ms-btn-green">
                    ▶ Start Journey
                  </a>
                }
              >
                <p>
                  Ah, a new adventurer! Welcome to ShinyMS — the classic GMS v83 experience,
                  preserved with care and running live in your browser.
                </p>
                <p style={{ marginTop: 8 }}>
                  {exp}× EXP · {meso}× Meso · {drop}× Drop. No pay-to-win. Just pure nostalgia.
                </p>
              </NpcBox>
            </div>
          </div>
        </div>
      </section>

      {/* ── Live smega ticker ── */}
      <SmegaTicker smegas={smegas} />

      {/* ── Why ShinyMS — four NPC quest cards ── */}
      <section style={{ backgroundColor: 'var(--ms-npc-bg)', borderTop: '4px solid #1a0a04', borderBottom: '4px solid #1a0a04' }}>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
          <div className="text-center mb-12">
            <SectionBanner>WHY SHINYMS</SectionBanner>
            <h2 className="ms-section-title">The MapleStory you remember, made effortless</h2>
            <p className="ms-muted">Everything from the golden era — preserved, balanced, one click away.</p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              {
                title: 'Play Instantly',
                body: 'No download, no installer, no patch. Click Play and you are in Maple World within seconds — on any machine.',
                cls: 'gm',
              },
              {
                title: 'Fair Rates',
                body: `Balanced ${exp}× EXP, ${meso}× Meso & ${drop}× Drop. Everything is earned in-game — no cash shop power, ever.`,
                cls: 'warrior',
              },
              {
                title: 'All Platforms',
                body: 'The same nostalgic v83 experience whether you are on Windows, Mac, or Linux. One server, every device.',
                cls: 'magician',
              },
              {
                title: 'Living Community',
                body: 'Active players, regular events, guild rankings and a friendly Discord. Adventure with old friends and meet new ones.',
                cls: 'bowman',
              },
            ].map(({ title, body, cls }) => (
              <NpcBox key={title} title={title} npcCls={cls}>
                <p>{body}</p>
              </NpcBox>
            ))}
          </div>
        </div>
      </section>

      {/* ── Maple World screenshot ── */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 lg:gap-14 items-center">
          <div>
            <SectionBanner>A FAMILIAR WORLD</SectionBanner>
            <h2 className="ms-section-title">Step right back into Maple World</h2>
            <p className="ms-muted" style={{ marginBottom: 16 }}>
              The same hand-drawn towns, cozy mushroom houses, original soundtrack — running live in your browser, exactly as you left it.
            </p>
            <ul style={{ display: 'flex', flexDirection: 'column', gap: 8, marginBottom: 20 }}>
              {['Classic v83 maps, jobs and monsters', 'Pixel-perfect sprites and original UI', 'Smooth, lag-free play right in the tab'].map((line) => (
                <li key={line} style={{ fontFamily: 'var(--font-fredoka), system-ui, sans-serif', fontSize: 16, fontWeight: 400, color: 'var(--ms-text)', display: 'flex', gap: 10, alignItems: 'center' }}>
                  <span style={{ color: '#4caf30', fontFamily: 'var(--ms-font-d)', fontSize: 10 }}>✓</span>
                  {line}
                </li>
              ))}
            </ul>
            <a href="https://play.shinyms.com" className="ms-btn ms-btn-green">▶ Play Now</a>
          </div>
          <div>
            <div className="ms-screenshot-frame">
              <div className="ms-screenshot-badge">🍁 Maple World — Live</div>
              <Image
                src="/gameplay.jpeg"
                alt="ShinyMS gameplay — a character exploring Maple Road"
                width={960}
                height={540}
                sizes="(max-width: 768px) 100vw, 960px"
                className="w-full h-auto block"
                loading="lazy"
              />
            </div>
          </div>
        </div>
      </section>

      {/* ── Field Guide — mob parade + inventory grid ── */}
      <section style={{ backgroundColor: 'var(--ms-npc-bg)', borderTop: '4px solid #1a0a04', borderBottom: '4px solid #1a0a04' }}>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
          <div className="text-center mb-12">
            <SectionBanner>THE CREATURES OF MAPLE WORLD</SectionBanner>
            <h2 className="ms-section-title">The faces you grew up with</h2>
            <p className="ms-muted">From your very first snail to Henesys&rsquo; Mushmom — every classic v83 monster.</p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-10 items-start">
            <NpcBox title="Field Guide" npcName="Scholar Rondo" npcCls="magician">
              <p>
                The forests of Maple World are teeming with life — curious, colourful, and sometimes
                very eager to fight you. Every classic v83 mob is here, exactly as you remember.
              </p>
              <div
                style={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  alignItems: 'flex-end',
                  gap: 16,
                  marginTop: 16,
                  padding: '12px 0',
                  borderTop: '2px dashed var(--ms-slot-shadow)',
                }}
              >
                {MONSTERS.slice(0, 6).map((m, i) => (
                  <div key={m.name} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                    <div style={{ height: 56, display: 'flex', alignItems: 'flex-end' }}>
                      <Sprite src={m.src} alt={m.name} height={Math.round(m.h * 0.7)} anim={m.anim} delay={i * 200} />
                    </div>
                    <span style={{ fontFamily: 'var(--ms-font-d)', fontSize: 7, color: '#4a3220' }}>{m.name}</span>
                  </div>
                ))}
              </div>
            </NpcBox>

            <div>
              <div style={{ fontFamily: 'var(--ms-font-d)', fontSize: 10, color: 'var(--ms-text)', marginBottom: 8, letterSpacing: 1 }}>
                EXPLORER PACK — INVENTORY
              </div>
              <InvPanel />
            </div>
          </div>
        </div>
      </section>

      {/* ── Bosses ── */}
      <section
        className="relative overflow-hidden"
        style={{ background: 'linear-gradient(160deg, #1a2438 0%, #2a1f3a 60%, #3a1f2e 100%)', borderTop: '4px solid #f8c34a' }}
      >
        <div className="absolute inset-0 pointer-events-none" style={{ backgroundImage: 'radial-gradient(rgba(255,90,90,0.08) 1px, transparent 1px)', backgroundSize: '26px 26px' }} />
        <div className="relative max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20 text-center">
          <SectionBanner>END GAME</SectionBanner>
          <h2 className="ms-section-title" style={{ color: '#f8c34a', textShadow: '2px 2px 0 #2a1810' }}>
            Then test your might
          </h2>
          <p className="ms-muted" style={{ color: 'rgba(248,227,176,0.8)', marginBottom: 48 }}>
            Gear up, rally a party, and take on the bosses that defined the era.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 max-w-3xl mx-auto">
            {BOSSES.map((b, i) => (
              <div key={b.name} className="ms-boss-card">
                <div className="ms-boss-label">{b.label}</div>
                <div style={{ height: 120, display: 'flex', alignItems: 'flex-end', justifyContent: 'center', marginBottom: 12 }}>
                  <Sprite src={b.src} alt={b.name} height={b.h} anim="bob" delay={i * 350} />
                </div>
                <div style={{ fontFamily: 'var(--ms-font-d)', fontSize: 10, color: '#f8c34a', marginBottom: 10 }}>{b.name}</div>
                <StatBar kind="hp" label="HP" value={b.hp} max={100} displayValue={`${b.hp}%`} />
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── What's inside ── */}
      <section style={{ backgroundColor: 'var(--ms-npc-bg)', borderTop: '4px solid #1a0a04', borderBottom: '4px solid #1a0a04' }}>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
          <div className="text-center mb-12">
            <SectionBanner>IN THE GAME</SectionBanner>
            <h2 className="ms-section-title">Everything packed in</h2>
            <p className="ms-muted">From your first party quest to endgame bossing — all the systems you loved.</p>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
              gap: 10,
            }}
          >
            {OFFERS.map(({ title, desc }) => (
              <div
                key={title}
                className="ms-quest-card"
                style={{ padding: '10px 12px' }}
              >
                <div style={{ fontFamily: 'var(--ms-font-d)', fontSize: 9, color: '#c64b1b', marginBottom: 4, letterSpacing: 1 }}>{title}</div>
                <div style={{ fontFamily: 'var(--font-fredoka), system-ui, sans-serif', fontSize: 14, fontWeight: 400, color: '#4a3220', lineHeight: 1.4 }}>{desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Choose your class ── */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
        <div className="text-center mb-12">
          <SectionBanner>CHOOSE YOUR CLASS</SectionBanner>
          <h2 className="ms-section-title">Who will you be?</h2>
          <p className="ms-muted">Five iconic paths through Maple World — each with its own playstyle, skills, and story.</p>
        </div>
        <JobPicker />
      </section>

      {/* ── Getting started ── */}
      <section style={{ backgroundColor: 'var(--ms-npc-bg)', borderTop: '4px solid #1a0a04', borderBottom: '4px solid #1a0a04' }}>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
          <div className="text-center mb-14">
            <SectionBanner>GET STARTED</SectionBanner>
            <h2 className="ms-section-title">Playing takes about a minute</h2>
            <p className="ms-muted">No downloads, no waiting room. Three quick steps and you are back in the game.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-10 mb-12">
            {STEPS.map(({ n, title, desc }) => (
              <div key={n} className="ms-step-card">
                <div className="ms-step-num">{n}</div>
                <div className="ms-step-title">{title}</div>
                <p className="ms-step-body">{desc}</p>
              </div>
            ))}
          </div>

          <div className="flex flex-wrap items-center justify-center gap-4">
            <Link href="/register" className="ms-btn ms-btn-green">Create Free Account</Link>
            <Link href="/guide" className="ms-btn">Read the Guide</Link>
          </div>
        </div>
      </section>

      {/* ── Testimonials ── */}
      <section className="max-w-6xl mx-auto px-4 sm:px-6 py-14 sm:py-20">
        <div className="text-center mb-14">
          <SectionBanner>FROM THE COMMUNITY</SectionBanner>
          <h2 className="ms-section-title">Loved by returning Maplers</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-10">
          {TESTIMONIALS.map(({ quote, name, detail, cls }) => (
            <figure key={name} className="ms-testimony">
              <div className="ms-testimony-tag">REVIEW</div>
              <blockquote className="ms-testimony-body">&ldquo;{quote}&rdquo;</blockquote>
              <figcaption className="ms-testimony-who">
                <CharAvatar cls={cls} size={52} showLabel={false} />
                <div>
                  <div className="ms-testimony-name">{name}</div>
                  <div className="ms-testimony-meta">{detail}</div>
                </div>
              </figcaption>
            </figure>
          ))}
        </div>
      </section>

      {/* ── Announcements + sidebar ── */}
      <section style={{ backgroundColor: 'var(--ms-npc-bg)', borderTop: '4px solid #1a0a04', borderBottom: '4px solid #1a0a04' }}>
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10 sm:py-14">
          <HomeAnnouncements posts={posts} />
        </div>
      </section>

      {/* ── Newsletter ── */}
      <NewsletterSignup />

      {/* ── Walking mob parade ── */}
      <div className="ms-mob-parade" aria-hidden="true">
        {[
          { src: '/maple/mobs/orange-mushroom.gif', delay: 0, duration: 28, h: 48 },
          { src: '/maple/mobs/snail.gif', delay: 7, duration: 38, h: 32 },
          { src: '/maple/mobs/slime.gif', delay: 14, duration: 24, h: 40 },
          { src: '/maple/mobs/pig.gif', delay: 20, duration: 30, h: 44 },
          { src: '/maple/mobs/blue-snail.gif', delay: 27, duration: 40, h: 32 },
          { src: '/maple/mobs/green-mushroom.gif', delay: 33, duration: 26, h: 48 },
        ].map((m, i) => (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            key={i}
            src={m.src}
            alt=""
            className="ms-mob-sprite"
            style={{
              height: m.h,
              animation: `ms-walk ${m.duration}s linear ${m.delay}s infinite`,
            }}
          />
        ))}
      </div>
    </div>
  )
}
