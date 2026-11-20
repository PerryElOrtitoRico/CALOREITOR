package com.caloreitor.app.modelo;

import java.time.DayOfWeek;
import java.util.Set;

public record SemanaEntrenamiento(Set<DayOfWeek> gimnasio, Set<DayOfWeek> running, Set<DayOfWeek> pliometria) {
}
