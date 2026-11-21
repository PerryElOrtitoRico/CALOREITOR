package com.caloreitor.app.logica;

import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.HistorialDias;
import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.logica.PlanesEntrenamiento.Plan;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lo que comparten todas las pantallas (escritorio y Android): repositorio, objetivos del día, planes y base de alimentos.
 * El repositorio no admite hilos: cada plataforma decide desde qué hilo se usa.
 */
public final class Nucleo {
	private final Repositorio repo;
	private final HistorialDias historial;
	private final ArchivoEventos archivo;
	private final List<Alimento> base;
	private volatile Objetivos objetivos;
	private volatile List<Plan> planes;
	private volatile LocalDate hoy;
	private volatile boolean leyendo;

	/** Lee los eventos y corre la depuración: llamar fuera del hilo de la interfaz. */
	public Nucleo(ArchivoEventos archivo, HistorialDias historial, List<Alimento> base) {
		this.archivo = archivo;
		this.repo = new Repositorio(archivo, System::currentTimeMillis);
		this.historial = historial;
		this.base = base;
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

	/** Después de importar un plan: los objetivos se calculan con los planes nuevos. */
	public void planesCambiaron() {
		planes = Objetivos.planesDe(repo);
		objetivos = new Objetivos(repo, planes, hoy);
	}

	/** La interfaz lo marca mientras relee: el repositorio no admite cambios en ese rato. */
	public void leyendo(boolean v) { leyendo = v; }

	public boolean leyendo() { return leyendo; }

	public Repositorio repo() { return repo; }
	public HistorialDias historial() { return historial; }
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
