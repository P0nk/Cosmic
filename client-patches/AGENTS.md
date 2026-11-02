# client-patches/ — v83 client changes (owned by `cosmic-client`)

The client is a **prebuilt Windows binary** (`HeavenMS-localhost-WINDOW.exe`) plus custom `.wz` files from
`P0nk/Cosmic-client`. Agents on the Ubuntu VM **cannot run or visually test the client**. So this lane produces
*reviewable, reproducible patch specs*; the human applies and play-tests them on the Windows PC.

## Layout
```
client-patches/
  exe/<NNN>-<slug>.yaml   # byte patches for the client exe
  wz/<NNN>-<slug>.md      # WZ edit specs (HaRepacker steps) + matching server wz/ XML diff
  apply_exe_patches.py    # applies exe/*.yaml in order, verifying original bytes first
  CHANGELOG.md            # what each patch does + which server PR depends on it
```

## Exe patch format (`exe/*.yaml`)
```yaml
id: 001
title: Raise damage cap display
target_sha256: <sha256 of the unpatched exe this applies to>
requires_server_pr: "#123"        # optional
patches:
  - offset: 0x00ABCDEF             # file offset, NOT virtual address
    original: "68 0F 27 00 00"     # must match exactly or the script aborts
    replacement: "68 3F 42 0F 00"
    note: "push 9999 -> push 999999"
rollback: "Restore the backed-up exe (apply script writes <exe>.bak-<id>)"
```
- Always record `original` bytes; the apply script refuses to patch if they don't match.
- Explain *how* the offset was found (string xref, pattern scan, known v83 address list) so a human can verify.
- Changing the server IP is a patch too (the 127.0.0.1 strings) — but prefer Windows `netsh portproxy` for dev.
- Anything security-sensitive (disabling client checks) must be called out in the PR description.

## WZ change spec (`wz/*.md`)
1. Which `.wz` file / node path changes, old → new values.
2. Exact HaRepacker-resurrected steps (encryption **GMS (old)**), then "Export → Private Server" into `wz/`.
3. The resulting server `wz/` XML diff (the agent writes this in the same PR so server + client agree).

## Done = human verified
Cards in this lane always end with `kanban_request_review` and a `kanban_block` question:
"Applied on Windows and play-tested? (yes / issue: …)". Never mark them complete yourself.
