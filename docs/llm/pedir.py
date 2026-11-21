#!/usr/bin/env python3
"""Manda una tarea (.md) a LM Studio y escribe en el repo los archivos que devuelve (bloques con la ruta en la 1a linea).
Uso: pedir.py <tarea.md> <qwen|gemma> [--razonar low|medium] [--no-aplicar]"""
import argparse, json, pathlib, re, sys, time, urllib.request
REPO = pathlib.Path(__file__).resolve().parents[2]
MODELOS = {"qwen": "qwen/qwen3.5-9b", "gemma": "google/gemma-4-e4b-qat"}
SYSTEM = """Sos un programador Java 17 para Android (API 26, vistas por código, sin Kotlin, sin AndroidX salvo lo que ya se usa) cuidadoso.
Te paso un esqueleto con métodos `// TODO`: completá solo lo que pide la tarea y dejá todo lo demás idéntico (package, imports, firmas, campos, constantes).
Los records se leen con x(): d.hoy(), no existe getHoy(). Comentarios en español, solo el porqué. No uses imports que no existan en lo que te muestro.
Devolvé el archivo completo (sin '...') en un solo bloque de código con la ruta en la primera línea, sin explicaciones. Ejemplo:
```java
// android/app/src/main/java/com/caloreitor/app/ui/Ejemplo.java
package com.caloreitor.app.ui;

final class Ejemplo { }
```"""
a = argparse.ArgumentParser(); a.add_argument("tarea"); a.add_argument("modelo"); a.add_argument("--razonar"); a.add_argument("--no-aplicar", action="store_true")
a = a.parse_args()
tarea = pathlib.Path(a.tarea).read_text()
cuerpo = {"model": MODELOS[a.modelo], "messages": [{"role": "system", "content": SYSTEM}, {"role": "user", "content": tarea}],
          "temperature": 0.2, "max_tokens": 4500}
if a.razonar: cuerpo["reasoning_effort"] = a.razonar
t0 = time.time()
req = urllib.request.Request("http://localhost:1234/v1/chat/completions", json.dumps(cuerpo).encode(), {"Content-Type": "application/json"})
r = json.load(urllib.request.urlopen(req, timeout=1800))
texto = r["choices"][0]["message"]["content"] or ""
fin = r["choices"][0]["finish_reason"]
nombre = pathlib.Path(a.tarea).stem + "-" + a.modelo
(REPO / "docs/llm/registros" / (nombre + ".md")).write_text(texto)
print(f"finish_reason={fin} uso={r['usage']} {time.time()-t0:.0f}s")
if fin == "length": sys.exit("respuesta cortada: no se extrae nada")
bloques = re.findall(r"```[a-z]*\n(.*?)```", texto, re.S) or [texto]  # a veces contesta sin ```
for b in bloques:
    m = re.match(r"\s*//\s*(\S+\.java)\s*\n", b)
    if not m: print("bloque sin ruta ignorado"); continue
    print("archivo:", m.group(1))
    if not a.no_aplicar: (REPO / m.group(1)).write_text(b[m.end():])
