# ADR 0001 — How simulated players work

Status: **Proposed** (human to accept before E3 cards move to `ready`)

## Context
The live-server feel needs other "players" in towns, training maps and the Free Market. Options:

| Option | How | Pros | Cons |
|---|---|---|---|
| A. Server-side residents | Real `Character` objects with a socketless `Client`; server broadcasts their movement/attacks/chat to real players | Cheap (hundreds per JVM), no networking, full access to server state | Needs care so code paths that assume a socket don't NPE; not a load test |
| B. Headless protocol bots | Separate process speaks v83 protocol like a real client | Exercises real code paths; doubles as load test | Must implement login, crypto, movement packets; heavier per bot |
| C. Scripted NPC look-alikes | NPCs styled as players | Trivial | Not interactive; doesn't feel live |

## Decision (proposed)
Start with **A** for population/feel, gated by `SIM_PLAYERS_ENABLED`. Build **B** in E0 as the test client anyway,
and later let it drive a small number of sims for load testing.

## Consequences
- New package `src/main/java/sim/` owned by `cosmic-sim`; touches to `Client`/`Character` go via small, reviewed
  `cosmic-core` cards (e.g. "make Client tolerate null channel").
- Sim characters are stored in DB with a flag so they can be wiped in one query.
- Economy guardrails tracked in E2 telemetry.
