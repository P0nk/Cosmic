#!/usr/bin/env python3
"""Apply client-patches/exe/*.yaml byte patches to the v83 client exe.

Usage (on the Windows PC, Python 3 + PyYAML):
    python apply_exe_patches.py "C:\\Nexon\\MapleStory\\HeavenMS-localhost-WINDOW.exe" [--only 001,003] [--dry-run]

Each patch file is verified before anything is written:
  * the exe's current bytes at every offset must equal `original` (or already equal `replacement` -> skipped)
  * a backup <exe>.bak-<id> is written before the first change of each patch file
"""
import argparse
import hashlib
import pathlib
import shutil
import sys

import yaml

HERE = pathlib.Path(__file__).resolve().parent


def hexbytes(s: str) -> bytes:
    return bytes.fromhex(s.replace(" ", ""))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("exe")
    ap.add_argument("--only", help="comma-separated patch ids")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    exe = pathlib.Path(args.exe)
    data = bytearray(exe.read_bytes())
    only = set(args.only.split(",")) if args.only else None

    files = sorted((HERE / "exe").glob("*.yaml"))
    if not files:
        print("No patches in client-patches/exe/")
        return 0

    for f in files:
        spec = yaml.safe_load(f.read_text())
        pid = str(spec["id"]).zfill(3)
        if only and pid not in only:
            continue
        print(f"== {pid} {spec['title']}")
        pending = []
        for p in spec["patches"]:
            off = int(str(p["offset"]), 16) if isinstance(p["offset"], str) else int(p["offset"])
            orig, repl = hexbytes(p["original"]), hexbytes(p["replacement"])
            if len(orig) != len(repl):
                print(f"   ! {hex(off)}: original/replacement length differ - aborting")
                return 1
            cur = bytes(data[off:off + len(orig)])
            if cur == repl:
                print(f"   = {hex(off)} already applied")
            elif cur == orig:
                pending.append((off, repl, p.get("note", "")))
            else:
                print(f"   ! {hex(off)}: expected {orig.hex(' ')} found {cur.hex(' ')} - wrong exe version? aborting")
                return 1
        if not pending:
            continue
        if args.dry_run:
            for off, repl, note in pending:
                print(f"   would patch {hex(off)} -> {repl.hex(' ')}  {note}")
            continue
        backup = exe.with_name(exe.name + f".bak-{pid}")
        if not backup.exists():
            exe.write_bytes(bytes(data))  # persist previous patches before backing up
            shutil.copy2(exe, backup)
        for off, repl, note in pending:
            data[off:off + len(repl)] = repl
            print(f"   patched {hex(off)}  {note}")
        exe.write_bytes(bytes(data))

    print("sha256:", hashlib.sha256(bytes(data)).hexdigest())
    return 0


if __name__ == "__main__":
    sys.exit(main())
