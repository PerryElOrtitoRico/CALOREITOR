package com.caloreitor.app.modelo;

import java.util.List;
import java.util.Map;

/** lunes es yyyy-MM-dd; objetivos: fecha -> objetivo fijado; comidas: consumos planeados. */
public record PlanSemanal(String lunes, Map<String, Objetivo> objetivos, List<Consumo> comidas) {
}
