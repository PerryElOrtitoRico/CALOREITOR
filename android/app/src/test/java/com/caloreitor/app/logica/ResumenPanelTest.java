package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.ArchivoEventosTest;
import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.modelo.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ResumenPanelTest {
	private final AtomicLong reloj = new AtomicLong(1000);
	private final Repositorio repo = new Repositorio(new ArchivoEventos(new ArchivoEventosTest.Memoria(), "pc"), reloj::incrementAndGet);
	private final LocalDate hoy = LocalDate.of(2026, 11, 20);

	private Consumo consumo(String id, String fecha, double kcalPor100) {
		return new Consumo(id, fecha, "Almuerzo", null, null, List.of(new Item("a", "arroz", new Macros(kcalPor100, 10, 20, 5), 100)));
	}

	@Test
	void saltaDiasSinConsumosYLosFueraDeVentana() {
		repo.guardar(consumo("c1", "2026-11-20", 500));
		repo.guardar(consumo("c2", "2026-11-18", 300));
		repo.guardar(consumo("viejo", "2026-11-06", 300)); // 14 días atrás: fuera de una ventana de 14 (06 < 07)
		List<ResumenDia> r = ResumenPanel.dias(repo, hoy, 14);
		assertEquals(List.of("2026-11-18", "2026-11-20"), r.stream().map(ResumenDia::fecha).toList());
		assertEquals(500, r.get(1).consumido().kcal(), 1e-9);
		assertNull(r.get(1).objetivo());
	}

	@Test
	void actualizadoEnEsElUltimoCambioDelDiaNoLaHoraActual() {
		repo.guardar(consumo("c1", "2026-11-20", 500)); // ts 1001
		repo.guardar(consumo("c2", "2026-11-19", 500)); // ts 1002
		repo.guardar(consumo("c1", "2026-11-20", 600)); // ts 1003
		assertEquals(1003, ResumenPanel.dias(repo, hoy, 14).get(1).actualizadoEn());
	}

	@Test
	void usaElObjetivoGuardadoYElJsonLoIncluyeSoloSiHay() {
		repo.guardar(consumo("c1", "2026-11-20", 500));
		repo.guardar(consumo("c2", "2026-11-19", 400));
		repo.guardarObjetivo("2026-11-20", new Objetivo(new Macros(2000, 150, 200, 60), "x"));
		List<ResumenDia> r = ResumenPanel.dias(repo, hoy, 14);
		JsonObject sin = JsonParser.parseString(ResumenPanel.detalleJson(r.get(0))).getAsJsonObject();
		assertFalse(sin.has("objetivoKcal"));
		assertEquals(400, sin.getAsJsonObject("consumido").get("kcal").getAsDouble(), 1e-9);
		JsonObject con = JsonParser.parseString(ResumenPanel.detalleJson(r.get(1))).getAsJsonObject();
		assertEquals(2000, con.get("objetivoKcal").getAsDouble(), 1e-9);
		assertEquals(500, con.getAsJsonObject("consumido").get("kcal").getAsDouble(), 1e-9);
		assertEquals(20, con.getAsJsonObject("consumido").get("carbohidratos").getAsDouble(), 1e-9);
	}

	@Test
	void diaBorradoSaleVacioConElTsDelBorradoYDiaSinEventosNoSale() {
		repo.guardar(consumo("c1", "2026-11-19", 500));
		repo.borrarConsumo("c1"); // ts 1002
		List<ResumenDia> r = ResumenPanel.dias(repo, hoy, 14);
		assertEquals(1, r.size());
		assertEquals("2026-11-19", r.get(0).fecha());
		assertTrue(r.get(0).vacio());
		assertEquals(1002, r.get(0).actualizadoEn());
	}
}
