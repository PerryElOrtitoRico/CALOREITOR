package com.caloreitor.app.datos;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.PlanSemanal;
import com.caloreitor.app.modelo.Receta;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Entidades de la app sobre el archivo de eventos; el estado vive en memoria y se actualiza al guardar. */
public final class Repositorio {
	private static final Gson GSON = new Gson();

	private record Vivo(JsonObject datos, long ts) {
	}

	private final ArchivoEventos archivo;
	private final LongSupplier reloj;
	private final Map<String, Map<String, Vivo>> vivos = new HashMap<>();
	// Fecha de cada consumo (aunque esté borrado) y último cambio por fecha: un borrado también cambia el día.
	private final Map<String, String> fechaDeConsumo = new HashMap<>();
	private final Map<String, Long> cambios = new HashMap<>();
	// ts del primer put de cada consumo: el orden del día no cambia al editar.
	private final Map<String, Long> primerTs = new HashMap<>();

	public Repositorio(ArchivoEventos archivo, LongSupplier reloj) {
		this.archivo = archivo;
		this.reloj = reloj;
		recargar();
	}

	public void recargar() {
		List<Evento> eventos = archivo.leerTodos();
		Estado estado = Estado.de(eventos);
		vivos.clear();
		fechaDeConsumo.clear();
		cambios.clear();
		primerTs.clear();
		for (String ent : List.of("consumo", "alimento", "receta", "config", "objetivo", "plansemanal", "plan")) {
			Map<String, Vivo> m = new HashMap<>();
			estado.vivos(ent).forEach((id, d) -> m.put(id, new Vivo(d, estado.tsMaximo(ent, id))));
			vivos.put(ent, m);
		}
		// Primero los put (fijan la fecha de cada consumo), después todos suman al último cambio.
		for (Evento e : eventos) {
			if ("consumo".equals(e.entidad()) && "put".equals(e.op())) {
				fechaDeConsumo.put(e.entidadId(), e.datos().get("fecha").getAsString());
				primerTs.merge(e.entidadId(), e.ts(), Math::min);
			}
		}
		for (Evento e : eventos) {
			String fecha = "consumo".equals(e.entidad()) ? fechaDeConsumo.get(e.entidadId())
					: "objetivo".equals(e.entidad()) ? e.entidadId() : null;
			if (fecha != null) cambios.merge(fecha, e.ts(), Math::max);
		}
	}

	// --- consumos
	public List<Consumo> consumos(String fecha) {
		List<Consumo> r = ordenados("consumo", Consumo.class, c -> fecha.equals(c.fecha()), Comparator.comparingLong(Vivo::ts));
		r.sort(Comparator.comparingLong(c -> primerTs.get(c.id())));
		return r;
	}

	/** Fechas con al menos un consumo vivo. */
	public java.util.Set<String> fechasConConsumos() {
		java.util.Set<String> r = new java.util.TreeSet<>();
		for (String id : vivos.get("consumo").keySet()) r.add(fechaDeConsumo.get(id));
		return r;
	}

	public void guardar(Consumo c) {
		fechaDeConsumo.put(c.id(), c.fecha());
		poner("consumo", c.id(), GSON.toJsonTree(c).getAsJsonObject(), c.fecha(), c.fecha());
	}

	public void borrarConsumo(String id) {
		Vivo v = vivos.get("consumo").get(id);
		if (v == null) return;
		String fecha = GSON.fromJson(v.datos(), Consumo.class).fecha();
		sacar("consumo", id, fecha, fecha);
	}

	// --- alimentos propios
	public List<Alimento> alimentosPropios() {
		return ordenados("alimento", Alimento.class, a -> true, Comparator.comparing(v -> nombre(v)));
	}

	public void guardar(Alimento a) {
		poner("alimento", a.id(), GSON.toJsonTree(a).getAsJsonObject(), null, null);
	}

	public void borrarAlimento(String id) {
		sacar("alimento", id, null, null);
	}

