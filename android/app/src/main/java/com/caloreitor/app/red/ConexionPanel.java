package com.caloreitor.app.red;

/** Datos para hablar con el Panel; enviar = false deja la publicación apagada. */
public record ConexionPanel(String url, String token, String dispositivo, boolean enviar) {
}
