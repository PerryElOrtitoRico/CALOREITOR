package com.caloreitor.escritorio;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.Planificador;
import com.caloreitor.app.modelo.*;
import com.caloreitor.escritorio.alimentos.PestanaAlimentos;
import com.caloreitor.escritorio.config.PestanaConfiguracion;
import com.caloreitor.escritorio.infra.AjustesLocales;
import com.caloreitor.escritorio.planificador.PestanaPlanificador;
import com.caloreitor.escritorio.recetas.PestanaRecetas;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Humo sin pantalla: las pestañas se arman y se redibujan con datos reales, sin excepciones. */
class PestanasNuevasTest {
	@TempDir Path dir;

	@Test void sePuedenArmarYRedibujarConDatos() throws Exception {
		Path eventos = Files.createDirectory(dir.resolve("eventos"));
		Sesion s = new Sesion(eventos, "pc", dir.resolve("h.db"));
		var cfg = new Configuracion(new Perfil(70.0, 175.0, 30, "M", 6000), Modo.AUTOMATICO, Meta.BAJAR, 2000, 30, 45, 25,
				List.of("Desayuno", "Almuerzo"), new SemanaEntrenamiento(Set.of(DayOfWeek.MONDAY), Set.of(), Set.of()), Map.of("deficit", 0.2));
		s.repo().guardar(cfg);
		Alimento arroz = s.todosLosAlimentos().get(0);
		s.repo().guardar(new Receta(UUID.randomUUID().toString(), "Plato", List.of(new ItemReceta(arroz.id(), 150))));
		var lunes = s.hoy().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		var plan = new Planificador(s.repo(), s.objetivos()).crear(lunes);
		new Planificador(s.repo(), s.objetivos()).agregar(plan, new Consumo(UUID.randomUUID().toString(), s.hoy().toString(),
				"Almuerzo", null, null, List.of(new Item(arroz.id(), arroz.nombre(), arroz.por100(), 200))));

		Runnable nada = () -> { };
		var plani = new PestanaPlanificador(s, nada);
		plani.refrescar();
		var recetas = new PestanaRecetas(s, nada);
		recetas.refrescar();
		var alimentos = new PestanaAlimentos(s, nada);
		alimentos.refrescar();
		var config = new PestanaConfiguracion(s, AjustesLocales.abrir(dir.resolve("a.properties")), nada);
		config.refrescar();
		assertTrue(plani.getComponentCount() > 0 && recetas.getComponentCount() > 0 && alimentos.getComponentCount() > 0 && config.getComponentCount() > 0);
	}
}
