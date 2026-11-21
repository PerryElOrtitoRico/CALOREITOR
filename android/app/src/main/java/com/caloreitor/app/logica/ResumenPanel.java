package com.caloreitor.app.logica;

import com.caloreitor.app.datos.Repositorio;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Macros;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Arma lo que se manda al Panel: un resumen por día con consumos. */
public final class ResumenPanel {
	private static final Gson GSON = new Gson();

	private ResumenPanel() {
	}

	/** Días con consumos (o con cambios, si quedaron vacíos) entre hoy − cantidad + 1 y hoy, del más viejo al más nuevo. El objetivo es el guardado (no se calcula ni se congela acá). */
	public static List<ResumenDia> dias(Repositorio repo, LocalDate hoy, int cantidad) {
		List<ResumenDia> r = new ArrayList<>();
		for (int i = cantidad - 1; i >= 0; i--) {
			String fecha = hoy.minusDays(i).toString();
			List<Consumo> consumos = repo.consumos(fecha);
			if (consumos.isEmpty()) {
				// sin consumos pero con cambios: se borraron, y el Panel tiene que enterarse
				long cambio = repo.ultimoCambio(fecha);
				if (cambio > 0) r.add(new ResumenDia(fecha, null, null, cambio));
				continue;
			}
			Macros total = Macros.CERO;
			for (Consumo c : consumos) total = total.mas(c.macros());
			r.add(new ResumenDia(fecha, total, repo.objetivoGuardado(fecha).orElse(null), repo.ultimoCambio(fecha)));
		}
		return r;
	}

	/** Detalle del área nutricion del Panel: consumido y, si hay, objetivoKcal. */
	public static String detalleJson(ResumenDia d) {
		JsonObject o = new JsonObject();
		o.add("consumido", GSON.toJsonTree(d.consumido()));
		if (d.objetivo() != null) o.addProperty("objetivoKcal", d.objetivo().macros().kcal());
		return o.toString();
	}
}
