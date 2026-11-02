# Bug triage (first pass, UNCONFIRMED)

Owner: cosmic-lead. Status: nothing here has been reproduced. No server could be run when this was written
(no JDK/MySQL). "Code present" = a grep found related code, NOT that it works. Every item needs a cosmic-qa
repro with the E0 test client (or manual play) before a fix card is `ready`.

Sources: `docs/issues.txt` (old HeavenMS known issues), `docs/leftover.txt` (HeavenMS wishlist, not bugs),
upstream tracker https://github.com/P0nk/Cosmic/issues (54 open when checked; only the titles seen in a search
snippet are listed - the full list still needs a read).

Priority = effect on "feels like a live 2009 GMS server".  P1 = hurts normal play/economy, P2 = noticeable, P3 = niche/defer.

## A. From docs/issues.txt (HeavenMS)

| # | Item | Lane | Pri | Note |
|---|------|------|-----|------|
| A1 | Deadlocks after long uptime with many players | core | P1 | Needs soak test; depends on E0 harness |
| A2 | Monster Magnet crashes player on fixed mobs | core | P2 | Skill code present (grep) |
| A3 | Mystic Door deploys players wrongly | core | P2 | DoorHandler exists |
| A4 | Mob positions desync between clients | core | P2 | Hard to repro; needs 2 clients |
| A5 | Boss HP bar flickers with several bosses | core | P3 | Cosmetic |
| A6 | Magic damage not shown to others at close range | core | P2 | Packet issue -> cosmic-packet-work skill |
| A7 | Crits (Aran) not shown to others | core | P3 | Aran is post-v83 era |
| A8 | Dragon Roar stun effect not shown | core | P3 | Cosmetic |
| A9 | Snipe damage shown too high | core | P3 | Cosmetic |
| A10 | Steal does not remove loot from drop pool | core | P3 | Thief 4th job, minor |
| A11 | Weapon/magic reflect misbehaves | core | P2 | Mob mechanics |
| A12 | Pet commands not sent to other players | core | P2 | Pets are common in 2009 |
| A13 | Wind Archer Final Attack broken | core | P3 | Cygnus, defer |
| A14 | Battleship d/c with buff system | core | P3 | Pirate 4th-job-era, defer |
| A15 | Pirate skills not working for 3rd parties | core | P2 | Same class of bug as A6/A12 |
| A16 | Login freeze when summoning a logging-in player | core | P3 | Low-end connections only |
| A17 | Equip EXP watch value glitches | - | P3 | Client limit, wontfix |
| A18 | "Update Java version" | - | closed | Already Java 21 |
| A19 | "Check autoban system" | core | P2 | Anti-cheat review, pairs with E2 |

## B. Features listed as "missing" - code exists, verify

| # | Item | Evidence | Next step |
|---|------|----------|-----------|
| B1 | MTS | `MTSHandler`, `EnterMTSHandler` present | qa: does it open and work? |
| B2 | Name change / world transfer | `TransferName*`, `TransferWorldHandler` present | qa: verify; may be unneeded for 2009 |
| B3 | Nett's Pyramid PQ | 8 Java / 5 script files mention Pyramid | qa: end-to-end run |
| B4 | Medal quests, Family | not checked | content/qa audit under E1 |

## C. From upstream GitHub (titles only, unverified)

| Issue | Title | Lane | Pri |
|-------|-------|------|-----|
| #223 | Merchant item duping | core | P1 (E2 dupe safety) |
| #273 | Dupe: one-of-a-kind item + Fredrick | core | P1 |
| #277 (PR) | Fix Hired Merchant duplication | core | P1 - review, do not blindly merge |
| #289 | Maker cost calc wrong (formula given in issue) | core | P2 |
| #338 / #279 (PR) | Mob skills not working / not applying to players | core | P1 |
| #290 | Wind Walk not cancelled by attacking | core | P2 |
| #312 | Wrong value in player shot packet | core | P2 (packet skill) |
| #292 | ZOMBIFY stat calc | core | P3 |
| #252 | Cash-shop EXP coupons stack | core | P2 |
| #256 | Kerning PQ stage 1 ligator coupon drop rate | content | P2 |
| #324 | Missing quest script 1028 (To Lith Harbor) | content | P3 |
| #335 | Items referenced in handbook are missing | content | P3 |
| #267 | NPC sendSimple 'next' disconnects | core/content | P2 |
| #266 | `@stat` with no parameter errors | core | P3 |
| #254 | No level 30 for mounts | core | P3 |
| #328 | 1 channel configured, 3 started | core | P3 |
| #313 | Safer map connectivity checks | core | P3 |
| #325 | Toggle for Showa Town censorship | content | P3 |
| #326 | Family Guide | content | P3 |
| #345, #317, #319, #291 | Client/startup errors, Mac, DB pool hangs | qa | support - check against BASELINE |

Open upstream PRs worth a look later (reviewer lane): #353 concurrent map entry, #352 ConcurrentModificationException,
#323 Orbis PQ jump puzzle, #295 quest 6225, #287 drop rates, #348 queststatus perf.

## D. Not bugs - route elsewhere
- `docs/leftover.txt`: wishlist of events/PQs/later-version content. Use as reference for E4 (events) and E1 (PQs).
  Most "post-v83" entries are out of scope for a v83 server.
- `docs/mychanges_ptbr.txt`, `docs/feature_list.md`, `docs/wzchanges_gist.txt`: HeavenMS-era history/reference.
- `docs/area_bosses/AreaBoss.js`: stray script (only referenced indirectly via `weakenAreaBoss`); ask cosmic-content
  whether it belongs in `scripts/`.
- `client-patches/AGENTS.md` still says "Ubuntu VM" but everything runs on one Windows PC - small fix, not in my lane.

## Proposed order once E0 harness exists
1. P1 economy/dupe: C #223, #273, review #277.  2. P1 mob skills: #338.  3. A1 deadlock soak test.
4. P2 packet/visibility group (A6, A12, A15, #312) as one cosmic-core packet-work series.

## Board cards in suggested order (all still in `triage`; none started)
Do not promote any of these until the baseline card t_08de931a is done. Max 4 running at once.
Note: t_3d261a7f and t_c0fe3e3e both edit PacketCreator.java/Character.java - never run together.

| Order | Card | Why |
|-------|------|-----|
| 1 | t_a546c9a5 item dupe sweep | P1 economy |
| 2 | t_a561b3b2 session hijack on channel change | P1 security |
| 3 | t_0315aa2c mob skills / monster status | P1 |
| 4 | t_e0df81e8 concurrency / deadlocks | P1 |
| 5 | t_2bcf2c03 repro sweep (qa) | confirms the rest |
| 6 | t_f000aac2 EXP/rate correctness | P2 |
| 7 | t_2039424a pets | P2 |
| 8 | t_3d261a7f skills not visible to others | P2 (after 9 or before, not together) |
| 9 | t_c0fe3e3e skill logic bugs | P2 |
| 10 | t_af99ec2b, t_5ae0d411, t_7084ba40 content fixes | P2 |
| 11 | t_a78fbf7a, t_66aac4a3, t_77b47db8, t_8ccb8fd6 misc core | P2-P3 |
| later | t_6ef7cd3e MTS/name change, t_cabd9a2d family, t_4cfc4aa4 uncoded PQs, t_d8010074 events, t_2b09b658 client | P3 / needs E0 or research first |

Research card t_ef9290c0 (v83 availability) is running first; its result decides t_4cfc4aa4 and t_d8010074.
