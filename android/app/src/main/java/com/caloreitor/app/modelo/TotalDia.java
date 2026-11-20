package com.caloreitor.app.modelo;

/** Totales de un día archivado; objetivo es null si ese día no hubo objetivo. */
public record TotalDia(String fecha, Macros consumido, Macros objetivo) {
}
