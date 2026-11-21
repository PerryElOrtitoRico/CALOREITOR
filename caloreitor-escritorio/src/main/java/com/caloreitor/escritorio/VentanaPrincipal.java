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
	// Cubre toda la ventana mientras se relee: come el mouse y se queda con el teclado.
	private final JPanel vidrio = new JPanel(new java.awt.GridBagLayout());
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
		vidrio.setOpaque(false);
		vidrio.add(estado);
		vidrio.addMouseListener(new java.awt.event.MouseAdapter() { });
		vidrio.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() { });
		vidrio.setFocusTraversalKeysEnabled(false); // Tab no debe sacar el foco del vidrio
		vidrio.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override public void keyPressed(java.awt.event.KeyEvent e) { e.consume(); }
			@Override public void keyReleased(java.awt.event.KeyEvent e) { e.consume(); }
			@Override public void keyTyped(java.awt.event.KeyEvent e) { e.consume(); }
		});
		vidrio.setFocusable(true);
		setGlassPane(vidrio);
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
		// Primero se suelta el foco: si un campo de gramos tiene una edición sin guardar, su focusLost la guarda ahora,
		// con leyendo todavía en false. El resto va por invokeLater para correr después de esos eventos de foco.
		java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().clearFocusOwner();
		javax.swing.SwingUtilities.invokeLater(this::empezarLectura);
	}

	private void empezarLectura() {
		sesion.leyendo(true);
		actualizar.setEnabled(false);
		estado.setText("Leyendo…");
		vidrio.setVisible(true);
		vidrio.requestFocusInWindow();
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
				vidrio.setVisible(false);
				sesion.leyendo(false);
				refrescar();
			}
		}.execute();
	}
}
