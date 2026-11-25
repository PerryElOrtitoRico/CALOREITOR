package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.PlanesEntrenamiento.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanesEntrenamientoTest {
	static String recurso(String n) throws Exception {
		try (InputStream in = PlanesEntrenamientoTest.class.getResourceAsStream("/" + n)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test void leeElPlanRealDeRunning() throws Exception {
		Plan p = PlanesEntrenamiento.leer(recurso("running-plan-c2.json"), 5L);
		assertEquals("running-plan", p.formato());
		assertEquals(37, p.running().size());
		assertEquals(LocalDate.of(2026, 11, 2), p.inicio());
		assertEquals(LocalDate.of(2026, 11, 2).plusWeeks(13).minusDays(1), p.fin());
		assertEquals(5L, p.importadoEn());
		SesionRunning s = p.running().get(0);
		assertEquals(1, s.semana());
		assertEquals(3, s.bloques().size());
		assertEquals(1, s.bloques().get(0).zona());
		assertEquals(5.0, s.bloques().get(0).minutos(), 0);
		assertEquals(20.0, s.bloques().get(1).minutos(), 0);
		assertEquals(3, s.bloques().get(2).zona());
	}

	@Test void leeElPlanDeRunningVersion2SinFechaPorSesion() throws Exception {
		// version 2: las sesiones traen semana + día numerado y cada semana trae su lunes
		Plan p = PlanesEntrenamiento.leer(recurso("running-plan-v2.json"), 7L);
		assertEquals(12, p.running().size());
		assertEquals(LocalDate.of(2026, 11, 23), p.inicio());
		assertEquals(LocalDate.of(2026, 12, 20), p.fin());
		SesionRunning s = p.running().get(0);
		assertEquals(1, s.semana());
		assertEquals(4, s.bloques().size());
		assertEquals(2, s.bloques().get(1).zona());
		assertEquals(4, p.running().get(11).semana());
	}

	@Test void leeElPlanRealDeGimnasioConPliometria() throws Exception {
		Plan p = PlanesEntrenamiento.leer(recurso("Franco_2026-11-09_Potencia_Pliometria_4s3d.json"), 1L);
		assertEquals("rutineitor-plan", p.formato());
		assertEquals(LocalDate.of(2026, 11, 9), p.inicio());
		SesionGym g = p.gym().stream().filter(x -> x.microciclo() == 1 && x.dia() == 1).findFirst().get();
		assertEquals(6, g.series().get(0).reps());
		assertEquals(44.0, g.series().get(0).kg(), 0);
		assertEquals(55.0, g.series().get(0).pct(), 0);
		SesionPlio pl = p.plio().stream().filter(x -> x.semana() == 1 && x.dia() == 1).findFirst().get();
		assertEquals(250, pl.contactos());
		assertFalse(p.fin().isBefore(LocalDate.of(2026, 12, 6)));
	}

	@Test void formatoDesconocidoOJsonRotoLanzaConMensaje() {
		for (String[] c : new String[][] { { "{no", "El archivo no es un JSON válido" },
				{ "{\"formato\":\"x\"}", "Formato de plan desconocido: x" },
				{ "{\"formato\":\"running-plan\"}", "El plan no tiene sesiones" } }) {
			try {
				PlanesEntrenamiento.leer(c[0], 0);
				fail();
			} catch (IllegalArgumentException e) {
				assertEquals(c[1], e.getMessage());
			}
		}
	}

	@Test void planRotoLanzaFormatoInvalidoEnVezDeRomper() {
		for (String j : new String[] {
				"{\"formato\":\"running-plan\",\"sesiones\":[{\"semana\":1,\"fecha\":\"2026-11-03\"}]}",
				"{\"formato\":\"rutineitor-plan\",\"inicio\":\"mañana\",\"microciclos\":4,\"sesiones\":[{\"microciclo\":1,\"dia\":1,\"ejercicios\":[]}]}" }) {
			var e = assertThrows(IllegalArgumentException.class, () -> PlanesEntrenamiento.leer(j, 0));
			assertEquals("El plan tiene un formato inválido", e.getMessage());
		}
	}
}
