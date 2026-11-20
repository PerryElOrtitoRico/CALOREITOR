package com.caloreitor.app.modelo;

/** Alimento de la base de datos; los valores son por 100 g. porcionNombre y porcionGramos pueden ser null. */
public record Alimento(String id, String nombre, Macros por100, String porcionNombre, Double porcionGramos, boolean base) {
}
