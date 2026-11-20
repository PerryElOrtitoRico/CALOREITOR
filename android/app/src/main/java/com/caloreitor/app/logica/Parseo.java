package com.caloreitor.app.logica;

import java.util.OptionalDouble;

public final class Parseo {
    private Parseo() {
    }

    /** Gramos escritos a mano ("150", "150.5", "150,5", con espacios); vacío si no es un número mayor que 0. */
    public static OptionalDouble gramos(String texto) {
        if (texto == null) return OptionalDouble.empty();
        try {
            double v = Double.parseDouble(texto.trim().replace(',', '.'));
            // NaN e infinito no sirven como gramos
            return v > 0 && !Double.isInfinite(v) ? OptionalDouble.of(v) : OptionalDouble.empty();
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}
