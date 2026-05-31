const THEMES: Record<string, { color: string; shade: string; glyph: string; name: string }> = {
  warrior:   { color: '#e23b3b', shade: '#8a1818', glyph: '⚔',  name: 'Warrior'     },
  hero:      { color: '#e23b3b', shade: '#8a1818', glyph: '⚔',  name: 'Hero'        },
  paladin:   { color: '#dca830', shade: '#6a4f1a', glyph: '🛡', name: 'Paladin'     },
  dk:        { color: '#8a3030', shade: '#4a1010', glyph: '🔱', name: 'Dark Knight' },
  magician:  { color: '#2e80e2', shade: '#143d80', glyph: '✨', name: 'Magician'    },
  bishop:    { color: '#f0d878', shade: '#a08018', glyph: '✝',  name: 'Bishop'      },
  fp:        { color: '#c64b1b', shade: '#6a1010', glyph: '🔥', name: 'F/P Mage'   },
  il:        { color: '#5cb0e2', shade: '#143d80', glyph: '❄',  name: 'I/L Mage'   },
  bowman:    { color: '#4caf30', shade: '#1e5208', glyph: '🏹', name: 'Bowman'      },
  bm:        { color: '#4caf30', shade: '#1e5208', glyph: '🏹', name: 'Bowmaster'   },
  mm:        { color: '#3aa078', shade: '#0a4830', glyph: '🎯', name: 'Marksman'    },
  thief:     { color: '#8030a0', shade: '#3a0a48', glyph: '🗡', name: 'Thief'       },
  nightlord: { color: '#5018a8', shade: '#1c0640', glyph: '☄',  name: 'Night Lord'  },
  shadower:  { color: '#6c3098', shade: '#2a0a3a', glyph: '🌙', name: 'Shadower'    },
  pirate:    { color: '#f8a020', shade: '#a05808', glyph: '⚓', name: 'Pirate'      },
  buccaneer: { color: '#c87018', shade: '#683008', glyph: '👊', name: 'Buccaneer'   },
  corsair:   { color: '#f8c34a', shade: '#a06808', glyph: '🔫', name: 'Corsair'     },
  gm:        { color: '#c64b1b', shade: '#6a1010', glyph: '🍁', name: 'GM'          },
  npc:       { color: '#4a3220', shade: '#1a0a04', glyph: '💬', name: 'NPC'         },
  cygnus:    { color: '#88a0e2', shade: '#3a4a8a', glyph: '👸', name: 'Cygnus'      },
}

const AVATAR_IMAGES: Record<string, string> = {
  gm: '/maple/npcs/maple-admin.gif',
}

export function charForJob(job: string): string {
  const j = job.toLowerCase()
  if (j.includes('hero') || j.includes('warrior')) return 'hero'
  if (j.includes('paladin')) return 'paladin'
  if (j.includes('dark knight') || j.includes(' dk')) return 'dk'
  if (j.includes('night lord')) return 'nightlord'
  if (j.includes('shadower')) return 'shadower'
  if (j.includes('thief')) return 'thief'
  if (j.includes('bishop')) return 'bishop'
  if (j.includes('f/p')) return 'fp'
  if (j.includes('i/l')) return 'il'
  if (j.includes('magician')) return 'magician'
  if (j.includes('bowmaster') || j.includes('bow master')) return 'bm'
  if (j.includes('marksman')) return 'mm'
  if (j.includes('bowman')) return 'bowman'
  if (j.includes('buccaneer')) return 'buccaneer'
  if (j.includes('corsair')) return 'corsair'
  if (j.includes('pirate')) return 'pirate'
  if (j.includes('gm') || j.includes('admin')) return 'gm'
  if (j.includes('cygnus')) return 'cygnus'
  return 'npc'
}

export default function CharAvatar({
  cls = 'hero',
  size = 72,
  showLabel = true,
  label,
}: {
  cls?: string
  size?: number
  showLabel?: boolean
  label?: string | false
}) {
  const t = THEMES[cls] ?? THEMES.hero
  const imgSrc = AVATAR_IMAGES[cls]
  const glyphSize = Math.round(size * 0.45)
  const labelSize = Math.max(7, Math.round(size * 0.11))
  const labelTop = Math.round(size * 0.06)

  return (
    <div
      style={{
        width: size,
        height: size,
        flex: `0 0 ${size}px`,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: imgSrc ? 'flex-end' : 'center',
        background: `linear-gradient(180deg, ${t.color} 0%, ${t.shade} 100%)`,
        border: '3px solid #1a0a04',
        boxShadow:
          'inset 2px 2px 0 rgba(255,255,255,0.35), inset -2px -2px 0 rgba(0,0,0,0.35), 3px 3px 0 rgba(0,0,0,0.3)',
        color: '#fff8d8',
        fontFamily: 'var(--ms-font-d)',
        position: 'relative',
        overflow: 'hidden',
      }}
      title={typeof label === 'string' ? label : t.name}
    >
      {imgSrc ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={imgSrc}
          alt=""
          style={{
            height: showLabel ? Math.round(size * 0.80) : Math.round(size * 0.92),
            width: 'auto',
            imageRendering: 'pixelated',
            display: 'block',
            flexShrink: 0,
          }}
        />
      ) : (
        <div style={{ fontSize: glyphSize, lineHeight: 1, textShadow: '0 2px 0 rgba(0,0,0,0.35)' }}>
          {t.glyph}
        </div>
      )}

      {label !== false && showLabel && (
        <div
          style={{
            fontSize: labelSize,
            letterSpacing: 1,
            marginTop: imgSrc ? 0 : labelTop,
            textShadow: '1px 1px 0 rgba(0,0,0,0.5)',
            textTransform: 'uppercase',
            ...(imgSrc
              ? {
                  position: 'absolute',
                  bottom: 0,
                  left: 0,
                  right: 0,
                  textAlign: 'center',
                  background: 'rgba(0,0,0,0.55)',
                  padding: '2px 0',
                }
              : {}),
          }}
        >
          {typeof label === 'string' ? label : t.name}
        </div>
      )}

      <span
        style={{
          position: 'absolute',
          inset: 0,
          background:
            'repeating-linear-gradient(-45deg, transparent 0 6px, rgba(255,255,255,0.06) 6px 8px)',
          pointerEvents: 'none',
        }}
      />
    </div>
  )
}
