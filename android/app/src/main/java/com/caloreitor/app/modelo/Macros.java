package com.caloreitor.app.modelo;

/** Valores nutricionales (kcal y gramos de cada macronutriente). */
public record Macros(double kcal, double proteinas, double carbohidratos, double grasas) {
    public static final Macros CERO = new Macros(0, 0, 0, 0);

    public Macros mas(Macros o) {
        return new Macros(kcal + o.kcal, proteinas + o.proteinas, carbohidratos + o.carbohidratos, grasas + o.grasas);
    }
}
