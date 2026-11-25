package com.caloreitor.escritorio.config;

import com.caloreitor.escritorio.tema.Tema;
import com.caloreitor.app.logica.Calculo;
import com.caloreitor.app.logica.PlanesEntrenamiento;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Meta;
import com.caloreitor.app.modelo.Modo;
import com.caloreitor.app.modelo.Perfil;
import com.caloreitor.app.modelo.SemanaEntrenamiento;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.escritorio.Sesion;
import com.caloreitor.app.logica.Textos;
import com.caloreitor.escritorio.infra.AjustesLocales;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Perfil, comidas, modo y meta, manual, semana de entrenamiento, planes, cómo se calculó hoy y ajustes del equipo. */
public final class PestanaConfiguracion extends JPanel {
	private static final Locale ES = Locale.forLanguageTag("es");

	private final Sesion sesion;
	private final AjustesLocales ajustes;
	private final Runnable alCambiar;

	private final JTextField peso = new JTextField(6), altura = new JTextField(6), edad = new JTextField(6), pasos = new JTextField(6);
	private final JComboBox<String> sexo = new JComboBox<>(new String[] {"Sin definir", "Masculino", "Femenino"});
	private final DefaultListModel<String> comidas = new DefaultListModel<>();
	private final JList<String> listaComidas = new JList<>(comidas);
	private final JComboBox<String> modo = new JComboBox<>(new String[] {"Automático", "Manual"});
	private final JComboBox<String> meta = new JComboBox<>(new String[] {"Bajar", "Mantener", "Subir"});
	private final JTextField kcal = new JTextField(6), pProt = new JTextField(6), pCarb = new JTextField(6), pGrasa = new JTextField(6);
	private final JCheckBox[][] semana = new JCheckBox[7][3];
	private final Map<String, JTextField> coef = new TreeMap<>();
	private final JTextArea calculo = new JTextArea(8, 40);
	private final JTextField dispositivo = new JTextField(14);
	private final JLabel carpeta = new JLabel();
	// true cuando hay algo escrito sin guardar: al releer no se pisa con lo guardado
	private boolean sucio;
	private boolean cargando;

	public PestanaConfiguracion(Sesion sesion, AjustesLocales ajustes, Runnable alCambiar) {
		super(new BorderLayout());
		this.sesion = sesion;
		this.ajustes = ajustes;
		this.alCambiar = alCambiar;
		JPanel col = new JPanel();
		col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
		col.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		col.add(seccion("Datos personales", perfilPanel()));
		col.add(seccion("Comidas del día", comidasPanel()));
		col.add(seccion("Modo de objetivo y meta", modoPanel()));
		col.add(seccion("Objetivo manual (los % deben sumar 100)", manualPanel()));
		col.add(seccion("Semana de entrenamiento", semanaPanel()));
		col.add(seccion("Avanzado: coeficientes del cálculo automático", avanzadoPanel()));
		JButton guardar = new JButton("Guardar configuración");
		guardar.addActionListener(e -> guardar());
		JButton descartar = new JButton("Descartar cambios");
		descartar.addActionListener(e -> {
			sucio = false;
			cargar();
		});
		JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
		botones.add(guardar);
		botones.add(descartar);
		col.add(botones);
		col.add(seccion("Planes de entrenamiento", planesPanel()));
		col.add(seccion("Cómo se calculó hoy", calculoPanel()));
		col.add(seccion("Este equipo", equipoPanel()));
		add(Tema.desplazable(col), BorderLayout.CENTER);
		cargar();
		refrescar();
	}

