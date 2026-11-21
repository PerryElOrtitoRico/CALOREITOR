// android/app/src/main/java/com/caloreitor/app/ui/FormularioConfig.java
package com.caloreitor.app.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.logica.Calculo;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Meta;
import com.caloreitor.app.modelo.Modo;
import com.caloreitor.app.modelo.Perfil;
import com.caloreitor.app.modelo.SemanaEntrenamiento;
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

/** El formulario de configuración: perfil, comidas, modo y meta, objetivo manual, semana de entrenamiento y coeficientes. */
final class FormularioConfig extends LinearLayout {
	private static final Locale ES = Locale.forLanguageTag("es");

	private final EditText peso, altura, edad, pasos, kcal, pProt, pCarb, pGrasa;
	private final Spinner sexo, modo, meta;
	private final LinearLayout listaComidas;
	private final List<String> comidas = new ArrayList<>();
	/** semana[d][k]: d = lunes..domingo, k = gimnasio, running, pliometría. */
	private final CheckBox[][] semana = new CheckBox[7][3];
	private final Map<String, EditText> coef = new TreeMap<>();
	// true cuando hay algo escrito sin guardar: al releer no se pisa con lo guardado
	private boolean sucio;
	private boolean cargando;

	FormularioConfig(Context c) {
		super(c);
		setOrientation(VERTICAL);
		addView(Ui.texto(c, "Datos personales", false, true));
		peso = agregar(c, "Peso (kg)", true);
		altura = agregar(c, "Altura (cm)", true);
		edad = agregar(c, "Edad", true);
		pasos = agregar(c, "Pasos diarios", true);
		sexo = desplegable(c, "Sexo", "Sin definir", "Masculino", "Femenino");
		addView(Ui.texto(c, "Comidas del día", false, true));
		listaComidas = new LinearLayout(c);
		listaComidas.setOrientation(VERTICAL);
		addView(listaComidas);
		addView(Ui.boton(c, "Agregar comida…", v -> pedirComida()));
		addView(Ui.texto(c, "Modo de objetivo y meta", false, true));
		modo = desplegable(c, "Modo", "Automático", "Manual");
		meta = desplegable(c, "Meta", "Bajar", "Mantener", "Subir");
		addView(Ui.texto(c, "Objetivo manual (los % deben sumar 100)", false, true));
		kcal = agregar(c, "kcal", true);
		pProt = agregar(c, "% proteínas", true);
		pCarb = agregar(c, "% carbohidratos", true);
		pGrasa = agregar(c, "% grasas", true);
		addView(Ui.texto(c, "Semana de entrenamiento", false, true));
		String[] tipos = {"Gimnasio", "Running", "Pliometría"};
		for (int d = 0; d < 7; d++) {
			addView(Ui.texto(c, DayOfWeek.of(d + 1).getDisplayName(TextStyle.FULL, ES), true, false));
			LinearLayout fila = Ui.renglon(c);
			for (int k = 0; k < 3; k++) {
				semana[d][k] = new CheckBox(c);
				semana[d][k].setText(tipos[k]);
				semana[d][k].setOnClickListener(v -> sucio = true);
				fila.addView(semana[d][k]);
			}
			addView(fila);
		}
		addView(Ui.texto(c, "Avanzado: coeficientes del cálculo automático", false, true));
		for (String k : new TreeMap<>(Calculo.DEFECTOS).keySet()) coef.put(k, agregar(c, k, true));
	}

	boolean sucio() { return sucio; }

	void sucio(boolean v) { sucio = v; }

