package com.caloreitor.app.modelo;

import java.util.List;

/** Una comida del día. fecha es yyyy-MM-dd; recetaId y recetaNombre son null si son alimentos sueltos. */
public record Consumo(String id, String fecha, String comida, String recetaId, String recetaNombre, List<Item> items) {
    public Macros macros() {
        Macros m = Macros.CERO;
        for (Item i : items) m = m.mas(i.macros());
        return m;
    }
}
