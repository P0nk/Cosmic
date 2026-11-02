---
name: cosmic-content-script
description: Write or fix NPC, quest, portal, reactor and event JS scripts and drop/shop data for Cosmic so they match GMS v83. Use for content-accuracy cards.
version: 1.0.0
metadata:
  hermes:
    tags: [cosmic, scripts, quests, npc, drops, v83]
    related_skills: [cosmic-build-test, cosmic-pr]
---
# Content scripts & data

## Procedure
1. Identify ids: `grep -i "<name>" handbook/NPC.txt handbook/Quest.txt handbook/Map.txt` and
   `wz/String.wz/*.img.xml`. Quest requirements/rewards: `wz/Quest.wz/{Check,Act}.img.xml`.
2. Find the v83 behaviour (web search the NPC/quest name + "v83" or "2009"; prefer data over wiki prose).
3. Copy the closest existing script in the same folder as a style template. Script API = methods on
   `scripting/npc/NPCConversationManager.java` and `scripting/AbstractPlayerInteraction.java`. Only call methods
   that exist.
4. Every dialog exit path calls `cm.dispose()`; handle `mode == -1` and `mode == 0` (End Chat / No).
5. Drop / shop changes: add a NEW Liquibase changeset file in `src/main/resources/db/extensions/`
   (auto-included by `changelog-root.xml` via `includeAll`, run in filename order — prefix with a timestamp,
   e.g. `20261102-1200-henesys-drops.xml`). Never edit the old seed files in `db/data/` in place.
6. `node --check` the script, then run `cosmic-build-test`.

## Pitfalls
- Giving items: check inventory space first (`cm.canHold(itemId, qty)`), or players lose rewards.
- Quest scripts must start/complete via the quest API so quest state saves; don't fake it with NPC dialog only.
- Don't change map/portal ids in `wz/` XML without a linked `cosmic-client` card.
