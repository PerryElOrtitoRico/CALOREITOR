package com.caloreitor.escritorio;

import static java.time.DayOfWeek.FRIDAY;
import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.Asignacion;
import com.caloreitor.app.logica.Calculo;
import com.caloreitor.app.logica.PlanesEntrenamiento;
import com.caloreitor.app.modelo.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Los dos planes reales de Franco (copias idénticas en los recursos de test): el día combinado suma los tres
 * entrenamientos y la explicación los nombra.
 */
class PlanesRealesTest {
	private static String leer(String nombre) throws Exception {
		try (var in = PlanesRealesTest.class.getResourceAsStream("/" + nombre)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test void diaCombinadoExplicaGimnasioRunningYPliometria() throws Exception {
		var planes = List.of(
				PlanesEntrenamiento.leer(leer("running-plan-c2.json"), 1),
				PlanesEntrenamiento.leer(leer("Franco_2026-11-09_Potencia_Pliometria_4s3d.json"), 2));
		var semana = new SemanaEntrenamiento(Set.of(FRIDAY), Set.of(FRIDAY), Set.of(FRIDAY));
		var cfg = new Configuracion(new Perfil(70.0, 175.0, 30, "M", 6000), Modo.AUTOMATICO, Meta.MANTENER, 2000, 30, 45, 25,
				List.of("Desayuno"), semana, java.util.Map.of());
		var dia = LocalDate.of(2026, 11, 13);
		var e = Calculo.objetivo(cfg, Asignacion.para(dia, semana, planes)).orElseThrow().explicacion();
		assertTrue(e.contains("Gimnasio"), e);
		assertTrue(e.contains("Running"), e);
		assertTrue(e.contains("Pliometría"), e);
		// un día sin marcar solo tiene la base
		var libre = Calculo.objetivo(cfg, Asignacion.para(dia.plusDays(1), semana, planes)).orElseThrow().explicacion();
		assertFalse(libre.contains("Gimnasio") || libre.contains("Running"), libre);
	}
}
