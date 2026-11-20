package com.caloreitor.app.logica;

import static java.time.DayOfWeek.*;
import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.PlanesEntrenamiento.Plan;
import com.caloreitor.app.modelo.SemanaEntrenamiento;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AsignacionTest {
	static Plan running() throws Exception {
		return PlanesEntrenamiento.leer(PlanesEntrenamientoTest.recurso("running-plan-c2.json"), 1);
	}

	static Plan gimnasio() throws Exception {
		return PlanesEntrenamiento.leer(PlanesEntrenamientoTest.recurso("Franco_2026-11-09_Potencia_Pliometria_4s3d.json"), 1);
	}

	static SemanaEntrenamiento sem(Set<DayOfWeek> g, Set<DayOfWeek> r, Set<DayOfWeek> p) {
		return new SemanaEntrenamiento(g, r, p);
	}

	@Test void ignoraLaFechaDeZancadeitorYUsaLosDiasMarcados() throws Exception {
		var s = sem(Set.of(), Set.of(WEDNESDAY, FRIDAY, SUNDAY), Set.of());
		var plan = running();
		var sem1 = plan.running().subList(0, 3);
		assertEquals("2026-11-03", sem1.get(0).fecha());
		assertEquals(sem1.get(0), Asignacion.para(LocalDate.of(2026, 11, 4), s, List.of(plan)).running());
		assertEquals(sem1.get(1), Asignacion.para(LocalDate.of(2026, 11, 6), s, List.of(plan)).running());
		assertEquals(sem1.get(2), Asignacion.para(LocalDate.of(2026, 11, 8), s, List.of(plan)).running());
		assertNull(Asignacion.para(LocalDate.of(2026, 11, 3), s, List.of(plan)).running());
	}

	@Test void gimnasioYPliometriaConDiasPropiosPuedenCoincidir() throws Exception {
		var s = sem(Set.of(MONDAY, WEDNESDAY, FRIDAY), Set.of(), Set.of(TUESDAY, FRIDAY));
		var d = Asignacion.para(LocalDate.of(2026, 11, 13), s, List.of(gimnasio()));
		assertEquals(1, d.gym().microciclo());
		assertEquals(3, d.gym().dia());
		assertEquals(1, d.plio().semana());
		assertEquals(2, d.plio().dia());
	}

	@Test void sesionesDeMasNoSeAsignan() throws Exception {
		var s = sem(Set.of(), Set.of(MONDAY), Set.of());
		var plan = running();
		var lun = Asignacion.para(LocalDate.of(2026, 11, 2), s, List.of(plan)).running();
		assertEquals("2026-11-03", lun.fecha());
		assertNull(Asignacion.para(LocalDate.of(2026, 11, 4), s, List.of(plan)).running());
	}

	@Test void fueraDelRangoNoHayEntrenamiento() throws Exception {
		var s = sem(Set.of(MONDAY, WEDNESDAY, FRIDAY), Set.of(), Set.of());
		assertNull(Asignacion.para(LocalDate.of(2026, 11, 1), s, List.of(gimnasio())).gym());
	}

	@Test void planNuevoNoPisaFechasQueNoCubre() {
		var s = sem(Set.of(), Set.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY), Set.of());
		Plan a = PlanesEntrenamiento.leer(corto("2026-11-02", 5), 1);
		Plan b = PlanesEntrenamiento.leer(corto("2026-12-07", 4), 2);
		var l = List.of(a, b);
		assertEquals("2026-11-09", Asignacion.para(LocalDate.of(2026, 11, 9), s, l).running().fecha());
		assertEquals("2026-12-14", Asignacion.para(LocalDate.of(2026, 12, 14), s, l).running().fecha());
	}

	static String corto(String fecha, int semanas) {
		StringBuilder sb = new StringBuilder("{\"formato\":\"running-plan\",\"sesiones\":[");
		for (int i = 1; i <= semanas; i++) {
			sb.append(i > 1 ? "," : "").append("{\"semana\":").append(i).append(",\"fecha\":\"")
					.append(LocalDate.parse(fecha).plusWeeks(i - 1)).append("\",\"bloques\":[]}");
		}
		return sb.append("]}").toString();
	}
}
