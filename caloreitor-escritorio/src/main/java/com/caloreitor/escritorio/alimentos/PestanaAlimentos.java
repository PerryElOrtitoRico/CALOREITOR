package com.caloreitor.escritorio.alimentos;

import com.caloreitor.app.logica.Busqueda;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.escritorio.Cuentas;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.escritorio.Textos;
import com.caloreitor.escritorio.comidas.DialogoAlimentoNuevo;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Base y alimentos propios con buscador: crear, editar (uno base queda como copia propia) y borrar propios. */
public final class PestanaAlimentos extends JPanel {
	private final Sesion sesion;
	private final Runnable alCambiar;
	private final JTextField buscar = new JTextField();
	private final DefaultListModel<Alimento> modelo = new DefaultListModel<>();
	private final JList<Alimento> lista = new JList<>(modelo);
	private final JButton editar = new JButton("Editar");
	private final JButton borrar = new JButton("Borrar");

	public PestanaAlimentos(Sesion sesion, Runnable alCambiar) {
		super(new BorderLayout(0, 8));
		this.sesion = sesion;
		this.alCambiar = alCambiar;
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lista.setCellRenderer((l, a, i, sel, foco) -> {
			JLabel e = new JLabel(texto(a));
			e.setOpaque(true);
			e.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
			e.setBackground(sel ? l.getSelectionBackground() : l.getBackground());
			e.setForeground(sel ? l.getSelectionForeground() : l.getForeground());
			return e;
		});
		buscar.putClientProperty("JTextField.placeholderText", "Buscar alimento");
		buscar.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { refrescar(); }
			public void removeUpdate(DocumentEvent e) { refrescar(); }
			public void changedUpdate(DocumentEvent e) { refrescar(); }
		});
		JButton nuevo = new JButton("Nuevo…");
		nuevo.addActionListener(e -> nuevo());
		editar.addActionListener(e -> editar());
		borrar.addActionListener(e -> borrar());
		lista.addListSelectionListener(e -> botones());
		JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		acciones.add(nuevo);
		acciones.add(editar);
		acciones.add(borrar);
		JPanel arriba = new JPanel(new BorderLayout(0, 6));
		arriba.add(buscar, BorderLayout.NORTH);
		arriba.add(acciones, BorderLayout.SOUTH);
		add(arriba, BorderLayout.NORTH);
		add(new JScrollPane(lista), BorderLayout.CENTER);
		refrescar();
	}

	private static String texto(Alimento a) {
		Macros m = a.por100();
		return a.nombre() + (a.base() ? "  (base)" : "") + " — " + Math.round(m.kcal()) + " kcal · P " + Math.round(m.proteinas())
				+ " · C " + Math.round(m.carbohidratos()) + " · G " + Math.round(m.grasas()) + "  (por 100 g)";
	}

	private void botones() {
		Alimento a = lista.getSelectedValue();
		editar.setEnabled(a != null);
		borrar.setEnabled(a != null && !a.base());
	}

	public void refrescar() {
		Alimento sel = lista.getSelectedValue();
		List<Alimento> visibles = Cuentas.visibles(sesion.todosLosAlimentos());
		List<Alimento> mostrar;
		if (buscar.getText().isBlank()) {
			mostrar = visibles.stream().sorted(Comparator.comparing(a -> a.nombre().toLowerCase())).toList();
		} else {
			// la misma búsqueda que la carga rápida: sin tildes y por palabras
			Map<String, Alimento> porId = visibles.stream().collect(Collectors.toMap(Alimento::id, a -> a, (x, y) -> x));
			mostrar = new Busqueda(visibles, List.of(), List.of()).buscar(buscar.getText(), "", LocalDate.now()).stream()
					.map(r -> porId.get(r.id())).toList();
		}
		modelo.clear();
		mostrar.forEach(modelo::addElement);
		if (sel != null) lista.setSelectedIndex(mostrar.indexOf(sel));
		botones();
	}

	private void nuevo() {
		Alimento a = DialogoAlimentoNuevo.pedir(this, "");
		if (a != null && sesion.mutar(this, () -> sesion.repo().guardar(a))) alCambiar.run();
	}

	private void editar() {
		Alimento previo = lista.getSelectedValue();
		if (previo == null) return;
		Alimento a = DialogoAlimentoNuevo.editar(this, previo);
		if (a != null && sesion.mutar(this, () -> sesion.repo().guardar(a))) alCambiar.run();
	}

	private void borrar() {
		Alimento a = lista.getSelectedValue();
		if (a == null || a.base()) return;
		if (JOptionPane.showConfirmDialog(this, "¿Borrar «" + a.nombre() + "»? Lo ya registrado no cambia.", Textos.TITULO,
				JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
		if (sesion.mutar(this, () -> sesion.repo().borrarAlimento(a.id()))) alCambiar.run();
	}
}
