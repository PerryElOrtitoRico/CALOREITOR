package com.caloreitor.app.logica;

import com.caloreitor.app.logica.PlanesEntrenamiento.*;
import com.caloreitor.app.modelo.SemanaEntrenamiento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Reparte las sesiones de los planes en los días de la semana que el usuario marcó (ignora las fechas del plan). */
public final class Asignacion {
	private Asignacion() {
	}

	public record EntrenamientoDia(SesionGym gym, SesionRunning running, SesionPlio plio) {
	}

	public static EntrenamientoDia para(LocalDate fecha, SemanaEntrenamiento semana, List<Plan> planes) {
		SesionRunning r = null;
		SesionGym g = null;
		SesionPlio p = null;
		Plan pr = vigente(planes, "running-plan", fecha);
		if (pr != null) {
			int sem = semanaDe(pr.inicio(), fecha);
			int k = orden(semana.running(), fecha.getDayOfWeek());
			if (k > 0) r = pr.running().stream().filter(s -> s.semana() == sem).skip(k - 1).findFirst().orElse(null);
		}
		Plan pg = vigente(planes, "rutineitor-plan", fecha);
		if (pg != null) {
			int sem = semanaDe(pg.inicio(), fecha);
			int k = orden(semana.gimnasio(), fecha.getDayOfWeek());
			if (k > 0) g = pg.gym().stream().filter(s -> s.microciclo() == sem && s.dia() == k).findFirst().orElse(null);
			int kp = orden(semana.pliometria(), fecha.getDayOfWeek());
			if (kp > 0) {
				p = pg.plio().stream()
						.filter(s -> s.dia() == kp && semanaDe(s.lunes(), fecha) == s.semana()).findFirst().orElse(null);
			}
		}
		return new EntrenamientoDia(g, r, p);
	}

	private static Plan vigente(List<Plan> planes, String formato, LocalDate f) {
		Optional<Plan> p = planes.stream()
				.filter(x -> x.formato().equals(formato) && !f.isBefore(x.inicio()) && !f.isAfter(x.fin()))
				.max(Comparator.comparingLong(Plan::importadoEn));
		return p.orElse(null);
	}

	private static int semanaDe(LocalDate lunes, LocalDate f) {
		// floorDiv: antes del lunes da semana <= 0 (WEEKS.between truncaría a 1)
		return (int) Math.floorDiv(ChronoUnit.DAYS.between(lunes, f), 7) + 1;
	}

	/** Posición 1-based del día entre los marcados (lunes→domingo); 0 si no está marcado. */
	private static int orden(Set<DayOfWeek> marcados, DayOfWeek d) {
		if (!marcados.contains(d)) return 0;
		return (int) marcados.stream().filter(x -> x.getValue() <= d.getValue()).count();
	}
}
