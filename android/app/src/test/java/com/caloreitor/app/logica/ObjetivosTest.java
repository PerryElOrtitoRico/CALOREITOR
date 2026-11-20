package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.datos.*;
import com.caloreitor.app.modelo.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ObjetivosTest {
	private static class Memoria implements CarpetaEventos {
		final Map<String, List<String>> f = new TreeMap<>();

		public List<String> archivos() {
			return new ArrayList<>(f.keySet());
		}

		public List<String> leerLineas(String a) {
			return f.getOrDefault(a, List.of());
		}

		public void agregarLinea(String a, String l) {
			f.computeIfAbsent(a, k -> new ArrayList<>()).add(l);
		}

		public void borrar(String a) {
			f.remove(a);
		}
	}

	private final Memoria carpeta = new Memoria();
	private final AtomicLong reloj = new AtomicLong(1000);
	private final Repositorio repo = new Repositorio(new ArchivoEventos(carpeta, "pc"), reloj::incrementAndGet);
	private final LocalDate hoy = LocalDate.of(2026, 11, 20);

	private void peso(double kg) {
		var d = Configuracion.porDefecto();
		repo.guardar(new Configuracion(new Perfil(kg, 178.0, 25, "M", 6000), Modo.AUTOMATICO, Meta.MANTENER, d.manualKcal(),
				d.manualPctProt(), d.manualPctCarb(), d.manualPctGrasa(), d.comidas(), d.semana(), Map.of()));
	}

	@Test void diaPasadoQuedaGuardadoAunqueCambieLaConfiguracion() {
		peso(75);
		var obj = new Objetivos(repo, List.of(), hoy);
		var ayer = hoy.minusDays(1);
		var a = obj.del(ayer).get();
		assertTrue(repo.objetivoGuardado(ayer.toString()).isPresent());
		peso(90);
		assertEquals(a, obj.del(ayer).get());
		var h1 = obj.del(hoy).get();
		assertNotEquals(a, h1);
		assertTrue(repo.objetivoGuardado(hoy.toString()).isEmpty());
	}

	@Test void planRotoNoRompeElObjetivo() {
		repo.guardarPlan("no es json");
		assertTrue(Objetivos.planesDe(repo).isEmpty());
	}
}