	private static JPanel seccion(String titulo, JPanel contenido) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(titulo));
		p.add(contenido, BorderLayout.CENTER);
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	private JPanel perfilPanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		for (var par : List.of(Map.entry("Peso (kg)", peso), Map.entry("Altura (cm)", altura), Map.entry("Edad", edad), Map.entry("Pasos diarios", pasos))) {
			p.add(new JLabel(par.getKey()));
			p.add(marcar(par.getValue()));
		}
		p.add(new JLabel("Sexo"));
		p.add(marcar(sexo));
		return p;
	}

	private JPanel comidasPanel() {
		JButton agregar = new JButton("Agregar…"), quitar = new JButton("Quitar"), sube = new JButton("▲"), baja = new JButton("▼");
		agregar.addActionListener(e -> {
			String n = JOptionPane.showInputDialog(this, "Nombre de la comida");
			if (n != null && !n.isBlank()) {
				comidas.addElement(n.trim());
				sucio = true;
			}
		});
		quitar.addActionListener(e -> {
			int i = listaComidas.getSelectedIndex();
			if (i >= 0) {
				comidas.remove(i);
				sucio = true;
			}
		});
		sube.addActionListener(e -> mover(-1));
		baja.addActionListener(e -> mover(1));
		JPanel b = new JPanel(new GridLayout(0, 1, 0, 4));
		for (JButton x : List.of(agregar, quitar, sube, baja)) b.add(x);
		JPanel lado = new JPanel(new BorderLayout());
		lado.add(b, BorderLayout.NORTH);
		JPanel p = new JPanel(new BorderLayout(8, 0));
		JScrollPane s = Tema.desplazable(listaComidas);
		s.setPreferredSize(new java.awt.Dimension(240, 110));
		p.add(s, BorderLayout.WEST);
		p.add(lado, BorderLayout.CENTER);
		return p;
	}

	private void mover(int delta) {
		int i = listaComidas.getSelectedIndex(), j = i + delta;
		if (i < 0 || j < 0 || j >= comidas.size()) return;
		String x = comidas.remove(i);
		comidas.add(j, x);
		listaComidas.setSelectedIndex(j);
		sucio = true;
	}

	private JPanel modoPanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		p.add(new JLabel("Modo"));
		p.add(marcar(modo));
		p.add(new JLabel("Meta"));
		p.add(marcar(meta));
		return p;
	}

	private JPanel manualPanel() {
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		for (var par : List.of(Map.entry("kcal", kcal), Map.entry("% proteínas", pProt), Map.entry("% carbohidratos", pCarb), Map.entry("% grasas", pGrasa))) {
			p.add(new JLabel(par.getKey()));
			p.add(marcar(par.getValue()));
		}
		return p;
	}

	private JPanel semanaPanel() {
		JPanel p = new JPanel(new GridLayout(8, 4, 4, 2));
		p.add(new JLabel(""));
		for (String t : List.of("Gimnasio", "Running", "Pliometría")) p.add(new JLabel(t));
		for (int d = 0; d < 7; d++) {
			p.add(new JLabel(DayOfWeek.of(d + 1).getDisplayName(TextStyle.FULL, ES)));
			for (int k = 0; k < 3; k++) {
				semana[d][k] = new JCheckBox();
				marcar(semana[d][k]);
				p.add(semana[d][k]);
			}
		}
		return p;
	}

	private JPanel avanzadoPanel() {
		JPanel p = new JPanel(new GridLayout(0, 4, 6, 2));
		new TreeMap<>(Calculo.DEFECTOS).keySet().forEach(k -> {
			JTextField f = new JTextField(6);
			coef.put(k, f);
			p.add(new JLabel(k));
			p.add(marcar(f));
		});
		return p;
	}

	private final java.util.Map<String, JLabel> planCargado = new java.util.HashMap<>();

	/** Dos mitades: gimnasio (PESEITOR) y running (ZANCADEITOR), cada una con su plan cargado. */
	private JPanel planesPanel() {
		JPanel p = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
		p.add(mitadPlan("Gimnasio (PESEITOR)", PlanesEntrenamiento.GIMNASIO));
		p.add(mitadPlan("Running (ZANCADEITOR)", PlanesEntrenamiento.RUNNING));
		return p;
	}

	private JPanel mitadPlan(String titulo, String formato) {
		JLabel cargado = new JLabel(" ");
		planCargado.put(formato, cargado);
		JButton importar = new JButton("Cargar plan de " + PlanesEntrenamiento.nombre(formato) + "…");
		importar.addActionListener(e -> importarPlan(formato));
		JPanel m = new JPanel(new java.awt.GridLayout(0, 1, 0, 4));
		m.setBorder(BorderFactory.createTitledBorder(titulo));
		m.add(cargado);
		JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		b.add(importar);
		m.add(b);
		return m;
	}

	private void refrescarPlanes() {
		planCargado.forEach((formato, l) -> l.setText(PlanesEntrenamiento.resumen(sesion.planes(), formato, sesion.hoy())));
	}

	private JPanel calculoPanel() {
		calculo.setEditable(false);
		JPanel p = new JPanel(new BorderLayout());
		p.add(Tema.desplazable(calculo), BorderLayout.CENTER);
		return p;
	}

	private JPanel equipoPanel() {
		JButton cambiarCarpeta = new JButton("Cambiar carpeta…");
		cambiarCarpeta.addActionListener(e -> {
			JFileChooser c = new JFileChooser(ajustes.carpetaEventos() == null ? null : ajustes.carpetaEventos().toFile());
			c.setDialogTitle("Carpeta de sincronización de CALOREITOR");
			c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
			guardarAjuste(() -> ajustes.elegirCarpeta(c.getSelectedFile().toPath()));
		});
		JButton guardarNombre = new JButton("Guardar nombre");
		guardarNombre.addActionListener(e -> {
			String n = dispositivo.getText().trim();
			// el nombre forma parte del nombre de los archivos de eventos: nada de separadores ni símbolos raros
			if (!Cuentas.dispositivoValido(n)) {
				JOptionPane.showMessageDialog(this, "El nombre admite letras, números, guion y guion bajo (hasta 30)");
				return;
			}
			guardarAjuste(() -> ajustes.elegirDispositivo(n));
		});
		JButton conexionPanel = new JButton("Conexión con el panel…");
		conexionPanel.addActionListener(e -> DialogoConexionPanel.mostrar(this, ajustes));
		JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
		JPanel a = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		a.add(new JLabel("Carpeta de sincronización:"));
		a.add(carpeta);
		a.add(cambiarCarpeta);
		JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		b.add(new JLabel("Nombre del dispositivo:"));
		b.add(dispositivo);
		b.add(guardarNombre);
		JPanel c = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
		c.add(conexionPanel);
		p.add(a);
		p.add(b);
		p.add(c);
		dispositivo.setText(ajustes.dispositivo());
		carpeta.setText(textoCarpeta());
		return p;
	}

	private String textoCarpeta() {
		return ajustes.carpetaEventos() == null ? "(sin elegir)" : ajustes.carpetaEventos().toString();
	}

	private void guardarAjuste(Runnable r) {
		try {
			r.run();
			JOptionPane.showMessageDialog(this, "Guardado. Se usa la próxima vez que abras CALOREITOR.");
		} catch (RuntimeException ex) {
			JOptionPane.showMessageDialog(this, "No se pudo guardar el ajuste: " + ex, Textos.TITULO, JOptionPane.ERROR_MESSAGE);
		}
		carpeta.setText(textoCarpeta());
	}

	// --- marcar cambios sin guardar
	private JTextField marcar(JTextField f) {
		f.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { sucio |= !cargando; }
			public void removeUpdate(DocumentEvent e) { sucio |= !cargando; }
			public void changedUpdate(DocumentEvent e) { sucio |= !cargando; }
		});
		return f;
	}

	private <T extends javax.swing.AbstractButton> T marcar(T b) {
		b.addActionListener(e -> sucio = true);
		return b;
	}

	private JComboBox<String> marcar(JComboBox<String> c) {
		c.addActionListener(e -> sucio |= !cargando);
		return c;
	}

	// --- formulario <-> configuración
	/** Llena el formulario con lo guardado. */
	private void cargar() {
		cargando = true;
		Configuracion c = sesion.repo().configuracion();
		Perfil p = c.perfil();
		peso.setText(texto(p.pesoKg()));
		altura.setText(texto(p.alturaCm()));
		edad.setText(texto(p.edad()));
		pasos.setText(texto(p.pasosDiarios()));
		sexo.setSelectedIndex("M".equals(p.sexo()) ? 1 : "F".equals(p.sexo()) ? 2 : 0);
		comidas.clear();
		c.comidas().forEach(comidas::addElement);
		modo.setSelectedIndex(c.modo().ordinal());
		meta.setSelectedIndex(c.meta().ordinal());
		kcal.setText(texto(c.manualKcal()));
		pProt.setText(texto(c.manualPctProt()));
		pCarb.setText(texto(c.manualPctCarb()));
		pGrasa.setText(texto(c.manualPctGrasa()));
		for (int d = 0; d < 7; d++) {
			DayOfWeek dia = DayOfWeek.of(d + 1);
			semana[d][0].setSelected(c.semana().gimnasio().contains(dia));
			semana[d][1].setSelected(c.semana().running().contains(dia));
			semana[d][2].setSelected(c.semana().pliometria().contains(dia));
		}
		coef.forEach((k, f) -> f.setText(texto(c.coeficientes().getOrDefault(k, Calculo.DEFECTOS.get(k)))));
		cargando = false;
	}

	private static String texto(Number n) {
		if (n == null) return "";
		double d = n.doubleValue();
		return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	/** Número del campo; IllegalArgumentException con el aviso si está mal. En blanco: null si se permite. */
	private static Double numero(JTextField f, String nombre, boolean obligatorio, boolean positivo) {
		if (f.getText().isBlank() && !obligatorio) return null;
		OptionalDouble v = Cuentas.numero(f.getText());
		if (v.isEmpty() || (positivo && v.getAsDouble() == 0)) {
			throw new IllegalArgumentException(nombre + ": escribí un número " + (positivo ? "mayor que 0" : "de 0 o más"));
		}
		return v.getAsDouble();
	}

	private void guardar() {
		Configuracion nueva;
		try {
			Double pesoKg = numero(peso, "Peso", false, true), alturaCm = numero(altura, "Altura", false, true);
			Double ed = numero(edad, "Edad", false, true), ps = numero(pasos, "Pasos diarios", false, false);
			if (ed != null && ed != Math.rint(ed) || ps != null && ps != Math.rint(ps)) {
				throw new IllegalArgumentException("Edad y pasos: escribí números enteros");
			}
			Perfil perfil = new Perfil(pesoKg, alturaCm, ed == null ? null : ed.intValue(),
					sexo.getSelectedIndex() == 1 ? "M" : sexo.getSelectedIndex() == 2 ? "F" : null, ps == null ? null : ps.intValue());
			// los campos manuales solo se exigen en modo Manual; en Automático se guardan como estén (o los defectos)
			boolean manual = modo.getSelectedIndex() == Modo.MANUAL.ordinal();
			Configuracion previa = sesion.repo().configuracion();
			double k = previa.manualKcal(), pp = previa.manualPctProt(), pc = previa.manualPctCarb(), pg = previa.manualPctGrasa();
			if (manual) {
				k = numero(kcal, "kcal manuales", true, true);
				pp = numero(pProt, "% proteínas", true, false);
				pc = numero(pCarb, "% carbohidratos", true, false);
				pg = numero(pGrasa, "% grasas", true, false);
				if (!Cuentas.sumaCien(pp, pc, pg)) {
					throw new IllegalArgumentException("Los porcentajes manuales suman " + Math.round((pp + pc + pg) * 10) / 10.0 + ", no 100: no se guardó");
				}
			}
			if (comidas.isEmpty()) throw new IllegalArgumentException("Dejá al menos una comida en el día");
			Map<String, Double> valores = new TreeMap<>();
			for (var e : coef.entrySet()) valores.put(e.getKey(), numero(e.getValue(), e.getKey(), true, false));
			nueva = new Configuracion(perfil, Modo.values()[modo.getSelectedIndex()], Meta.values()[meta.getSelectedIndex()], k, pp, pc, pg,
					new ArrayList<>(java.util.Collections.list(comidas.elements())), semanaMarcada(), Cuentas.soloCambios(valores, Calculo.DEFECTOS));
		} catch (IllegalArgumentException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), Textos.TITULO, JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (sesion.mutar(this, () -> sesion.repo().guardar(nueva))) {
			sucio = false;
			alCambiar.run();
		}
	}

	private SemanaEntrenamiento semanaMarcada() {
		Set<DayOfWeek> g = EnumSet.noneOf(DayOfWeek.class), r = EnumSet.noneOf(DayOfWeek.class), p = EnumSet.noneOf(DayOfWeek.class);
		for (int d = 0; d < 7; d++) {
			if (semana[d][0].isSelected()) g.add(DayOfWeek.of(d + 1));
			if (semana[d][1].isSelected()) r.add(DayOfWeek.of(d + 1));
			if (semana[d][2].isSelected()) p.add(DayOfWeek.of(d + 1));
		}
		return new SemanaEntrenamiento(g, r, p);
	}

	private void importarPlan(String formato) {
		JFileChooser c = new JFileChooser();
		c.setDialogTitle("Plan de entrenamiento (.json de PESEITOR o ZANCADEITOR)");
		c.setFileFilter(new FileNameExtensionFilter("Plan (.json)", "json"));
		if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
		String json;
		try {
			json = Files.readString(c.getSelectedFile().toPath(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "No se pudo leer el archivo: " + ex.getMessage(), Textos.TITULO, JOptionPane.ERROR_MESSAGE);
			return;
		}
		PlanesEntrenamiento.Plan plan;
		try {
			plan = PlanesEntrenamiento.leer(json, System.currentTimeMillis());
		} catch (IllegalArgumentException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Plan inválido", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (!plan.formato().equals(formato)) {
			JOptionPane.showMessageDialog(this, "Ese archivo es un plan de " + PlanesEntrenamiento.nombre(plan.formato())
					+ ": cargalo en la mitad de " + PlanesEntrenamiento.nombre(plan.formato()) + ".", "Otro tipo de plan", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (sesion.mutar(this, () -> {
			sesion.repo().guardarPlan(json);
			sesion.planesCambiaron();
		})) {
			refrescarPlanes();
			alCambiar.run();
		}
	}

	/** Actualiza "Cómo se calculó hoy" y, si no hay nada sin guardar, el formulario con lo que llegó de afuera. */
	public void refrescar() {
		if (!sucio) cargar();
		refrescarPlanes();
		calculo.setText(sesion.objetivos().calcular(sesion.hoy()).map(o -> o.explicacion()).orElse(Textos.SIN_DATOS_PERSONALES));
		calculo.setCaretPosition(0);
	}
}
