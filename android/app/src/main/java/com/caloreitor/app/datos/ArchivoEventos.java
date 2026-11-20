package com.caloreitor.app.datos;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lee los eventos de todos los dispositivos y escribe solo en los archivos propios. */
public final class ArchivoEventos {
	private static final Set<String> CATALOGO = Set.of("alimento", "receta", "config", "plan");
	private static final Gson GSON = new Gson();

	private final CarpetaEventos carpeta;
	private final String dispositivo;

	public ArchivoEventos(CarpetaEventos carpeta, String dispositivo) {
		this.carpeta = carpeta;
		this.dispositivo = dispositivo;
	}

	public List<Evento> leerTodos() {
		List<Evento> r = new ArrayList<>();
		for (String archivo : carpeta.archivos()) {
			for (String linea : carpeta.leerLineas(archivo)) {
				Evento e = parsear(linea);
				if (e != null) r.add(e);
			}
		}
		return r;
	}

	/** fechaDelDia (yyyy-MM-dd): obligatoria fuera del catálogo, null en el catálogo. */
	public Evento escribir(String entidad, String entidadId, String op, JsonObject datos, long ts, String fechaDelDia) {
		boolean catalogo = CATALOGO.contains(entidad);
		if (catalogo != (fechaDelDia == null)) {
			throw new IllegalArgumentException("fechaDelDia " + (catalogo ? "debe ser null" : "es obligatoria") + " para " + entidad);
		}
		String archivo = catalogo
				? "catalogo-" + dispositivo + ".jsonl"
				: "dia-" + dispositivo + "-" + LocalDate.parse(fechaDelDia).toString().substring(0, 7) + ".jsonl";
		Evento e = new Evento(UUID.randomUUID().toString(), entidad, entidadId, op, ts, dispositivo, datos);
		JsonObject o = new JsonObject();
		o.addProperty("id", e.id());
		o.addProperty("ent", entidad);
		o.addProperty("eid", entidadId);
		o.addProperty("op", op);
		o.addProperty("ts", ts);
		o.addProperty("disp", dispositivo);
		if (datos != null) o.add("datos", datos);
		carpeta.agregarLinea(archivo, GSON.toJson(o));
		return e;
	}

	// Una línea cortada o ilegible (sincronización a medias) se ignora: ya llegará completa.
	private static Evento parsear(String linea) {
		try {
			JsonObject o = JsonParser.parseString(linea).getAsJsonObject();
			var datos = o.get("datos");
			return new Evento(o.get("id").getAsString(), o.get("ent").getAsString(), o.get("eid").getAsString(),
					o.get("op").getAsString(), o.get("ts").getAsLong(), o.get("disp").getAsString(),
					datos == null || datos.isJsonNull() ? null : datos.getAsJsonObject());
		} catch (RuntimeException ex) {
			return null;
		}
	}
}
