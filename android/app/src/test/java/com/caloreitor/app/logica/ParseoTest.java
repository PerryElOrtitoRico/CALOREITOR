package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ParseoTest {
    @Test void parseoDeGramos() {
        assertEquals(150.0, Parseo.gramos("150").getAsDouble(), 1e-9);
        assertEquals(150.5, Parseo.gramos("150.5").getAsDouble(), 1e-9);
        assertEquals(150.5, Parseo.gramos(" 150,5 ").getAsDouble(), 1e-9);
        assertTrue(Parseo.gramos("").isEmpty());
        assertTrue(Parseo.gramos("0").isEmpty());
        assertTrue(Parseo.gramos("-3").isEmpty());
        assertTrue(Parseo.gramos("mucho").isEmpty());
        assertTrue(Parseo.gramos("NaN").isEmpty());
    }
}
