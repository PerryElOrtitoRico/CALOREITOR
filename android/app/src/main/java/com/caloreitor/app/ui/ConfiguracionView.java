package com.caloreitor.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.logica.PlanesEntrenamiento;
import com.caloreitor.app.modelo.Configuracion;

/** Ajustes: configuración, planes de entrenamiento, alimentos, cómo se calculó hoy y lo de este equipo. */
final class ConfiguracionView extends VistaBase {
	/** Lo que se muestra, calculado en el hilo de E/S. */
	private record Datos(Configuracion config, String calculo, String planGym, String planRun) {
	}

	private final FormularioConfig formulario;
	private final AlimentosSeccion alimentos;
	private final View seccionesConSesion;
	private final TextView calculo, carpeta, planGym, planRun;
	private final EditText dispositivo;
	/** Lo último guardado: sirve de base al leer el formulario (valores manuales). */
	private Configuracion guardada = Configuracion.porDefecto();

	ConfiguracionView(Context c, Anfitrion anfitrion) {
		super(c, anfitrion, R.layout.view_lista, false);
		LinearLayout contenedor = ver(R.id.contenedor);
		LinearLayout conSesion = new LinearLayout(c);
		conSesion.setOrientation(LinearLayout.VERTICAL);
		seccionesConSesion = conSesion;
		formulario = new FormularioConfig(c);
		conSesion.addView(formulario);
		LinearLayout botones = Ui.renglon(c);
		botones.addView(Ui.boton(c, c.getString(R.string.guardar_configuracion), v -> guardar()));
		botones.addView(Ui.boton(c, c.getString(R.string.descartar_cambios), v -> {
			formulario.sucio(false);
			refrescar();
		}));
		conSesion.addView(botones);
		conSesion.addView(Ui.texto(c, "Planes de entrenamiento", false, true));
		// dos mitades: gimnasio (PESEITOR) y running (ZANCADEITOR), cada una con su plan
		LinearLayout mitades = new LinearLayout(c);
		mitades.setOrientation(LinearLayout.HORIZONTAL);
		planGym = Ui.texto(c, " ", true, false);
		planRun = Ui.texto(c, " ", true, false);
		mitades.addView(mitadPlan(c, "Gimnasio", planGym, PlanesEntrenamiento.GIMNASIO));
		mitades.addView(mitadPlan(c, "Running", planRun, PlanesEntrenamiento.RUNNING));
		conSesion.addView(mitades);
		alimentos = new AlimentosSeccion(c, anfitrion);
		conSesion.addView(alimentos);
		conSesion.addView(Ui.texto(c, "Cómo se calculó hoy", false, true));
		calculo = Ui.texto(c, " ", true, false);
		conSesion.addView(calculo);
		contenedor.addView(conSesion);
		contenedor.addView(Ui.texto(c, "Este equipo", false, true));
		carpeta = Ui.texto(c, " ", true, false);
		contenedor.addView(carpeta);
		contenedor.addView(Ui.boton(c, "Cambiar carpeta…", v -> anfitrion.elegirCarpeta()));
		contenedor.addView(Ui.texto(c, "Nombre del dispositivo", true, false));
		dispositivo = Ui.campo(c, anfitrion.ajustes().dispositivo(), false);
		contenedor.addView(dispositivo);
		contenedor.addView(Ui.boton(c, "Guardar nombre", v -> guardarNombre()));
		contenedor.addView(Ui.boton(c, c.getString(R.string.conexion_panel), v -> ConexionPanelDialogo.mostrar(c, anfitrion.ajustes())));
	}

	private LinearLayout mitadPlan(Context c, String titulo, TextView cargado, String formato) {
		LinearLayout m = new LinearLayout(c);
		m.setOrientation(LinearLayout.VERTICAL);
		int pad = Ui.dp(c, 4);
		m.setPadding(pad, pad, pad, pad);
		m.addView(Ui.texto(c, titulo, false, true));
		m.addView(cargado);
		m.addView(Ui.boton(c, "Cargar plan", v -> anfitrion.importarPlan(formato)));
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		m.setLayoutParams(lp);
		return m;
	}

	@Override
	protected void cargar(Sesion s) {
		android.net.Uri arbol = anfitrion.ajustes().carpeta();
		carpeta.setText(arbol == null ? "(sin elegir)" : arbol.toString());
		seccionesConSesion.setVisibility(s == null ? View.GONE : View.VISIBLE);
		if (s == null) return;
		alimentos.refrescar();
		s.leer(() -> calcular(s.nucleo()), this::pintar);
	}

	private void guardar() {
		Sesion s = anfitrion.sesion();
		if (s == null) return;
		Configuracion nueva;
		try {
			nueva = formulario.leer(guardada);
		} catch (IllegalArgumentException e) {
			Ui.aviso(getContext(), e.getMessage());
			return;
		}
		s.escribir(() -> s.nucleo().repo().guardar(nueva), () -> {
			formulario.sucio(false);
			anfitrion.cambio();
		});
	}

	private void guardarNombre() {
		String n = dispositivo.getText().toString().trim();
		// el nombre forma parte del nombre de los archivos de eventos: nada de separadores ni símbolos raros
		if (!Cuentas.dispositivoValido(n)) {
			Ui.aviso(getContext(), "El nombre admite letras, números, guion y guion bajo (hasta 30)");
			return;
		}
		anfitrion.ajustes().elegirDispositivo(n);
		Ui.aviso(getContext(), "Guardado");
		anfitrion.reabrir();
	}

	/** Corre en el hilo de E/S. */
	private static Datos calcular(Nucleo n) {
		// 1. Calcular el texto basado en el objetivo de hoy o el mensaje de sin datos.
		String texto = n.objetivos().calcular(n.hoy()).map(o -> o.explicacion()).orElse(com.caloreitor.app.logica.Textos.SIN_DATOS_PERSONALES);
		// 2. Devolver el record Datos con la configuración actual y el texto calculado.
		return new Datos(n.repo().configuracion(), texto,
				PlanesEntrenamiento.resumen(n.planes(), PlanesEntrenamiento.GIMNASIO, n.hoy()),
				PlanesEntrenamiento.resumen(n.planes(), PlanesEntrenamiento.RUNNING, n.hoy()));
	}

	/** Hilo principal: recuerda lo guardado, llena el formulario solo si no hay nada escrito sin guardar y muestra el cálculo. */
	private void pintar(Datos d) {
		// 1. Guardar la configuración recibida como la última guardada.
		guardada = d.config();
		// 2. Si el formulario no está sucio (es decir, los datos coinciden con lo guardado), llenarlo con la configuración actual.
		if (!formulario.sucio()) formulario.llenar(d.config());
		// 3. Mostrar el texto del cálculo en el TextView correspondiente.
		calculo.setText(d.calculo());
		planGym.setText(d.planGym());
		planRun.setText(d.planRun());
	}
}
