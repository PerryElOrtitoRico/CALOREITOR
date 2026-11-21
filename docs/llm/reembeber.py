#!/usr/bin/env python3
"""reembeber.py tarea.md archivo: vuelve a pegar el archivo actual al final de la tarea (despues de otra subtarea)."""
import sys, pathlib
t = pathlib.Path(sys.argv[1]); f = sys.argv[2]
cab = t.read_text().split("\nArchivo actual (")[0]
t.write_text(cab + f"\nArchivo actual ({f}):\n```java\n// {f}\n" + pathlib.Path(f).read_text() + "```\n")
