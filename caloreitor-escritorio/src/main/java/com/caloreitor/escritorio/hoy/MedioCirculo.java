package com.caloreitor.escritorio.hoy;

import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.escritorio.tema.Colores;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import javax.swing.JComponent;

/** Arco de 180° con el porcentaje de lo consumido; lleno desde el 100 %, y el número muestra el exceso (sin rojo). */
public final class MedioCirculo extends JComponent {
	private final String titulo;
	private final String unidad;
	private final Color color;
	private double consumido;
	private double objetivo;

	public MedioCirculo(String titulo, String unidad, Color color) {
		this.titulo = titulo;
		this.unidad = unidad;
		this.color = color;
		setPreferredSize(new Dimension(200, 150));
	}

	/** objetivo <= 0 significa sin objetivo: el arco queda vacío. */
	public void valores(double consumido, double objetivo) {
		this.consumido = consumido;
		this.objetivo = objetivo;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g0) {
		Graphics2D g = (Graphics2D) g0.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int w = getWidth(), h = getHeight();
		int grosor = 14;
		// bajo el centro del arco van dos renglones (hasta ~40 px): el diámetro se achica para que entren
		int d = Math.max(0, Math.min(w - 2 * grosor, 2 * (h - grosor - 40)));
		int x = (w - d) / 2, y = grosor;
		g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		g.setColor(Colores.de("barra"));
		g.draw(new Arc2D.Double(x, y, d, d, 180, -180, Arc2D.OPEN));
		g.setColor(color);
		g.draw(new Arc2D.Double(x, y, d, d, 180, -180 * Cuentas.llenado(consumido, objetivo), Arc2D.OPEN));
		g.setColor(Colores.de("texto"));
		g.setFont(getFont().deriveFont(Font.BOLD, 20f));
		centrado(g, Cuentas.textoPorcentaje(consumido, objetivo), w / 2, y + d / 2 - 6);
		g.setFont(getFont().deriveFont(Font.BOLD, 13f));
		centrado(g, titulo, w / 2, y + d / 2 + 14);
		g.setColor(Colores.de("secundario"));
		g.setFont(getFont().deriveFont(12f));
		String detalle = Math.round(consumido) + (objetivo > 0 ? " / " + Math.round(objetivo) : "") + " " + unidad;
		centrado(g, detalle, w / 2, y + d / 2 + 32);
		g.dispose();
	}

	private static void centrado(Graphics2D g, String t, int cx, int baseline) {
		FontMetrics fm = g.getFontMetrics();
		g.drawString(t, cx - fm.stringWidth(t) / 2, baseline);
	}
}
