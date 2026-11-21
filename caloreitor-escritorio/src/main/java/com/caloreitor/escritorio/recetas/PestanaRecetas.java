package com.caloreitor.escritorio.recetas;

import com.caloreitor.app.logica.Parseo;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Receta;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.escritorio.Textos;
import com.caloreitor.escritorio.comidas.BuscadorRapido;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/** Lista de recetas con totales; crear, editar (nombre e ingredientes con gramos) y borrar con confirmación. */
public final class PestanaRecetas extends JPanel {
	private final Sesion sesion;
	private final Runnable alCambiar;
	private final DefaultListModel<Receta> modelo = new DefaultListModel<>();
	private final JList<Receta> lista = new JList<>(modelo);
	private final JButton editar = new JButton("Editar");
	private final JButton borrar = new JButton("Borrar");

	public PestanaRecetas(Sesion sesion, Runnable alCambiar) {
		super(new BorderLayout(0, 8));
		this.sesion = sesion;
		this.alCambiar = alCambiar;
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setCellRenderer((l, r, i, sel, foco) -> {
			JLabel e = new JLabel(r.nombre() + " — " + texto(Cuentas.totalReceta(r.ingredientes(), sesion.todosLosAlimentos()))
					+ " (" + r.ingredientes().size() + " ingredientes)");
			e.setOpaque(true);
			e.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
			e.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
			e.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
			return e;
		});
		JButton nueva = new JButton("Nueva…");
		nueva.addActionListener(e -> abrirEditor(null));
		editar.addActionListener(e -> abrirEditor(lista.getSelectedValue()));
		borrar.addActionListener(e -> borrar());
		lista.addListSelectionListener(e -> botones());
		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		acciones.add(nueva);
		acciones.add(editar);
		acciones.add(borrar);
		add(acciones, BorderLayout.NORTH);
		add(new JScrollPane(lista), BorderLayout.CENTER);
		refrescar();
	}

	static String texto(Macros m) {
		return Math.round(m.kcal()) + " kcal · P " + Math.round(m.proteinas()) + " · C " + Math.round(m.carbohidratos()) + " · G " + Math.round(m.grasas());
	}

	private void botones() {
		editar.setEnabled(lista.getSelectedValue() != null);
		borrar.setEnabled(lista.getSelectedValue() != null);
	}

	public void refrescar() {
		String sel = lista.getSelectedValue() == null ? null : lista.getSelectedValue().id();
		modelo.clear();
		sesion.repo().recetas().forEach(modelo::addElement);
		for (int i = 0; i < modelo.size(); i++) if (modelo.get(i).id().equals(sel)) lista.setSelectedIndex(i);
		botones();
	}

