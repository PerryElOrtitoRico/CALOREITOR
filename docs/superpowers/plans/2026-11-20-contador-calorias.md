# CALOREITOR — plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-development (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** contador de calorías y macros para celular (Android) y PC (Swing), con objetivo diario calculado del entrenamiento, recetas, planificador semanal con lista del súper, sincronización por Syncthing y publicación al Panel.

**Architecture:** igual que el Panel: el código común (modelo, lógica, datos) vive **una sola vez** en `android/app/src/main/java/com/caloreitor/app/{modelo,logica,datos}` y lo compilan la app de Android (Gradle) y el programa de escritorio (Maven, con `build-helper-maven-plugin` sumando esas carpetas y sus tests). El estado se arma en memoria repitiendo eventos de archivos `.jsonl` (propios de cada dispositivo: un catálogo permanente y uno por mes de días) en una carpeta sincronizada por Syncthing; los días de más de un mes pasan a una tabla SQLite local de totales y sus archivos mensuales se borran. Celular y PC publican al Panel con un cliente HTTP común (`HttpURLConnection`).

**Tech Stack:** Java 21 en la PC / código común compatible con Java 17 (Android); Maven 3.9; Swing + FlatLaf 3.4.1; Gson 2.10.1; sqlite-jdbc 3.45.1.0 (PC) / `android.database.sqlite` (Android); JUnit 5.10.2; Android Gradle Plugin 8.7.3, compileSdk 35, minSdk 26, appcompat 1.7.0, material 1.12.0.

**Spec:** `docs/superpowers/specs/2026-11-20-contador-calorias-design.md`

## Global Constraints

- Raíz del proyecto: `~/Escritorio/Hobbies/programacion/Proyectos/ContadorCalorias/`. Paquete común `com.caloreitor.app`; escritorio `com.caloreitor.escritorio`; Android `com.caloreitor.app` (applicationId igual).
- El código común no usa nada posterior a Java 17 ni APIs de escritorio/Android (`java.awt`, `javax.swing`, `android.*`, `java.net.http`; `java.net.HttpURLConnection` sí). Records sí.
- Textos de interfaz, comentarios y docs en español. Nombre visible: **CALOREITOR**.
- Valores nutricionales siempre **por 100 g**: kcal, proteínas, carbohidratos, grasas (`double`). kcal por gramo de macro: 4 / 4 / 9.
- Sin servidor obligatorio. La conexión al Panel es opcional, en los dos dispositivos; sin configurar, la app no usa la red.
- Nunca reescribir un archivo de eventos: solo agregar líneas al final de los propios. El único borrado permitido es el de la Tarea 7b (archivos `dia-` propios ya archivados).
- **Git:** el proyecto no es repositorio todavía. Antes del primer commit preguntar a Franco si se hace `git init` (Git no tiene `user.name`/`user.email` configurados). Si no quiere, saltear los pasos de commit y decirlo. En el repo `Panel` (ya es Git, rama `main`), commitear en una rama nueva `feature/area-nutricion`. **Nunca push.**
- Tests: `mvn -q test` desde la raíz corre los tests del escritorio y los del código común. `cd android && ./gradlew test` corre los comunes en Android.

## Review Focus

1. **Archivo de eventos a medio sincronizar** (Syncthing copia mientras se lee; última línea cortada o JSON roto): se saltea esa línea, la app abre igual y no la reescribe. → test en Tarea 2.
2. **Dos dispositivos editan lo mismo con el mismo milisegundo**: el resultado es el mismo en los dos (desempate por id de evento). → test en Tarea 2.
3. **Gramos inválidos o vacíos** al cargar (0, negativo, texto, coma decimal "150,5"): "150,5" se acepta como 150.5; 0, negativo o texto se rechazan con mensaje, sin guardar. → test en Tarea 4 (`Parseo.gramos`).
4. **Plan de entrenamiento roto o de otro formato** (JSON inválido, `formato` desconocido, campos faltantes): importar avisa y no guarda nada; nunca rompe el cálculo de días ya cargados. → test en Tarea 5.
5. **Datos personales sin cargar** (primer uso): el objetivo automático no se calcula con ceros; Hoy muestra "Completá tus datos en Configuración" y los consumos se ven igual. → test en Tarea 6 (`Objetivos.del` devuelve vacío).

---

## Estructura de archivos

```
ContadorCalorias/
  pom.xml                                  raíz (packaging pom, módulo caloreitor-escritorio)
  caloreitor-escritorio/pom.xml            suma android/app/src/{main,test}/java como fuentes
  caloreitor-escritorio/src/main/java/com/caloreitor/escritorio/
    Main.java, VentanaPrincipal.java, Textos.java
    infra/CarpetaArchivos.java             CarpetaEventos sobre java.nio
    infra/HistorialSqlite.java             HistorialDias con sqlite-jdbc
    infra/AjustesLocales.java              ~/.config/caloreitor/ajustes.properties (carpeta, dispositivo)
    hoy/ comidas/ recetas/ alimentos/ planificador/ config/   una pestaña por paquete
  caloreitor-escritorio/caloreitor.desktop
  android/ (settings.gradle, build.gradle, gradle wrapper copiado del Panel, app/)
  android/app/src/main/java/com/caloreitor/app/
    modelo/   Macros, Alimento, Item, Consumo, Receta, ItemReceta, Perfil, Configuracion, Objetivo, SemanaEntrenamiento, PlanSemanal
    datos/    Evento, CarpetaEventos, ArchivoEventos, Estado, Repositorio, HistorialDias, HistorialMemoria
    logica/   Parseo, Busqueda, BaseAlimentos, PlanesEntrenamiento, Asignacion, Calculo, Objetivos, Planificador, ResumenPanel
    red/      Transporte, TransporteHttp, PublicadorPanel
    android/  CarpetaSaf, Ajustes
    ui/       MainActivity y una vista por pestaña
  android/app/src/main/resources/alimentos-base.csv
  android/app/src/test/java/com/caloreitor/app/...   tests comunes
  README.md, PRUEBAS.md
```

---

### Task 1: Esqueleto y modelo nutricional

