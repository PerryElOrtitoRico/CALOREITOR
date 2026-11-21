package com.caloreitor.escritorio.config;

import com.caloreitor.app.red.ConexionPanel;
import com.caloreitor.app.red.PublicadorPanel;
import com.caloreitor.app.red.TransporteHttp;
import com.caloreitor.escritorio.Textos;
import com.caloreitor.escritorio.infra.AjustesLocales;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** «Conexión con el panel»: dirección, token, dispositivo y prueba; se guarda en los ajustes de este equipo. */
final class DialogoConexionPanel extends JDialog {
	private final AjustesLocales ajustes;
	private final JTextField url = new JTextField(32);
	private final JPasswordField token = new JPasswordField(32);
	private final JCheckBox mostrar = new JCheckBox("Mostrar");
	private final JTextField dispositivo = new JTextField(32);
	private final JCheckBox enviar = new JCheckBox("Enviar al panel");
	private final JButton probar = new JButton("Probar conexión");
	private final JButton guardar = new JButton("Guardar");
	private final JButton cancelar = new JButton("Cancelar");

	private DialogoConexionPanel(Component dueno, AjustesLocales ajustes) {
		super(SwingUtilities.getWindowAncestor(dueno), "Conexión con el panel", ModalityType.APPLICATION_MODAL);
		this.ajustes = ajustes;
		ConexionPanel c = ajustes.conexionPanel();
		url.setText(c.url());
		token.setText(c.token());
		dispositivo.setText(c.dispositivo());
		enviar.setSelected(c.enviar());

		JPanel f = new JPanel(new GridBagLayout());
		f.setBorder(BorderFactory.createEmptyBorder(12, 12, 6, 12));
		f.add(new JLabel("Dirección del panel:"), celda(0, 0, 1));
		f.add(url, celda(1, 0, 2));
		f.add(new JLabel("Token:"), celda(0, 1, 1));
		f.add(token, celda(1, 1, 1));
		f.add(mostrar, celda(2, 1, 1));
		f.add(new JLabel("Dispositivo:"), celda(0, 2, 1));
		f.add(dispositivo, celda(1, 2, 2));
		f.add(enviar, celda(1, 3, 2));

		JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		botones.add(probar);
		botones.add(guardar);
		botones.add(cancelar);
		setLayout(new BorderLayout());
		add(f, BorderLayout.CENTER);
		add(botones, BorderLayout.SOUTH);

		char ocultar = token.getEchoChar();
		mostrar.addActionListener(e -> token.setEchoChar(mostrar.isSelected() ? (char) 0 : ocultar));
		probar.addActionListener(e -> probarConexion());
		guardar.addActionListener(e -> guardarConexion());
		cancelar.addActionListener(e -> dispose());
		getRootPane().setDefaultButton(guardar);
		pack();
		setLocationRelativeTo(dueno);
	}

	static void mostrar(Component dueno, AjustesLocales ajustes) {
		new DialogoConexionPanel(dueno, ajustes).setVisible(true);
	}

	private static GridBagConstraints celda(int x, int y, int ancho) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x;
		c.gridy = y;
		c.gridwidth = ancho;
		c.anchor = GridBagConstraints.WEST;
		c.fill = x == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		c.weightx = x == 1 ? 1 : 0;
		c.insets = new Insets(3, 3, 3, 3);
		return c;
	}

	private ConexionPanel escrita() {
		String d = dispositivo.getText().trim();
		return new ConexionPanel(url.getText().trim(), new String(token.getPassword()).trim(), d.isEmpty() ? ajustes.dispositivo() : d, enviar.isSelected());
	}

	/** Avisa y devuelve false si falta la dirección o el token, o la dirección no es http(s). */
	private boolean validar(ConexionPanel c) {
		String u = c.url().toLowerCase();
		if (c.token().isEmpty() || !(u.startsWith("http://") || u.startsWith("https://"))) {
			JOptionPane.showMessageDialog(this, "Escribí la dirección (http://… o https://…) y el token.", "Revisá los datos", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return true;
	}

	private void probarConexion() {
		ConexionPanel c = escrita();
		if (!validar(c)) return;
		ocupado(true);
		new SwingWorker<Boolean, Void>() {
			@Override protected Boolean doInBackground() {
				return new PublicadorPanel(new TransporteHttp(), c).probar();
			}

			@Override protected void done() {
				ocupado(false);
				boolean ok;
				try {
					ok = get();
				} catch (InterruptedException | ExecutionException e) {
					ok = false;
				}
				JOptionPane.showMessageDialog(DialogoConexionPanel.this, ok ? "Conectado" : "No se pudo conectar: revisá la dirección y el token",
						"Probar conexión", ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
			}
		}.execute();
	}

	private void guardarConexion() {
		ConexionPanel c = escrita();
		if (!validar(c)) return;
		try {
			ajustes.guardarConexionPanel(c);
			dispose();
		} catch (RuntimeException e) {
			JOptionPane.showMessageDialog(this, "No se pudo guardar: " + e, Textos.TITULO, JOptionPane.ERROR_MESSAGE);
		}
	}

	private void ocupado(boolean si) {
		for (JButton b : new JButton[] {probar, guardar, cancelar}) b.setEnabled(!si);
		setCursor(si ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
	}
}