	private void borrar() {
		Receta r = lista.getSelectedValue();
		if (r == null) return;
		if (JOptionPane.showConfirmDialog(this, "¿Borrar la receta «" + r.nombre() + "»? Lo ya registrado no cambia.", Textos.TITULO,
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
		if (sesion.mutar(this, () -> sesion.repo().borrarReceta(r.id()))) alCambiar.run();
	}

	private void abrirEditor(Receta previa) {
		Receta nueva = new Editor(SwingUtilities.getWindowAncestor(this), previa).pedir();
		if (nueva != null && sesion.mutar(this, () -> sesion.repo().guardar(nueva))) alCambiar.run();
	}

	/** Diálogo modal con el nombre y los ingredientes; trabaja sobre una copia hasta apretar Guardar. */
	private final class Editor {
		private final JDialog dialogo;
		private final Receta previa;
		private final JTextField nombre = new JTextField(24);
		private final JPanel filas = new JPanel(new GridBagLayout());
		private final JLabel total = new JLabel(" ");
		private final List<ItemReceta> ingredientes = new ArrayList<>();
		private Receta resultado;

		Editor(Window padre, Receta previa) {
			this.previa = previa;
			dialogo = new JDialog(padre, previa == null ? "Receta nueva" : "Editar receta", JDialog.DEFAULT_MODALITY_TYPE);
			if (previa != null) {
				nombre.setText(previa.nombre());
				ingredientes.addAll(previa.ingredientes());
			}
			JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
			arriba.add(new JLabel("Nombre"));
			arriba.add(nombre);
			JButton agregar = new JButton("+ Ingrediente");
			agregar.addActionListener(e -> BuscadorRapido.abrir(dialogo, sesion, sesion.hoy(), "la receta", this::dibujar, this::agregarIngredientes));
			JButton guardar = new JButton("Guardar");
			guardar.addActionListener(e -> guardar());
			JButton cancelar = new JButton("Cancelar");
			cancelar.addActionListener(e -> dialogo.dispose());
			JPanel abajo = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			abajo.add(total);
			abajo.add(agregar);
			abajo.add(guardar);
			abajo.add(cancelar);
			JPanel envoltura = new JPanel(new BorderLayout());
			envoltura.add(filas, BorderLayout.NORTH);
			JScrollPane scroll = new JScrollPane(envoltura);
			scroll.setPreferredSize(new java.awt.Dimension(560, 300));
			JPanel contenido = new JPanel(new BorderLayout(0, 8));
			contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
			contenido.add(arriba, BorderLayout.NORTH);
			contenido.add(scroll, BorderLayout.CENTER);
			contenido.add(abajo, BorderLayout.SOUTH);
			dialogo.setContentPane(contenido);
			dialogo.getRootPane().setDefaultButton(guardar);
			dialogo.pack();
			dialogo.setLocationRelativeTo(padre);
			dibujar();
		}

		Receta pedir() {
			dialogo.setVisible(true);
			return resultado;
		}

		/** Un resultado del buscador (alimento o receta ya desarmada en sus ítems) se suma como ingredientes. */
		private void agregarIngredientes(com.caloreitor.app.modelo.Consumo c) {
			for (Item i : c.items()) ingredientes.add(new ItemReceta(i.alimentoId(), i.gramos()));
		}

		private void guardar() {
			if (nombre.getText().isBlank()) {
				JOptionPane.showMessageDialog(dialogo, "Escribí un nombre para la receta");
				return;
			}
			if (ingredientes.isEmpty()) {
				JOptionPane.showMessageDialog(dialogo, "Agregá al menos un ingrediente");
				return;
			}
			resultado = new Receta(previa == null ? UUID.randomUUID().toString() : previa.id(), nombre.getText().trim(), List.copyOf(ingredientes));
			dialogo.dispose();
		}

		private void dibujar() {
			filas.removeAll();
			List<Alimento> todos = sesion.todosLosAlimentos();
			for (int i = 0; i < ingredientes.size(); i++) {
				int pos = i;
				ItemReceta ing = ingredientes.get(i);
				String nom = todos.stream().filter(a -> a.id().equals(ing.alimentoId())).map(Alimento::nombre).findFirst().orElse("(alimento borrado)");
				String mostrado = String.valueOf(Math.round(ing.gramos() * 10) / 10.0).replaceAll("\\.0$", "");
				JTextField g = new JTextField(mostrado, 6);
				Runnable cambiar = () -> {
					if (g.getText().equals(mostrado)) return;
					OptionalDouble n = Parseo.gramos(g.getText());
					if (n.isEmpty()) JOptionPane.showMessageDialog(dialogo, Textos.GRAMOS_INVALIDOS);
					else ingredientes.set(pos, new ItemReceta(ing.alimentoId(), n.getAsDouble()));
					SwingUtilities.invokeLater(this::dibujar);
				};
				g.addActionListener(e -> cambiar.run());
				g.addFocusListener(new java.awt.event.FocusAdapter() {
					@Override
					public void focusLost(java.awt.event.FocusEvent e) { if (!e.isTemporary()) cambiar.run(); }
				});
				JButton quitar = new JButton("✕");
				quitar.setToolTipText("Sacar el ingrediente");
				quitar.addActionListener(e -> {
					ingredientes.remove(pos);
					dibujar();
				});
				JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
				p.add(new JLabel(nom));
				p.add(g);
				p.add(new JLabel("g"));
				p.add(quitar);
				GridBagConstraints gc = new GridBagConstraints();
				gc.gridy = i;
				gc.weightx = 1;
				gc.fill = GridBagConstraints.HORIZONTAL;
				gc.insets = new Insets(0, 0, 0, 0);
				filas.add(p, gc);
			}
			total.setText("Total: " + texto(Cuentas.totalReceta(ingredientes, todos)));
			filas.revalidate();
			filas.repaint();
		}
	}
}
