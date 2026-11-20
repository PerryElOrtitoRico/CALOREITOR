package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.datos.*;
import com.caloreitor.app.modelo.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class PlanificadorTest {
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

	private final AtomicLong reloj = new AtomicLong(1000);
	private final Repositorio repo = new Repositorio(new ArchivoEventos(new Memoria(), "pc"), reloj::incrementAndGet);
	private final LocalDate hoy = LocalDate.of(2026, 11, 20);
	private final Planificador pl = new Planificador(repo, new Objetivos(repo, List.of(), hoy));
	private final LocalDate lunes = LocalDate.of(2026, 11, 23);

	private void peso(double kg) {
		var d = Configuracion.porDefecto();
		repo.guardar(new Configuracion(new Perfil(kg, 178.0, 25, "M", 6000), Modo.AUTOMATICO, Meta.MANTENER, d.manualKcal(),
				d.manualPctProt(), d.manualPctCarb(), d.manualPctGrasa(), d.comidas(), d.semana(), Map.of()));
	}

	private static Consumo consumo(String id, String fecha, String alimento, String nombre, double g) {
		var por100 = new Macros(130, 3, 28, 0.3);
		return new Consumo(id, fecha, "almuerzo", null, null, List.of(new Item(alimento, nombre, por100, g)));
	}

	@Test void crearFijaLosSieteObjetivosYRecalcularLosActualiza() {
		peso(75);
		var plan = pl.crear(lunes);
		assertEquals(7, plan.objetivos().size());
		assertEquals(plan, repo.planSemanal("2026-11-23").get());
		assertTrue(repo.objetivoGuardado(hoy.minusDays(1).toString()).isEmpty());
		peso(90);
		assertEquals(plan.objetivos(), repo.planSemanal("2026-11-23").get().objetivos());
		assertNotEquals(plan.objetivos(), pl.recalcular(plan).objetivos());
	}

	@Test void crearExigeLunesYAgregarExigeFechaDeLaSemana() {
		assertThrows(IllegalArgumentException.class, () -> pl.crear(lunes.plusDays(1)));
		var plan = pl.crear(lunes);
		assertThrows(IllegalArgumentException.class, () -> pl.agregar(plan, consumo("x", "2026-11-30", "a", "Arroz", 1)));
	}

	@Test void agregarQuitarYPlaneado() {
		var plan = pl.agregar(pl.crear(lunes), consumo("c1", "2026-11-24", "arroz", "Arroz", 200));
		assertEquals(260, pl.planeado(plan, "2026-11-24").kcal(), 1e-9);
		assertEquals(0, pl.planeado(plan, "2026-11-25").kcal());
		plan = pl.quitar(plan, "c1");
		assertTrue(plan.comidas().isEmpty());
		assertTrue(repo.planSemanal("2026-11-23").get().comidas().isEmpty());
	}

	@Test void listaSuperSumaPorAlimentoYFiltraFechas() {
		var plan = pl.crear(lunes);
		for (int d = 0; d < 7; d++) {
			var f = lunes.plusDays(d).toString();
			plan = pl.agregar(plan, new Consumo("r" + d, f, "almuerzo", "rec", "Receta",
					List.of(new Item("arroz", "Arroz", new Macros(130, 3, 28, 0.3), 160))));
		}
		plan = pl.agregar(plan, consumo("s", "2026-11-25", "arroz", "Arroz suelto", 200));
		plan = pl.agregar(plan, consumo("p", "2026-11-25", "pan", "Árbol", 350));
		var semana = Set.of("2026-11-23", "2026-11-24", "2026-11-25", "2026-11-26", "2026-11-27", "2026-11-28", "2026-11-29");
		assertEquals("Árbol — 350 g\nArroz — 1,3 kg", Planificador.comoTexto(pl.listaSuper(plan, semana)));
		assertEquals("Arroz — 160 g", Planificador.comoTexto(pl.listaSuper(plan, Set.of("2026-11-23"))));
		assertEquals("x — 1,0 kg", Planificador.comoTexto(List.of(new Planificador.LineaSuper("x", 1000))));
	}

	@Test void comiLoPlaneadoCopiaConIdNuevoYNoTocaElPlan() {
		var plan = pl.agregar(pl.crear(lunes), consumo("c1", "2026-11-24", "arroz", "Arroz", 200));
		var real = pl.comiLoPlaneado(plan, "c1");
		assertNotEquals("c1", real.id());
		assertEquals(plan.comidas().get(0).items(), real.items());
		assertEquals(1, repo.consumos("2026-11-24").size());
		assertEquals(plan, repo.planSemanal("2026-11-23").get());
	}
}
