package com.caloreitor.app.datos;

import java.util.List;

/** Carpeta sincronizada con los archivos de eventos de todos los dispositivos. */
public interface CarpetaEventos {
	/** Solo nombres catalogo-*.jsonl y dia-*.jsonl. */
	List<String> archivos();

	List<String> leerLineas(String archivo);

	void agregarLinea(String archivo, String linea);

	void borrar(String archivo);
}
