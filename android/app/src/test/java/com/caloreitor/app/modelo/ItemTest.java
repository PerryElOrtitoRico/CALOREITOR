package com.caloreitor.app.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ItemTest {
    @Test void macrosDeUnItem() {
        Item arroz = new Item("a1", "Arroz", new Macros(130, 2.7, 28, 0.3), 160);
        Macros m = arroz.macros();
        assertEquals(208.0, m.kcal(), 1e-9);
        assertEquals(4.32, m.proteinas(), 1e-9);
        assertEquals(44.8, m.carbohidratos(), 1e-9);
        assertEquals(0.48, m.grasas(), 1e-9);
    }

    @Test void sumaDeMacros() {
        assertEquals(new Macros(3, 3, 3, 3), new Macros(1, 1, 1, 1).mas(new Macros(2, 2, 2, 2)));
    }
}
