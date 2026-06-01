# syntax=docker/dockerfile:1
FROM node:20-alpine AS base

FROM base AS deps
WORKDIR /app
COPY web/package*.json ./
RUN --mount=type=cache,id=npm,target=/root/.npm npm ci

FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY web/ .
# NEXT_PUBLIC_* values are inlined into the client bundle by `next build`, so
# they must exist as env vars at build time — not just at runtime. Railway
# supplies matching service variables as Docker build args, but only for ARGs
# the Dockerfile declares. These are public values (they ship in client JS), so
# baking them into the image is expected; the Turnstile *secret* stays runtime.
ARG NEXT_PUBLIC_TURNSTILE_SITE_KEY
ARG NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_TURNSTILE_SITE_KEY=$NEXT_PUBLIC_TURNSTILE_SITE_KEY
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
RUN npx prisma generate
RUN npm run build

FROM base AS runner
WORKDIR /app
ENV NODE_ENV=production
# Prisma needs openssl present to detect OpenSSL 3 on Alpine; without it the
# client defaults to the openssl-1.1.x engine and fails (libssl.so.1.1 missing).
RUN apk add --no-cache openssl
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static
# Idempotent CMS table provisioning, run before the server boots (see
# scripts/ensure-cms-tables.mjs). The standalone bundle already includes
# @prisma/client, so the script reuses it; it only needs the SQL + itself.
COPY --from=builder --chown=nextjs:nodejs /app/prisma/sql ./prisma/sql
COPY --from=builder --chown=nextjs:nodejs /app/scripts ./scripts
USER nextjs
EXPOSE 3000
ENV PORT=3000
# Liveness probe using Node's built-in fetch (no extra packages needed).
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD node -e "fetch('http://127.0.0.1:'+(process.env.PORT||3000)+'/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
# Ensure CMS tables exist, then start the server. `;` (not `&&`) so a
# provisioning hiccup never prevents the server from coming up.
CMD ["sh", "-c", "node scripts/ensure-cms-tables.mjs; node server.js"]
