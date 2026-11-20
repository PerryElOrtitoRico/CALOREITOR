package com.caloreitor.app.datos;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Estado reconstruido: por (entidad, id) gana el evento con mayor (ts, id de evento). */
public final class Estado {
	private final Map<String, Map<String, Evento>> ganadores = new HashMap<>();

	private Estado() {
	}

	public static Estado de(List<Evento> eventos) {
		Estado s = new Estado();
		for (Evento e : eventos) {
			s.ganadores.computeIfAbsent(e.entidad(), k -> new HashMap<>()).merge(e.entidadId(), e,
					(a, b) -> b.ts() > a.ts() || (b.ts() == a.ts() && b.id().compareTo(a.id()) > 0) ? b : a);
		}
		return s;
	}

	public Map<String, JsonObject> vivos(String entidad) {
		Map<String, JsonObject> r = new HashMap<>();
		ganadores.getOrDefault(entidad, Map.of()).forEach((id, e) -> {
			if (!"del".equals(e.op())) r.put(id, e.datos());
		});
		return r;
	}

	/** 0 si la entidad no tiene eventos. */
	public long tsMaximo(String entidad, String entidadId) {
		Evento e = ganadores.getOrDefault(entidad, Map.of()).get(entidadId);
		return e == null ? 0 : e.ts();
	}
}
