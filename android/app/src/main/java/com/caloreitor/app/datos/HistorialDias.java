package com.caloreitor.app.datos;

import com.caloreitor.app.modelo.TotalDia;
import java.util.List;
import java.util.Optional;

/** Una fila por fecha; guardar reemplaza la que hubiera. */
public interface HistorialDias {
	void guardar(TotalDia t);

	Optional<TotalDia> leer(String fecha);

	/** Desde y hasta inclusive (yyyy-MM-dd), ordenado por fecha. */
	List<TotalDia> entre(String desde, String hasta);

	boolean existe(String fecha);
}
