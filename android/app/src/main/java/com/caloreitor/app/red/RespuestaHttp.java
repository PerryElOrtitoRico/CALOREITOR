package com.caloreitor.app.red;

/** Respuesta de un pedido HTTP: código de estado y cuerpo como texto (vacío si no hay). */
public record RespuestaHttp(int estado, String cuerpo) {
}
