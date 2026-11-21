#!/usr/bin/env python3
"""Reextrae un registro ya contestado: aplicar.py docs/llm/registros/X.md"""
import pathlib, re, sys
REPO = pathlib.Path(__file__).resolve().parents[2]
t = pathlib.Path(sys.argv[1]).read_text()
for b in re.findall(r"```[a-z]*\n(.*?)```", t, re.S) or [t]:
    m = re.match(r"\s*//\s*(\S+\.java)\s*\n", b)
    if m: (REPO / m.group(1)).write_text(b[m.end():]); print("archivo:", m.group(1))
