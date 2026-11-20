package com.caloreitor.app.modelo;

import java.util.List;

public record Receta(String id, String nombre, List<ItemReceta> ingredientes) {
}
