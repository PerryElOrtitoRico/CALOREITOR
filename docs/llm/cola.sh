#!/bin/bash
# cola de subtareas: espera a que termine T12d y las corre una tras otra
cd "$(dirname "$0")/../.."
while pgrep -f 'ciclo.sh T12d' >/dev/null; do sleep 3; done
for p in "T13a qwen RecetaEditor" "T13b qwen RecetasView" "T14a qwen PlanificadorView" "T14b qwen PlanificadorView" "T14c qwen PlanificadorView" "T15a gemma ConexionPanelDialogo" "T15b qwen ConexionPanelDialogo" "T16a gemma AlimentosSeccion" "T17a qwen FormularioConfig" "T17b gemma FormularioConfig" "T17c qwen FormularioConfig" "T18a gemma ConfiguracionView"; do
  set -- $p
  echo "=== $1 $2 $(date +%T)"
  docs/llm/ciclo.sh $1 $2 android/app/src/main/java/com/caloreitor/app/ui/$3.java 2>&1 | cut -c1-300 | head -40
done
echo "=== COLA TERMINADA"
