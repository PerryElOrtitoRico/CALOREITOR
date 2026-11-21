# Mejoras con qwen: app de Android (Tarea 12)
Retrospectiva por iteración (qué falló, qué cambié).

## Iteración 1: Tarea 12, app de Android (20/11/2026)
Reparto: 8 subtareas qwen-9b al primer pedido, 3 Gemma, 1 corrección de qwen, 0 escritas por Claude como reemplazo. Claude hizo esqueletos y cableado (MainActivity ya armado, `mutar`/`escribir`, constructores de vistas, layouts, manifest). Pedidos con `docs/llm/pedir.py`, cola sin supervisión con `docs/llm/cola.sh`.

Qué falló y qué cambié:
- **T12c (qwen): no compilaba.** Asignó `json` y `plan` dos veces (`= null` y después el valor): no eran efectivamente finales en las lambdas, y llamó a `Sesion.escribir` desde el hilo de E/S. Corrección T12d: le di la estructura del método en Java literal con `final String json;` y un `return` en cada `catch`; copió bien al segundo pedido. Regla: cuando hay variables que entran en lambdas, dar el cuerpo con `final` ya puesto.
- **T17b (Gemma): se pasó de subtarea.** Con el `// TODO T17c` + `throw` visibles en el esqueleto, completó `leer` con setters inventados (`c.setPesoKg`) sobre un record y no compilaba. Mi `pedir.py` no reescribe los TODO ajenos como "lo completa otra subtarea". No hizo falta corregir: T17c (qwen) pisó ese cuerpo y quedó bien al primer pedido. Cambio pendiente: que `pedir.py` marque los TODO de otras subtareas.
- **Sin ``` en la respuesta (qwen, T12a)**: devolvió el archivo con `// ruta` en la primera línea y sin bloque; el extractor aceptó el texto entero.
- **Velocidad**: 4–6 tok/s medidos (T12c 3,8; T14c 6,4; Gemma T17b 5,4), 175–680 s por pedido. Más lento que lo anotado en `qwen-aprendizajes.md` (10–12).
- **Trampa de Android que ningún test agarra**: el `Spinner` dispara `onItemSelected` al abrirse; se resolvió en el esqueleto (tag con la posición conocida, `elegir()`), no se le pidió al modelo.
