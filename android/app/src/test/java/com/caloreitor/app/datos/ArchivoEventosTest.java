package com.caloreitor.app.datos;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import java.util.*;
import org.junit.jupiter.api.Test;

class ArchivoEventosTest {
	static class Memoria implements CarpetaEventos {
		final Map<String, List<String>> f = new TreeMap<>();

		public List<String> archivos() {
			return new ArrayList<>(f.keySet());
		}

		public List<String> leerLineas(String a) {
			return f.getOrDefault(a, List.of());
		}

		public void agregarLinea(String a, String l) {
			f.computeIfAbsent(a, k -> new ArrayList<>()).add(l);
		}

		public void borrar(String a) {
			f.remove(a);
		}
	}

	@Test
	void lineaCortadaOJsonRotoSeSaltea() {
		Memoria m = new Memoria();
		ArchivoEventos a = new ArchivoEventos(m, "pc");
		a.escribir("alimento", "a1", "del", null, 5, null);
		m.agregarLinea("catalogo-pc.jsonl", "{\"id\":\"x\",");
		m.agregarLinea("catalogo-pc.jsonl", "basura");
		List<Evento> r = a.leerTodos();
		assertEquals(1, r.size());
		assertNull(r.get(0).datos());
		assertEquals("a1", r.get(0).entidadId());
	}

	@Test
	void delConDatosNullExplicitoSeParsea() {
		Memoria m = new Memoria();
		m.agregarLinea("catalogo-pc.jsonl", "{\"id\":\"d\",\"ent\":\"alimento\",\"eid\":\"a1\",\"op\":\"del\",\"ts\":7,\"disp\":\"pc\",\"datos\":null}");
		List<Evento> r = new ArchivoEventos(m, "pc").leerTodos();
		assertEquals(1, r.size());
		assertNull(r.get(0).datos());
	}

	@Test
	void escribirSoloAgregaAlArchivoPropio() {
		Memoria m = new Memoria();
		m.agregarLinea("dia-celular-2026-11.jsonl", "{}");
		ArchivoEventos a = new ArchivoEventos(m, "pc");
		JsonObject d = new JsonObject();
		d.addProperty("fecha", "2026-11-20");
		a.escribir("receta", "r1", "put", new JsonObject(), 1, null);
		Evento c = a.escribir("consumo", "c1", "put", d, 2, "2026-11-20");
		assertEquals(1, m.f.get("catalogo-pc.jsonl").size());
		assertEquals(1, m.f.get("dia-pc-2026-11.jsonl").size());
		assertEquals(List.of("{}"), m.f.get("dia-celular-2026-11.jsonl"));
		assertEquals(c, a.leerTodos().stream().filter(e -> e.id().equals(c.id())).findFirst().get());
	}

	@Test
	void fechaDelDiaSeValidaSegunEntidad() {
		ArchivoEventos a = new ArchivoEventos(new Memoria(), "pc");
		assertThrows(IllegalArgumentException.class, () -> a.escribir("consumo", "c", "del", null, 1, null));
		assertThrows(IllegalArgumentException.class, () -> a.escribir("alimento", "c", "del", null, 1, "2026-11-20"));
	}
}
