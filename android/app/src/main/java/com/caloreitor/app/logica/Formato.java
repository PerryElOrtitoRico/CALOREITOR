package com.caloreitor.app.logica;

import com.caloreitor.app.modelo.Macros;

/** Cómo se escriben las cantidades en pantalla. */
public final class Formato {
	private Formato() {
	}

	/** "520 kcal · P 30 · C 60 · G 15". */
	public static String macros(Macros m) {
		return Math.round(m.kcal()) + " kcal · P " + Math.round(m.proteinas()) + " · C " + Math.round(m.carbohidratos()) + " · G " + Math.round(m.grasas());
	}

	/** Gramos con un decimal y sin ".0": 150, 150,5. Sin Locale: el separador es la coma. */
	public static String gramos(double g) {
		return String.valueOf(Math.round(g * 10) / 10.0).replaceAll("\\.0$", "").replace('.', ',');
	}
}
