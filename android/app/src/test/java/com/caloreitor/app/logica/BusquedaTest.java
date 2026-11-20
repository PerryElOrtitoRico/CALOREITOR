package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.modelo.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusquedaTest {
    static final LocalDate HOY = LocalDate.of(2026, 11, 20);
    static final Macros M = new Macros(100, 10, 10, 1);

    static Alimento al(String id, String nombre, Double porcion, boolean base) {
        return new Alimento(id, nombre, M, porcion == null ? null : "P", porcion, base);
    }

    static Consumo uso(String fecha, String comida, String alimentoId, double g) {
        return new Consumo("c", fecha, comida, null, null, List.of(new Item(alimentoId, alimentoId, M, g)));
    }

    static final List<Alimento> TODOS = List.of(al("arroz", "Arroz blanco hervido", 180.0, true),
            al("lentejas", "Lentejas cocidas", null, true), al("avena", "Avena arrollada", null, true),
            al("mila", "Milanesa de carne (frita)", 150.0, true));

    static Busqueda b(List<Consumo> h) {
        return new Busqueda(TODOS, List.of(new Receta("r1", "Guiso de arroz", List.of())), h);
    }

    @Test void buscaSinTildesNiMayusculasPorPrefijoDePalabras() {
        assertEquals("arroz", b(List.of()).buscar("arroz bla", "Almuerzo", HOY).get(0).id());
        assertEquals("lentejas", b(List.of()).buscar("LÉNTEJ", "Almuerzo", HOY).get(0).id());
        assertTrue(b(List.of()).buscar("blanco arroz", "Almuerzo", HOY).stream().anyMatch(r -> r.id().equals("arroz")));
        assertTrue(b(List.of()).buscar("roz", "Almuerzo", HOY).isEmpty());
    }

    @Test void vacioSugiereLosMasUsadosEnEsaComida() {
        List<Consumo> h = new ArrayList<>();
        for (int i = 1; i <= 3; i++) h.add(uso("2026-11-1" + i, "Desayuno", "avena", 40));
        for (int i = 1; i <= 5; i++) h.add(uso("2026-11-1" + i, "Almuerzo", "mila", 150));
        List<Busqueda.Resultado> r = b(h).buscar("", "Desayuno", HOY);
        assertEquals("avena", r.get(0).id());
        assertTrue(r.stream().noneMatch(x -> x.id().equals("mila")));
    }

    @Test void vacioMaximo15() {
        List<Alimento> muchos = new ArrayList<>();
        List<Consumo> h = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            muchos.add(al("a" + i, "Alimento " + i, null, false));
            h.add(uso("2026-11-19", "Cena", "a" + i, 100));
        }
        assertEquals(15, new Busqueda(muchos, List.of(), h).buscar("", "Cena", HOY).size());
    }

    @Test void conTextoOrdenaPorUsosEnCualquierComidaYRecetasCuentanUnaVez() {
        List<Consumo> h = new ArrayList<>();
        h.add(new Consumo("c", "2026-11-19", "Cena", "r1", "Guiso de arroz",
                List.of(new Item("arroz", "x", M, 50), new Item("arroz", "x", M, 50))));
        h.add(uso("2026-11-18", "Almuerzo", "arroz", 100));
        h.add(uso("2026-11-17", "Cena", "arroz", 100));
        List<Busqueda.Resultado> r = b(h).buscar("arroz", "Desayuno", HOY);
        assertEquals("arroz", r.get(0).id()); // 2 usos propios; la receta suma 1 y sus items no suman
        assertEquals("r1", r.get(1).id());
        assertTrue(r.get(1).esReceta());
        assertEquals(0, r.get(1).gramosSugeridos());
    }

    @Test void sugiereLosUltimosGramosUsados() {
        List<Consumo> h = List.of(uso("2026-11-10", "Almuerzo", "arroz", 100), uso("2026-11-15", "Almuerzo", "arroz", 180),
                uso("2026-11-12", "Cena", "arroz", 90));
        assertEquals(180, b(h).buscar("arroz bla", "Almuerzo", HOY).get(0).gramosSugeridos());
    }

    @Test void sinUsoSugierePorcionOCien() {
        assertEquals(180, b(List.of()).buscar("arroz bla", "Almuerzo", HOY).get(0).gramosSugeridos());
        assertEquals(100, b(List.of()).buscar("lent", "Almuerzo", HOY).get(0).gramosSugeridos());
    }

    @Test void historialDeMasDe30DiasNoCuenta() {
        // 2026-10-22 es hoy-29 (cuenta); 2026-10-21 es hoy-30 (no cuenta)
        List<Consumo> h = List.of(uso("2026-10-21", "Cena", "avena", 40), uso("2026-10-21", "Cena", "avena", 40),
                uso("2026-10-22", "Cena", "mila", 150));
        List<Busqueda.Resultado> r = b(h).buscar("", "Cena", HOY);
        assertEquals(List.of("mila"), r.stream().map(Busqueda.Resultado::id).toList());
        // fuera de la ventana no pesa en el orden, pero sí en los gramos sugeridos
        assertEquals(40, b(h).buscar("avena", "Cena", HOY).get(0).gramosSugeridos());
    }

    @Test void unPropioReemplazaAlBaseConElMismoNombre() {
        List<Alimento> todos = List.of(al("b1", "Dulce de leche", null, true), al("p1", "dulce de léche", 30.0, false));
        List<Busqueda.Resultado> r = new Busqueda(todos, List.of(), List.of()).buscar("dulce", "Cena", HOY);
        assertEquals(List.of("p1"), r.stream().map(Busqueda.Resultado::id).toList());
    }
}
