```java
// android/app/src/main/java/com/caloreitor/app/ui/ConexionPanelDialogo.java
package com.caloreitor.app.ui;

import android.content.Context;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.android.Ajustes;
import com.caloreitor.app.android.Hilos;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.red.ConexionPanel;
import com.caloreitor.app.red.PublicadorPanel;
import com.caloreitor.app.red.TransporteHttp;

/** Diálogo de la conexión con el Panel: dirección, token, nombre del dispositivo, enviar sí/no; probar y guardar. */
final class ConexionPanelDialogo {
	private final Context c;
	private final Ajustes ajustes;
	private final EditText url, token, dispositivo;
	private final CheckBox mostrar, enviar;
	private AlertDialog dialogo;

	private ConexionPanelDialogo(Context c, Ajustes ajustes) {
		this.c = c;
		this.ajustes = ajustes;
		ConexionPanel actual = ajustes.conexionPanel();
		url = Ui.campo(c, actual.url(), false);
		url.setHint("Dirección del panel (http://…)");
		url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
		token = Ui.campo(c, actual.token(), false);
		token.setHint("Token");
		token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		mostrar = new CheckBox(c);
		mostrar.setText("Mostrar el token");
		mostrar.setOnCheckedChangeListener((b, si) -> {
			token.setInputType(InputType.TYPE_CLASS_TEXT | (si ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
			token.setSelection(token.getText().length());
		});
		dispositivo = Ui.campo(c, actual.dispositivo(), false);
		dispositivo.setHint("Dispositivo");
		enviar = new CheckBox(c);
		enviar.setText("Enviar al panel");
		enviar.setChecked(actual.enviar());
	}

	static void mostrar(Context c, Ajustes ajustes) {
		ConexionPanelDialogo d = new ConexionPanelDialogo(c, ajustes);
		LinearLayout cuerpo = new LinearLayout(c);
		cuerpo.setOrientation(LinearLayout.VERTICAL);
		for (android.view.View v : new android.view.View[] {d.url, d.token, d.mostrar, d.dispositivo, d.enviar}) cuerpo.addView(v);
		d.dialogo = new AlertDialog.Builder(c).setTitle("Conexión con el panel").setView(Ui.conMargen(c, cuerpo))
				.setPositiveButton("Guardar", null).setNeutralButton("Probar conexión", null).setNegativeButton("Cancelar", null).create();
		d.dialogo.show();
		// listeners después de show(): validar sin cerrar el diálogo
		d.dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> d.guardarConexion());
		d.dialogo.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> d.probarConexion());
	}

	/** Lo que está escrito; dispositivo vacío usa el del equipo. */
	private ConexionPanel escrita() {
		// Se crea un objeto ConexionPanel a partir de los valores actuales de los campos de entrada.
		String d = dispositivo.getText().toString().trim();
		return new ConexionPanel(url.getText().toString().trim(), token.getText().toString().trim(), d.isEmpty() ? ajustes.dispositivo() : d, enviar.isChecked());
	}

	/** Avisa (Ui.aviso) y devuelve false si falta la dirección o el token o la dirección no es http(s). */
	private boolean validar(ConexionPanel con) {
		// Se verifica si la conexión es válida usando la utilidad Cuentas.conexionValida.
		if (!Cuentas.conexionValida(con)) { Ui.aviso(c, "Escribí la dirección (http://… o https://…) y el token."); return false; }
		return true;
	}

	private void probarConexion() {
		// TODO T15b
		throw new UnsupportedOperationException();
	}

	private void guardarConexion() {
		// TODO T15b
		throw new UnsupportedOperationException();
	}
}
```