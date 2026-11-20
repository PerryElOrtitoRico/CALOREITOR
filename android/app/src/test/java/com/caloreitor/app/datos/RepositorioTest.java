package com.caloreitor.app.datos;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.modelo.*;
import com.google.gson.JsonObject;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RepositorioTest {
	private final ArchivoEventosTest.Memoria carpeta = new ArchivoEventosTest.Memoria();
	private final AtomicLong reloj = new AtomicLong(1000);
	private final Repositorio repo = nuevo("pc");

	private Repositorio nuevo(String disp) {
		return new Repositorio(new ArchivoEventos(carpeta, disp), reloj::incrementAndGet);
	}

	private static Item item(String nombre, double g) {
		return new Item("a1", nombre, new Macros(100, 10, 20, 5), g);
	}

	private static Consumo consumo(String id, String fecha) {
		return new Consumo(id, fecha, "Almuerzo", null, null, List.of(item("arroz", 150)));
	}

	@Test
	void consumosIdaYVueltaOrdenadosPorTsYFiltradosPorFecha() {
		repo.guardar(consumo("c2", "2026-11-20"));
		repo.guardar(consumo("c1", "2026-11-20"));
		repo.guardar(consumo("c3", "2026-11-21"));
		assertEquals(List.of("c2", "c1"), repo.consumos("2026-11-20").stream().map(Consumo::id).toList());
		assertEquals(consumo("c3", "2026-11-21"), repo.consumos("2026-11-21").get(0));
		assertEquals(150, repo.consumos("2026-11-21").get(0).macros().kcal(), 1e-9);
	}

	@Test
	void borrarConsumoLoSacaYPersiste() {
		repo.guardar(consumo("c1", "2026-11-20"));
		repo.borrarConsumo("c1");
		repo.borrarConsumo("inexistente");
		assertTrue(repo.consumos("2026-11-20").isEmpty());
		nuevo("pc").recargar();
		assertTrue(nuevo("pc").consumos("2026-11-20").isEmpty());
	}

	@Test
	void alimentosYRecetasOrdenadosPorNombre() {
		repo.guardar(new Alimento("b", "Zapallo", new Macros(1, 2, 3, 4), "taza", 200.0, false));
		Alimento pan = new Alimento("a", "Pan", new Macros(250, 8, 50, 2), null, null, false);
		repo.guardar(pan);
		assertEquals(List.of(pan.id(), "b"), repo.alimentosPropios().stream().map(Alimento::id).toList());
		assertEquals(pan, repo.alimentosPropios().get(0));
		Receta r = new Receta("r1", "Tarta", List.of(new ItemReceta("a", 100)));
		repo.guardar(r);
		assertEquals(List.of(r), repo.recetas());
		repo.borrarAlimento("a");
		repo.borrarReceta("r1");
		assertEquals(1, repo.alimentosPropios().size());
		assertTrue(repo.recetas().isEmpty());
	}

	@Test
	void configuracionPorDefectoYIdaYVuelta() {
		assertEquals(Configuracion.porDefecto(), repo.configuracion());
		assertEquals(List.of("Desayuno", "Almuerzo", "Merienda", "Cena"), repo.configuracion().comidas());
		Configuracion c = new Configuracion(new Perfil(80.0, 175.0, 30, "M", null), Modo.MANUAL, Meta.BAJAR, 1800, 40, 35, 25,
				List.of("Desayuno", "Cena"), new SemanaEntrenamiento(Set.of(DayOfWeek.MONDAY), Set.of(), Set.of(DayOfWeek.FRIDAY)),
				Map.of("k", 1.5));
		repo.guardar(c);
		assertEquals(c, repo.configuracion());
		assertEquals(c, nuevo("pc").configuracion());
	}

	@Test
	void objetivoPlanesYPlanSemanal() {
		assertTrue(repo.objetivoGuardado("2026-11-20").isEmpty());
		Objetivo o = new Objetivo(new Macros(2000, 150, 200, 60), "porque sí");
		repo.guardarObjetivo("2026-11-20", o);
		assertEquals(o, repo.objetivoGuardado("2026-11-20").orElseThrow());
		repo.guardarPlan("{\"x\":1}");
		List<JsonObject> planes = repo.planesEntrenamiento();
		assertEquals(1, planes.size());
		assertEquals("{\"x\":1}", planes.get(0).get("json").getAsString());
		assertTrue(planes.get(0).has("importadoEn"));
		PlanSemanal p = new PlanSemanal("2026-11-16", Map.of("2026-11-17", o), List.of(consumo("p1", "2026-11-17")));
		repo.guardar(p);
		assertEquals(p, repo.planSemanal("2026-11-16").orElseThrow());
		assertEquals(p, nuevo("pc").planSemanal("2026-11-16").orElseThrow());
	}

	@Test
	void recargarVeEventosDeOtroDispositivo() {
		Repositorio celu = nuevo("celular");
		celu.guardar(consumo("c9", "2026-11-20"));
		assertTrue(repo.consumos("2026-11-20").isEmpty());
		repo.recargar();
		assertEquals("c9", repo.consumos("2026-11-20").get(0).id());
	}

	@Test
	void ultimoCambioIncluyeConsumosObjetivoYBorrados() {
		assertEquals(0, repo.ultimoCambio("2026-11-20"));
		repo.guardar(consumo("c1", "2026-11-20"));
		repo.guardar(consumo("c2", "2026-11-21"));
		long tsObjetivo = reloj.get() + 1;
		repo.guardarObjetivo("2026-11-20", new Objetivo(Macros.CERO, ""));
		assertEquals(tsObjetivo, repo.ultimoCambio("2026-11-20"));
		repo.borrarConsumo("c1");
		long tsBorrado = reloj.get();
		assertEquals(tsBorrado, repo.ultimoCambio("2026-11-20"));
		assertEquals(1002, repo.ultimoCambio("2026-11-21")); // c2 no cambió después
		assertEquals(tsBorrado, nuevo("pc").ultimoCambio("2026-11-20"));
	}
}