	/** Etiqueta y campo numérico; cada cambio escrito marca el formulario como sucio. */
	private EditText agregar(Context c, String etiqueta, boolean numero) {
		addView(Ui.texto(c, etiqueta, true, false));
		EditText e = Ui.campo(c, "", numero);
		e.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int d) { sucio |= !cargando; }
			@Override public void afterTextChanged(Editable s) { }
		});
		addView(e);
		return e;
	}

	/** El Spinner dispara onItemSelected solo al abrir: el tag guarda la posición ya conocida para no marcar sucio sin cambio real. */
	private Spinner desplegable(Context c, String etiqueta, String... opciones) {
		addView(Ui.texto(c, etiqueta, true, false));
		Spinner s = new Spinner(c);
		s.setAdapter(new ArrayAdapter<>(c, android.R.layout.simple_spinner_dropdown_item, opciones));
		s.setTag(0);
		s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
				if (!Integer.valueOf(pos).equals(s.getTag())) {
					s.setTag(pos);
					sucio = true;
				}
			}
			@Override public void onNothingSelected(AdapterView<?> p) { }
		});
		addView(s);
		return s;
	}

	/** Cambia la selección sin marcar el formulario como sucio. */
	private static void elegir(Spinner s, int posicion) {
		s.setTag(posicion);
		s.setSelection(posicion);
	}

	private void pedirComida() {
		Context c = getContext();
		EditText e = Ui.campo(c, "", false);
		new AlertDialog.Builder(c).setTitle("Nombre de la comida").setView(Ui.conMargen(c, e)).setPositiveButton("Agregar", (d, i) -> {
			String n = e.getText().toString().trim();
			if (!n.isEmpty()) {
				comidas.add(n);
				sucio = true;
				dibujarComidas();
			}
		}).setNegativeButton("Cancelar", null).show();
	}

	private void mover(int i, int delta) {
		int j = i + delta;
		if (j < 0 || j >= comidas.size()) return;
		comidas.add(j, comidas.remove(i));
		sucio = true;
		dibujarComidas();
	}

	private void quitar(int i) {
		comidas.remove(i);
		sucio = true;
		dibujarComidas();
	}

	// ---- T17b ----

	/** Una fila por comida: nombre (peso 1) y botones ▲ ▼ ✕ que llaman a mover(i, -1), mover(i, 1) y quitar(i). */
	private void dibujarComidas() {
		throw new UnsupportedOperationException();
	}

	/** Número del campo; IllegalArgumentException con el aviso si está mal. En blanco: null si no es obligatorio. */
	private static Double numero(EditText f, String nombre, boolean obligatorio, boolean positivo) {
		throw new UnsupportedOperationException();
	}

	private SemanaEntrenamiento semanaMarcada() {
		throw new UnsupportedOperationException();
	}

	/** "150", "150.5" y "" para null. */
	private static String texto(Number n) {
		if (n == null) return "";
		double d = n.doubleValue();
		return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	// ---- T17a ----

	/** Llena el formulario con lo guardado (sin marcarlo sucio) y dibuja las comidas. */
	void llenar(Configuracion c) {
		cargando = true;
		Perfil p = c.perfil();
		peso.setText(texto(p.pesoKg())); altura.setText(texto(p.alturaCm())); edad.setText(texto(p.edad())); pasos.setText(texto(p.pasosDiarios()));
		elegir(sexo, "M".equals(p.sexo()) ? 1 : "F".equals(p.sexo()) ? 2 : 0);
		comidas.clear(); comidas.addAll(c.comidas()); dibujarComidas();
		elegir(modo, c.modo().ordinal()); elegir(meta, c.meta().ordinal());
		kcal.setText(texto(c.manualKcal())); pProt.setText(texto(c.manualPctProt())); pCarb.setText(texto(c.manualPctCarb())); pGrasa.setText(texto(c.manualPctGrasa()));
		for (int d = 0; d < 7; d++) { DayOfWeek dia = DayOfWeek.of(d + 1); semana[d][0].setChecked(c.semana().gimnasio().contains(dia)); semana[d][1].setChecked(c.semana().running().contains(dia)); semana[d][2].setChecked(c.semana().pliometria().contains(dia)); }
		for (Map.Entry<String, EditText> e : coef.entrySet()) e.getValue().setText(texto(c.coeficientes().getOrDefault(e.getKey(), Calculo.DEFECTOS.get(e.getKey()))));
		cargando = false;
	}

	// ---- T17c ----

	/** La configuración escrita. IllegalArgumentException con el aviso en español si algo está mal. previa trae los valores manuales guardados. */
	Configuracion leer(Configuracion previa) {
		throw new UnsupportedOperationException();
	}
}