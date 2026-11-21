package com.caloreitor.escritorio;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Macros;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

/** Cuentas chicas de las pestañas, sin Swing para poder probarlas. */
public final class Cuentas {
	private Cuentas() {
	}

	/** Los tres porcentajes del modo manual deben dar 100 (con un margen por los decimales escritos a mano). */
	public static boolean sumaCien(double prot, double carb, double grasa) {
		return Math.abs(prot + carb + grasa - 100) < 0.05;
	}

	/** Objetivo menos planeado, por macro: positivo falta, negativo sobra. */
	public static Macros faltaSobra(Macros objetivo, Macros planeado) {
		return new Macros(objetivo.kcal() - planeado.kcal(), objetivo.proteinas() - planeado.proteinas(),
				objetivo.carbohidratos() - planeado.carbohidratos(), objetivo.grasas() - planeado.grasas());
	}

	/** Totales de una receta; un ingrediente cuyo alimento ya no existe no suma. */
	public static Macros totalReceta(List<ItemReceta> ingredientes, List<Alimento> alimentos) {
		Map<String, Alimento> porId = new HashMap<>();
		for (Alimento a : alimentos) porId.put(a.id(), a);
		Macros t = Macros.CERO;
		for (ItemReceta i : ingredientes) {
			Alimento a = porId.get(i.alimentoId());
			if (a == null) continue;
			double f = i.gramos() / 100.0;
			t = t.mas(new Macros(a.por100().kcal() * f, a.por100().proteinas() * f, a.por100().carbohidratos() * f,
					a.por100().grasas() * f));
		}
		return t;
	}

	/** Base y propios sin duplicar: un propio con el nombre de uno base (así se guarda la edición) lo tapa. */
	public static List<Alimento> visibles(List<Alimento> todos) {
		Set<String> propios = new HashSet<>();
		for (Alimento a : todos) if (!a.base()) propios.add(clave(a.nombre()));
		return todos.stream().filter(a -> !a.base() || !propios.contains(clave(a.nombre()))).toList();
	}

	/** De los coeficientes escritos, solo los que difieren del defecto: así un defecto nuevo se aplica solo. */
	public static Map<String, Double> soloCambios(Map<String, Double> valores, Map<String, Double> defectos) {
		Map<String, Double> r = new HashMap<>();
		valores.forEach((k, v) -> { if (!v.equals(defectos.get(k))) r.put(k, v); });
		return r;
	}

	/** Número escrito a mano (coma o punto), 0 o más; vacío si está en blanco o no es un número válido. */
	public static OptionalDouble numero(String texto) {
		if (texto == null) return OptionalDouble.empty();
		try {
			double v = Double.parseDouble(texto.trim().replace(',', '.'));
			return v >= 0 && !Double.isInfinite(v) ? OptionalDouble.of(v) : OptionalDouble.empty();
		} catch (NumberFormatException e) {
			return OptionalDouble.empty();
		}
	}

	private static String clave(String s) {
		return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
	}
}
