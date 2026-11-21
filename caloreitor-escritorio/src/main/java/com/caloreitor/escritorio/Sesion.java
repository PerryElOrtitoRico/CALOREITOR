package com.caloreitor.escritorio;

import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.logica.BaseAlimentos;
import com.caloreitor.app.logica.Depuracion;
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
import java.util.ArrayList;
import java.util.List;

/** Todo lo que comparten las pestañas: repositorio, historial, objetivos y la base de alimentos. */
public final class Sesion {
	private final Repositorio repo;
	private final HistorialSqlite historial;
	private final ArchivoEventos archivo;
	private final List<Alimento> base;
	private volatile Objetivos objetivos;
	private volatile List<Plan> planes;
	private volatile LocalDate hoy;
	private volatile boolean leyendo;

	/** Lee los eventos y corre la depuración: llamar fuera del hilo de la interfaz. */
	public Sesion(Path carpetaEventos, String dispositivo, Path rutaHistorial) {
		archivo = new ArchivoEventos(new CarpetaArchivos(carpetaEventos), dispositivo);
		repo = new Repositorio(archivo, System::currentTimeMillis);
		historial = new HistorialSqlite(rutaHistorial);
		try (InputStream in = Sesion.class.getResourceAsStream("/alimentos-base.csv")) {
			base = BaseAlimentos.cargar(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		depurar();
	}

	/** Relee todos los archivos y depura: fuera del hilo de la interfaz. */
	public void recargar() {
		repo.recargar();
		depurar();
	}

	private void depurar() {
		hoy = LocalDate.now();
		planes = Objetivos.planesDe(repo);
		objetivos = new Objetivos(repo, planes, hoy);
		new Depuracion(repo, objetivos, historial, archivo).correr(hoy);
	}

	/** La ventana lo marca mientras relee: el repositorio no admite cambios en ese rato. */
	public void leyendo(boolean v) { leyendo = v; }

	/**
	 * Corre un cambio al repositorio (hilo de la interfaz). Si se está releyendo lo descarta; si falla muestra el error
	 * en vez de dejarlo solo en la consola. Devuelve true si se hizo.
	 */
	public boolean mutar(java.awt.Component padre, Runnable cambio) {
		if (leyendo) return false;
		try {
			cambio.run();
			return true;
		} catch (RuntimeException e) {
			javax.swing.JOptionPane.showMessageDialog(padre, "No se pudo guardar el cambio: " + e,
					Textos.TITULO, javax.swing.JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	public Repositorio repo() { return repo; }
	public HistorialSqlite historial() { return historial; }
	public Objetivos objetivos() { return objetivos; }
	public List<Plan> planes() { return planes; }
	public LocalDate hoy() { return hoy; }

	/** Base más alimentos propios. */
	public List<Alimento> todosLosAlimentos() {
		List<Alimento> r = new ArrayList<>(base);
		r.addAll(repo.alimentosPropios());
		return r;
	}

	/** shortcut: solo los últimos 30 días alimentan sugerencias y "últimos gramos"; subir si se quiere recordar gramos de comidas más viejas. */
	public List<Consumo> consumosRecientes() {
		List<Consumo> r = new ArrayList<>();
		for (int i = 29; i >= 0; i--) r.addAll(repo.consumos(hoy.minusDays(i).toString()));
		return r;
	}
}
