package com.caloreitor.app.logica;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.modelo.Macros;
import org.junit.jupiter.api.Test;

class FormatoTest {
	@Test void macrosRedondeaCadaValor() {
		assertEquals("520 kcal · P 30 · C 60 · G 15", Formato.macros(new Macros(519.6, 29.5, 60.4, 14.8)));
	}

	@Test void gramosSinCeroDecimalYConComa() {
		assertEquals("150", Formato.gramos(150));
		assertEquals("150,5", Formato.gramos(150.5));
		assertEquals("150,6", Formato.gramos(150.55));
	}
}
