package com.caloreitor.escritorio.planificador;

import com.caloreitor.app.logica.Planificador;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.PlanSemanal;
import com.caloreitor.escritorio.Cuentas;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.escritorio.Textos;
import com.caloreitor.escritorio.comidas.BuscadorRapido;
import com.caloreitor.escritorio.tema.Colores;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** La semana lunes→domingo: objetivo fijado de cada día, comidas planeadas, falta/sobra y lista del súper. */
public final class PestanaPlanificador extends JPanel {
	private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d/M", Locale.forLanguageTag("es"));
	private static final DateTimeFormatter CORTO = DateTimeFormatter.ofPattern("d/M", Locale.forLanguageTag("es"));

	private final Sesion sesion;
	private final Runnable alCambiar;
	private final JLabel titulo = new JLabel(" ", JLabel.CENTER);
	private final JPanel contenido = new JPanel(new GridBagLayout());
	private final JButton recalcular = new JButton("Recalcular objetivos");
	private final JButton superLista = new JButton("Lista del súper…");
	private LocalDate lunes;

	public PestanaPlanificador(Sesion sesion, Runnable alCambiar) {
		super(new BorderLayout(0, 8));
		this.sesion = sesion;
		this.alCambiar = alCambiar;
		this.lunes = sesion.hoy().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		JButton anterior = new JButton("◀"), siguiente = new JButton("▶"), hoy = new JButton("Esta semana");
		anterior.addActionListener(e -> ir(lunes.minusWeeks(1)));
		siguiente.addActionListener(e -> ir(lunes.plusWeeks(1)));
		hoy.addActionListener(e -> ir(sesion.hoy().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));
		recalcular.addActionListener(e -> {
			if (sesion.mutar(this, () -> planificador().recalcular(plan()))) alCambiar.run();
		});
		superLista.addActionListener(e -> listaDelSuper());
		JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		izq.add(anterior);
		izq.add(siguiente);
		izq.add(hoy);
		JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		der.add(recalcular);
		der.add(superLista);
		JPanel barra = new JPanel(new BorderLayout(8, 0));
		barra.add(izq, BorderLayout.WEST);
		barra.add(titulo, BorderLayout.CENTER);
		barra.add(der, BorderLayout.EAST);
		add(barra, BorderLayout.NORTH);
		JPanel envoltura = new JPanel(new BorderLayout());
		envoltura.add(contenido, BorderLayout.NORTH);
		add(new JScrollPane(envoltura), BorderLayout.CENTER);
	}

	private void ir(LocalDate nuevoLunes) {
		lunes = nuevoLunes;
		refrescar();
	}

	/** Objetivos nuevo en cada uso: la ventana los reemplaza al releer. */
	private Planificador planificador() {
		return new Planificador(sesion.repo(), sesion.objetivos());
	}

	private PlanSemanal plan() {
		return sesion.repo().planSemanal(lunes.toString()).orElseThrow();
	}

	public void refrescar() {
		titulo.setText("Semana del " + CORTO.format(lunes) + " al " + CORTO.format(lunes.plusDays(6)));
		contenido.removeAll();
		boolean hay = sesion.repo().planSemanal(lunes.toString()).isPresent();
		recalcular.setEnabled(hay);
		superLista.setEnabled(hay);
		if (!hay) {
			JButton armar = new JButton("Armar semana");
			armar.addActionListener(e -> {
				if (sesion.mutar(this, () -> planificador().crear(lunes))) alCambiar.run();
			});
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
			p.add(new JLabel("Esta semana todavía no está armada."));
			p.add(armar);
			fila(p, 0);
		} else {
			PlanSemanal plan = plan();
			int fila = 0;
			for (int d = 0; d < 7; d++) fila = dibujarDia(plan, lunes.plusDays(d), fila);
		}
		contenido.revalidate();
		contenido.repaint();
	}

