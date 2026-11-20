package com.caloreitor.app.logica;

import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.logica.PlanesEntrenamiento.Plan;
import com.caloreitor.app.modelo.Objetivo;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Objetivo de una fecha: los días pasados se fijan la primera vez, hoy y el futuro se calculan al vuelo. */
public final class Objetivos {
	private final Repositorio repo;
	private final List<Plan> planes;
	private final LocalDate hoy;

	public Objetivos(Repositorio repo, List<Plan> planes, LocalDate hoy) {
		this.repo = repo;
		this.planes = planes;
		this.hoy = hoy;
	}

	/** Planes guardados; uno roto se saltea para que nunca rompa el objetivo. */
	public static List<Plan> planesDe(Repositorio repo) {
		List<Plan> r = new ArrayList<>();
		for (JsonObject o : repo.planesEntrenamiento()) {
			try {
				r.add(PlanesEntrenamiento.leer(o.get("json").getAsString(), o.get("importadoEn").getAsLong()));
			} catch (RuntimeException e) {
				// plan ilegible: se ignora
			}
		}
		return r;
	}

	public Optional<Objetivo> del(LocalDate fecha) {
		boolean pasado = fecha.isBefore(hoy);
		if (pasado) {
			Optional<Objetivo> g = repo.objetivoGuardado(fecha.toString());
			if (g.isPresent()) return g;
		}
		var cfg = repo.configuracion();
		Optional<Objetivo> o = Calculo.objetivo(cfg, Asignacion.para(fecha, cfg.semana(), planes));
		if (pasado) o.ifPresent(x -> repo.guardarObjetivo(fecha.toString(), x));
		return o;
	}
}
