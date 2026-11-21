package com.caloreitor.escritorio.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AjustesLocalesTest {
	@TempDir Path dir;

	@Test
	void sinArchivoHayValoresPorDefecto() {
		AjustesLocales a = AjustesLocales.abrir(dir.resolve("x/ajustes.properties"));
		assertNull(a.carpetaEventos());
		assertEquals("pc", a.dispositivo());
	}

	@Test
	void guardaYReleeCarpetaYDispositivo() {
		Path archivo = dir.resolve("x/ajustes.properties");
		AjustesLocales a = AjustesLocales.abrir(archivo);
		a.elegirCarpeta(dir.resolve("syncthing"));
		a.elegirDispositivo("notebook");
		AjustesLocales b = AjustesLocales.abrir(archivo);
		assertEquals(dir.resolve("syncthing"), b.carpetaEventos());
		assertEquals("notebook", b.dispositivo());
	}

	@Test
	void archivoNuevoSoloLeeElUsuario() throws Exception {
		Path archivo = dir.resolve("x/ajustes.properties");
		AjustesLocales.abrir(archivo).elegirDispositivo("pc2");
		assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(archivo)));
	}
}
