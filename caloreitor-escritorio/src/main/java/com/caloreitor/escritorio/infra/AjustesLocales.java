package com.caloreitor.escritorio.infra;

import com.caloreitor.app.red.ConexionPanel;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Properties;

/** Ajustes de este equipo (carpeta de Syncthing y nombre del dispositivo) en un .properties solo legible por el usuario. */
public final class AjustesLocales {
	private final Path archivo;
	private final Properties datos = new Properties();

	private AjustesLocales(Path archivo) {
		this.archivo = archivo;
	}

	/** ~/.config/caloreitor/ajustes.properties */
	public static Path rutaPorDefecto() {
		return Path.of(System.getProperty("user.home"), ".config", "caloreitor", "ajustes.properties");
	}

	/** Un archivo que falta o está dañado se lee como vacío. */
	public static AjustesLocales abrir(Path archivo) {
		AjustesLocales a = new AjustesLocales(archivo);
		if (Files.isRegularFile(archivo)) {
			try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
				a.datos.load(r);
			} catch (IOException | IllegalArgumentException e) {
				a.datos.clear();
			}
		}
		return a;
	}

	/** Null si todavía no se eligió. */
	public Path carpetaEventos() {
		String c = datos.getProperty("carpetaEventos");
		return c == null || c.isBlank() ? null : Path.of(c);
	}

	public String dispositivo() {
		return datos.getProperty("dispositivo", "pc");
	}

	/** Conexión con el Panel; sin dispositivo guardado usa el nombre de este equipo. */
	public ConexionPanel conexionPanel() {
		return new ConexionPanel(datos.getProperty("panelUrl", ""), datos.getProperty("panelToken", ""),
				datos.getProperty("panelDispositivo", dispositivo()), Boolean.parseBoolean(datos.getProperty("panelEnviar", "false")));
	}

	public void guardarConexionPanel(ConexionPanel c) {
		datos.setProperty("panelUrl", c.url());
		datos.setProperty("panelToken", c.token());
		datos.setProperty("panelDispositivo", c.dispositivo());
		datos.setProperty("panelEnviar", String.valueOf(c.enviar()));
		guardar();
	}

	public void elegirCarpeta(Path carpeta) {
		datos.setProperty("carpetaEventos", carpeta.toString());
		guardar();
	}

	public void elegirDispositivo(String nombre) {
		datos.setProperty("dispositivo", nombre);
		guardar();
	}

	// Temporal con permisos rw------- y movida atómica: el archivo nunca queda legible por otros ni a medias.
	private void guardar() {
		try {
			Path carpeta = archivo.toAbsolutePath().getParent();
			Files.createDirectories(carpeta);
			Path tmp = Files.createTempFile(carpeta, "ajustes", ".tmp",
					java.nio.file.attribute.PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				datos.store(w, "CALOREITOR escritorio");
			}
			Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}
}
