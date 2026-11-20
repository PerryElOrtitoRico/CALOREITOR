package com.caloreitor.app.modelo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Configuracion(Perfil perfil, Modo modo, Meta meta, double manualKcal, double manualPctProt,
        double manualPctCarb, double manualPctGrasa, List<String> comidas, SemanaEntrenamiento semana,
        Map<String, Double> coeficientes) {
    public static Configuracion porDefecto() {
        return new Configuracion(new Perfil(null, null, null, null, null), Modo.AUTOMATICO, Meta.MANTENER, 2000, 30, 45, 25,
                List.of("Desayuno", "Almuerzo", "Merienda", "Cena"),
                new SemanaEntrenamiento(Set.of(), Set.of(), Set.of()), Map.of());
    }
}
