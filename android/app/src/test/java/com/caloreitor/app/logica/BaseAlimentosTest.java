package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Macros;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class BaseAlimentosTest {
    @Test void laBaseCargaYTieneValoresSanos() throws Exception {
        List<Alimento> base = BaseAlimentos.cargar(getClass().getResourceAsStream("/alimentos-base.csv"));
        assertTrue(base.size() >= 200 && base.size() <= 400, "filas: " + base.size());
        assertEquals(base.size(), new HashSet<>(base.stream().map(Alimento::id).toList()).size());
        for (Alimento a : base) {
            assertTrue(a.base());
            Macros m = a.por100();
            assertTrue(m.kcal() >= 0 && m.proteinas() >= 0 && m.carbohidratos() >= 0 && m.grasas() >= 0, a.nombre());
            double calc = 4 * m.proteinas() + 4 * m.carbohidratos() + 9 * m.grasas();
            double dif = Math.abs(m.kcal() - calc);
            assertTrue(dif <= 0.15 * m.kcal() || dif <= 15, a.nombre() + " kcal " + m.kcal() + " vs " + calc);
        }
    }

    @Test void conservaTildesYPorcion() throws Exception {
        List<Alimento> base = BaseAlimentos.cargar(getClass().getResourceAsStream("/alimentos-base.csv"));
        Alimento a = base.stream().filter(x -> x.nombre().equals("Milanesa de carne (frita)")).findFirst().orElseThrow();
        assertEquals("Milanesa", a.porcionNombre());
        assertEquals(150.0, a.porcionGramos());
    }
}
