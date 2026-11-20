package com.caloreitor.app.modelo;

/** Lo comido: copia de los valores del alimento al momento de cargarlo. */
public record Item(String alimentoId, String nombre, Macros por100, double gramos) {
    public Macros macros() {
        double f = gramos / 100.0;
        return new Macros(por100.kcal() * f, por100.proteinas() * f, por100.carbohidratos() * f, por100.grasas() * f);
    }
}
