package com.caloreitor.escritorio.comidas;

import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.TotalDia;
import com.caloreitor.app.logica.Parseo;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.app.logica.Textos;
import com.caloreitor.escritorio.tema.Colores;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** Las comidas de un día, navegable: agregar, editar gramos, borrar y repetir de otro día. */
public final class PestanaComidas extends JPanel {
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale.forLanguageTag("es"));

	private final Sesion sesion;
	private final Runnable alCambiar;
	private final JLabel titulo = new JLabel(" ", JLabel.CENTER);
	private final JPanel contenido = new JPanel(new GridBagLayout());
	private final JButton repetir = new JButton("Repetir de otro día…");
	private LocalDate dia;

	/** alCambiar se llama después de cada cambio, para que la ventana refresque todas las pestañas. */
	public PestanaComidas(Sesion sesion, Runnable alCambiar) {
		super(new BorderLayout(0, 8));
		this.sesion = sesion;
		this.alCambiar = alCambiar;
		this.dia = sesion.hoy();
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		JButton anterior = new JButton("◀"), siguiente = new JButton("▶"), hoy = new JButton("Hoy");
		anterior.addActionListener(e -> ir(dia.minusDays(1)));
		siguiente.addActionListener(e -> ir(dia.plusDays(1)));
		hoy.addActionListener(e -> ir(sesion.hoy()));
		repetir.addActionListener(e -> repetirDeOtroDia());
		JPanel barra = new JPanel(new BorderLayout(8, 0));
		JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		izq.add(anterior);
		izq.add(siguiente);
		izq.add(hoy);
		JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		der.add(repetir);
		barra.add(izq, BorderLayout.WEST);
		barra.add(titulo, BorderLayout.CENTER);
		barra.add(der, BorderLayout.EAST);
		add(barra, BorderLayout.NORTH);
		JPanel envoltura = new JPanel(new BorderLayout());
		envoltura.add(contenido, BorderLayout.NORTH);
		add(new JScrollPane(envoltura), BorderLayout.CENTER);
	}

	private void ir(LocalDate d) {
		dia = d;
		refrescar();
	}

	/** Más de 31 días: solo quedan los totales en el historial (igual que Depuracion). */
	private boolean archivado() {
		return dia.isBefore(sesion.hoy().minusDays(31));
	}

	public void refrescar() {
		titulo.setText(FECHA.format(dia));
		contenido.removeAll();
		repetir.setEnabled(!archivado());
		if (archivado()) dibujarArchivado();
		else dibujarDia();
		contenido.revalidate();
		contenido.repaint();
	}

	private void dibujarArchivado() {
		Optional<TotalDia> t = sesion.historial().leer(dia.toString());
		List<String> lineas = new ArrayList<>();
		if (t.isEmpty()) lineas.add("Sin datos");
		else {
			lineas.add("Consumido: " + texto(t.get().consumido()));
			Macros o = t.get().objetivo();
			lineas.add(o == null ? "Sin objetivo ese día" : "Objetivo: " + texto(o));
			lineas.add(Textos.SOLO_TOTALES);
		}
		int fila = 0;
		for (String l : lineas) fila(new JLabel(l), fila++);
	}

	private void dibujarDia() {
		var cfg = sesion.repo().configuracion();
		List<Consumo> consumos = sesion.repo().consumos(dia.toString());
		// las comidas configuradas y, al final, las que tienen consumos pero ya no están en la lista
		Set<String> comidas = new LinkedHashSet<>(cfg.comidas());
		for (Consumo c : consumos) comidas.add(c.comida());
		int fila = 0;
		for (String comida : comidas) {
			List<Consumo> deEsta = consumos.stream().filter(c -> comida.equals(c.comida())).toList();
			Macros total = Macros.CERO;
			for (Consumo c : deEsta) total = total.mas(c.macros());
			JButton agregar = new JButton("+ Agregar");
			agregar.addActionListener(e -> BuscadorRapido.abrir(SwingUtilities.getWindowAncestor(this), sesion, dia, comida, alCambiar));
			JPanel cab = new JPanel(new BorderLayout(8, 0));
			JLabel nombre = new JLabel(comida + " — " + texto(total));
			nombre.putClientProperty("FlatLaf.styleClass", "h3");
			cab.add(nombre, BorderLayout.CENTER);
			cab.add(agregar, BorderLayout.EAST);
			cab.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Colores.de("borde")));
			fila(cab, fila++);
			for (Consumo c : deEsta) fila = dibujarConsumo(c, fila);
		}
	}

	private int dibujarConsumo(Consumo c, int fila) {
		if (c.recetaNombre() != null) {
			JButton quitar = new JButton("✕");
			quitar.setToolTipText("Borrar la receta entera");
			quitar.addActionListener(e -> { if (sesion.mutar(this, () -> sesion.repo().borrarConsumo(c.id()))) alCambiar.run(); });
			JPanel p = new JPanel(new BorderLayout(8, 0));
			p.add(new JLabel("Receta: " + c.recetaNombre()), BorderLayout.CENTER);
			p.add(quitar, BorderLayout.EAST);
			fila(p, fila++);
		}
		for (int i = 0; i < c.items().size(); i++) fila(renglon(c, i), fila++);
		return fila;
	}

	private JPanel renglon(Consumo c, int pos) {
		Item it = c.items().get(pos);
		String mostrado = String.valueOf(Math.round(it.gramos() * 10) / 10.0).replaceAll("\\.0$", "");
		JTextField g = new JTextField(mostrado, 6);
		JLabel kcal = new JLabel(Math.round(it.macros().kcal()) + " kcal");
		JButton quitar = new JButton("✕");
		quitar.setToolTipText("Borrar");
		// Enter o salir del campo guarda; un valor inválido se avisa y vuelve al anterior.
		Runnable guardar = () -> {
			// el campo muestra gramos redondeados: sin tocarlo no se guarda nada (si no, 150,55 pasaría a 150,6)
			if (g.getText().equals(mostrado)) return;
			OptionalDouble nuevo = Parseo.gramos(g.getText());
			if (nuevo.isEmpty()) {
				JOptionPane.showMessageDialog(this, Textos.GRAMOS_INVALIDOS);
				refrescar();
			} else if (nuevo.getAsDouble() != it.gramos()) {
				List<Item> items = new ArrayList<>(c.items());
				items.set(pos, new Item(it.alimentoId(), it.nombre(), it.por100(), nuevo.getAsDouble()));
				if (sesion.mutar(this, () -> sesion.repo().guardar(new Consumo(c.id(), c.fecha(), c.comida(), c.recetaId(), c.recetaNombre(), items)))) alCambiar.run();
			}
		};
		g.addActionListener(e -> guardar.run());
		g.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) { if (!e.isTemporary()) guardar.run(); }
		});
		quitar.addActionListener(e -> {
			List<Item> items = new ArrayList<>(c.items());
			items.remove(pos);
			boolean ok = sesion.mutar(this, () -> {
				if (items.isEmpty()) sesion.repo().borrarConsumo(c.id());
				else sesion.repo().guardar(new Consumo(c.id(), c.fecha(), c.comida(), c.recetaId(), c.recetaNombre(), items));
			});
			if (ok) alCambiar.run();
		});
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		p.setBorder(BorderFactory.createEmptyBorder(0, c.recetaNombre() != null ? 24 : 8, 0, 0));
		p.add(new JLabel(it.nombre()));
		p.add(g);
		p.add(new JLabel("g"));
		p.add(kcal);
		p.add(quitar);
		return p;
	}

	/** Copia los consumos de una comida de otro día a este día (misma comida), con ids nuevos. */
	private void repetirDeOtroDia() {
		JTextField fecha = new JTextField(dia.minusDays(1).toString(), 10);
		JComboBox<String> comida = new JComboBox<>(sesion.repo().configuracion().comidas().toArray(new String[0]));
		JPanel p = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
		p.add(new JLabel("Día (AAAA-MM-DD)"));
		p.add(fecha);
		p.add(new JLabel("Comida"));
		p.add(comida);
		if (JOptionPane.showConfirmDialog(this, p, "Repetir de otro día", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
		String origen;
		try {
			origen = LocalDate.parse(fecha.getText().trim()).toString();
		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this, "Fecha inválida: usá AAAA-MM-DD");
			return;
		}
		String nombre = (String) comida.getSelectedItem();
		List<Consumo> fuente = sesion.repo().consumos(origen).stream().filter(c -> c.comida().equals(nombre)).toList();
		if (fuente.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Ese día no tiene consumos en " + nombre);
			return;
		}
		boolean ok = sesion.mutar(this, () -> {
			for (Consumo c : fuente) {
				sesion.repo().guardar(new Consumo(UUID.randomUUID().toString(), dia.toString(), nombre, c.recetaId(), c.recetaNombre(), c.items()));
			}
		});
		if (ok) alCambiar.run();
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

	private static String texto(Macros m) {
		return Math.round(m.kcal()) + " kcal · P " + Math.round(m.proteinas()) + " · C " + Math.round(m.carbohidratos()) + " · G " + Math.round(m.grasas());
	}
}
