package com.caloreitor.escritorio.comidas;

import com.caloreitor.app.logica.Busqueda;
import com.caloreitor.app.logica.Busqueda.Resultado;
import com.caloreitor.app.logica.Parseo;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Receta;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.escritorio.Textos;
import com.caloreitor.escritorio.tema.Colores;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Agregar comida rápido: escribir, Enter (agrega con los gramos sugeridos). Los gramos se editan en el campo de abajo
 * (Tab) antes del Enter. El diálogo queda abierto para seguir agregando; Esc lo cierra.
 */
public final class BuscadorRapido {
	/** Un renglón de la lista: un resultado o la opción de crear. */
	private record Opcion(Resultado resultado, String crear) {
		@Override
		public String toString() {
			if (resultado == null) return "Crear «" + crear + "»…";
			return resultado.nombre() + (resultado.esReceta() ? "  (receta)" : "");
		}
	}

	private final Sesion sesion;
	private final LocalDate fecha;
	private final String comida;
	private final Runnable alCambiar;
	private final JDialog dialogo;
	private final JTextField campo = new JTextField();
	private final DefaultListModel<Opcion> modelo = new DefaultListModel<>();
	private final JList<Opcion> lista = new JList<>(modelo);
	private final JTextField gramos = new JTextField("100", 6);
	private final JLabel estado = new JLabel(" ");
	private Busqueda busqueda;

	private BuscadorRapido(Window padre, Sesion sesion, LocalDate fecha, String comida, Runnable alCambiar) {
		this.sesion = sesion;
		this.fecha = fecha;
		this.comida = comida;
		this.alCambiar = alCambiar;
		dialogo = new JDialog(padre, "Agregar a " + comida.toLowerCase(), JDialog.DEFAULT_MODALITY_TYPE);
		lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JPanel abajo = new JPanel(new FlowLayout(FlowLayout.LEFT));
		abajo.add(new JLabel("Gramos:"));
		abajo.add(gramos);
		abajo.add(estado);
		JPanel contenido = new JPanel(new BorderLayout(0, 8));
		contenido.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		contenido.add(campo, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(lista);
		scroll.setPreferredSize(new Dimension(460, 280));
		contenido.add(scroll, BorderLayout.CENTER);
		contenido.add(abajo, BorderLayout.SOUTH);
		dialogo.setContentPane(contenido);
		dialogo.pack();
		dialogo.setLocationRelativeTo(padre);

		campo.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { buscar(); }
			public void removeUpdate(DocumentEvent e) { buscar(); }
			public void changedUpdate(DocumentEvent e) { buscar(); }
		});
		KeyAdapter teclas = new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE) dialogo.dispose();
				else if (e.getKeyCode() == KeyEvent.VK_ENTER) agregar();
				else if (e.getSource() == campo && (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP)) {
					int i = lista.getSelectedIndex() + (e.getKeyCode() == KeyEvent.VK_DOWN ? 1 : -1);
					if (i >= 0 && i < modelo.size()) lista.setSelectedIndex(i);
					e.consume();
				}
			}
		};
		campo.addKeyListener(teclas);
		gramos.addKeyListener(teclas);
		lista.addListSelectionListener(e -> sugerirGramos());
		lista.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (lista.locationToIndex(e.getPoint()) >= 0) agregar();
			}
		});
		// Al abrir, el foco va al campo de búsqueda (la primera ventana ya tiene otro por defecto).
		dialogo.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent e) { campo.requestFocusInWindow(); }
		});
		busqueda = new Busqueda(sesion.todosLosAlimentos(), sesion.repo().recetas(), sesion.consumosRecientes());
		buscar();
	}

	/** Abre el buscador (modal) para agregar a esa fecha y comida; alCambiar se llama después de cada alta. */
	public static void abrir(Window padre, Sesion sesion, LocalDate fecha, String comida, Runnable alCambiar) {
		new BuscadorRapido(padre, sesion, fecha, comida, alCambiar).dialogo.setVisible(true);
	}

	private void buscar() {
		String texto = campo.getText();
		modelo.clear();
		for (Resultado r : busqueda.buscar(texto, comida, sesion.hoy())) modelo.addElement(new Opcion(r, null));
		if (!texto.isBlank()) modelo.addElement(new Opcion(null, texto.trim()));
		if (!modelo.isEmpty()) lista.setSelectedIndex(0);
		sugerirGramos();
	}

	private void sugerirGramos() {
		Opcion o = lista.getSelectedValue();
		boolean receta = o != null && o.resultado() != null && o.resultado().esReceta();
		gramos.setEnabled(!receta);
		if (o == null) return;
		if (receta) estado.setText("Receta: los gramos de cada ingrediente se editan en la lista");
		else {
			estado.setText(" ");
			gramos.setText(o.resultado() == null ? "100" : formato(o.resultado().gramosSugeridos()));
		}
	}

	private static String formato(double g) {
		return g == Math.rint(g) ? String.valueOf((long) g) : String.format(Locale.ROOT, "%.1f", g);
	}

	private void agregar() {
		Opcion o = lista.getSelectedValue();
		if (o == null) return;
		Consumo c;
		if (o.resultado() != null && o.resultado().esReceta()) {
			c = deReceta(o.resultado().id());
			if (c == null) return;
		} else {
			OptionalDouble g = Parseo.gramos(gramos.getText());
			if (g.isEmpty()) {
				estado.setForeground(Colores.de("error"));
				estado.setText(Textos.GRAMOS_INVALIDOS);
				return;
			}
			Alimento a = o.resultado() == null ? DialogoAlimentoNuevo.pedir(dialogo, o.crear()) : porId(o.resultado().id());
			if (a == null) return;
			if (o.resultado() == null) sesion.repo().guardar(a);
			c = new Consumo(UUID.randomUUID().toString(), fecha.toString(), comida, null, null,
					List.of(new Item(a.id(), a.nombre(), a.por100(), g.getAsDouble())));
		}
		sesion.repo().guardar(c);
		alCambiar.run();
		estado.setForeground(Colores.de("secundario"));
		estado.setText("Agregado: " + (c.recetaNombre() != null ? c.recetaNombre() : c.items().get(0).nombre()));
		// el alimento creado ahora también se encuentra
		busqueda = new Busqueda(sesion.todosLosAlimentos(), sesion.repo().recetas(), sesion.consumosRecientes());
		campo.setText("");
		campo.requestFocusInWindow();
	}

	private Alimento porId(String id) {
		return sesion.todosLosAlimentos().stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
	}

	/** Un solo consumo con los ingredientes como ítems (los que ya no existen como alimento se saltean). */
	private Consumo deReceta(String id) {
		Receta r = sesion.repo().recetas().stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
		if (r == null) return null;
		List<Item> items = new ArrayList<>();
		for (ItemReceta ing : r.ingredientes()) {
			Alimento a = porId(ing.alimentoId());
			if (a != null) items.add(new Item(a.id(), a.nombre(), a.por100(), ing.gramos()));
		}
		if (items.isEmpty()) {
			estado.setText("La receta no tiene ingredientes disponibles");
			return null;
		}
		return new Consumo(UUID.randomUUID().toString(), fecha.toString(), comida, r.id(), r.nombre(), items);
	}
}
