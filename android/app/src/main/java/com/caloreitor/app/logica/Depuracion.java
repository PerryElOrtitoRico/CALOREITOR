package com.caloreitor.app.logica;

import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.datos.HistorialDias;
import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.TotalDia;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Set;

/** Pasa los días de más de un mes al historial y borra los archivos mensuales propios ya archivados. */
public final class Depuracion {
	private final Repositorio repo;
	private final Objetivos objetivos;
	private final HistorialDias historial;
	private final ArchivoEventos archivo;

	public Depuracion(Repositorio repo, Objetivos objetivos, HistorialDias historial, ArchivoEventos archivo) {
		this.repo = repo;
		this.objetivos = objetivos;
		this.historial = historial;
		this.archivo = archivo;
	}

	public void correr(LocalDate hoy) {
		Set<String> fechas = repo.fechasConConsumos();
		Set<String> validas = new java.util.TreeSet<>();
		for (String f : fechas) {
			LocalDate fecha;
			try {
				fecha = LocalDate.parse(f);
			} catch (DateTimeParseException e) {
				continue; // fecha ilegible: no frena a las demás
			}
			validas.add(f);
			if (!fecha.isBefore(hoy.minusDays(31))) continue;
			var previo = historial.leer(f);
			// Más viejo que 62 días no se toca: un consumo "resucitado" no pisa lo archivado.
			if (previo.isPresent() && fecha.isBefore(hoy.minusDays(62))) continue;
			Macros total = Macros.CERO;
			for (var c : repo.consumos(f)) total = total.mas(c.macros());
			Macros obj = objetivos.del(fecha).map(Objetivo::macros).orElse(null);
			// Con el día ya archivado se refresca solo si llegó algo tardío por sincronización.
			var t = new TotalDia(f, total, obj);
			if (!t.equals(previo.orElse(null))) historial.guardar(t);
		}
		String prefijo = "dia-" + archivo.dispositivo() + "-";
		for (String nombre : archivo.carpeta().archivos()) {
			if (!nombre.startsWith(prefijo) || !nombre.endsWith(".jsonl")) continue;
			YearMonth mes;
			try {
				mes = YearMonth.parse(nombre.substring(prefijo.length(), nombre.length() - ".jsonl".length()));
			} catch (DateTimeParseException e) {
				continue;
			}
			if (!mes.atEndOfMonth().isBefore(hoy.minusDays(62))) continue;
			boolean archivado = validas.stream()
					.filter(f -> YearMonth.from(LocalDate.parse(f)).equals(mes))
					.allMatch(historial::existe);
			if (archivado) archivo.carpeta().borrar(nombre);
		}
	}
}
