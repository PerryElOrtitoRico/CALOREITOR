package com.caloreitor.app.red;

import java.io.IOException;

/** Manda un pedido HTTP. Lanza IOException si no hay conexión o se agota el tiempo. */
public interface Transporte {

    /** cuerpoJson es null cuando el pedido no lleva cuerpo. */
    RespuestaHttp enviar(String metodo, String url, String token, String cuerpoJson) throws IOException;
}
