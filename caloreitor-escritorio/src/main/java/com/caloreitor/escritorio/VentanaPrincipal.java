package com.caloreitor.escritorio;

import com.caloreitor.escritorio.comidas.PestanaComidas;
import com.caloreitor.escritorio.hoy.PestanaHoy;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

/** Ventana con la barra (Actualizar) y las pestañas Hoy y Comidas. */
public final class VentanaPrincipal extends JFrame {
	// al volver el foco no se relee más seguido que esto (cerrar un diálogo también devuelve el foco)
	private static final long MIN_MS_ENTRE_RELECTURAS = 5000;

	private final Sesion sesion;
	private final PestanaHoy hoy;
	private final PestanaComidas comidas;
	private final JTabbedPane pestanas = new JTabbedPane();
	private final JButton actualizar = new JButton("Actualizar");
	private final JLabel estado = new JLabel(" ");
	private long ultimaLectura = System.currentTimeMillis();

	public VentanaPrincipal(Sesion sesion) {
		super(Textos.TITULO);
		this.sesion = sesion;
		hoy = new PestanaHoy(sesion, this::refrescar);
		comidas = new PestanaComidas(sesion, this::refrescar);
		pestanas.addTab("Hoy", hoy);
		pestanas.addTab("Comidas", comidas);
		JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
		barra.add(actualizar);
		barra.add(estado);
		actualizar.addActionListener(e -> releer());
		add(barra, BorderLayout.NORTH);
		add(pestanas, BorderLayout.CENTER);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowActivated(WindowEvent e) {
				if (System.currentTimeMillis() - ultimaLectura > MIN_MS_ENTRE_RELECTURAS) releer();
			}
		});
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(980, 680);
		setLocationRelativeTo(null);
		refrescar();
	}

	/** Redibuja las pestañas con lo que ya está en memoria. */
	public void refrescar() {
		hoy.refrescar();
		comidas.refrescar();
	}

	/** Relee los archivos fuera del hilo de la interfaz y después redibuja. */
	public void releer() {
		if (!actualizar.isEnabled()) return;
		actualizar.setEnabled(false);
		pestanas.setEnabled(false);
		estado.setText("Leyendo…");
		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() {
				sesion.recargar();
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
					estado.setText(" ");
				} catch (Exception e) {
					estado.setText("No se pudo leer la carpeta: " + e.getCause());
				}
				ultimaLectura = System.currentTimeMillis();
				actualizar.setEnabled(true);
				pestanas.setEnabled(true);
				refrescar();
			}
		}.execute();
	}
}
