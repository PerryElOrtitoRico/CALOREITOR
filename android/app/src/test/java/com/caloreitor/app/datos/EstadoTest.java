package com.caloreitor.app.datos;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

class EstadoTest {
	private static JsonObject v(String valor) {
		JsonObject o = new JsonObject();
		o.addProperty("v", valor);
		return o;
	}

	private static Evento put(String eid, long ts, String disp, String valor) {
		return putConId(disp + ts, eid, ts, valor);
	}

	private static Evento putConId(String id, String eid, long ts, String valor) {
		return new Evento(id, "consumo", eid, "put", ts, "pc", v(valor));
	}

	@Test
	void ganaElMasNuevoAunqueVengaDeOtroArchivo() {
		Estado e = Estado.de(List.of(put("c1", 100, "pc", "a"), put("c1", 200, "celular", "b")));
		assertEquals("b", e.vivos("consumo").get("c1").get("v").getAsString());
	}

	@Test
	void empateDeTsDesempataPorIdDeEvento() {
		var x = putConId("a", "c1", 100, "x");
		var y = putConId("z", "c1", 100, "y");
		assertEquals("y", Estado.de(List.of(x, y)).vivos("consumo").get("c1").get("v").getAsString());
		assertEquals("y", Estado.de(List.of(y, x)).vivos("consumo").get("c1").get("v").getAsString());
	}

	@Test
	void borradoMasNuevoSacaLaEntidad() {
		var del = new Evento("d", "consumo", "c1", "del", 200, "pc", null);
		Estado e = Estado.de(List.of(put("c1", 100, "pc", "a"), del));
		assertTrue(e.vivos("consumo").isEmpty());
		assertEquals(200, e.tsMaximo("consumo", "c1"));
		assertEquals(0, e.tsMaximo("consumo", "otro"));
	}
}
