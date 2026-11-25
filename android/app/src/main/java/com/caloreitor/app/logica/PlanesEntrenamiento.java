package com.caloreitor.app.logica;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Lee los planes exportados por ZANCADEITOR (running-plan) y PESEITOR (rutineitor-plan). */
public final class PlanesEntrenamiento {
	private PlanesEntrenamiento() {
	}

	public record BloqueRunning(int zona, double minutos, int repeticiones, Integer zonaPausa, Double minutosPausa) {
	}

	public record SesionRunning(int semana, String fecha, List<BloqueRunning> bloques) {
	}

	public record SerieGym(int reps, double kg, double pct) {
	}

	public record SesionGym(int microciclo, int dia, List<SerieGym> series) {
	}

	/**
	 * {@code semana} es 1-based dentro de su nivel; {@code lunes} es el lunes de la semana 1 de ese nivel (cada nivel de
	 * pliometría tiene su propio inicio).
	 */
	public record SesionPlio(int semana, int dia, int contactos, LocalDate lunes) {
	}

	public static final String RUNNING = "running-plan", GIMNASIO = "rutineitor-plan";

	/** El último plan importado de ese formato: el que se muestra en Configuración. */
	public static java.util.Optional<Plan> ultimo(List<Plan> planes, String formato) {
		return planes.stream().filter(p -> p.formato().equals(formato)).max(Comparator.comparingLong(Plan::importadoEn));
	}

	/** Una línea para mostrar el plan cargado de ese formato. */
	public static String resumen(List<Plan> planes, String formato, LocalDate hoy) {
		return ultimo(planes, formato).map(p -> {
			java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
			int n = p.running().size() + p.gym().size();
			String r = "Del " + p.inicio().format(f) + " al " + p.fin().format(f) + " · " + n + " sesiones";
			if (!p.plio().isEmpty()) r += " + pliometría";
			if (p.fin().isBefore(hoy)) r += " · ya terminó, no cuenta";
			return r;
		}).orElse("Sin plan cargado");
	}

	/** "gimnasio" o "running", para los avisos. */
	public static String nombre(String formato) {
		return RUNNING.equals(formato) ? "running" : "gimnasio";
	}

	public record Plan(String formato, LocalDate inicio, LocalDate fin, long importadoEn, List<SesionRunning> running,
			List<SesionGym> gym, List<SesionPlio> plio) {
	}

	static LocalDate lunes(LocalDate f) {
		return f.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	}

