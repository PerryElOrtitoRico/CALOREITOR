package com.caloreitor.escritorio.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CarpetaArchivosTest {
	@TempDir Path dir;

	@Test
	void agregaLeeListaYBorra() throws Exception {
		CarpetaArchivos c = new CarpetaArchivos(dir);
		c.agregarLinea("dia-pc-2026-11.jsonl", "uno ñ");
		c.agregarLinea("dia-pc-2026-11.jsonl", "dos");
		c.agregarLinea("catalogo-pc.jsonl", "x");
		Files.writeString(dir.resolve("otro.txt"), "no");
		assertEquals(List.of("uno ñ", "dos"), c.leerLineas("dia-pc-2026-11.jsonl"));
		assertEquals(List.of("catalogo-pc.jsonl", "dia-pc-2026-11.jsonl"), c.archivos().stream().sorted().toList());
		assertEquals(List.of(), c.leerLineas("dia-no-existe.jsonl"));
		c.borrar("catalogo-pc.jsonl");
		assertEquals(List.of("dia-pc-2026-11.jsonl"), c.archivos());
	}

	@Test
	void carpetaInexistenteNoTieneArchivos() {
		assertEquals(List.of(), new CarpetaArchivos(dir.resolve("nueva")).archivos());
	}

	@Test
	void caracterMultibyteCortadoNoRompeLaLectura() throws Exception {
		byte[] ok = "linea ok\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		byte[] cortada = {'{', (byte) 0xC3}; // mitad de "ñ"
		byte[] todo = java.util.Arrays.copyOf(ok, ok.length + cortada.length);
		System.arraycopy(cortada, 0, todo, ok.length, cortada.length);
		Files.write(dir.resolve("dia-x-2026-11.jsonl"), todo);
		List<String> l = new CarpetaArchivos(dir).leerLineas("dia-x-2026-11.jsonl");
		assertEquals(2, l.size());
		assertEquals("linea ok", l.get(0));
	}
}
