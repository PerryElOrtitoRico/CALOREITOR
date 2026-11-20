package com.caloreitor.app.modelo;

/** Cualquier campo puede ser null (todavía no cargado). sexo es "M" o "F". */
public record Perfil(Double pesoKg, Double alturaCm, Integer edad, String sexo, Integer pasosDiarios) {
}
