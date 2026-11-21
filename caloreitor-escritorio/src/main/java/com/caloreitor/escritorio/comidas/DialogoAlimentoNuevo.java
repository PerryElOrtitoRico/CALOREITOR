package com.caloreitor.escritorio.comidas;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Macros;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.UUID;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Pide nombre y valores por 100 g de un alimento propio nuevo. */
public final class DialogoAlimentoNuevo {
	private DialogoAlimentoNuevo() {
	}

	/** El alimento creado (id nuevo, no base), o null si se canceló. Repite el pedido mientras haya datos inválidos. */
	public static Alimento pedir(Component padre, String nombreInicial) {
		return pedir(padre, nombreInicial, null);
	}

	/**
	 * Con {@code previo} edita: el resultado conserva el id si es propio y trae id nuevo si era base (la edición de uno
	 * base queda como copia propia con el mismo nombre, que la búsqueda prefiere).
	 */
	public static Alimento editar(Component padre, Alimento previo) {
		return pedir(padre, previo.nombre(), previo);
	}

	private static Alimento pedir(Component padre, String nombreInicial, Alimento previo) {
		JTextField nombre = new JTextField(nombreInicial, 20);
		JTextField[] v = {new JTextField("0"), new JTextField("0"), new JTextField("0"), new JTextField("0")};
		if (previo != null) {
			Macros m = previo.por100();
			double[] ini = {m.kcal(), m.proteinas(), m.carbohidratos(), m.grasas()};
			for (int i = 0; i < 4; i++) v[i].setText(String.valueOf(Math.round(ini[i] * 100) / 100.0).replaceAll("\\.0$", ""));
		}
		JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
		p.add(new JLabel("Nombre"));
		p.add(nombre);
		String[] et = {"kcal por 100 g", "Proteínas (g) por 100 g", "Carbohidratos (g) por 100 g", "Grasas (g) por 100 g"};
		for (int i = 0; i < 4; i++) {
			p.add(new JLabel(et[i]));
			p.add(v[i]);
		}
		while (JOptionPane.showConfirmDialog(padre, p, previo == null ? "Alimento nuevo" : "Editar alimento", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
			double[] n = new double[4];
			boolean ok = !nombre.getText().isBlank();
			for (int i = 0; i < 4 && ok; i++) {
				try {
					n[i] = Double.parseDouble(v[i].getText().trim().replace(',', '.'));
					ok = n[i] >= 0 && !Double.isInfinite(n[i]) && !Double.isNaN(n[i]);
				} catch (NumberFormatException e) {
					ok = false;
				}
			}
			if (ok) {
				String id = previo != null && !previo.base() ? previo.id() : UUID.randomUUID().toString();
				return new Alimento(id, nombre.getText().trim(), new Macros(n[0], n[1], n[2], n[3]),
						previo == null ? null : previo.porcionNombre(), previo == null ? null : previo.porcionGramos(), false);
			}
			JOptionPane.showMessageDialog(padre, "Escribí un nombre y números (0 o más) en los cuatro valores.");
		}
		return null;
	}
}
