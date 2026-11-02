---
name: cosmic-client-patch
description: Produce a v83 client exe byte patch or a WZ change spec in client-patches/ for human application on Windows. Use for any change the Cosmic client binary or .wz files must receive.
version: 1.0.0
metadata:
  hermes:
    tags: [cosmic, client, wz, reverse-engineering]
---
# Client patches (spec-only lane)

Read `client-patches/AGENTS.md` first — it defines the formats.

## Procedure — exe
1. Get the exe: `git lfs pull` in the Cosmic-client checkout (`~/work/Cosmic-client`), record its sha256.
2. Locate code with static tools only (`radare2`, `rizin`, `objdump`, `strings -el`, Python pattern scans). Never run the exe.
3. Write `client-patches/exe/NNN-slug.yaml` with offset, original, replacement, note, target_sha256.
4. Verify on a copy: `python client-patches/apply_exe_patches.py /tmp/copy.exe --dry-run`, then apply to the copy and
   check sha256 changes only at the intended offsets (`cmp -l`).
5. Update `client-patches/CHANGELOG.md`.

## Procedure — WZ
1. Describe the change in `client-patches/wz/NNN-slug.md` (file, node path, old → new, HaRepacker steps, encryption
   "GMS (old)").
2. Make the matching server-side XML edit in `wz/` in the same PR.

## Finish
`kanban_request_review` + `kanban_block` asking the human to apply on Windows and play-test. Never self-complete.
