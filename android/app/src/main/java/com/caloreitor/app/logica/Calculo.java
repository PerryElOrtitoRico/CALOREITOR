package com.caloreitor.app.logica;

import com.caloreitor.app.logica.Asignacion.EntrenamientoDia;
import com.caloreitor.app.logica.PlanesEntrenamiento.*;
import com.caloreitor.app.modelo.*;
import java.util.Map;
import java.util.Optional;

/** Objetivo diario: metabolismo + pasos + entrenamientos del día, ajustado por la meta; o el objetivo manual. */
public final class Calculo {
	private Calculo() {
	}

	private static final Map<String, Double> DEFECTOS = Map.ofEntries(Map.entry("factorBase", 1.1),
			Map.entry("kcalPorPasoPorKg", 0.0005), Map.entry("metZ1", 7.0), Map.entry("metZ2", 8.5),
			Map.entry("metZ3", 10.0), Map.entry("metZ4", 11.5), Map.entry("metZ5", 13.0), Map.entry("minPorSerie", 2.5),
			Map.entry("metGymBajo", 3.5), Map.entry("metGymMedio", 5.0), Map.entry("metGymAlto", 6.0),
			Map.entry("kcalPorContacto", 0.1), Map.entry("deficit", 0.15), Map.entry("superavit", 0.10),
			Map.entry("protPorKg", 2.0), Map.entry("grasaPorKg", 0.8), Map.entry("grasaMinPorKg", 0.6));

	private static double c(Map<String, Double> coef, String k) {
		return coef != null && coef.containsKey(k) ? coef.get(k) : DEFECTOS.get(k);
	}

	public static double bmr(Perfil p) {
		return 10 * p.pesoKg() + 6.25 * p.alturaCm() - 5 * p.edad() + ("M".equals(p.sexo()) ? 5 : -161);
	}

	public static double base(Perfil p, Map<String, Double> coef) {
		return bmr(p) * c(coef, "factorBase") + p.pasosDiarios() * c(coef, "kcalPorPasoPorKg") * p.pesoKg();
	}

	private static double met(int zona, Map<String, Double> coef) {
		return c(coef, "metZ" + zona);
	}

	public static double running(SesionRunning s, double kg, Map<String, Double> coef) {
		double total = 0;
		for (BloqueRunning b : s.bloques()) {
			total += b.minutos() * b.repeticiones() * met(b.zona(), coef);
			if (b.zonaPausa() != null && b.minutosPausa() != null) {
				total += b.minutosPausa() * b.repeticiones() * met(b.zonaPausa(), coef);
			}
		}
		return total * kg / 60;
	}

	public static double volumen(SesionGym s) {
		return s.series().stream().mapToDouble(x -> x.reps() * x.kg()).sum();
	}

	public static double intensidad(SesionGym s) {
		return s.series().stream().mapToDouble(SerieGym::pct).average().orElse(0);
	}

	public static double gym(SesionGym s, double kg, Map<String, Double> coef) {
		double pct = intensidad(s);
		String k = pct < 60 ? "metGymBajo" : pct <= 80 ? "metGymMedio" : "metGymAlto";
		return s.series().size() * c(coef, "minPorSerie") * c(coef, k) * kg / 60;
	}

	public static double plio(SesionPlio s, double kg, Map<String, Double> coef) {
		return s.contactos() * c(coef, "kcalPorContacto") * kg / 70;
	}

	private static long r(double x) {
		return Math.round(x);
	}

	/** Vacío si falta algún dato del perfil en modo automático. {@code dia} puede ser null (sin entrenamiento). */
	public static Optional<Objetivo> objetivo(Configuracion cfg, EntrenamientoDia dia) {
		if (cfg.modo() == Modo.MANUAL) {
			double k = cfg.manualKcal();
			var m = new Macros(k, k * cfg.manualPctProt() / 100 / 4, k * cfg.manualPctCarb() / 100 / 4,
					k * cfg.manualPctGrasa() / 100 / 9);
			return Optional.of(new Objetivo(m, "Objetivo manual: " + r(k) + " kcal."));
		}
		Perfil p = cfg.perfil();
		if (p.pesoKg() == null || p.alturaCm() == null || p.edad() == null || p.sexo() == null || p.pasosDiarios() == null) {
			return Optional.empty();
		}
		var coef = cfg.coeficientes();
		double kg = p.pesoKg();
		StringBuilder e = new StringBuilder();
		e.append("Metabolismo basal: ").append(r(bmr(p))).append(" kcal\n");
		double base = base(p, coef);
		e.append("Base con pasos (").append(p.pasosDiarios()).append("): ").append(r(base)).append(" kcal\n");
		double total = base;
		if (dia != null) {
			if (dia.gym() != null) {
				double g = gym(dia.gym(), kg, coef);
				total += g;
				e.append("Gimnasio: ").append(r(g)).append(" kcal (volumen ").append(r(volumen(dia.gym())))
						.append(" kg, intensidad ").append(r(intensidad(dia.gym()))).append(" %)\n");
			}
			if (dia.running() != null) {
				double x = running(dia.running(), kg, coef);
				total += x;
				e.append("Running: ").append(r(x)).append(" kcal (").append(minutosPorZona(dia.running())).append(")\n");
			}
			if (dia.plio() != null) {
				double x = plio(dia.plio(), kg, coef);
				total += x;
				e.append("Pliometría: ").append(r(x)).append(" kcal (").append(dia.plio().contactos())
						.append(" contactos)\n");
			}
		}
		if (cfg.meta() == Meta.BAJAR) {
			total *= 1 - c(coef, "deficit");
			e.append("Meta bajar: déficit del ").append(r(c(coef, "deficit") * 100)).append(" %\n");
		} else if (cfg.meta() == Meta.SUBIR) {
			total *= 1 + c(coef, "superavit");
			e.append("Meta subir: superávit del ").append(r(c(coef, "superavit") * 100)).append(" %\n");
		} else {
			e.append("Meta mantener\n");
		}
		double prot = c(coef, "protPorKg") * kg;
		double grasa = c(coef, "grasaPorKg") * kg;
		double carb = (total - prot * 4 - grasa * 9) / 4;
		if (carb < 0) {
			carb = 0;
			grasa = Math.max(c(coef, "grasaMinPorKg") * kg, (total - prot * 4) / 9);
			// La grasa mínima puede no alcanzar: el objetivo queda corto de kcal y se avisa
			if (prot * 4 + grasa * 9 > total + 1e-9) {
				e.append("Ojo: el objetivo es muy bajo para cubrir proteína y grasa mínimas\n");
			}
		}
		e.append("Total: ").append(r(total)).append(" kcal");
		return Optional.of(new Objetivo(new Macros(total, prot, carb, grasa), e.toString()));
	}

	private static String minutosPorZona(SesionRunning s) {
		double[] z = new double[6];
		for (BloqueRunning b : s.bloques()) {
			z[b.zona()] += b.minutos() * b.repeticiones();
			if (b.zonaPausa() != null && b.minutosPausa() != null) z[b.zonaPausa()] += b.minutosPausa() * b.repeticiones();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= 5; i++) {
			if (z[i] > 0) sb.append(sb.length() > 0 ? ", " : "").append(r(z[i])).append(" min Z").append(i);
		}
		return sb.toString();
	}
}
