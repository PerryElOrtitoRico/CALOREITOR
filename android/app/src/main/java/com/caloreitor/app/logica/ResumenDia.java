package com.caloreitor.app.logica;

import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;

/** Lo comido en un día (fecha yyyy-MM-dd); objetivo es null si ese día no tiene uno guardado. consumido es null si el día quedó sin consumos (se borraron). */
public record ResumenDia(String fecha, Macros consumido, Objetivo objetivo, long actualizadoEn) {
	public boolean vacio() {
		return consumido == null;
	}
}
