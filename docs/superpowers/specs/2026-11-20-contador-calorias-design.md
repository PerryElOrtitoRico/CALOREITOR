# CALOREITOR — contador de calorías del ecosistema (diseño)

Fecha: 2026-11-20. Estado: borrador para aprobar.

## Para qué y por qué

Franco quiere registrar lo que come y compararlo contra un objetivo diario de calorías y macros, calculado a partir de su entrenamiento real (gimnasio con PESEITOR, running con ZANCADEITOR). Es una app más del ecosistema (PANEL, PESEITOR, ZANCADEITOR, ESTUDIEITOR) y se usa igual que ellas: **app de Android en el celular** (donde más carga) y **programa de escritorio en la PC** (donde también carga). Además quiere planificar las comidas de la semana para hacer la compra del supermercado más fácil, y ver en el PANEL si ese día cumplió las calorías.

**Éxito:** abre la app, ve de un vistazo cuánto comió contra el objetivo del día, y cargar una comida le lleva segundos.

**Anti-objetivo (lo que la haría fracasar aunque funcione):** que cargar comidas sea lento o tenga muchos pasos. Cada decisión de pantalla se juzga contra esto.

## Objetivos

1. Pestaña **Hoy**: calorías y cada macro del día con medio círculo de % completado.
2. Pestaña **Comidas**: comidas del día (desayuno, almuerzo, cena, etc.) con alimentos y gramos.
3. **Alimentos** con calorías y macros ya cargados; búsqueda por nombre con los más consumidos primero.
4. **Recetas**: platos armados con gramos por ingrediente; al usarlos se ajustan los gramos para esa vez.
5. **Planificador semanal** estático y **lista del súper**.
6. **Configuración**: objetivo automático (a partir del entrenamiento) o manual.
7. **PANEL**: muestra las calorías del día, en verde si están a ±5 % del objetivo.
8. Carga en celular y PC sin conflictos, sin depender de internet.

## No-objetivos (v1)

- Escanear códigos de barras.
- Buscar en bases online (Open Food Facts) — *decisión mía*: se puede sumar después.
- Fotos de comida, IA que reconoce platos, chat.
- Leer los días de la semana que trae ZANCADEITOR (Franco sabe que están mal y los va a arreglar él).
- Pasos reales por día (los pasos son un promedio fijo, ver abajo).
- Gráficos de evolución histórica (se puede sumar después).

## Restricciones

- Java 21 + Maven 3.9, como el resto. Escritorio en Swing + FlatLaf, jar `jar-with-dependencies` con acceso directo en el Escritorio. Android nativo con Gradle en `android/`, instalado por cable (`./gradlew installDebug`).
- Seguir el patrón del Panel: **una sola copia** del código común (lógica, modelo, datos) usada por la app de Android y por el programa de escritorio, con sus tests en JUnit 5 corriendo en `mvn test`.
- La fuente de verdad del último mes son los archivos de eventos (ver Sincronización); el estado se arma en memoria al abrir. Lo anterior queda como totales por día en SQLite (ver Historial).
- Sin servidor obligatorio: la app funciona aunque server-franco esté caído.
- Textos de la interfaz, comentarios y docs en español; identificadores pueden ir en inglés o español según el resto del ecosistema (mayormente español).
- Proyecto nuevo en `~/Escritorio/Hobbies/programacion/Proyectos/ContadorCalorias/`. El nombre CALOREITOR es *decisión mía*, siguiendo el estilo `-EITOR`; Franco lo puede cambiar.

## Pantallas

### Hoy

- Arriba: calorías consumidas / objetivo, con medio círculo de %.
- Debajo: proteínas, carbohidratos y grasas, cada uno con su medio círculo (g consumidos / g objetivo).
- Indica qué entrenamientos tiene el día (descanso, o cualquier combinación de gimnasio, running y pliometría) y de dónde sale el objetivo (automático o manual).
- *Decisión mía:* pasado el 100 % el círculo queda lleno y el número muestra el exceso; sin rojo.
- Acceso directo a "agregar a…" la próxima comida, para no tener que cambiar de pestaña.

### Comidas

- Lista de comidas del día, en el orden que Franco configura (nombre libre, ej. desayuno, almuerzo, merienda, cena, post-entreno). La cantidad y los nombres se definen en Configuración y se pueden agregar o sacar comidas en el día.
- Cada comida muestra sus alimentos con gramos, y sus totales (kcal y macros).
- Navegar a días anteriores y siguientes.
- **Carga rápida (el anti-objetivo manda):**
  - Un campo de búsqueda por nombre. Con el campo vacío ya se ven sugerencias: los alimentos y recetas que más usa en **esa comida** (frecuencia de los últimos ~30 días), primero.
  - Tocar un resultado lo agrega con los **últimos gramos usados** para ese alimento; los gramos se editan en el mismo renglón. Agregar un alimento = buscar + un toque (+ editar gramos si cambia).
  - "Repetir": copiar una comida de otro día (ej. el desayuno de ayer) en un toque.
  - Si el alimento no existe, crearlo desde la misma búsqueda (nombre + kcal, proteínas, carbohidratos y grasas por 100 g), sin salir de la pantalla.
