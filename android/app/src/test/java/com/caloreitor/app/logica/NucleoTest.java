package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.ArchivoEventosTest.Memoria;
import com.caloreitor.app.datos.HistorialMemoria;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NucleoTest {
	static Alimento al(String id, boolean base) {
		return new Alimento(id, id, new Macros(100, 10, 10, 10), null, null, base);
	}

	static Consumo consumo(String id, LocalDate f) {
		return new Consumo(id, f.toString(), "Almuerzo", null, null, List.of(new Item("a", "a", new Macros(100, 10, 10, 10), 100)));
	}

	@Test void alimentosSonLaBaseMasLosPropios() {
		Nucleo n = new Nucleo(new ArchivoEventos(new Memoria(), "pc"), new HistorialMemoria(), List.of(al("b", true)));
		n.repo().guardar(al("p", false));
		assertEquals(List.of("b", "p"), n.todosLosAlimentos().stream().map(Alimento::id).toList());
	}

	@Test void recientesSonLosUltimos30DiasHoyIncluido() {
		Nucleo n = new Nucleo(new ArchivoEventos(new Memoria(), "pc"), new HistorialMemoria(), List.of());
		LocalDate hoy = n.hoy();
		n.repo().guardar(consumo("hoy", hoy));
		n.repo().guardar(consumo("dia29", hoy.minusDays(29)));
		n.repo().guardar(consumo("dia30", hoy.minusDays(30)));
		assertEquals(List.of("dia29", "hoy"), n.consumosRecientes().stream().map(Consumo::id).toList());
	}

	@Test void recargarTraeLoQueEscribioOtroDispositivo() {
		Memoria carpeta = new Memoria();
		Nucleo n = new Nucleo(new ArchivoEventos(carpeta, "pc"), new HistorialMemoria(), List.of());
		new Nucleo(new ArchivoEventos(carpeta, "cel"), new HistorialMemoria(), List.of()).repo().guardar(consumo("c1", n.hoy()));
		assertTrue(n.repo().consumos(n.hoy().toString()).isEmpty());
		n.recargar();
		assertEquals(1, n.repo().consumos(n.hoy().toString()).size());
	}
}
