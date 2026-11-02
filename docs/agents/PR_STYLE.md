# PR descriptions - write for a non-programmer reviewer

The human approves PRs and is new to server development. Every PR body uses exactly this shape,
plain language, no jargon (or explain it in brackets). Max ~15 lines.

## What this does
One or two sentences: what changes for the server or players.

## Why
One sentence: the problem it fixes or the goal it serves.

## How to check it
First line: the exact folder to run from, the PR branch's worktree (the main folder lacks unmerged code):
`cd C:\Users\joshs\Documents\GitHub\Cosmic\.worktrees\<card-id>`
Then numbered, human steps (e.g. "start the server, log in as admin, open Henesys, NPC X now sells Y").
If it cannot be checked by hand, say "no visible change, tests only".

## Risk
Low / Medium / High, plus one line why. Say what could break.

## Tests
Build: pass/fail (N tests). Smoke test: pass/fail/not run (why).

## Needs from you
"Safe to merge" OR the exact question/decision you need. Never leave this blank.

## Technical notes (optional, last)
File paths, packet/opcode notes, sources. Keep code detail here only.
