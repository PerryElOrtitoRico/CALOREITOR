package com.caloreitor.escritorio.infra;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.TotalDia;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistorialSqliteTest {
	@TempDir Path dir;

	@Test
	void historialSqliteIdaYVuelta() {
		var h = new HistorialSqlite(dir.resolve("sub/historial.db"));
		var a = new TotalDia("2026-10-01", new Macros(1800, 100, 200, 60), new Macros(2000, 120, 220, 70));
		var b = new TotalDia("2026-10-02", new Macros(1500, 90, 150, 50), null);
		h.guardar(b);
		h.guardar(a);
		assertEquals(a, h.leer("2026-10-01").orElseThrow());
		assertNull(h.leer("2026-10-02").orElseThrow().objetivo());
		assertTrue(h.leer("2026-10-03").isEmpty());
		assertEquals(List.of(a, b), h.entre("2026-10-01", "2026-10-02"));
		assertEquals(List.of(b), h.entre("2026-10-02", "2026-10-31"));
		var a2 = new TotalDia("2026-10-01", new Macros(1, 2, 3, 4), null);
		h.guardar(a2);
		assertEquals(a2, h.leer("2026-10-01").orElseThrow());
		assertTrue(new HistorialSqlite(dir.resolve("sub/historial.db")).existe("2026-10-01"));
	}
}
