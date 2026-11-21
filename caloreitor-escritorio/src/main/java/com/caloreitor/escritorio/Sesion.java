package com.caloreitor.escritorio;

import com.caloreitor.app.logica.Textos;
import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.logica.BaseAlimentos;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.logica.Objetivos;
import com.caloreitor.app.logica.PlanesEntrenamiento.Plan;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.escritorio.infra.CarpetaArchivos;
import com.caloreitor.escritorio.infra.HistorialSqlite;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Lo común (Nucleo) más lo propio del escritorio: la carpeta en disco, el SQLite y los diálogos de error. */
public final class Sesion {
	private final Nucleo nucleo;
	private final HistorialSqlite historial;

	/** Lee los eventos y corre la depuración: llamar fuera del hilo de la interfaz. */
	public Sesion(Path carpetaEventos, String dispositivo, Path rutaHistorial) {
		historial = new HistorialSqlite(rutaHistorial);
		List<Alimento> base;
		try (InputStream in = Sesion.class.getResourceAsStream("/alimentos-base.csv")) {
			base = BaseAlimentos.cargar(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		nucleo = new Nucleo(new ArchivoEventos(new CarpetaArchivos(carpetaEventos), dispositivo), historial, base);
	}

	/** Relee todos los archivos y depura: fuera del hilo de la interfaz. */
	public void recargar() { nucleo.recargar(); }

	/** Después de importar un plan: los objetivos se calculan con los planes nuevos. */
	public void planesCambiaron() { nucleo.planesCambiaron(); }

	/** La ventana lo marca mientras relee: el repositorio no admite cambios en ese rato. */
	public void leyendo(boolean v) { nucleo.leyendo(v); }

	/**
	 * Corre un cambio al repositorio (hilo de la interfaz). Si se está releyendo lo descarta; si falla muestra el error
	 * en vez de dejarlo solo en la consola. Devuelve true si se hizo.
	 */
	public boolean mutar(java.awt.Component padre, Runnable cambio) {
		if (nucleo.leyendo()) {
			javax.swing.JOptionPane.showMessageDialog(padre, "Se está actualizando: el cambio no se guardó, repetilo.",
					Textos.TITULO, javax.swing.JOptionPane.WARNING_MESSAGE);
			return false;
		}
		try {
			cambio.run();
			return true;
		} catch (RuntimeException e) {
			javax.swing.JOptionPane.showMessageDialog(padre, "No se pudo guardar el cambio: " + e,
					Textos.TITULO, javax.swing.JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public Repositorio repo() { return nucleo.repo(); }
	public HistorialSqlite historial() { return historial; }
	public Objetivos objetivos() { return nucleo.objetivos(); }
	public List<Plan> planes() { return nucleo.planes(); }
	public LocalDate hoy() { return nucleo.hoy(); }
	public List<Alimento> todosLosAlimentos() { return nucleo.todosLosAlimentos(); }
	public List<Consumo> consumosRecientes() { return nucleo.consumosRecientes(); }
}
