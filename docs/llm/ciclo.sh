#!/bin/bash
# ciclo.sh Txx qwen|gemma archivo.java : reembebe el archivo, pide, aplica, saca los TODO del tipo Txx y compila
cd "$(dirname "$0")/../.."
python3 docs/llm/reembeber.py docs/llm/tareas/$1.md "$3"
python3 docs/llm/pedir.py docs/llm/tareas/$1.md $2 || exit 1
sed -i "/\/\/ TODO $1\b/d" "$3"
(cd android && ./gradlew assembleDebug --offline -q 2>&1 | grep -E 'error:|FAIL' -A2 | head -30)
echo "compilado $1"
