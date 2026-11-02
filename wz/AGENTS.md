# wz/ — server-side WZ XML rules

- This folder is the **server's XML export** of game data. The real `.wz` binaries live in the client
  (`Cosmic-client/cosmic-wz`). Server and client data must agree (map ids, portals, item ids, mob stats) or players
  see desyncs, crashes or invisible objects.
- Direction of truth: **client .wz → (HaRepacker "Private Server" export) → this folder.** Never generate client .wz
  from this XML.
- Agents may edit XML here only for **server-only** data (drop rates, mob stats the server owns, quest checks) or when
  the matching client WZ change is specified in `client-patches/wz/` in the same PR series. If a change also needs the
  client, create a `cosmic-client` card and link it.
- Files are large. Use `grep`/`Select-String` to find the node; never rewrite a whole file. Keep formatting and
  attribute order identical to minimise diffs.
- Validate after editing: `python -c "import xml.dom.minidom as m,sys; m.parse(sys.argv[1])" <file>` (fails loudly
  on malformed XML; needs no extra install beyond Python).
