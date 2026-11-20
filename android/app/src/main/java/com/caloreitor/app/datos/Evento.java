package com.caloreitor.app.datos;

import com.google.gson.JsonObject;

/** Un cambio sobre una entidad. op es "put" o "del" (con datos null). */
public record Evento(String id, String entidad, String entidadId, String op, long ts, String dispositivo, JsonObject datos) {
}