	private int dibujarDia(PlanSemanal plan, LocalDate f, int fila) {
		Configuracion cfg = sesion.repo().configuracion();
		Objetivo obj = plan.objetivos().get(f.toString());
		Macros planeado = planificador().planeado(plan, f.toString());
		JLabel cab = new JLabel(DIA.format(f) + (f.equals(sesion.hoy()) ? "  (hoy)" : ""));
		cab.putClientProperty("FlatLaf.styleClass", "h3");
		JLabel resumen = new JLabel(obj == null ? "Sin objetivo: " + Textos.SIN_DATOS_PERSONALES
				: "Objetivo " + texto(obj.macros()) + "   ·   Planeado " + texto(planeado) + "   ·   " + faltaSobra(Cuentas.faltaSobra(obj.macros(), planeado)));
		resumen.setForeground(Colores.de("secundario"));
		JPanel cabecera = new JPanel(new BorderLayout());
		cabecera.add(cab, BorderLayout.NORTH);
		cabecera.add(resumen, BorderLayout.SOUTH);
		cabecera.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colores.de("borde")));
		fila(cabecera, fila++);
		// las comidas configuradas y, al final, las que ya tienen algo planeado pero se sacaron de la lista
		Set<String> comidas = new LinkedHashSet<>(cfg.comidas());
		for (Consumo c : plan.comidas()) if (c.fecha().equals(f.toString())) comidas.add(c.comida());
		for (String comida : comidas) {
			List<Consumo> deEsta = plan.comidas().stream().filter(c -> c.fecha().equals(f.toString()) && c.comida().equals(comida)).toList();
			JButton agregar = new JButton("+ Agregar");
			agregar.addActionListener(e -> BuscadorRapido.abrir(SwingUtilities.getWindowAncestor(this), sesion, f, comida, alCambiar,
					c -> planificador().agregar(plan(), c)));
			JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
			p.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
			p.add(new JLabel(comida));
			p.add(agregar);
			if (f.equals(sesion.hoy()) && !deEsta.isEmpty()) {
				var reales = sesion.repo().consumos(f.toString()).stream().filter(c -> c.comida().equals(comida)).toList();
				boolean hecho = Cuentas.yaComido(deEsta, reales);
				JButton comi = new JButton(hecho ? "Ya registrado" : "Comí lo planeado");
				comi.setEnabled(!hecho);
				comi.addActionListener(e -> {
					comi.setEnabled(false);
					if (sesion.mutar(this, () -> deEsta.forEach(c -> planificador().comiLoPlaneado(plan(), c.id())))) alCambiar.run();
					else comi.setEnabled(true);
				});
				p.add(comi);
			}
			fila(p, fila++);
			for (Consumo c : deEsta) fila(renglon(c), fila++);
		}
		return fila;
	}

	private JPanel renglon(Consumo c) {
		String que = c.recetaNombre() != null ? "Receta " + c.recetaNombre() + ": " : "";
		String items = c.items().stream().map(i -> i.nombre() + " " + gramos(i)).collect(Collectors.joining(", "));
		JButton quitar = new JButton("✕");
		quitar.setToolTipText("Sacar del plan");
		quitar.addActionListener(e -> {
			if (sesion.mutar(this, () -> planificador().quitar(plan(), c.id()))) alCambiar.run();
		});
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		p.setBorder(BorderFactory.createEmptyBorder(0, 28, 0, 0));
		p.add(new JLabel(que + items + " — " + Math.round(c.macros().kcal()) + " kcal"));
		p.add(quitar);
		return p;
	}

	private static String gramos(Item i) {
		return String.valueOf(Math.round(i.gramos() * 10) / 10.0).replaceAll("\\.0$", "") + " g";
	}

	private void listaDelSuper() {
		PlanSemanal plan = plan();
		JTextArea texto = new JTextArea(14, 36);
		texto.setEditable(false);
		JPanel dias = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		List<JCheckBox> marcas = new ArrayList<>();
		Runnable actualizar = () -> {
			Set<String> fechas = new TreeSet<>();
			for (int d = 0; d < 7; d++) if (marcas.get(d).isSelected()) fechas.add(lunes.plusDays(d).toString());
			String t = Planificador.comoTexto(planificador().listaSuper(plan, fechas));
			texto.setText(t.isEmpty() ? "(No hay nada planeado en los días elegidos)" : t);
		};
		for (int d = 0; d < 7; d++) {
			JCheckBox m = new JCheckBox(lunes.plusDays(d).getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("es")), true);
			m.addActionListener(e -> actualizar.run());
			marcas.add(m);
			dias.add(m);
		}
		JButton copiar = new JButton("Copiar");
		copiar.addActionListener(e -> {
			try {
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto.getText()), null);
			} catch (IllegalStateException ex) {
				JOptionPane.showMessageDialog(this, "No se pudo copiar: " + ex.getMessage());
			}
		});
		JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this), "Lista del súper", JDialog.DEFAULT_MODALITY_TYPE);
		JPanel c = new JPanel(new BorderLayout(0, 8));
		c.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		c.add(dias, BorderLayout.NORTH);
		c.add(new JScrollPane(texto), BorderLayout.CENTER);
		JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		abajo.add(copiar);
		c.add(abajo, BorderLayout.SOUTH);
		d.setContentPane(c);
		actualizar.run();
		d.pack();
		d.setLocationRelativeTo(this);
		d.setVisible(true);
	}

	private static String texto(Macros m) {
		return Math.round(m.kcal()) + " kcal · P " + Math.round(m.proteinas()) + " · C " + Math.round(m.carbohidratos()) + " · G " + Math.round(m.grasas());
	}

	/** "falta 300 kcal" / "sobra 12" por macro; vale 0 cuando justo da. */
	private static String faltaSobra(Macros m) {
		return "kcal " + fs(m.kcal()) + " · P " + fs(m.proteinas()) + " · C " + fs(m.carbohidratos()) + " · G " + fs(m.grasas());
	}

	private static String fs(double v) {
		long r = Math.round(v);
		return r > 0 ? "falta " + r : r < 0 ? "sobra " + (-r) : "justo";
	}

	private void fila(java.awt.Component c, int fila) {
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = fila;
		gc.weightx = 1;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.insets = new Insets(fila == 0 ? 0 : 2, 0, 2, 0);
		contenido.add(c, gc);
	}
}
