package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.datos.*;
import com.caloreitor.app.modelo.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class DepuracionTest {
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
	private final ArchivoEventos archivo = new ArchivoEventos(carpeta, "pc");
	private final Repositorio repo = new Repositorio(archivo, reloj::incrementAndGet);
	private final HistorialMemoria historial = new HistorialMemoria();
	private int n;

	private void consumo(Repositorio r, String fecha, double kcal) {
		var item = new Item("a", "Arroz", new Macros(kcal, 0, 0, 0), 100);
		r.guardar(new Consumo("c" + n++, fecha, "Almuerzo", null, null, List.of(item)));
	}

	private void correr(LocalDate hoy) {
		repo.recargar();
		new Depuracion(repo, new Objetivos(repo, List.of(), hoy), historial, archivo).correr(hoy);
	}

	@Test void archivaDiasDeMasDeUnMesConSuObjetivo() {
		var d = Configuracion.porDefecto();
		repo.guardar(new Configuracion(new Perfil(75.0, 178.0, 25, "M", 6000), Modo.AUTOMATICO, Meta.MANTENER, d.manualKcal(),
				d.manualPctProt(), d.manualPctCarb(), d.manualPctGrasa(), d.comidas(), d.semana(), Map.of()));
		consumo(repo, "2026-11-10", 1800);
		consumo(repo, "2026-12-01", 500);
		correr(LocalDate.of(2026, 12, 20));
		var t = historial.leer("2026-11-10").orElseThrow();
		assertEquals(1800, t.consumido().kcal());
		assertNotNull(t.objetivo());
		assertFalse(historial.existe("2026-12-01"));
	}

	@Test void diaSinObjetivoSeArchivaConObjetivoNulo() {
		consumo(repo, "2026-11-10", 1800);
		correr(LocalDate.of(2026, 12, 20));
		assertNull(historial.leer("2026-11-10").orElseThrow().objetivo());
	}

	@Test void noVuelveAArchivarNiPisaUnDiaYaArchivado() {
		var previo = new TotalDia("2026-11-10", new Macros(1, 0, 0, 0), null);
		historial.guardar(previo);
		consumo(repo, "2026-11-10", 1800);
		correr(LocalDate.of(2027, 2, 20)); // más de 62 días: no se toca
		assertEquals(previo, historial.leer("2026-11-10").orElseThrow());
	}

	@Test void consumoTardioRefrescaElDiaArchivadoDentroDeLaVentana() {
		consumo(repo, "2026-11-10", 1800);
		correr(LocalDate.of(2026, 12, 20));
		consumo(repo, "2026-11-10", 200); // llegó tarde por sincronización
		correr(LocalDate.of(2026, 12, 20));
		assertEquals(2000, historial.leer("2026-11-10").orElseThrow().consumido().kcal());
	}

	@Test void fechaIlegibleSeSaltea() {
		// escrito a mano: el repositorio no deja guardar una fecha inválida
		carpeta.agregarLinea("dia-pc-2026-11.jsonl", "{\"id\":\"e1\",\"ent\":\"consumo\",\"eid\":\"x\",\"op\":\"put\",\"ts\":5,"
				+ "\"disp\":\"pc\",\"datos\":{\"id\":\"x\",\"fecha\":\"no-es-fecha\",\"comida\":\"Cena\",\"items\":[]}}");
		consumo(repo, "2026-11-10", 1800);
		correr(LocalDate.of(2026, 12, 20));
		assertTrue(historial.existe("2026-11-10"));
	}

	@Test void borraSoloElArchivoMensualPropioYaArchivado() {
		consumo(repo, "2026-10-15", 100);
		consumo(repo, "2026-11-15", 100);
		var celular = new Repositorio(new ArchivoEventos(carpeta, "celular"), reloj::incrementAndGet);
		consumo(celular, "2026-10-16", 100);
		repo.guardar(new Alimento("x", "Pan", new Macros(1, 1, 1, 1), null, null, false));
		correr(LocalDate.of(2027, 1, 5));
		assertFalse(carpeta.f.containsKey("dia-pc-2026-10.jsonl"));
		assertTrue(carpeta.f.containsKey("dia-pc-2026-11.jsonl"));
		assertTrue(carpeta.f.containsKey("dia-celular-2026-10.jsonl"));
		assertTrue(carpeta.f.containsKey("catalogo-pc.jsonl"));
	}

	@Test void noBorraSiFaltaArchivarAlgunDia() {
		consumo(repo, "2026-10-15", 100);
		HistorialDias sordo = new HistorialMemoria() {
			@Override public void guardar(TotalDia t) {
			}
		};
		repo.recargar();
		var hoy = LocalDate.of(2027, 1, 5);
		new Depuracion(repo, new Objetivos(repo, List.of(), hoy), sordo, archivo).correr(hoy);
		assertTrue(carpeta.f.containsKey("dia-pc-2026-10.jsonl"));
	}
}
