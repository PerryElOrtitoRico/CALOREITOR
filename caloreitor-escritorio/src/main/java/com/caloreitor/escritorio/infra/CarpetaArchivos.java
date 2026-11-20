package com.caloreitor.escritorio.infra;

import com.caloreitor.app.datos.CarpetaEventos;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Carpeta de eventos en disco (la que sincroniza Syncthing). */
public final class CarpetaArchivos implements CarpetaEventos {
	private final Path carpeta;

	public CarpetaArchivos(Path carpeta) {
		this.carpeta = carpeta;
	}

	@Override
	public List<String> archivos() {
		try (Stream<Path> s = Files.list(carpeta)) {
			List<String> r = new ArrayList<>();
			s.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".jsonl") && (n.startsWith("catalogo-") || n.startsWith("dia-")))
					.forEach(r::add);
			return r;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public List<String> leerLineas(String archivo) {
		try {
			Path p = carpeta.resolve(archivo);
			return Files.exists(p) ? Files.readAllLines(p, StandardCharsets.UTF_8) : List.of();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void agregarLinea(String archivo, String linea) {
		try {
			Files.createDirectories(carpeta);
			Files.writeString(carpeta.resolve(archivo), linea + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void borrar(String archivo) {
		try {
			Files.deleteIfExists(carpeta.resolve(archivo));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
