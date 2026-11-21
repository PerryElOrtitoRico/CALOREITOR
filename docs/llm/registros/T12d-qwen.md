// android/app/src/main/java/com/caloreitor/app/ui/MainActivity.java
package com.caloreitor.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Ajustes;
import com.caloreitor.app.android.Hilos;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.PlanesEntrenamiento;
import com.caloreitor.app.logica.ResumenPanel;
import com.caloreitor.app.red.ConexionPanel;
import com.caloreitor.app.red.PublicadorPanel;
import com.caloreitor.app.red.TransporteHttp;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Pantalla única: las cinco pestañas con navegación inferior; es el Anfitrión de las vistas. */
public final class MainActivity extends AppCompatActivity implements Anfitrion {
	private Ajustes ajustes;
	private Sesion sesion;
	/** Plan que llegó por "Abrir con" antes de tener la carpeta abierta: se importa apenas haya sesión. */
	private Uri planPendiente;
	private Sesion.Estado estado = Sesion.Estado.SIN_ELEGIR;
	private VistaBase[] vistas;
	private FrameLayout marco;
	private final ActivityResultLauncher<Uri> eligeCarpeta =
			registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), this::carpetaElegida);
	private final ActivityResultLauncher<String[]> eligePlan =
			registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::planElegido);

	// ---- T12a: navegación (subtarea A) ----

	/** Arma la pantalla: marco de vistas arriba y BottomNavigationView abajo; las 5 vistas en el orden del menú. */
	@Override
	protected void onCreate(Bundle guardado) {
		super.onCreate(guardado);
		ajustes = new Ajustes(this);
		LinearLayout raiz = new LinearLayout(this);
		raiz.setOrientation(LinearLayout.VERTICAL);
		marco = new FrameLayout(this);
		raiz.addView(marco, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
		vistas = new VistaBase[] { new HoyView(this, this), new ComidasView(this, this), new RecetasView(this, this), new PlanificadorView(this, this), new ConfiguracionView(this, this) };
		for (VistaBase v : vistas) marco.addView(v);
		BottomNavigationView nav = new BottomNavigationView(this);
		nav.inflateMenu(R.menu.navegacion);
		raiz.addView(nav, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		nav.setOnItemSelectedListener(item -> { int id = item.getItemId(); mostrar(id == R.id.nav_hoy ? 0 : id == R.id.nav_comidas ? 1 : id == R.id.nav_recetas ? 2 : id == R.id.nav_semana ? 3 : 4); return true; });
		setContentView(raiz);
		mostrar(0);
		if (guardado == null) intentDePlan(getIntent());
	}

	/** Muestra solo la vista de la posición dada (View.VISIBLE/GONE) y la refresca. */
	private void mostrar(int posicion) {
		for (int i = 0; i < vistas.length; i++) vistas[i].setVisibility(i == posicion ? View.VISIBLE : View.GONE);
		vistas[posicion].refrescar();
	}

	// ---- T12b: ciclo de vida y publicación (subtarea B) ----

	@Override
	protected void onResume() {
		super.onResume();
		if (sesion == null) abrir(); else sesion.releer(ok -> { cambio(); if (ok) publicar(); });
	}

	/** Si no hay sesión la abre en el hilo de E/S (Sesion.abrir); después refresca y publica. Sin carpeta elegida queda el estado. */
	private void abrir() {
		Hilos.enEs(() -> {
			Sesion.Apertura ap = Sesion.abrir(this, ajustes);
			Hilos.enPrincipal(() -> { estado = ap.estado(); sesion = ap.sesion(); cambio(); if (sesion != null) publicar(); if (sesion != null && planPendiente != null) { Uri u = planPendiente; planPendiente = null; planElegido(u); } });
		});
	}

	/** Manda al Panel los últimos 14 días, si está configurado. Llamar desde el hilo principal. */
	private void publicar() {
		Sesion s = sesion; ConexionPanel con = ajustes.conexionPanel();
		if (s == null || !con.enviar()) return;
		Hilos.enEs(() -> {
			try { new PublicadorPanel(new TransporteHttp(), con).publicar(ResumenPanel.dias(s.nucleo().repo(), s.nucleo().hoy(), 14)); } catch (RuntimeException e) { /* sin token ni URL en el log */ android.util.Log.w("MainActivity", "falló la publicación al panel (" + e.getClass().getSimpleName() + ")"); }
		});
	}

	// ---- T12c: carpeta y plan (subtarea C) ----

	private void carpetaElegida(Uri arbol) {
		if (arbol == null) return;
		getContentResolver().takePersistableUriPermission(arbol, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		ajustes.elegirCarpeta(arbol);
		sesion = null; abrir();
	}

	/** Importa el .json del plan (desde el selector o desde "Abrir con"/"Compartir"). */
	private void planElegido(Uri uri) {
		if (uri == null) return;
		if (sesion == null) { planPendiente = uri; Toast.makeText(this, "El plan se importa en cuanto abras la carpeta.", Toast.LENGTH_LONG).show(); return; }
		Sesion s = sesion;
		Hilos.enEs(() -> {
			final String json;
			try (InputStream is = getContentResolver().openInputStream(uri)) {
				if (is == null) throw new IOException("sin contenido");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[8192];
				int leido;
				while ((leido = is.read(buffer)) != -1) baos.write(buffer, 0, leido);
				json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				Hilos.enPrincipal(() -> Toast.makeText(this, "No se pudo leer el archivo.", Toast.LENGTH_LONG).show());
				return;
			}
			final PlanesEntrenamiento.Plan plan;
			try {
				plan = PlanesEntrenamiento.leer(json, System.currentTimeMillis());
			} catch (IllegalArgumentException e) {
				Hilos.enPrincipal(() -> Toast.makeText(this, "No se importó: " + e.getMessage(), Toast.LENGTH_LONG).show());
				return;
			}
			Hilos.enPrincipal(() -> s.escribir(() -> { s.nucleo().repo().guardarPlan(json); s.nucleo().planesCambiaron(); },
					() -> { Toast.makeText(this, "Importado: " + plan.formato() + " del " + plan.inicio() + " al " + plan.fin(), Toast.LENGTH_LONG).show(); cambio(); }));
		});
	}

	/** Lee los intents "Abrir con" (ACTION_VIEW, data) y "Compartir" (ACTION_SEND, EXTRA_STREAM). */
	private void intentDePlan(Intent intent) {
		String accion = intent.getAction(); Uri uri = null;
		if (Intent.ACTION_VIEW.equals(accion)) uri = intent.getData();
		else if (Intent.ACTION_SEND.equals(accion)) uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
		if (uri != null) planElegido(uri);
	}

	// ---- Anfitrion: lo escribe Claude ----

	@Override public Sesion sesion() { return sesion; }
	@Override public Sesion.Estado estado() { return estado; }
	@Override public Ajustes ajustes() { return ajustes; }
	@Override public void cambio() { for (VistaBase v : vistas) v.refrescar(); }
	@Override public void elegirCarpeta() { eligeCarpeta.launch(null); }
	@Override public void importarPlan() { eligePlan.launch(new String[] {"application/json", "application/octet-stream", "text/plain"}); }
	@Override public void reabrir() { sesion = null; abrir(); }

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		intentDePlan(intent);
	}
}