- Editar gramos o borrar un ítem desde la lista.

### Alimentos

- Cada alimento guarda valores **por 100 g**: kcal, proteínas, carbohidratos, grasas. Opcional: porción típica con nombre (ej. "1 huevo = 50 g").
- Base **precargada** de alimentos genéricos, de una tabla pública con nombres en castellano rioplatense; preferencia por la tabla argentina (ARGENFOODS) y completar con USDA. La elección de fuente y licencia queda al builder (ver abajo).
- Alimentos propios: crear, editar, borrar (los precargados se pueden editar; la edición crea una copia propia para no perder el original).
- Búsqueda insensible a tildes y mayúsculas, por palabras ("arroz blanco" encuentra "Arroz blanco hervido").

### Recetas

- Una receta = nombre + lista de ingredientes (alimento + gramos). Ej.: Milanesa (250 g) con arroz (160 g) y verduras (100 g).
- Crear, editar y borrar recetas.
- Al agregarla a una comida aparecen sus ingredientes con los gramos de la receta; Franco los ajusta **para esa vez** sin cambiar la receta guardada. En el registro queda como receta con sus ingredientes y gramos usados (no se convierte en un alimento suelto), para que la frecuencia de sugerencias cuente la receta.
- Aparece en la búsqueda junto con los alimentos.

### Planificador semanal

- Calendario de lunes a domingo de la semana elegida.
- Cada día muestra su objetivo de kcal y macros, calculado con la Semana de entrenamiento (ver Configuración) al armar el plan.
- En cada día y comida se agregan alimentos o recetas con gramos, con la misma carga rápida de la pestaña Comidas. Se ve cuánto falta o sobra contra el objetivo del día.
- **Estático:** el objetivo de cada día se guarda al crear la semana; cambiar la configuración después no lo modifica. Un botón "Recalcular objetivos" lo actualiza si Franco quiere.
- **Lista del súper:** suma los gramos de cada alimento de la semana (o de los días elegidos), con las recetas desarmadas en sus ingredientes, agrupada por alimento. Ej.: "Arroz 1,1 kg; Milanesa 1,75 kg". Se puede copiar como texto.
- En el día en curso, cada comida planeada tiene "Comí lo planeado": la copia al registro real, donde se ajusta si comió distinto.

### Configuración

**Datos personales (los carga Franco):** peso (kg), altura (cm), edad, sexo (para la fórmula de metabolismo), pasos diarios promedio. Los pasos son un valor fijo que representa su día típico (ej. 6000): no varía el cálculo de un día a otro aunque un día camine 4000 y otro 8000.

**Comidas del día:** nombres y orden de las comidas por defecto.

**Modo de objetivo:**

- **Automático** (por defecto): ver Cálculo.
- **Manual:** Franco elige kcal totales y % de proteínas, carbohidratos y grasas (deben sumar 100 %). Mismo objetivo todos los días.

**Meta:** bajar, mantener o subir de peso.

**Semana de entrenamiento:** Franco marca, para cada día de la semana, si hace gimnasio, running y/o pliometría (tres marcas independientes; un día puede tener varias o ninguna).

**Planes de entrenamiento:** importar el `.json` exportado por PESEITOR (`"formato": "rutineitor-plan"`) y por ZANCADEITOR (`"formato": "running-plan"`). Ejemplos reales en `~/Documentos/Carpeta personal/Entrenamientos/Gimnasio/` y `.../Running/`. Importar desde la PC (selector de archivo) y desde el celular (Cargar plan, o "Abrir con"/"Compartir", como ZANCADEITOR). Un plan nuevo del mismo formato reemplaza al anterior.

## Cálculo del objetivo automático

Todo el cálculo se muestra en Configuración con sus números ("cómo se calculó hoy"), para que Franco lo entienda y desconfíe si algo es raro. Los coeficientes marcados *decisión mía* son editables en Configuración (sección avanzada).

