package com.caloreitor.app.red;

import static org.junit.jupiter.api.Assertions.*;

import com.caloreitor.app.logica.ResumenDia;
import com.caloreitor.app.modelo.Macros;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicadorPanelTest {
	private record Llamada(String metodo, String url, String token, String cuerpo) { }

	private static final class Falso implements Transporte {
		final List<Llamada> llamadas = new ArrayList<>();
		int estado = 200;
		boolean falla;

		public RespuestaHttp enviar(String metodo, String url, String token, String cuerpo) throws IOException {
			llamadas.add(new Llamada(metodo, url, token, cuerpo));
			if (falla) throw new IOException("sin red");
			return new RespuestaHttp(estado, "");
		}
	}

	private final Falso falso = new Falso();
	private final List<ResumenDia> dias = List.of(
			new ResumenDia("2026-11-19", new Macros(1800, 100, 200, 50), null, 11),
			new ResumenDia("2026-11-20", new Macros(2000, 100, 200, 50), null, 22));

	private PublicadorPanel con(String url, String token, boolean enviar) {
		return new PublicadorPanel(falso, new ConexionPanel(url, token, "pc", enviar));
	}

	@Test
	void armaUrlEncabezadoYCuerpo() {
		assertEquals(2, con("http://h:8099/", "tok", true).publicar(dias));
		Llamada l = falso.llamadas.get(1);
		assertEquals("POST", l.metodo());
		assertEquals("http://h:8099/api/registros", l.url());
		assertEquals("tok", l.token());
		JsonObject c = JsonParser.parseString(l.cuerpo()).getAsJsonObject();
		assertEquals("resultado", c.get("tipo").getAsString());
		assertEquals("caloreitor", c.get("fuente").getAsString());
		assertEquals("2026-11-20", c.get("idEnFuente").getAsString());
		assertEquals("nutricion", c.get("area").getAsString());
		assertEquals("2026-11-20", c.get("fecha").getAsString());
		assertEquals("kcal", c.get("metrica").getAsString());
		assertEquals(2000, c.get("valor").getAsDouble(), 1e-9);
		assertEquals(22, c.get("actualizadoEn").getAsLong());
		assertFalse(c.get("borrado").getAsBoolean());
		assertEquals(2000, JsonParser.parseString(c.get("detalleJson").getAsString()).getAsJsonObject()
				.getAsJsonObject("consumido").get("kcal").getAsDouble(), 1e-9);
	}

	@Test
	void cuentaSoloLos2xx() {
		falso.estado = 401;
		assertEquals(0, con("http://h", "tok", true).publicar(dias));
		assertEquals(2, falso.llamadas.size());
	}

	@Test
	void unErrorDeRedCortaSinLanzar() {
		falso.falla = true;
		assertEquals(0, con("http://h", "tok", true).publicar(dias));
		assertEquals(1, falso.llamadas.size());
	}

	@Test
	void sinEnviarSinUrlOSinTokenNoLlamaAlTransporte() {
		assertEquals(0, con("http://h", "tok", false).publicar(dias));
		assertEquals(0, con("", "tok", true).publicar(dias));
		assertEquals(0, con("http://h", "", true).publicar(dias));
		assertTrue(falso.llamadas.isEmpty());
	}

	@Test
	void probarPideElDiaDeHoyYDevuelveSi200() {
		assertTrue(con("http://h/", "tok", true).probar());
		assertEquals("http://h/api/dia/" + LocalDate.now(), falso.llamadas.get(0).url());
		assertEquals("GET", falso.llamadas.get(0).metodo());
		falso.estado = 401;
		assertFalse(con("http://h", "tok", true).probar());
		falso.falla = true;
		assertFalse(con("http://h", "tok", true).probar());
	}
}
