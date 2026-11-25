package com.caloreitor.app.modelo;

/**
 * Cualquier campo puede ser null (todavía no cargado). sexo es "M" o "F".
 * formula: "MIFFLIN" (o null) o "CUNNINGHAM"; Cunningham necesita grasaPct.
 */
public record Perfil(Double pesoKg, Double alturaCm, Integer edad, String sexo, Integer pasosDiarios, Double grasaPct, String formula) {
	public static final String MIFFLIN = "MIFFLIN", CUNNINGHAM = "CUNNINGHAM";

	public Perfil(Double pesoKg, Double alturaCm, Integer edad, String sexo, Integer pasosDiarios) {
		this(pesoKg, alturaCm, edad, sexo, pasosDiarios, null, null);
	}

	/** Se usa Cunningham solo si está elegido y hay % de grasa; si no, Mifflin-St Jeor. */
	public boolean usaCunningham() {
		return CUNNINGHAM.equals(formula) && grasaPct != null;
	}
}
