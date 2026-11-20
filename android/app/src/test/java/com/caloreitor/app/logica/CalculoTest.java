package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.Asignacion.EntrenamientoDia;
import com.caloreitor.app.logica.PlanesEntrenamiento.*;
import com.caloreitor.app.modelo.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CalculoTest {
	static final Perfil perfil = new Perfil(75.0, 178.0, 25, "M", 6000);
	static final Map<String, Double> coef = Map.of();
	static final EntrenamientoDia nada = new EntrenamientoDia(null, null, null);

	static Configuracion cfg(Perfil p, Modo modo, Meta meta, Map<String, Double> c) {
		var d = Configuracion.porDefecto();
		return new Configuracion(p, modo, meta, d.manualKcal(), d.manualPctProt(), d.manualPctCarb(), d.manualPctGrasa(),
				d.comidas(), d.semana(), c);
	}

	static SesionRunning correr() {
		return new SesionRunning(1, "2026-11-03", List.of(new BloqueRunning(1, 5, 1, null, null),
				new BloqueRunning(2, 20, 1, null, null), new BloqueRunning(3, 5, 1, null, null)));
	}

	static SesionGym gimnasio() {
		var s = new ArrayList<SerieGym>();
		for (int i = 0; i < 4; i++) s.add(new SerieGym(6, 91, 70));
		for (int i = 0; i < 4; i++) s.add(new SerieGym(6, 21, 70));
		return new SesionGym(1, 1, s);
	}

	@Test void bmrYBase() {
		assertEquals(1742.5, Calculo.bmr(perfil), 1e-9);
		assertEquals(2141.75, Calculo.base(perfil, coef), 1e-9);
	}

	@Test void runningPorMinutosEnZona() {
		assertEquals(318.75, Calculo.running(correr(), 75, coef), 1e-9);
	}

	@Test void runningConPausas() {
		var s = new SesionRunning(1, "x", List.of(new BloqueRunning(4, 2, 3, 1, 2.0)));
		assertEquals(138.75, Calculo.running(s, 75, coef), 1e-9);
	}

	@Test void gimnasioPorSeriesEIntensidad() {
		var g = gimnasio();
		assertEquals(125.0, Calculo.gym(g, 75, coef), 1e-9);
		assertEquals(2688, Calculo.volumen(g), 1e-9);
		assertEquals(70, Calculo.intensidad(g), 1e-9);
	}

	@Test void pliometria() {
		assertEquals(26.785714, Calculo.plio(new SesionPlio(1, 1, 250, null), 75, coef), 1e-5);
	}

	@Test void diaDeRunningParaBajar() {
		var o = Calculo.objetivo(cfg(perfil, Modo.AUTOMATICO, Meta.BAJAR, coef), new EntrenamientoDia(null, correr(), null)).get();
		assertEquals(2091.425, o.macros().kcal(), 1e-6);
		assertEquals(150, o.macros().proteinas(), 1e-6);
		assertEquals(60, o.macros().grasas(), 1e-6);
		assertEquals(237.85625, o.macros().carbohidratos(), 1e-6);
		assertTrue(o.explicacion().contains("Running"));
	}

	@Test void diaCombinadoSumaTodo() {
		var dia = new EntrenamientoDia(gimnasio(), correr(), new SesionPlio(1, 1, 250, null));
		var o = Calculo.objetivo(cfg(perfil, Modo.AUTOMATICO, Meta.MANTENER, coef), dia).get();
		assertEquals(2612.285714, o.macros().kcal(), 1e-5);
		assertTrue(o.explicacion().contains("Gimnasio") && o.explicacion().contains("2688"));
	}

	@Test void manual() {
		var o = Calculo.objetivo(cfg(perfil, Modo.MANUAL, Meta.MANTENER, coef), null).get();
		assertEquals(2000, o.macros().kcal(), 1e-6);
		assertEquals(150, o.macros().proteinas(), 1e-6);
		assertEquals(225, o.macros().carbohidratos(), 1e-6);
		assertEquals(55.5556, o.macros().grasas(), 1e-4);
	}

	@Test void sinDatosNoCalcula() {
		var vacio = Configuracion.porDefecto();
		assertTrue(Calculo.objetivo(vacio, nada).isEmpty());
		assertTrue(Calculo.objetivo(cfg(new Perfil(75.0, 178.0, 25, "M", null), Modo.AUTOMATICO, Meta.MANTENER, coef), nada).isEmpty());
	}

	@Test void objetivoMuyBajoBajaGrasaYAvisa() {
		// total 388 kcal: proteína 320 + grasa mínima 216 no entran -> carb 0 y grasa en el mínimo
		var c = Map.of("factorBase", 0.5, "kcalPorPasoPorKg", 0.0);
		var o = Calculo.objetivo(cfg(new Perfil(40.0, 150.0, 80, "F", 0), Modo.AUTOMATICO, Meta.MANTENER, c), nada).get();
		assertEquals(0, o.macros().carbohidratos(), 1e-9);
		assertEquals(24, o.macros().grasas(), 1e-9);
		assertTrue(o.explicacion().contains("Ojo: el objetivo es muy bajo para cubrir proteína y grasa mínimas"));
		// si bajar la grasa alcanza (total 582 kcal): carb 0 y grasa = resto/9, sin aviso
		var c2 = Map.of("factorBase", 0.75, "kcalPorPasoPorKg", 0.0);
		var o2 = Calculo.objetivo(cfg(new Perfil(40.0, 150.0, 80, "F", 0), Modo.AUTOMATICO, Meta.MANTENER, c2), nada).get();
		assertEquals(0, o2.macros().carbohidratos(), 1e-9);
		assertEquals((o2.macros().kcal() - 320) / 9, o2.macros().grasas(), 1e-9);
		assertFalse(o2.explicacion().contains("Ojo"));
	}
}
