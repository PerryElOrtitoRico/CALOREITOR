package com.caloreitor.app.red;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Transporte real con HttpURLConnection: UTF-8, no sigue redirecciones y nunca registra el token. */
public final class TransporteHttp implements Transporte {

    public static final int CONECTAR_MS = 5000;
    public static final int LEER_MS = 10000;

    private final int conectarMs;
    private final int leerMs;

    public TransporteHttp() {
        this(CONECTAR_MS, LEER_MS);
    }

    public TransporteHttp(int conectarMs, int leerMs) {
        this.conectarMs = conectarMs;
        this.leerMs = leerMs;
    }

    @Override
    public RespuestaHttp enviar(String metodo, String url, String token, String cuerpoJson) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        try {
            con.setRequestMethod(metodo);
            con.setConnectTimeout(conectarMs);
            con.setReadTimeout(leerMs);
            con.setInstanceFollowRedirects(false);
            try {
                con.setRequestProperty("Authorization", "Bearer " + token);
            } catch (IllegalArgumentException e) {
                throw new IOException("El token tiene caracteres que no se pueden mandar.");
            }
            con.setRequestProperty("Accept", "application/json");
            if (cuerpoJson != null) {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream salida = con.getOutputStream()) {
                    salida.write(cuerpoJson.getBytes(StandardCharsets.UTF_8));
                }
            }
            int estado = con.getResponseCode();
            InputStream entrada = estado >= 400 ? con.getErrorStream() : con.getInputStream();
            return new RespuestaHttp(estado, leerTodo(entrada));
        } finally {
            con.disconnect();
        }
    }

    static String leerTodo(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
        try (InputStream i = entrada) {
            return new String(i.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
