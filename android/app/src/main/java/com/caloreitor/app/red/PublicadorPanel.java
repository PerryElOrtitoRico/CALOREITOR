package com.caloreitor.app.red;

import com.caloreitor.app.logica.ResumenDia;
import com.caloreitor.app.logica.ResumenPanel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/** Manda las kcal de cada día al Panel. Sin cola: lo que falla se reenvía en la próxima relectura. */
public final class PublicadorPanel {
	private static final Gson GSON = new Gson();

	private final Transporte transporte;
	private final ConexionPanel conexion;

	public PublicadorPanel(Transporte transporte, ConexionPanel conexion) {
		this.transporte = transporte;
		this.conexion = conexion;
	}

	/** Un POST por día; devuelve cuántos respondieron 2xx. Un error de red corta sin lanzar. */
	public int publicar(List<ResumenDia> dias) {
		if (!listo()) return 0;
		int ok = 0;
		for (ResumenDia d : dias) {
			try {
				if (transporte.enviar("POST", base() + "/api/registros", conexion.token(), cuerpo(d)).estado() / 100 == 2) ok++;
			} catch (IOException e) {
				System.err.println("CALOREITOR: no se pudo publicar al panel: " + e.getMessage());
				break;
			}
		}
		return ok;
	}

	/** El panel responde 200 al pedido del día de hoy con ese token. */
	public boolean probar() {
		if (!listo()) return false;
		try {
			return transporte.enviar("GET", base() + "/api/dia/" + LocalDate.now(), conexion.token(), null).estado() == 200;
		} catch (IOException e) {
			return false;
		}
	}

	private boolean listo() {
		return conexion.enviar() && !conexion.url().isBlank() && !conexion.token().isBlank();
	}

	private String base() {
		String u = conexion.url().trim();
		return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
	}

	private static String cuerpo(ResumenDia d) {
		JsonObject o = new JsonObject();
		o.addProperty("tipo", "resultado");
		o.addProperty("fuente", "caloreitor");
		o.addProperty("idEnFuente", d.fecha());
		o.addProperty("area", "nutricion");
		o.addProperty("fecha", d.fecha());
		o.addProperty("metrica", "kcal");
		o.addProperty("valor", d.consumido().kcal());
		o.addProperty("detalleJson", ResumenPanel.detalleJson(d));
		o.addProperty("actualizadoEn", d.actualizadoEn());
		o.addProperty("borrado", false);
		return GSON.toJson(o);
	}
}