**Files:**
- Create: `pom.xml`, `caloreitor-escritorio/pom.xml` (copiar el patrón de `Panel/panel-escritorio/pom.xml`: propiedades `compartido.main`/`compartido.test`, `build-helper` add-source/add-test-source, includes `com/caloreitor/app/{modelo,logica,datos,red}/**` y `com/caloreitor/escritorio/**`, assembly `jar-with-dependencies` con `mainClass` `com.caloreitor.escritorio.Main`).
- Create: `android/settings.gradle`, `android/build.gradle`, `android/app/build.gradle` (copiar del Panel; `namespace`/`applicationId` `com.caloreitor.app`, sin el bloque `afterEvaluate` de HttpServer), wrapper copiado de `Panel/android/`, `android/app/src/main/AndroidManifest.xml` con una `MainActivity` vacía.
- Create: `modelo/Macros.java`, `modelo/Alimento.java`, `modelo/Item.java`
- Test: `android/app/src/test/java/com/caloreitor/app/modelo/ItemTest.java`

**Interfaces:**
- Produces:
  - `record Macros(double kcal, double proteinas, double carbohidratos, double grasas)` con `Macros mas(Macros o)`, `static Macros CERO`.
  - `record Alimento(String id, String nombre, Macros por100, String porcionNombre, Double porcionGramos, boolean base)` — `porcion*` pueden ser null.
  - `record Item(String alimentoId, String nombre, Macros por100, double gramos)` — copia de los valores al momento de cargar; `Macros macros()` = por100 × gramos/100.

- [ ] **Step 1: test**

```java
@Test void macrosDeUnItem() {
    Item arroz = new Item("a1", "Arroz", new Macros(130, 2.7, 28, 0.3), 160);
    Macros m = arroz.macros();
    assertEquals(208.0, m.kcal(), 1e-9);
    assertEquals(4.32, m.proteinas(), 1e-9);
    assertEquals(44.8, m.carbohidratos(), 1e-9);
    assertEquals(0.48, m.grasas(), 1e-9);
}
@Test void sumaDeMacros() {
    assertEquals(new Macros(3, 3, 3, 3), new Macros(1, 1, 1, 1).mas(new Macros(2, 2, 2, 2)));
}
```

- [ ] **Step 2:** `mvn -q test` → falla de compilación (no existe `Item`).
- [ ] **Step 3:** implementar los tres records.
- [ ] **Step 4:** `mvn -q test` → PASS. `cd android && ./gradlew test` → BUILD SUCCESSFUL (requiere `android/local.properties` con `sdk.dir`; copiar el del Panel, no se versiona: agregarlo a `.gitignore` junto con `target/`, `build/`, `.gradle/`).
- [ ] **Step 5:** preguntar a Franco por `git init` (ver Global Constraints); si acepta, commit `chore: esqueleto de CALOREITOR y modelo nutricional`.

---

### Task 2: Eventos y estado (sincronización)

**Files:**
- Create: `datos/Evento.java`, `datos/CarpetaEventos.java`, `datos/ArchivoEventos.java`, `datos/Estado.java`
- Create: `caloreitor-escritorio/.../infra/CarpetaArchivos.java`
- Test: `datos/EstadoTest.java`, `datos/ArchivoEventosTest.java` (con una `CarpetaEventos` en memoria dentro del test), `caloreitor-escritorio/src/test/.../infra/CarpetaArchivosTest.java` (`@TempDir`)

