package com.caloreitor.app.ui;

import android.content.Context;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import com.caloreitor.app.R;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.logica.Parseo;
import com.caloreitor.app.logica.Textos;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Macros;
import com.google.android.material.button.MaterialButton;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Consumer;

/** Piezas de pantalla armadas en código: renglones, botones y los diálogos chicos. */
final class Ui {
	private Ui() {
	}

	static int dp(Context c, float v) {
		return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
	}

	static TextView texto(Context c, CharSequence t, boolean secundario, boolean negrita) {
		TextView v = new TextView(c);
		v.setText(t);
		v.setTextSize(TypedValue.COMPLEX_UNIT_SP, secundario ? 13 : 15);
		v.setTextColor(ContextCompat.getColor(c, secundario ? R.color.secundario : R.color.texto));
		if (negrita) v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
		return v;
	}

	static MaterialButton boton(Context c, String t, View.OnClickListener alTocar) {
		MaterialButton b = new MaterialButton(c);
		b.setText(t);
		b.setOnClickListener(alTocar);
		return b;
	}

	/** Renglón horizontal con padding vertical cómodo para el dedo (48 dp de alto como mínimo). */
	static LinearLayout renglon(Context c) {
		LinearLayout l = new LinearLayout(c);
		l.setOrientation(LinearLayout.HORIZONTAL);
		l.setGravity(Gravity.CENTER_VERTICAL);
		l.setMinimumHeight(dp(c, 48));
		return l;
	}

	static void pesar(View v) {
		v.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
	}

	/** Línea fina entre secciones. */
	static View separador(Context c) {
		View v = new View(c);
		v.setBackgroundColor(ContextCompat.getColor(c, R.color.borde));
		v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
		return v;
	}

	static EditText campo(Context c, String valor, boolean numero) {
		EditText e = new AppCompatEditText(c);
		e.setText(valor);
		e.setSingleLine();
		e.setInputType(numero ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
		return e;
	}

	static void aviso(Context c, String mensaje) {
		Toast.makeText(c, mensaje, Toast.LENGTH_LONG).show();
	}

	/** Pide gramos con el valor actual; llama a alAceptar solo con un número mayor que 0 y distinto del actual. */
	static void pedirGramos(Context c, String titulo, double actual, Consumer<Double> alAceptar) {
		String mostrado = com.caloreitor.app.logica.Formato.gramos(actual);
		EditText e = campo(c, mostrado, true);
		e.selectAll();
		AlertDialog d = new AlertDialog.Builder(c).setTitle(titulo).setMessage("Gramos").setView(conMargen(c, e))
				.setPositiveButton("Guardar", (x, i) -> {
					// el campo muestra gramos redondeados: sin tocarlo no se guarda nada (si no, 150,55 pasaría a 150,6)
					if (e.getText().toString().equals(mostrado)) return;
					OptionalDouble g = Parseo.gramos(e.getText().toString());
					if (g.isEmpty()) aviso(c, Textos.GRAMOS_INVALIDOS);
					else if (g.getAsDouble() != actual) alAceptar.accept(g.getAsDouble());
				}).setNegativeButton("Cancelar", null).create();
		d.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		d.show();
	}

	static View conMargen(Context c, View v) {
		LinearLayout l = new LinearLayout(c);
		l.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), 0);
		l.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return l;
	}

	/**
	 * Nombre y valores por 100 g de un alimento propio. Con previo edita: conserva el id si es propio y trae id nuevo si
	 * era base (la edición de uno base queda como copia propia con el mismo nombre, que la búsqueda prefiere).
	 */
	static void pedirAlimento(Context c, String nombreInicial, Alimento previo, Consumer<Alimento> alAceptar) {
		EditText nombre = campo(c, nombreInicial, false);
		String[] et = {"kcal por 100 g", "Proteínas (g) por 100 g", "Carbohidratos (g) por 100 g", "Grasas (g) por 100 g"};
		double[] ini = new double[4];
		if (previo != null) {
			Macros m = previo.por100();
			ini = new double[] {m.kcal(), m.proteinas(), m.carbohidratos(), m.grasas()};
		}
		LinearLayout col = new LinearLayout(c);
		col.setOrientation(LinearLayout.VERTICAL);
		col.addView(texto(c, "Nombre", true, false));
		col.addView(nombre);
		EditText[] v = new EditText[4];
		for (int i = 0; i < 4; i++) {
			v[i] = campo(c, previo == null ? "0" : com.caloreitor.app.logica.Formato.gramos(ini[i]), true);
			col.addView(texto(c, et[i], true, false));
			col.addView(v[i]);
		}
		android.widget.ScrollView s = new android.widget.ScrollView(c);
		s.addView(col);
		AlertDialog d = new AlertDialog.Builder(c).setTitle(previo == null ? "Alimento nuevo" : "Editar alimento").setView(conMargen(c, s))
				.setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
		d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
			double[] n = new double[4];
			boolean ok = !nombre.getText().toString().isBlank();
			for (int i = 0; i < 4 && ok; i++) {
				OptionalDouble o = Cuentas.numero(v[i].getText().toString());
				ok = o.isPresent();
				if (ok) n[i] = o.getAsDouble();
			}
			if (!ok) {
				aviso(c, "Escribí un nombre y números (0 o más) en los cuatro valores.");
				return;
			}
			String id = previo != null && !previo.base() ? previo.id() : UUID.randomUUID().toString();
			d.dismiss();
			alAceptar.accept(new Alimento(id, nombre.getText().toString().trim(), new Macros(n[0], n[1], n[2], n[3]),
					previo == null ? null : previo.porcionNombre(), previo == null ? null : previo.porcionGramos(), false));
		}));
		d.show();
	}
}
