package com.caloreitor.escritorio.hoy;

import com.caloreitor.app.logica.Asignacion;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Modo;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.app.logica.Textos;
import com.caloreitor.escritorio.comidas.BuscadorRapido;
import com.caloreitor.escritorio.tema.Colores;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Resumen del día de hoy: kcal y macros contra el objetivo, entrenamiento y acceso rápido a agregar comida. */
public final class PestanaHoy extends JPanel {
	private final Sesion sesion;
	private final Runnable alCambiar;
	private final MedioCirculo kcal = new MedioCirculo("kcal", "kcal", Colores.de("disco_azul"));
	private final MedioCirculo prot = new MedioCirculo("Proteínas", "g", Colores.de("disco_verde"));
	private final MedioCirculo carb = new MedioCirculo("Carbohidratos", "g", Colores.de("disco_amarillo"));
	private final MedioCirculo gras = new MedioCirculo("Grasas", "g", Colores.de("disco_violeta"));
	private final JLabel modo = new JLabel(" ");
	private final JLabel entrenamiento = new JLabel(" ");
	private final JLabel aviso = new JLabel(" ");
	private final JButton agregar = new JButton();
	private String proximaComida;

	/** alCambiar se llama después de agregar algo, para que la ventana refresque todas las pestañas. */
	public PestanaHoy(Sesion sesion, Runnable alCambiar) {
		super(new BorderLayout(0, 12));
		this.sesion = sesion;
		this.alCambiar = alCambiar;
		setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		JPanel circulos = new JPanel(new GridLayout(1, 4, 8, 0));
		circulos.setOpaque(false);
		for (MedioCirculo m : List.of(kcal, prot, carb, gras)) circulos.add(m);
		JPanel texto = new JPanel(new GridLayout(0, 1, 0, 4));
		texto.setOpaque(false);
		aviso.setForeground(Colores.de("error"));
		texto.add(modo);
		texto.add(entrenamiento);
		texto.add(aviso);
		JPanel boton = new JPanel(new FlowLayout(FlowLayout.LEFT));
		boton.setOpaque(false);
		boton.add(agregar);
		JPanel centro = new JPanel(new BorderLayout(0, 12));
		centro.setOpaque(false);
		centro.add(circulos, BorderLayout.NORTH);
		// en NORTH los renglones quedan juntos en vez de repartirse en todo el alto
		JPanel arriba = new JPanel(new BorderLayout());
		arriba.setOpaque(false);
		arriba.add(texto, BorderLayout.NORTH);
		centro.add(arriba, BorderLayout.CENTER);
		add(centro, BorderLayout.CENTER);
		add(boton, BorderLayout.SOUTH);
		agregar.addActionListener(e -> {
			Window v = SwingUtilities.getWindowAncestor(this);
			BuscadorRapido.abrir(v, sesion, sesion.hoy(), proximaComida, alCambiar);
		});
	}

	public void refrescar() {
		String hoy = sesion.hoy().toString();
		Configuracion cfg = sesion.repo().configuracion();
		List<Consumo> consumos = sesion.repo().consumos(hoy);
		Macros comido = Macros.CERO;
		for (Consumo c : consumos) comido = comido.mas(c.macros());
		Optional<Objetivo> obj = sesion.objetivos().del(sesion.hoy());
		Macros o = obj.map(Objetivo::macros).orElse(new Macros(0, 0, 0, 0));
		kcal.valores(comido.kcal(), o.kcal());
		prot.valores(comido.proteinas(), o.proteinas());
		carb.valores(comido.carbohidratos(), o.carbohidratos());
		gras.valores(comido.grasas(), o.grasas());
		aviso.setText(obj.isEmpty() ? Textos.SIN_DATOS_PERSONALES : " ");
		modo.setText(cfg.modo() == Modo.AUTOMATICO ? "Objetivo automático" : "Objetivo manual");

		var e = Asignacion.para(sesion.hoy(), cfg.semana(), sesion.planes());
		List<String> hoyToca = new java.util.ArrayList<>();
		if (e.gym() != null) hoyToca.add("Gimnasio");
		if (e.running() != null) hoyToca.add("Running");
		if (e.plio() != null) hoyToca.add("Pliometría");
		entrenamiento.setText(hoyToca.isEmpty() ? "Descanso" : String.join(" · ", hoyToca));

		// la primera comida sin consumos; si todas tienen, la última
		List<String> comidas = cfg.comidas().isEmpty() ? Configuracion.porDefecto().comidas() : cfg.comidas();
		proximaComida = comidas.get(comidas.size() - 1);
		for (String c : comidas) {
			if (consumos.stream().noneMatch(x -> c.equals(x.comida()))) { proximaComida = c; break; }
		}
		agregar.setText("Agregar a " + proximaComida.toLowerCase());
	}
}
