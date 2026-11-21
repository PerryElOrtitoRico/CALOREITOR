package com.caloreitor.escritorio;

import com.caloreitor.escritorio.infra.AjustesLocales;
import com.caloreitor.escritorio.infra.HistorialSqlite;
import com.caloreitor.escritorio.tema.Tema;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public final class Main {
	private Main() {
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			Tema.aplicar();
			AjustesLocales ajustes = AjustesLocales.abrir(AjustesLocales.rutaPorDefecto());
			if (ajustes.carpetaEventos() == null) {
				JFileChooser c = new JFileChooser();
				c.setDialogTitle("Elegí la carpeta de Syncthing de CALOREITOR");
				c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (c.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) System.exit(0);
				try {
					ajustes.elegirCarpeta(c.getSelectedFile().toPath());
				} catch (RuntimeException e) {
					JOptionPane.showMessageDialog(null, "No se pudieron guardar los ajustes: " + e, Textos.TITULO, JOptionPane.ERROR_MESSAGE);
					System.exit(1);
				}
			}
			Path carpeta = ajustes.carpetaEventos();
			// la lectura inicial (archivos + depuración) va fuera del hilo de la interfaz
			new SwingWorker<Sesion, Void>() {
				@Override
				protected Sesion doInBackground() {
					return new Sesion(carpeta, ajustes.dispositivo(), HistorialSqlite.rutaPorDefecto());
				}

				@Override
				protected void done() {
					try {
						new VentanaPrincipal(get(), ajustes).setVisible(true);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null, "No se pudo abrir " + carpeta + ": " + e.getCause(), Textos.TITULO, JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}
				}
			}.execute();
		});
	}
}