**Interfaces:**
- Produces:
  - `record Evento(String id, String entidad, String entidadId, String op, long ts, String dispositivo, JsonObject datos)`; `op` ∈ `"put"`, `"del"`; `datos` null en `del`. Una línea JSON por evento con claves `id, ent, eid, op, ts, disp, datos`.
  - `interface CarpetaEventos { List<String> archivos(); List<String> leerLineas(String archivo); void agregarLinea(String archivo, String linea); }` — `archivos()` devuelve solo nombres `catalogo-*.jsonl` y `dia-*.jsonl`. Agregar `void borrar(String archivo)` (lo usa la Tarea 7b).
  - `final class ArchivoEventos(CarpetaEventos carpeta, String dispositivo)` con `List<Evento> leerTodos()` (líneas inválidas se saltean) y `Evento escribir(String entidad, String entidadId, String op, JsonObject datos, long ts, String fechaDelDia)` (id = `UUID.randomUUID()`, escribe en `catalogo-<dispositivo>.jsonl` si `entidad` ∈ {alimento, receta, config, plan}; si no (consumo, objetivo, plansemanal), en `dia-<dispositivo>-<yyyy-MM>.jsonl` con el mes de `datos.fecha` = mes de `fechaDelDia` (`yyyy-MM-dd`; null para catálogo; el `Repositorio` la pasa también en los `del`).
  - `final class Estado` con `static Estado de(List<Evento> eventos)`, `Map<String, JsonObject> vivos(String entidad)` (id → datos, sin borrados) y `long tsMaximo(String entidad, String entidadId)`.
  - `CarpetaArchivos(Path carpeta)` implementa `CarpetaEventos` con `Files` y `StandardOpenOption.APPEND, CREATE`, UTF-8.

- [ ] **Step 1: tests**

```java
@Test void ganaElMasNuevoAunqueVengaDeOtroArchivo() {
    Estado e = Estado.de(List.of(put("c1", 100, "pc", "a"), put("c1", 200, "celular", "b")));
    assertEquals("b", e.vivos("consumo").get("c1").get("v").getAsString());
}
@Test void empateDeTsDesempataPorIdDeEvento() { // id "z" > "a": gana z, en cualquier orden de entrada
    var x = putConId("a", "c1", 100, "x"); var y = putConId("z", "c1", 100, "y");
    assertEquals("y", Estado.de(List.of(x, y)).vivos("consumo").get("c1").get("v").getAsString());
    assertEquals("y", Estado.de(List.of(y, x)).vivos("consumo").get("c1").get("v").getAsString());
}
@Test void borradoMasNuevoSacaLaEntidad() { /* put ts100, del ts200 → vivos vacío */ }
@Test void lineaCortadaOJsonRotoSeSaltea() { /* carpeta con líneas: válida, "{\"id\":\"x\",", "basura" → leerTodos().size()==1 */ }
@Test void escribirSoloAgregaAlArchivoPropio() { /* dispositivo "pc": una receta va a catalogo-pc.jsonl, un consumo del 2026-11-20 a dia-pc-2026-11.jsonl; dia-celular-2026-11.jsonl intacto */ }
```

- [ ] **Step 2:** `mvn -q test` → FAIL (clases inexistentes).
- [ ] **Step 3:** implementar. Orden total de eventos: `(ts, id)`; por `(entidad, entidadId)` gana el máximo.
- [ ] **Step 4:** `mvn -q test` → PASS.
- [ ] **Step 5:** commit `feat: eventos por dispositivo y estado con último-gana`.

---

### Task 3: Repositorio de entidades

**Files:**
- Create: `modelo/Consumo.java`, `modelo/Receta.java`, `modelo/ItemReceta.java`, `modelo/Perfil.java`, `modelo/Configuracion.java`, `modelo/Objetivo.java`, `modelo/SemanaEntrenamiento.java`, `datos/Repositorio.java`
- Test: `datos/RepositorioTest.java`

**Interfaces:**
- Consumes: Tarea 2.
- Produces:
  - `record Consumo(String id, String fecha, String comida, String recetaId, String recetaNombre, List<Item> items)` — `fecha` `yyyy-MM-dd`; `receta*` null si son alimentos sueltos; `Macros macros()`.
  - `record ItemReceta(String alimentoId, double gramos)`; `record Receta(String id, String nombre, List<ItemReceta> ingredientes)`.
  - `record Perfil(Double pesoKg, Double alturaCm, Integer edad, String sexo /* "M"|"F" */, Integer pasosDiarios)` — cualquiera puede ser null.
  - `enum Meta { BAJAR, MANTENER, SUBIR }`; `enum Modo { AUTOMATICO, MANUAL }`.
  - `record SemanaEntrenamiento(Set<DayOfWeek> gimnasio, Set<DayOfWeek> running, Set<DayOfWeek> pliometria)`.
  - `record Objetivo(Macros macros, String explicacion)`.
  - `record Configuracion(Perfil perfil, Modo modo, Meta meta, double manualKcal, double manualPctProt, double manualPctCarb, double manualPctGrasa, List<String> comidas, SemanaEntrenamiento semana, Map<String, Double> coeficientes)` con `static Configuracion porDefecto()`: comidas `["Desayuno","Almuerzo","Merienda","Cena"]`, modo AUTOMATICO, meta MANTENER, manual 2000 kcal 30/45/25, semana vacía, coeficientes vacíos (Tarea 6 define las claves y sus valores por defecto).
  - `final class Repositorio(ArchivoEventos archivo, LongSupplier reloj)` con `void recargar()` y:
    - `List<Consumo> consumos(String fecha)`, `void guardar(Consumo)`, `void borrarConsumo(String id)`
    - `List<Alimento> alimentosPropios()`, `void guardar(Alimento)`, `void borrarAlimento(String id)`
    - `List<Receta> recetas()`, `void guardar(Receta)`, `void borrarReceta(String id)`
    - `Configuracion configuracion()`, `void guardar(Configuracion)` (entidad `config`, id fijo `"config"`)
    - `Optional<Objetivo> objetivoGuardado(String fecha)`, `void guardarObjetivo(String fecha, Objetivo)` (entidad `objetivo`, id = fecha)
    - `List<JsonObject> planesEntrenamiento()` (entidad `plan`, id = UUID, `datos` = `{importadoEn, json}` con el texto original), `void guardarPlan(String jsonOriginal)`
    - `Optional<PlanSemanal> planSemanal(String lunes)`, `void guardar(PlanSemanal)` (definido en Tarea 7; acá solo la persistencia genérica por id = lunes)
    - `long ultimoCambio(String fecha)` = máximo ts de los consumos y el objetivo de esa fecha (para el Panel).
  - Serialización con Gson; cada `guardar` escribe un evento `put` y actualiza el estado en memoria sin releer.

- [ ] **Step 1: tests:** guardar y leer cada entidad ida y vuelta; `recargar()` con un evento de otro dispositivo agregado a mano lo ve; borrar consumo; `configuracion()` sin eventos = `porDefecto()`; `ultimoCambio("2026-11-20")` = ts del evento más nuevo de esa fecha.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** `mvn -q test` PASS.
- [ ] **Step 5:** commit `feat: repositorio de consumos, alimentos, recetas y configuración`.

---

### Task 4: Base de alimentos, búsqueda y carga rápida

**Files:**
- Create: `android/app/src/main/resources/alimentos-base.csv`, `logica/BaseAlimentos.java`, `logica/Busqueda.java`, `logica/Parseo.java`
- Modify: `android/app/build.gradle` si hace falta para que `src/main/resources` llegue al APK (`sourceSets.main.resources.srcDirs`).
- Test: `logica/BusquedaTest.java`, `logica/ParseoTest.java`, `logica/BaseAlimentosTest.java`

**Interfaces:**
- Consumes: `Repositorio` (Tarea 3).
- Produces:
  - CSV UTF-8 con encabezado `id;nombre;kcal;proteinas;carbohidratos;grasas;porcion_nombre;porcion_gramos`, valores por 100 g, ids `b0001…`. Entre 200 y 400 alimentos genéricos cotidianos en Argentina (carnes y cortes, milanesa, pollo, pescados, huevos, lácteos, quesos, pan, pastas, arroz, legumbres, verduras, frutas, aceites, frutos secos, dulce de leche, yerba no; cocidos y crudos cuando difieren). Fuente: USDA FoodData Central (dominio público) o ARGENFOODS si su licencia permite redistribuir; anotar la fuente en la primera línea del README. Nombres en castellano rioplatense.
  - `BaseAlimentos.cargar(InputStream csv) -> List<Alimento>` (base = true).
  - `Busqueda(List<Alimento> todos, List<Receta> recetas, List<Consumo> historial)`; `record Resultado(String id, String nombre, boolean esReceta, double gramosSugeridos)`; `List<Resultado> buscar(String texto, String comida, LocalDate hoy)`:
    - texto vacío → los de **esa comida** ordenados por cantidad de usos en los últimos 30 días (máx. 15).
    - texto no vacío → coincide si **cada palabra** del texto es prefijo de alguna palabra del nombre, sin tildes ni mayúsculas (`java.text.Normalizer`); ordenados por usos en 30 días (cualquier comida) y luego alfabético.
    - `gramosSugeridos` = gramos del último uso de ese alimento; si nunca se usó, `porcionGramos`, si no hay, 100. Para recetas: 0 (se usan los de la receta).
    - Un alimento propio con el mismo nombre que uno base reemplaza al base (edición = copia propia).
  - `Parseo.gramos(String) -> OptionalDouble`: acepta `"150"`, `"150.5"`, `"150,5"` y espacios; vacío si no es número o ≤ 0.

- [ ] **Step 1: tests**

```java
@Test void buscaSinTildesNiMayusculasPorPrefijoDePalabras() { // "arroz bla" → "Arroz blanco hervido"; "LÉNTEJ" → "Lentejas cocidas"
}
@Test void vacioSugiereLosMasUsadosEnEsaComida() { // historial: Avena 3 veces en Desayuno, Milanesa 5 en Almuerzo → buscar("", "Desayuno") empieza con Avena y no tiene Milanesa
}
@Test void sugiereLosUltimosGramosUsados() { // último uso de arroz 180 g → gramosSugeridos 180
}
@Test void historialDeMasDe30DiasNoCuenta() {}
@Test void parseoDeGramos() {
    assertEquals(150.5, Parseo.gramos("150,5").getAsDouble(), 1e-9);
    assertTrue(Parseo.gramos("0").isEmpty());
    assertTrue(Parseo.gramos("-3").isEmpty());
    assertTrue(Parseo.gramos("mucho").isEmpty());
}
@Test void laBaseCargaYTieneValoresSanos() { // ≥200 filas; ids únicos; todo ≥0; |kcal − (4p+4c+9g)| ≤ 15 % de kcal o ≤ 15 kcal (atrapa columnas corridas)
}
```

- [ ] **Step 2:** FAIL. **Step 3:** implementar y armar el CSV. **Step 4:** PASS.
- [ ] **Step 5:** commit `feat: base de alimentos y búsqueda con sugerencias`.

---

### Task 5: Planes de entrenamiento y asignación a días

**Files:**
- Create: `logica/PlanesEntrenamiento.java`, `logica/Asignacion.java`
- Test: `logica/PlanesEntrenamientoTest.java`, `logica/AsignacionTest.java`; copiar como recursos de test `running-plan-c2.json` y `Franco_2026-11-09_Potencia_Pliometria_4s3d.json` desde `~/Documentos/Carpeta personal/Entrenamientos/`.

**Interfaces:**
- Produces:
  - `record BloqueRunning(int zona, double minutos, int repeticiones, Integer zonaPausa, Double minutosPausa)`
  - `record SesionRunning(int semana, String fecha, List<BloqueRunning> bloques)`
  - `record SerieGym(int reps, double kg, double pct)` — `pct` = (pctMin+pctMax)/2 de su fila.
  - `record SesionGym(int microciclo, int dia, List<SerieGym> series)`
  - `record SesionPlio(int semana, int dia, int contactos)` — `semana` = índice 1-based dentro de `pliometria[i].semanas`.
  - `record Plan(String formato, LocalDate inicio, LocalDate fin, long importadoEn, List<SesionRunning> running, List<SesionGym> gym, List<SesionPlio> plio)`
  - `PlanesEntrenamiento.leer(String json, long importadoEn) -> Plan`; lanza `IllegalArgumentException` con mensaje en español si el JSON es inválido, `formato` no es `running-plan` ni `rutineitor-plan`, o faltan `sesiones`.
    - running: `inicio` = lunes de la semana de la primera `fecha`; `fin` = domingo de la semana `max(semana)`.
    - rutineitor: `inicio` = lunes de la semana de `inicio`; `fin` = domingo de la semana `microciclos`. Pliometría: semana 1 = semana de su propio `inicio`; se guardan con fechas relativas a ese inicio (ajustar `inicio`/`fin` del plan para cubrirla).
  - `record EntrenamientoDia(SesionGym gym, SesionRunning running, SesionPlio plio)` — cada uno puede ser null.
  - `Asignacion.para(LocalDate fecha, SemanaEntrenamiento semana, List<Plan> planes) -> EntrenamientoDia`:
    - Para cada formato, se usa el plan con `importadoEn` más nuevo cuyo `[inicio, fin]` cubre la fecha.
    - Semana del plan = semanas enteras desde su lunes de inicio + 1.
    - Running: sesiones de esa semana ordenadas por `fecha`; la k-ésima va al k-ésimo día marcado en `semana.running()` (orden lunes→domingo).
    - Gym: `microciclo` = semana; `dia` k → k-ésimo día de `semana.gimnasio()`.
    - Plio: `dia` k → k-ésimo día de `semana.pliometria()`, con su propia cuenta de semanas.
    - Sobran sesiones → no se asignan. Faltan → días sin sesión. Fecha fuera de rango → null.

- [ ] **Step 1: tests**

```java
@Test void leeElPlanRealDeRunning() { // running-plan-c2.json: 37 sesiones; semanas 1..13; inicio 2026-11-02; primera sesión 3 bloques (Z1 5', Z2 20', Z3 5')
}
@Test void leeElPlanRealDeGimnasioConPliometria() { // inicio 2026-11-09; sesión (1,1) primera serie reps 6 kg 44 pct 55; plio semana 1 día 1 contactos 250
}
@Test void formatoDesconocidoOJsonRotoLanzaConMensaje() {}
@Test void ignoraLaFechaDeZancadeitorYUsaLosDiasMarcados() {
    // running marcado MIÉ, VIE, DOM. Semana 1 del c2 (2026-11-02..08): la sesión del 03 (FINAL_RAPIDO) va al miércoles 04; FARTLEK al viernes 06; LARGA al domingo 08. El martes 03 no tiene running.
}
@Test void gimnasioYPliometriaConDiasPropiosPuedenCoincidir() {
    // gimnasio LUN MIÉ VIE; pliometría MAR VIE. 2026-11-13 (viernes): gym dia 3 y plio dia 2 de la semana 1.
}
@Test void sesionesDeMasNoSeAsignan() { // running con un solo día marcado → solo la 1.ª sesión de cada semana
}
@Test void fueraDelRangoNoHayEntrenamiento() { // 2026-11-01 con el plan de gimnasio → gym null
}
@Test void planNuevoNoPisaFechasQueNoCubre() { // plan A 1/11–30/11, plan B (más nuevo) 1/12–31/12: 15/11 usa A, 15/12 usa B
}
```

- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** PASS.
- [ ] **Step 5:** commit `feat: lectura de planes y asignación de sesiones a los días marcados`.

---

### Task 6: Cálculo del objetivo diario

**Files:**
- Create: `logica/Calculo.java`, `logica/Objetivos.java`
- Test: `logica/CalculoTest.java`, `logica/ObjetivosTest.java`

**Interfaces:**
- Consumes: `Configuracion`, `Perfil`, `EntrenamientoDia`, `Plan`, `Asignacion`, `Repositorio`.
- Produces:
  - Coeficientes (claves de `Configuracion.coeficientes`, con estos valores si faltan): `factorBase` 1.1, `kcalPorPasoPorKg` 0.0005, `metZ1` 7, `metZ2` 8.5, `metZ3` 10, `metZ4` 11.5, `metZ5` 13, `minPorSerie` 2.5, `metGymBajo` 3.5 (< 60 %), `metGymMedio` 5 (60–80 % inclusive), `metGymAlto` 6 (> 80 %), `kcalPorContacto` 0.1 (a 70 kg), `deficit` 0.15, `superavit` 0.10, `protPorKg` 2.0, `grasaPorKg` 0.8, `grasaMinPorKg` 0.6.
  - `Calculo.bmr(Perfil)` (Mifflin-St Jeor: 10·kg + 6,25·cm − 5·edad + 5 si M, −161 si F); `Calculo.base(Perfil, coef)`; `Calculo.running(SesionRunning, pesoKg, coef)` (cada bloque: `minutos·rep·MET(zona)` + `minutosPausa·rep·MET(zonaPausa)` si hay; total × kg / 60); `Calculo.gym(SesionGym, pesoKg, coef)`; `Calculo.volumen(SesionGym)` = Σ reps·kg; `Calculo.intensidad(SesionGym)` = promedio de `pct` por serie; `Calculo.plio(SesionPlio, pesoKg, coef)` = contactos · 0,1 · kg/70.
  - `Calculo.objetivo(Configuracion, EntrenamientoDia) -> Optional<Objetivo>`: vacío si modo AUTOMATICO y falta peso, altura, edad, sexo o pasos. Manual: kcal fijas y % → gramos. Automático: total = base + extras; × (1−deficit) / 1 / (1+superavit); prot = protPorKg·kg; grasa = grasaPorKg·kg; carb = resto/4; si resto < 0, bajar grasa hasta grasaMinPorKg y si aún no alcanza carb = 0; en ese caso la explicación incluye "Ojo: el objetivo es muy bajo para cubrir proteína y grasa mínimas".
  - `explicacion`: texto en varias líneas con cada término y su número redondeado (metabolismo, base, gimnasio con volumen kg e intensidad %, running con minutos por zona, pliometría con contactos, meta, total).
  - `Objetivos(Repositorio repo, List<Plan> planes, LocalDate hoy)`; `Optional<Objetivo> del(LocalDate fecha)`: si `fecha < hoy` y hay guardado → el guardado; si `fecha < hoy` y no hay → calcula, lo guarda con `guardarObjetivo` y lo devuelve; si `fecha ≥ hoy` → calcula al vuelo sin guardar.

- [ ] **Step 1: tests** (perfil: 75 kg, 178 cm, 25 años, M, 6000 pasos; coeficientes por defecto)

```java
@Test void bmrYBase() {
    assertEquals(1742.5, Calculo.bmr(perfil), 1e-9);
    assertEquals(2141.75, Calculo.base(perfil, coef), 1e-9); // 1742,5·1,1 + 6000·0,0005·75
}
@Test void runningPorMinutosEnZona() { // 5' Z1 + 20' Z2 + 5' Z3 → 318.75
}
@Test void runningConPausas() { // Z4 2' ×3 con pausa Z1 2': (11,5·6 + 7·6)·75/60 = 138.75
}
@Test void gimnasioPorSeriesEIntensidad() { // 8 series al 70 % (4×6×91 kg + 4×6×21 kg): 125.0 kcal; volumen 2688; intensidad 70
}
@Test void pliometria() { assertEquals(26.785714, Calculo.plio(new SesionPlio(1, 1, 250), 75, coef), 1e-5); }
@Test void diaDeRunningParaBajar() { // base + running 318,75 = 2460,5 × 0,85 = 2091.425 kcal; prot 150; grasa 60; carb 237.85625
}
@Test void diaCombinadoSumaTodo() { // gym + running + plio el mismo día, MANTENER: 2141,75+125+318,75+26,785714 = 2612.285714
}
@Test void manual() { // 2000 kcal 30/45/25 → prot 150, carb 225, grasa 55.5556
}
@Test void sinDatosNoCalcula() { assertTrue(Calculo.objetivo(conPerfilVacio, sinEntreno).isEmpty()); }
@Test void objetivoMuyBajoBajaGrasaYAvisa() {}
@Test void diaPasadoQuedaGuardadoAunqueCambieLaConfiguracion() {
    // del(ayer) guarda; cambiar el peso; del(ayer) devuelve el mismo; del(hoy) cambia
}
```

- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** PASS.
- [ ] **Step 5:** commit `feat: cálculo del objetivo diario automático y manual`.

---

### Task 7: Planificador semanal y lista del súper

**Files:**
- Create: `modelo/PlanSemanal.java`, `logica/Planificador.java`
- Test: `logica/PlanificadorTest.java`

**Interfaces:**
- Consumes: `Repositorio`, `Objetivos`, `Consumo`, `Receta`.
- Produces:
  - `record PlanSemanal(String lunes, Map<String, Objetivo> objetivos /* fecha → objetivo fijado */, List<Consumo> comidas /* consumos planeados, fecha dentro de la semana */)`.
  - `Planificador(Repositorio repo, Objetivos objetivos)`:
    - `PlanSemanal crear(LocalDate lunes)` — fija los 7 objetivos con `objetivos` calculando al vuelo (sin guardar objetivos del día) y persiste.
    - `PlanSemanal recalcular(PlanSemanal)` — vuelve a fijar los 7 objetivos.
    - `PlanSemanal agregar(PlanSemanal, Consumo)`, `PlanSemanal quitar(PlanSemanal, String consumoId)`.
    - `Macros planeado(PlanSemanal, String fecha)`.
    - `record LineaSuper(String nombre, double gramos)`; `List<LineaSuper> listaSuper(PlanSemanal, Set<String> fechas)` — suma por `alimentoId` (los consumos de receta ya traen sus ítems), orden alfabético.
    - `static String comoTexto(List<LineaSuper>)` — una línea por alimento: `"Arroz — 1,1 kg"` (≥ 1000 g en kg con una decimal y coma; si no `"350 g"`).
    - `Consumo comiLoPlaneado(PlanSemanal, String consumoId)` — copia el consumo planeado con id nuevo al registro real (`repo.guardar`) y lo devuelve.

- [ ] **Step 1: tests:** cambiar la configuración después de `crear` no cambia `objetivos`, y `recalcular` sí; lista del súper suma el arroz de una receta (160 g × 7 días) con arroz suelto 200 g = 1320 g → `"Arroz — 1,3 kg"`; filtrar por fechas; `comiLoPlaneado` crea un consumo real con otro id y deja el plan igual.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** PASS.
- [ ] **Step 5:** commit `feat: planificador semanal estático y lista del súper`.

---

### Task 7b: Historial de días en SQLite y depuración de eventos

**Files:**
- Create (común): `modelo/TotalDia.java`, `datos/HistorialDias.java` (interfaz), `datos/HistorialMemoria.java` (para tests), `logica/Depuracion.java`
- Create (escritorio): `caloreitor-escritorio/.../infra/HistorialSqlite.java` (sqlite-jdbc; base en `~/.config/caloreitor/historial.db`)
- Modify: `caloreitor-escritorio/pom.xml` (`org.xerial:sqlite-jdbc:3.45.1.0`)
- Test: `logica/DepuracionTest.java`, `caloreitor-escritorio/src/test/.../infra/HistorialSqliteTest.java` (`@TempDir`)

**Interfaces:**
- Consumes: `ArchivoEventos`, `Repositorio`, `Objetivos`.
- Produces:
  - `record TotalDia(String fecha, Macros consumido, Macros objetivo /* null si no hubo */)`.
  - `interface HistorialDias { void guardar(TotalDia t); Optional<TotalDia> leer(String fecha); List<TotalDia> entre(String desde, String hasta); boolean existe(String fecha); }` — una fila por fecha; `guardar` reemplaza.
  - Tabla `dias(fecha TEXT PRIMARY KEY, kcal REAL, proteinas REAL, carbohidratos REAL, grasas REAL, obj_kcal REAL, obj_proteinas REAL, obj_carbohidratos REAL, obj_grasas REAL)`; `obj_*` NULL si no hubo objetivo. **Cada dispositivo tiene su propia base, fuera de la carpeta de Syncthing** (un `.db` sincronizado se corrompe).
  - `Depuracion(Repositorio repo, Objetivos objetivos, HistorialDias historial, ArchivoEventos archivo)`; `void correr(LocalDate hoy)`, llamado tras cada `recargar()`:
    1. **Archivar:** cada fecha con consumos y `fecha < hoy − 31 días` que no `existe` en el historial → `guardar` con sus totales y su objetivo (`objetivos.del`, que ya lo deja fijado).
    2. **Borrar:** cada archivo **propio** `dia-<dispositivo>-<yyyy-MM>.jsonl` cuyo último día del mes sea `< hoy − 62 días` y cuyas fechas con consumos estén todas en el historial → `carpeta.borrar(archivo)`. Nunca toca archivos de otro dispositivo ni `catalogo-*`.
  - Así los eventos guardan entre 1 y 2 meses de días, y el historial todo lo anterior.
- [ ] **Step 1: tests**

```java
@Test void archivaDiasDeMasDeUnMesConSuObjetivo() {
    // hoy 2026-12-20; consumos el 2026-11-10 (1800 kcal) y el 2026-12-01 → historial tiene 2026-11-10 con kcal 1800 y objetivo; no tiene 2026-12-01
}
@Test void noVuelveAArchivarNiPisaUnDiaYaArchivado() {}
@Test void borraSoloElArchivoMensualPropioYaArchivado() {
    // hoy 2027-01-05: dia-pc-2026-10.jsonl se borra; dia-pc-2026-11.jsonl no (30/11 + 62 días > hoy); dia-celular-2026-10.jsonl no (es de otro); catalogo-pc.jsonl no
}
@Test void noBorraSiFaltaArchivarAlgunDia() {}
@Test void historialSqliteIdaYVuelta() { /* guardar, leer, entre, reemplazar; obj null queda null */ }
```
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** `mvn -q test` PASS.
- [ ] **Step 5:** commit `feat: historial de totales por día en SQLite y depuración de eventos viejos`.

---

### Task 8: Programa de escritorio — Hoy y Comidas

**Files:**
- Create: `caloreitor-escritorio/src/main/java/com/caloreitor/escritorio/{Main,VentanaPrincipal,Textos}.java`, `infra/AjustesLocales.java`, `hoy/PestanaHoy.java`, `hoy/MedioCirculo.java`, `comidas/PestanaComidas.java`, `comidas/BuscadorRapido.java`, `comidas/DialogoAlimentoNuevo.java`
- Test: `caloreitor-escritorio/src/test/java/.../infra/AjustesLocalesTest.java`

**Interfaces:**
- Consumes: Tareas 2–6.
- Produces:
  - `AjustesLocales.abrir(Path archivo)` con `carpetaEventos()` (Path, null si no se eligió), `dispositivo()` (por defecto `"pc"`), y setters que guardan; archivo `~/.config/caloreitor/ajustes.properties`, permisos solo del usuario (como el Panel).
  - Al abrir sin carpeta elegida: diálogo para elegir la carpeta de Syncthing (`JFileChooser` de directorios). Releer eventos al abrir, al volver el foco a la ventana y con un botón "Actualizar" en la barra.
  - `MedioCirculo extends JComponent`: arco de 180° con % (lleno a partir de 100 %, número muestra el exceso; sin rojo).
  - Hoy: kcal y los 3 macros con `MedioCirculo` (consumido / objetivo); entrenamientos del día ("Descanso" o "Gimnasio · Running · Pliometría" según haya); "Objetivo automático"/"manual"; si `Objetivos.del` está vacío: "Completá tus datos en Configuración". Botón "Agregar a <próxima comida>" (la primera comida del día sin consumos; si todas tienen, la última).
  - Comidas: navegar días (◀ fecha ▶); una sección por comida de `Configuracion.comidas` más las comidas con consumos que ya no estén en la lista; totales por comida.
  - `BuscadorRapido`: campo con foco al abrir; resultados mientras se escribe (`Busqueda.buscar`); Enter o clic agrega con `gramosSugeridos` y deja editar gramos en el renglón (validar con `Parseo.gramos`, mensaje si inválido); receta → agrega con los gramos de cada ingrediente editables para esa vez; última opción "Crear «texto»…" abre `DialogoAlimentoNuevo` (nombre + kcal, P, C, G por 100 g) y al guardar agrega el ítem.
  - "Repetir de otro día": elegir fecha y comida → copia los consumos con ids nuevos.
  - Un día con más de 31 días: si está en `HistorialDias`, muestra solo totales y objetivo ("Detalle no disponible: solo se guardan los totales de días con más de un mes"); sin agregar comidas.
  - Editar gramos y borrar ítems desde la lista.
- Tema FlatLaf como `Panel/panel-escritorio/.../tema/`.

- [ ] **Step 1:** test de `AjustesLocales` (guardar y releer carpeta y dispositivo; archivo nuevo con permisos `rw-------`).
- [ ] **Step 2:** FAIL. **Step 3:** implementar ajustes y pestañas.
- [ ] **Step 4:** `mvn -q test` PASS; `mvn -q package` y `java -jar caloreitor-escritorio/target/caloreitor-escritorio-1.0.0-SNAPSHOT-jar-with-dependencies.jar` con una carpeta temporal: agregar "arroz" al almuerzo en ≤ 3 acciones (escribir, Enter, ajustar gramos), verlo en Hoy.
- [ ] **Step 5:** commit `feat: escritorio con pestañas Hoy y Comidas`.

---

### Task 9: Programa de escritorio — Recetas, Alimentos, Planificador, Configuración

**Files:**
- Create: `recetas/PestanaRecetas.java`, `alimentos/PestanaAlimentos.java`, `planificador/PestanaPlanificador.java`, `config/PestanaConfiguracion.java`, `caloreitor-escritorio/caloreitor.desktop`

**Interfaces:**
- Consumes: Tareas 3–8 (`BuscadorRapido` se reusa para ingredientes y para el planificador).
- Produces:
  - Recetas: lista, crear/editar (nombre + ingredientes con gramos, totales), borrar con confirmación.
  - Alimentos: lista con buscador; crear, editar (editar uno base guarda un alimento propio con id nuevo y el mismo nombre), borrar propios.
  - Planificador: semana lunes→domingo (◀ ▶), cada día con objetivo fijado y planeado / falta-sobra por macro; agregar a cada comida con `BuscadorRapido`; "Recalcular objetivos"; "Lista del súper" (marcar días; muestra `comoTexto` con botón "Copiar"); en el día actual, "Comí lo planeado" por comida.
  - Configuración: perfil; comidas del día (agregar, quitar, ordenar); modo y meta; manual (kcal y %; aviso si no suman 100 y no guarda); semana de entrenamiento (tabla 7 días × Gimnasio/Running/Pliometría con casillas); importar plan (selector `.json`, muestra el error de `PlanesEntrenamiento.leer` si falla); "Cómo se calculó hoy" con `Objetivo.explicacion`; avanzado: coeficientes editables; carpeta de sincronización y nombre del dispositivo.
  - `caloreitor.desktop` como el del Panel (Exec al jar, nombre CALOREITOR).
- [ ] **Step 1:** `mvn -q test` sigue en verde. **Step 2:** implementar.
- [ ] **Step 3:** prueba a mano: importar los dos planes reales, marcar días, ver la explicación de un día combinado, armar una semana y copiar la lista del súper.
- [ ] **Step 4:** commit `feat: escritorio con recetas, alimentos, planificador y configuración`.

---

### Task 10: Área `nutricion` en el Panel (repo `Panel`, rama `feature/area-nutricion`)

**Files:**
- Modify: `Panel/sync-core/src/main/java/panel/core/Areas.java` (`NUTRICION = "nutricion"` en `TODAS`; sin esfuerzo)
- Modify: `Panel/panel-server/src/main/java/panel/server/PanelApp.java:35` (`AREAS`); revisar el bucle de `PanelApp.java:129` y `IntensidadCalculator` para que `nutricion` no sume al calendario de esfuerzo ni a los totales.
- Modify: `Panel/android/app/src/main/java/com/panel/app/logica/Areas.java` (clave `nutricion`, nombre "Nutrición"), `logica/Detalles.java` + nuevo `logica/DetalleNutricion.java`, vista del detalle del día en `ui/DetalleView.java` y en `panel-escritorio/.../calendario/`.
- Test: tests existentes del Panel + nuevos en `sync-core` (registro `nutricion` válido), `panel-server` (POST de un resultado `nutricion` aceptado y fuera de totales/esfuerzo) y `android/app/src/test/.../logica/DetallesTest` (o el que exista).

**Interfaces:**
- Produces: registro esperado de CALOREITOR: `tipo` `resultado`, `fuente` `caloreitor`, `idEnFuente` = fecha, `area` `nutricion`, `metrica` `kcal`, `valor` = kcal consumidas, `detalleJson` `{"objetivoKcal":2091.4,"consumido":{"kcal":..,"proteinas":..,"carbohidratos":..,"grasas":..},"objetivo":{...}}`.
  - `record DetalleNutricion(double kcal, Double objetivoKcal, boolean enRango) implements Detalle`; `enRango` = `objetivoKcal != null && |kcal − objetivo| ≤ 0,05·objetivo`. Se muestra "1985 / 2091 kcal" en verde si `enRango`, color neutro si no.

- [ ] **Step 1: tests**

```java
@Test void nutricionEnRangoDel5PorCiento() {
    assertTrue(DetalleNutricion.de(1990, 2091.4).enRango());   // −4,8 %
    assertFalse(DetalleNutricion.de(1980, 2091.4).enRango());  // −5,3 %
    assertTrue(DetalleNutricion.de(2195, 2091.4).enRango());   // +4,95 %
}
```
más: el server acepta el registro de ejemplo y `GET /api/totales` no tiene `nutricion` con minutos; `GET /api/mes` no colorea por él.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** `cd Panel && mvn -q test` y `cd Panel/android && ./gradlew test` PASS.
- [ ] **Step 5:** commit en `feature/area-nutricion`: `feat: área nutrición con verde a ±5 % del objetivo`. Anotar en `continuar-ecosistema.md` que hay que redesplegar el servidor (paso de Franco).

---

### Task 11: Publicación al Panel (celular y PC)

**Files:**
- Create (común): `logica/ResumenPanel.java`, `red/Transporte.java`, `red/TransporteHttp.java` (copiar de `Panel/android/app/src/main/java/com/panel/app/red/`: UTF-8, sin seguir redirecciones, timeouts, nunca registrar el token), `red/PublicadorPanel.java`
- Create (escritorio): diálogo de conexión en `config/` (aspecto de `Running/src/main/java/com/running/ui/ConexionPanelDialog.java`: dirección, token, dispositivo, "Enviar al panel", "Probar conexión"); se guarda en `AjustesLocales`.
- Test: `logica/ResumenPanelTest.java`, `red/PublicadorPanelTest.java` (con un `Transporte` falso)

**Interfaces:**
- Consumes: Tarea 10 (área), `Repositorio`, `Objetivos`.
- Produces:
  - `record ResumenDia(String fecha, Macros consumido, Objetivo objetivo /* null si no hay */, long actualizadoEn)`; `ResumenPanel.dias(Repositorio, Objetivos, LocalDate hoy, int cantidad) -> List<ResumenDia>` — días con al menos un consumo entre `hoy − cantidad + 1` y `hoy`; `actualizadoEn` = `repo.ultimoCambio(fecha)`; `String detalleJson(ResumenDia)` con el formato de la Tarea 10.
  - `record ConexionPanel(String url, String token, String dispositivo, boolean enviar)`.
  - `PublicadorPanel(Transporte transporte, ConexionPanel conexion)`; `int publicar(List<ResumenDia>)` → un `POST {url}/api/registros` por día con `Authorization: Bearer <token>` y cuerpo JSON de un registro del Panel (`tipo` `resultado`, `fuente` `caloreitor`, `idEnFuente` = fecha, `area` `nutricion`, `fecha`, `metrica` `kcal`, `valor`, `detalleJson`, `actualizadoEn`, `borrado` false); devuelve cuántos respondieron 2xx; ante un error de red corta sin lanzar. Sin `enviar` o sin url/token no hace nada. `boolean probar()` = `GET {url}/api/dia/<hoy>` responde 200.
  - Se llama en un hilo aparte tras cada `recargar()` (escritorio: al abrir, al volver el foco, "Actualizar"; Android: `onResume`) con `ResumenPanel.dias(..., 14)`. Sin cola: lo que falla se reenvía en la próxima relectura. Gana en el servidor el `actualizadoEn` más nuevo, así celular y PC no se pisan con datos viejos.
- [ ] **Step 1: tests:** `dias` saltea días sin consumos; `actualizadoEn` es el ts más nuevo del día (no la hora actual); `detalleJson` omite `objetivoKcal` si no hay objetivo; `publicar` arma URL, encabezado y cuerpo esperados (`Transporte` falso), devuelve 0 y no lanza si el transporte tira `IOException`, y no llama al transporte con `enviar` false.
- [ ] **Step 2:** FAIL. **Step 3:** implementar. **Step 4:** `mvn -q test` PASS; con `Panel/scripts/demo.sh` corriendo, configurar `http://localhost:8099` token `demo` en el escritorio, cargar comidas y ver el día en verde/neutro en el programa del Panel.
- [ ] **Step 5:** commit `feat: publicar las calorías del día al Panel`.

---

### Task 12: App de Android

**Files:**
- Create: `android/app/src/main/java/com/caloreitor/app/android/{CarpetaSaf,Ajustes}.java`, `ui/MainActivity.java` y una vista por pestaña (`HoyView`, `ComidasView`, `RecetasView`, `PlanificadorView`, `ConfiguracionView`, `MedioCirculoView`, `BuscadorRapidoView`), layouts en `res/layout/`, `res/values/strings.xml`.
- Modify: `AndroidManifest.xml` (intent-filter "Abrir con"/"Compartir" para `application/json`, como ZANCADEITOR).

**Interfaces:**
- Consumes: todo el código común.
- Produces:
  - `CarpetaSaf(Context, Uri arbol)` implementa `CarpetaEventos` con `DocumentsContract`/`ContentResolver`: listar `catalogo-*.jsonl` y `dia-*.jsonl`, leer, agregar con `openOutputStream(uri, "wa")`, borrar con `DocumentsContract.deleteDocument`; crear el archivo propio si no existe.
  - `HistorialSqlite` de Android (`SQLiteOpenHelper`, misma tabla que la Tarea 7b) implementa `HistorialDias`; `Depuracion.correr` en `onResume` tras recargar. El árbol se elige con `ACTION_OPEN_DOCUMENT_TREE` y `takePersistableUriPermission`.
  - `Ajustes` (SharedPreferences): URI de la carpeta, nombre de dispositivo (por defecto `"celular"`), `ConexionPanel`.
  - Navegación inferior con 5 pestañas: Hoy, Comidas, Recetas, Semana (planificador), Ajustes (configuración + alimentos propios). Mismo comportamiento que el escritorio (Tareas 8–9). Incluye la conexión al Panel en Ajustes (Tarea 11).
  - Carga rápida en Comidas: el campo de búsqueda abre con teclado; tocar un resultado lo agrega con los gramos sugeridos; los gramos se editan tocando el renglón.
  - Releer eventos en `onResume` y después publicar al Panel (Tarea 11) si está configurado. Manifest con permiso `INTERNET` y `android:usesCleartextTraffic="true"` (dirección local http, como el Panel). Lectura y escritura de archivos fuera del hilo principal (copiar `Hilos` del Panel).
  - Importar plan: botón en Ajustes (`ACTION_OPEN_DOCUMENT`) y por intent "Abrir con".
- [ ] **Step 1:** `cd android && ./gradlew test` en verde (corre todos los tests comunes).
- [ ] **Step 2:** implementar.
- [ ] **Step 3:** `./gradlew assembleDebug` → BUILD SUCCESSFUL. Instalar requiere el celular de Franco (`./gradlew installDebug`): queda en PRUEBAS.md.
- [ ] **Step 4:** commit `feat: app de Android`.

---

### Task 13: README y pruebas a mano

**Files:**
- Create: `README.md` (qué es, fuente de la base de alimentos y licencia, compilar `mvn -q package`, acceso directo, Android, Syncthing: qué carpeta compartir, conexión al Panel, cómo se calcula el objetivo), `PRUEBAS.md` (checklist a mano: primer uso sin datos, carga rápida en ≤ 3 acciones, receta con gramos cambiados sin tocar la receta, importar los dos planes, día combinado, cargar en el celular y verlo en la PC tras Syncthing y viceversa, cargar en los dos sin conexión y juntar, cargar solo en el celular y verlo en el Panel sin abrir la PC, planificador + lista del súper + "comí lo planeado", día pasado no cambia al cambiar el peso, un día de hace más de un mes muestra solo totales y objetivo, recetas viejas siguen estando, Panel verde a ±5 %).
- Modify: `~/Documentos/Carpeta personal/CLAUDE/continuar-ecosistema.md` (sumar CALOREITOR: estado, que no se pushea hasta probar, redesplegar el servidor del Panel por el área nueva).
- [ ] **Step 1:** `mvn -q test` y `cd android && ./gradlew test` en verde, salida pegada en el resumen final.
- [ ] **Step 2:** commit `docs: README y pruebas a mano de CALOREITOR`.