1. **Metabolismo basal (BMR):** Mifflin-St Jeor con peso, altura, edad y sexo.
2. **Base diaria (igual todos los días):** BMR × 1,1 (efecto térmico de la comida y actividad mínima, *decisión mía*) + gasto de los pasos promedio: pasos × 0,0005 kcal × peso en kg (≈ 0,035 kcal por paso a 70 kg, *decisión mía*).
3. **Extra de running del día:** si ese día está marcado como running y hay sesión asignada (ver Asignación): Σ por bloque de minutos × MET de la zona × peso × /60. Cada bloque cuenta `minutos × repeticiones`, más `minutosPausa × repeticiones` en `zonaPausa` si existe. MET por zona (*decisión mía*): Z1 7, Z2 8,5, Z3 10, Z4 11,5, Z5 13.
4. **Extra de gimnasio del día:** si ese día está marcado como gimnasio y hay sesión asignada: volumen = Σ reps × kg de todas las series; intensidad = % del máximo promedio de las series (promedio de `pctMin`/`pctMax`, ponderado por series). Estimación (*decisión mía*): duración = series totales × 2,5 min; MET = 3,5 si intensidad < 60 %, 5 entre 60 y 80 %, 6 si > 80 %; kcal = MET × peso × duración/60. **Extra de pliometría** (aparte, en los días de pliometría): `contactos` × 0,1 kcal × peso/70 (*decisión mía*). El volumen y la intensidad se muestran en la explicación del día aunque la fórmula solo use series e intensidad.
5. **Total del día** = base + extras. **Objetivo** = total × (1 − 0,15) para bajar, × 1 para mantener, × 1,10 para subir (*decisión mía*).
6. **Macros:** proteína = 2,0 g/kg; grasas = 0,8 g/kg (piso fijo); carbohidratos = el resto de las kcal (4/4/9 kcal por g). Así los carbohidratos suben los días de entrenamiento fuerte y bajan los de descanso. Si el resto diera negativo, bajar las grasas hasta 0,6 g/kg y avisar.
7. Si no hay ningún plan importado, o el día no tiene sesión asignada, el objetivo es solo la base (con la meta aplicada).

### Días pasados (*decisión mía*)

- El objetivo de **hoy** y de días futuros se calcula al vuelo con la configuración actual.
- Cuando un día termina, su objetivo **queda guardado** (evento "objetivo del día", que escribe el primer dispositivo que abre la app después de ese día; si los dos lo escriben, gana el más nuevo como cualquier entidad). Cambiar peso, meta, modo o Semana de entrenamiento no reescribe días pasados ni el verde/neutro del Panel.
- Importar un plan nuevo **no borra** el anterior: se guardan todos, y para cada fecha se usa el último plan importado de ese formato cuyo rango la cubre.

### Asignación de sesiones a días

Las fechas que traen los planes **no** se usan para elegir el día de la semana. Franco elige los días en la Semana de entrenamiento; la app reparte las sesiones de la semana del plan **en orden**:

- **Running:** las sesiones se agrupan por su campo `semana`. La semana 1 del plan es la semana (lunes–domingo) que contiene la fecha de la primera sesión; cada semana siguiente avanza una. Dentro de una semana, las sesiones se ordenan por `fecha` (solo para el orden) y la 1.ª va al primer día de running marcado, la 2.ª al segundo, etc.
- **Gimnasio:** la semana 1 es la que contiene `inicio`; `microciclo` N es la semana N. Las sesiones `dia` 1, 2, 3… van a los días de gimnasio marcados en orden.
- **Pliometría:** tiene **días propios** (decidido por Franco): su semana 1 es la que contiene su propio `inicio`, y su `dia` N va al N-ésimo día de pliometría marcado. Puede coincidir con un día de gimnasio, de running o caer sola.
- **Días combinados:** un mismo día puede tener gimnasio, running y pliometría a la vez; sus extras se **suman** (la base no incluye ejercicio, así que no se cuenta dos veces).
- Si una semana tiene más sesiones que días marcados, las que sobran no suman; si tiene menos, los días sin sesión no tienen extra. Fuera del rango del plan (antes del inicio o después del último microciclo/semana), no hay extra.

## Sincronización entre celular y PC

Mismo mecanismo que el resto del ecosistema: una carpeta sincronizada con **Syncthing** entre PC y celular (la ruta se elige en Configuración; Franco la agrega a Syncthing).

- Cada dispositivo escribe **solo sus propios archivos** dentro de esa carpeta (el nombre del dispositivo se configura una vez), agregando líneas al final:
  - `catalogo-<dispositivo>.jsonl`: alimentos propios, recetas, configuración y planes de entrenamiento. Se guarda para siempre (cambia poco).
  - `dia-<dispositivo>-<yyyy-MM>.jsonl`: consumos, objetivo del día y planes semanales, en el archivo del mes de **su fecha**. Estos se borran cuando ya pasaron al historial (ver Historial).
