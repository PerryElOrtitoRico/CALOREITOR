package com.caloreitor.app.logica;

import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.PlanSemanal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Plan de la semana: objetivos fijados, comidas planeadas y lista del súper. */
public final class Planificador {
	public record LineaSuper(String nombre, double gramos) {
	}

	private final Repositorio repo;
	private final Objetivos objetivos;

	public Planificador(Repositorio repo, Objetivos objetivos) {
		this.repo = repo;
		this.objetivos = objetivos;
	}

	public PlanSemanal crear(LocalDate lunes) {
		if (lunes.getDayOfWeek() != DayOfWeek.MONDAY) throw new IllegalArgumentException("No es lunes: " + lunes);
		return guardar(new PlanSemanal(lunes.toString(), fijar(lunes), List.of()));
	}

	public PlanSemanal recalcular(PlanSemanal p) {
		return guardar(new PlanSemanal(p.lunes(), fijar(LocalDate.parse(p.lunes())), p.comidas()));
	}

	public PlanSemanal agregar(PlanSemanal p, Consumo c) {
		LocalDate lunes = LocalDate.parse(p.lunes());
		LocalDate f = LocalDate.parse(c.fecha());
		if (f.isBefore(lunes) || f.isAfter(lunes.plusDays(6))) throw new IllegalArgumentException("Fuera de la semana: " + f);
		List<Consumo> l = new ArrayList<>(p.comidas());
		l.add(c);
		return guardar(new PlanSemanal(p.lunes(), p.objetivos(), l));
	}

	public PlanSemanal quitar(PlanSemanal p, String consumoId) {
		List<Consumo> l = new ArrayList<>(p.comidas());
		l.removeIf(c -> c.id().equals(consumoId));
		return guardar(new PlanSemanal(p.lunes(), p.objetivos(), l));
	}

	public Macros planeado(PlanSemanal p, String fecha) {
		Macros m = Macros.CERO;
		for (Consumo c : p.comidas()) if (c.fecha().equals(fecha)) m = m.mas(c.macros());
		return m;
	}

	public List<LineaSuper> listaSuper(PlanSemanal p, Set<String> fechas) {
		Map<String, LineaSuper> porId = new LinkedHashMap<>();
		for (Consumo c : p.comidas()) {
			if (!fechas.contains(c.fecha())) continue;
			for (Item i : c.items()) {
				porId.merge(i.alimentoId(), new LineaSuper(i.nombre(), i.gramos()),
						(a, b) -> new LineaSuper(a.nombre(), a.gramos() + b.gramos()));
			}
		}
		List<LineaSuper> r = new ArrayList<>(porId.values());
		r.sort((a, b) -> clave(a.nombre()).compareTo(clave(b.nombre())));
		return r;
	}

	public static String comoTexto(List<LineaSuper> lineas) {
		StringBuilder sb = new StringBuilder();
		for (LineaSuper l : lineas) {
			if (sb.length() > 0) sb.append('\n');
			// Sin Locale: el separador decimal es siempre la coma.
			String cant = l.gramos() >= 1000
					? String.format(java.util.Locale.ROOT, "%.1f kg", l.gramos() / 1000).replace('.', ',')
					: Math.round(l.gramos()) + " g";
			sb.append(l.nombre()).append(" — ").append(cant);
		}
		return sb.toString();
	}

	public Consumo comiLoPlaneado(PlanSemanal p, String consumoId) {
		Consumo o = p.comidas().stream().filter(c -> c.id().equals(consumoId)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No está en el plan: " + consumoId));
		Consumo real = new Consumo(UUID.randomUUID().toString(), o.fecha(), o.comida(), o.recetaId(), o.recetaNombre(), o.items());
		repo.guardar(real);
		return real;
	}

	private Map<String, Objetivo> fijar(LocalDate lunes) {
		Map<String, Objetivo> m = new TreeMap<>();
		for (int d = 0; d < 7; d++) {
			LocalDate f = lunes.plusDays(d);
			objetivos.calcular(f).ifPresent(o -> m.put(f.toString(), o));
		}
		return m;
	}

	private PlanSemanal guardar(PlanSemanal p) {
		repo.guardar(p);
		return p;
	}

	private static String clave(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
	}
}
