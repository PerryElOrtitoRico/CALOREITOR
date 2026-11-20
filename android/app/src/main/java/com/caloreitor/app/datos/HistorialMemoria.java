package com.caloreitor.app.datos;

import com.caloreitor.app.modelo.TotalDia;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** Historial en memoria, para tests. */
public class HistorialMemoria implements HistorialDias {
	private final TreeMap<String, TotalDia> filas = new TreeMap<>();

	public void guardar(TotalDia t) {
		filas.put(t.fecha(), t);
	}

	public Optional<TotalDia> leer(String fecha) {
		return Optional.ofNullable(filas.get(fecha));
	}

	public List<TotalDia> entre(String desde, String hasta) {
		return new ArrayList<>(filas.subMap(desde, true, hasta, true).values());
	}

	public boolean existe(String fecha) {
		return filas.containsKey(fecha);
	}
}
