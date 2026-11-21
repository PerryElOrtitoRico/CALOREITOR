package com.caloreitor.escritorio;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.Busqueda;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Macros;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CuentasTest {
	static Alimento al(String id, String nombre, double kcal, boolean base) {
		return new Alimento(id, nombre, new Macros(kcal, 10, 20, 5), null, null, base);
	}

	@Test void porcentajesSumanCien() {
		assertTrue(Cuentas.sumaCien(30, 45, 25));
		assertTrue(Cuentas.sumaCien(33.3, 33.3, 33.4));
		assertFalse(Cuentas.sumaCien(30, 45, 20));
		assertFalse(Cuentas.sumaCien(50, 50, 50));
	}

	@Test void faltaSobraEsObjetivoMenosPlaneado() {
		Macros r = Cuentas.faltaSobra(new Macros(2000, 150, 200, 60), new Macros(2100, 100, 250, 60));
		assertEquals(-100, r.kcal(), 1e-9);
		assertEquals(50, r.proteinas(), 1e-9);
		assertEquals(-50, r.carbohidratos(), 1e-9);
		assertEquals(0, r.grasas(), 1e-9);
	}

	@Test void totalDeRecetaSumaGramosPorValoresDeCadaAlimento() {
		var todos = List.of(al("a", "Arroz", 130, true), al("b", "Pollo", 200, false));
		Macros m = Cuentas.totalReceta(List.of(new ItemReceta("a", 200), new ItemReceta("b", 50), new ItemReceta("borrado", 100)), todos);
		assertEquals(360, m.kcal(), 1e-9); // 260 + 100; el alimento borrado no suma
		assertEquals(25, m.proteinas(), 1e-9);
	}

	@Test void unPropioConElNombreDeUnoBaseLoReemplazaEnLaListaYEnLaBusqueda() {
		var base = al("b1", "Arroz blanco hervido", 130, true);
		var otro = al("b2", "Pollo", 200, true);
		var editado = al("p1", "arroz BLANCO hervido", 150, false);
		var todos = List.of(base, otro, editado);
		assertEquals(List.of(otro, editado), Cuentas.visibles(todos));
		var r = new Busqueda(todos, List.of(), List.of()).buscar("arroz", "Almuerzo", LocalDate.of(2026, 11, 20));
		assertEquals(1, r.size());
		assertEquals("p1", r.get(0).id());
	}

	@Test void soloSeGuardanLosCoeficientesQueCambian() {
		Map<String, Double> m = Cuentas.soloCambios(Map.of("a", 1.0, "b", 2.5, "c", 3.0), Map.of("a", 1.0, "b", 2.0, "c", 3.0));
		assertEquals(Map.of("b", 2.5), m);
	}

	@Test void numeroAceptaComaYRechazaLoInvalido() {
		assertEquals(70.5, Cuentas.numero(" 70,5 ").getAsDouble());
		assertEquals(0, Cuentas.numero("0").getAsDouble());
		assertTrue(Cuentas.numero("").isEmpty());
		assertTrue(Cuentas.numero("abc").isEmpty());
		assertTrue(Cuentas.numero("-1").isEmpty());
		assertTrue(Cuentas.numero("NaN").isEmpty());
	}

	static com.caloreitor.app.modelo.Consumo con(String id, String receta, String alimento, double g) {
		var a = al(alimento, alimento, 100, true);
		return new com.caloreitor.app.modelo.Consumo(id, "2026-11-20", "Almuerzo", receta, receta == null ? null : "R",
				List.of(new com.caloreitor.app.modelo.Item(a.id(), a.nombre(), a.por100(), g)));
	}

	@Test void yaComidoSinConsumosConLosMismosItemsYConDistintos() {
		var plan = List.of(con("p1", null, "arroz", 150), con("p2", "r1", "pollo", 200));
		assertFalse(Cuentas.yaComido(plan, List.of()));
		// mismos ítems con ids de consumo nuevos (así los copia "Comí lo planeado")
		assertTrue(Cuentas.yaComido(plan, List.of(con("x1", null, "arroz", 150), con("x2", "r1", "pollo", 200))));
		assertFalse(Cuentas.yaComido(plan, List.of(con("x1", null, "arroz", 120), con("x2", "r1", "pollo", 200))));
		assertFalse(Cuentas.yaComido(List.of(), List.of(con("x1", null, "arroz", 150))));
	}
}
