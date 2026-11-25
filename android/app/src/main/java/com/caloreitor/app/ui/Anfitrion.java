package com.caloreitor.app.ui;

import com.caloreitor.app.android.Ajustes;
import com.caloreitor.app.android.Sesion;

/** Lo que las pestañas le piden a la pantalla principal. */
interface Anfitrion {
	/** Null si no hay carpeta elegida o se perdió el permiso. */
	Sesion sesion();

	Sesion.Estado estado();

	Ajustes ajustes();

	/** Vuelve a dibujar todas las pestañas (después de un cambio). */
	void cambio();

	void elegirCarpeta();

	/** formato: PlanesEntrenamiento.GIMNASIO o RUNNING; el archivo elegido tiene que ser de ese tipo. */
	void importarPlan(String formato);

	/** Cambió el nombre del dispositivo: hay que abrir de nuevo la carpeta para escribir en los archivos con el nombre nuevo. */
	void reabrir();
}
