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
final class DialogoAlimentoNuevo {
	private DialogoAlimentoNuevo() {
	}

	/** El alimento creado (id nuevo, no base), o null si se canceló. Repite el pedido mientras haya datos inválidos. */
	static Alimento pedir(Component padre, String nombreInicial) {
		JTextField nombre = new JTextField(nombreInicial, 20);
		JTextField[] v = {new JTextField("0"), new JTextField("0"), new JTextField("0"), new JTextField("0")};
		JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
		p.add(new JLabel("Nombre"));
		p.add(nombre);
		String[] et = {"kcal por 100 g", "Proteínas (g) por 100 g", "Carbohidratos (g) por 100 g", "Grasas (g) por 100 g"};
		for (int i = 0; i < 4; i++) {
			p.add(new JLabel(et[i]));
			p.add(v[i]);
		}
		while (JOptionPane.showConfirmDialog(padre, p, "Alimento nuevo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
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
				return new Alimento(UUID.randomUUID().toString(), nombre.getText().trim(), new Macros(n[0], n[1], n[2], n[3]), null, null, false);
			}
			JOptionPane.showMessageDialog(padre, "Escribí un nombre y números (0 o más) en los cuatro valores.");
		}
		return null;
	}
}