	public static Plan leer(String json, long importadoEn) {
		JsonObject o;
		try {
			o = JsonParser.parseString(json).getAsJsonObject();
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("El archivo no es un JSON válido");
		}
		String formato = o.has("formato") && o.get("formato").isJsonPrimitive() ? o.get("formato").getAsString() : null;
		try {
			if ("running-plan".equals(formato)) return running(o, importadoEn);
			if ("rutineitor-plan".equals(formato)) return rutineitor(o, importadoEn);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (RuntimeException e) {
			// campos que faltan o con tipo equivocado: un plan roto avisa, nunca rompe la app
			throw new IllegalArgumentException("El plan tiene un formato inválido");
		}
		throw new IllegalArgumentException("Formato de plan desconocido: " + formato);
	}

	private static JsonArray lista(JsonObject o, String clave) {
		return o.has(clave) && o.get(clave).isJsonArray() ? o.getAsJsonArray(clave) : new JsonArray();
	}

	private static Plan running(JsonObject o, long importadoEn) {
		// version 2 de ZANCADEITOR: sin fecha por sesión; cada semana trae su lunes
		java.util.Map<Integer, LocalDate> lunesDe = new java.util.HashMap<>();
		for (JsonElement e : lista(o, "semanas")) {
			JsonObject w = e.getAsJsonObject();
			if (w.has("numero") && w.has("lunes"))
				lunesDe.put(w.get("numero").getAsInt(), LocalDate.parse(w.get("lunes").getAsString()));
		}
		List<SesionRunning> ses = new ArrayList<>();
		for (JsonElement e : lista(o, "sesiones")) {
			JsonObject s = e.getAsJsonObject();
			List<BloqueRunning> bl = new ArrayList<>();
			for (JsonElement b : s.getAsJsonArray("bloques")) {
				JsonObject x = b.getAsJsonObject();
				bl.add(new BloqueRunning(x.get("zona").getAsInt(), x.get("minutos").getAsDouble(),
						x.has("repeticiones") ? x.get("repeticiones").getAsInt() : 1,
						x.has("zonaPausa") ? x.get("zonaPausa").getAsInt() : null,
						x.has("minutosPausa") ? x.get("minutosPausa").getAsDouble() : null));
			}
			int semana = s.get("semana").getAsInt();
			String fecha;
			if (s.has("fecha")) fecha = s.get("fecha").getAsString();
			// el día numerado solo ordena: CALOREITOR asigna los días que marca Franco
			else if (lunesDe.containsKey(semana))
				fecha = lunesDe.get(semana).plusDays(s.has("dia") ? s.get("dia").getAsInt() - 1 : 0).toString();
			else throw new IllegalArgumentException("La sesión no tiene fecha ni semana con lunes");
			ses.add(new SesionRunning(semana, fecha, bl));
		}
		if (ses.isEmpty()) throw new IllegalArgumentException("El plan no tiene sesiones");
		ses.sort(Comparator.comparing(SesionRunning::fecha));
		LocalDate inicio = lunes(LocalDate.parse(ses.get(0).fecha()));
		int max = ses.stream().mapToInt(SesionRunning::semana).max().getAsInt();
		return new Plan("running-plan", inicio, inicio.plusWeeks(max).minusDays(1), importadoEn, ses, List.of(), List.of());
	}

	private static Plan rutineitor(JsonObject o, long importadoEn) {
		List<SesionGym> gym = new ArrayList<>();
		for (JsonElement e : lista(o, "sesiones")) {
			JsonObject s = e.getAsJsonObject();
			List<SerieGym> series = new ArrayList<>();
			for (JsonElement ej : s.getAsJsonArray("ejercicios")) {
				for (JsonElement f : ej.getAsJsonObject().getAsJsonArray("filas")) {
					JsonObject fila = f.getAsJsonObject();
					double pct = fila.has("pctMin") && fila.has("pctMax")
							? (fila.get("pctMin").getAsDouble() + fila.get("pctMax").getAsDouble()) / 2 : 0;
					for (JsonElement se : fila.getAsJsonArray("series")) {
						JsonObject x = se.getAsJsonObject();
						series.add(new SerieGym(x.get("reps").getAsInt(), x.get("kg").getAsDouble(), pct));
					}
				}
			}
			gym.add(new SesionGym(s.get("microciclo").getAsInt(), s.get("dia").getAsInt(), series));
		}
		if (gym.isEmpty()) throw new IllegalArgumentException("El plan no tiene sesiones");
		LocalDate inicio = lunes(LocalDate.parse(o.get("inicio").getAsString()));
		LocalDate fin = inicio.plusWeeks(o.get("microciclos").getAsInt()).minusDays(1);
		List<SesionPlio> plio = new ArrayList<>();
		for (JsonElement n : lista(o, "pliometria")) {
			JsonObject nivel = n.getAsJsonObject();
			LocalDate l = lunes(LocalDate.parse(nivel.get("inicio").getAsString()));
			JsonArray semanas = nivel.getAsJsonArray("semanas");
			for (int w = 0; w < semanas.size(); w++) {
				for (JsonElement d : semanas.get(w).getAsJsonArray()) {
					plio.add(new SesionPlio(w + 1, d.getAsJsonObject().get("dia").getAsInt(),
							d.getAsJsonObject().get("contactos").getAsInt(), l));
				}
			}
			if (l.isBefore(inicio)) inicio = l;
			LocalDate f = l.plusWeeks(semanas.size()).minusDays(1);
			if (f.isAfter(fin)) fin = f;
		}
		return new Plan("rutineitor-plan", inicio, fin, importadoEn, List.of(), gym, plio);
	}
}