	// --- recetas
	public List<Receta> recetas() {
		return ordenados("receta", Receta.class, r -> true, Comparator.comparing(v -> nombre(v)));
	}

	public void guardar(Receta r) {
		poner("receta", r.id(), GSON.toJsonTree(r).getAsJsonObject(), null, null);
	}

	public void borrarReceta(String id) {
		sacar("receta", id, null, null);
	}

	// --- configuración
	public Configuracion configuracion() {
		Vivo v = vivos.get("config").get("config");
		return v == null ? Configuracion.porDefecto() : GSON.fromJson(v.datos(), Configuracion.class);
	}

	public void guardar(Configuracion c) {
		poner("config", "config", GSON.toJsonTree(c).getAsJsonObject(), null, null);
	}

	// --- objetivo fijado de un día
	public Optional<Objetivo> objetivoGuardado(String fecha) {
		Vivo v = vivos.get("objetivo").get(fecha);
		return Optional.ofNullable(v).map(x -> GSON.fromJson(x.datos(), Objetivo.class));
	}

	public void guardarObjetivo(String fecha, Objetivo o) {
		poner("objetivo", fecha, GSON.toJsonTree(o).getAsJsonObject(), fecha, fecha);
	}

	// --- planes de entrenamiento importados (texto original)
	public List<JsonObject> planesEntrenamiento() {
		return vivos.get("plan").values().stream().sorted(Comparator.comparingLong(Vivo::ts)).map(Vivo::datos).toList();
	}

	public void guardarPlan(String jsonOriginal) {
		JsonObject d = new JsonObject();
		d.addProperty("importadoEn", reloj.getAsLong());
		d.addProperty("json", jsonOriginal);
		poner("plan", UUID.randomUUID().toString(), d, null, null);
	}

	// --- plan semanal (id = lunes)
	public Optional<PlanSemanal> planSemanal(String lunes) {
		Vivo v = vivos.get("plansemanal").get(lunes);
		return Optional.ofNullable(v).map(x -> GSON.fromJson(x.datos(), PlanSemanal.class));
	}

	public void guardar(PlanSemanal p) {
		poner("plansemanal", p.lunes(), GSON.toJsonTree(p).getAsJsonObject(), p.lunes(), null);
	}

	/** Máximo ts de los eventos (put o del) de los consumos de esa fecha y de su objetivo; 0 si no hay. */
	public long ultimoCambio(String fecha) {
		return cambios.getOrDefault(fecha, 0L);
	}

	// --- internos
	private void poner(String ent, String id, JsonObject datos, String fechaDelDia, String fechaCambio) {
		long ts = reloj.getAsLong();
		archivo.escribir(ent, id, "put", datos, ts, fechaDelDia);
		vivos.get(ent).put(id, new Vivo(datos, ts));
		if (ent.equals("consumo")) primerTs.putIfAbsent(id, ts);
		if (fechaCambio != null) cambios.merge(fechaCambio, ts, Math::max);
	}

	private void sacar(String ent, String id, String fechaDelDia, String fechaCambio) {
		long ts = reloj.getAsLong();
		archivo.escribir(ent, id, "del", null, ts, fechaDelDia);
		vivos.get(ent).remove(id);
		if (fechaCambio != null) cambios.merge(fechaCambio, ts, Math::max);
	}

	private <T> List<T> ordenados(String ent, Class<T> tipo, java.util.function.Predicate<T> filtro, Comparator<Vivo> orden) {
		List<Vivo> l = new ArrayList<>(vivos.get(ent).values());
		l.sort(orden);
		List<T> r = new ArrayList<>();
		for (Vivo v : l) {
			T t = GSON.fromJson(v.datos(), tipo);
			if (filtro.test(t)) r.add(t);
		}
		return r;
	}

	private static String nombre(Vivo v) {
		return v.datos().get("nombre").getAsString();
	}
}