- Cada evento es una línea JSON con: id único del evento, id de la entidad, tipo de entidad (consumo, alimento, receta, comida-del-día, plan semanal, configuración, plan de entrenamiento importado), operación (crear/actualizar/borrar), contenido completo de la entidad, marca de tiempo y dispositivo.
- Cada dispositivo lee **todos** los archivos de eventos de la carpeta y arma su estado: por entidad gana el evento con marca de tiempo más nueva (desempate por id de evento). 
- Funciona sin conexión; al volver a verse, Syncthing copia los archivos y cada dispositivo se pone al día (releer al abrir la app y al volver a primer plano; en la PC también cada pocos minutos o con un botón).
- Como nunca dos dispositivos escriben el mismo archivo, Syncthing no genera conflictos.

## Historial (decidido por Franco)

De lo que tiene más de un mes solo interesan los totales del día.

- Cada dispositivo tiene una base **SQLite propia** (fuera de la carpeta de Syncthing) con una fila por día: fecha, kcal, proteínas, carbohidratos y grasas consumidas, y el objetivo de ese día (kcal y macros).
- Cuando un día cumple más de un mes (31 días), se pasa al historial con sus totales y su objetivo.
- Cuando todos los días de un mes ya están en el historial y ese mes terminó hace más de dos meses (62 días), el dispositivo borra **su propio** archivo `dia-…` de ese mes. Así los eventos guardan entre uno y dos meses; el margen da tiempo a que el otro dispositivo se sincronice.
- Recetas, alimentos, configuración y planes de entrenamiento no se borran nunca (van en `catalogo-…`).
- En Comidas, un día con más de un mes muestra solo sus totales y su objetivo (sin el detalle de alimentos).
- Riesgo aceptado: si un dispositivo pasa más de dos meses sin sincronizar, los días que el otro ya borró le quedan sin detalle ni total.

## Integración con el PANEL

El PANEL recibe registros por HTTP (`POST /api/registros` con token).

- CALOREITOR publica un registro por día: `tipo` resultado, área nueva `nutricion`, `metrica` "kcal", `valor` = kcal consumidas, `idEnFuente` = la fecha, `detalleJson` con el objetivo del día y los macros consumidos y objetivo. **Publican el celular y la PC** (decidido por Franco: si carga solo en el celular, igual tiene que llegar). Cada uno, al releer los eventos, republica los últimos 14 días con consumos; `actualizadoEn` = marca de tiempo del evento más nuevo de ese día, así gana el dispositivo que vio el dato más nuevo y los dos terminan en el mismo valor. Sin conexión no pasa nada: se reintenta la próxima vez que relee (republicar es idempotente). *Decisión del plan:* en vez de `sync-core` (no corre en Android) se usa un cliente HTTP común con `HttpURLConnection`, como el de la app de Android del Panel.
- La conexión al Panel es opcional (Configuración, en la PC y en el celular, como en ZANCADEITOR: dirección, token, dispositivo, "Enviar al panel", "Probar conexión"). Sin configurar, la app funciona igual.
- **Cambios en el repo `Panel`:** agregar el área `nutricion` a `Areas` (sin esfuerzo) y mostrarla en el programa de escritorio y en la app de Android, en el detalle del día: kcal consumidas / objetivo, **en verde si |consumidas − objetivo| ≤ 5 % del objetivo**, en color neutro si no. No suma al calendario de esfuerzo.
- **Alcance (*decisión mía*):** los cambios de código del Panel entran en este plan, con sus tests. Redesplegar el servidor en server-franco (que acepte el área nueva) es un paso de Franco, como el despliegue pendiente del 20/11; hasta entonces el servidor rechaza el área y la app reintenta en cada relectura. "Terminado" = tests en verde en los dos repos; la prueba de punta a punta queda en la lista de pruebas a mano.

## Lo que decide el builder

- Fuente concreta de la base de alimentos precargada (ARGENFOODS, USDA u otra), con licencia que permita incluirla, y cuántos alimentos incluir (cientos de genéricos alcanza; prioridad a lo cotidiano en Argentina).
- Diseño visual de cada pantalla dentro de lo descrito, siguiendo el estilo de las otras apps (FlatLaf en PC).
- Estructura de módulos, siguiendo el patrón del Panel (código común compartido entre Android y escritorio).
- Formato exacto de los eventos.
- Si un alimento borrado se mantiene en los registros viejos (debería: los consumos guardan una copia de los valores nutricionales al momento de cargarse).

## Qué falta del lado de Franco

- Agregar la carpeta de sincronización a Syncthing en PC y celular.
- Configurar el token del Panel para CALOREITOR en la PC y en el celular (uno por dispositivo).
- Probar en el celular antes de cualquier push (como el resto del ecosistema).
