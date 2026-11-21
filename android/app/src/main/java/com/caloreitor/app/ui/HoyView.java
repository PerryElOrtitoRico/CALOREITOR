package com.caloreitor.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Asignacion;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Modo;
import com.caloreitor.app.modelo.Objetivo;
import java.util.ArrayList;
import java.util.List;

/** Resumen del día de hoy: kcal y macros contra el objetivo, entrenamiento y acceso rápido a agregar comida. */
final class HoyView extends VistaBase {
	/** Lo que la pantalla necesita, ya calculado en el hilo de E/S. objetivo es null si falta completar los datos personales. */
	private record Datos(Macros comido, Macros objetivo, boolean automatico, String entrenamiento, String proximaComida, java.time.LocalDate hoy) {
	}

	private final MedioCirculoView kcal, prot, carb, gras;
	private String proximaComida;

	HoyView(Context c, Anfitrion anfitrion) {
		super(c, anfitrion, R.layout.view_hoy, false);
		kcal = ver(R.id.circulo_kcal);
		prot = ver(R.id.circulo_prot);
		carb = ver(R.id.circulo_carb);
		gras = ver(R.id.circulo_gras);
		kcal.configurar(c.getString(R.string.kcal), c.getString(R.string.kcal), ContextCompat.getColor(c, R.color.disco_azul));
		prot.configurar(c.getString(R.string.proteinas), c.getString(R.string.gramos_abrev), ContextCompat.getColor(c, R.color.disco_verde));
		carb.configurar(c.getString(R.string.carbohidratos), c.getString(R.string.gramos_abrev), ContextCompat.getColor(c, R.color.disco_amarillo));
		gras.configurar(c.getString(R.string.grasas), c.getString(R.string.gramos_abrev), ContextCompat.getColor(c, R.color.disco_violeta));
		ver(R.id.boton_elegir_carpeta).setOnClickListener(v -> anfitrion.elegirCarpeta());
		ver(R.id.boton_agregar).setOnClickListener(v -> {
			Sesion s = anfitrion.sesion();
			if (s != null && proximaComida != null) {
				BuscadorRapidoView.abrir(getContext(), s, s.nucleo().hoy(), proximaComida, anfitrion::cambio, x -> s.nucleo().repo().guardar(x));
			}
		});
	}

	@Override
	protected void cargar(Sesion s) {
		Sesion.Estado e = anfitrion.estado();
		View aviso = ver(R.id.aviso_carpeta);
		TextView textoAviso = ver(R.id.texto_aviso_carpeta);
		aviso.setVisibility(e == Sesion.Estado.LISTA ? View.GONE : View.VISIBLE);
		textoAviso.setText(e == Sesion.Estado.SIN_ELEGIR ? R.string.aviso_sin_elegir
				: e == Sesion.Estado.SIN_PERMISO ? R.string.aviso_sin_permiso : R.string.aviso_vacia);
		ver(R.id.contenido_hoy).setVisibility(s == null ? View.GONE : View.VISIBLE);
		if (s == null) return;
		s.leer(() -> calcular(s.nucleo()), this::pintar);
	}

	private static Datos calcular(Nucleo n) {
		String hoy = n.hoy().toString();
		Configuracion cfg = n.repo().configuracion();
		List<Consumo> consumos = n.repo().consumos(hoy);
		Macros comido = Macros.CERO;
		for (Consumo c : consumos) comido = comido.mas(c.macros());
		Objetivo obj = n.objetivos().del(n.hoy()).orElse(null);

		var e = Asignacion.para(n.hoy(), cfg.semana(), n.planes());
		List<String> toca = new ArrayList<>();
		if (e.gym() != null) toca.add("Gimnasio");
		if (e.running() != null) toca.add("Running");
		if (e.plio() != null) toca.add("Pliometría");

		// la primera comida sin consumos; si todas tienen, la última
		List<String> comidas = cfg.comidas().isEmpty() ? Configuracion.porDefecto().comidas() : cfg.comidas();
		String proxima = comidas.get(comidas.size() - 1);
		for (String c : comidas) {
			if (consumos.stream().noneMatch(x -> c.equals(x.comida()))) { proxima = c; break; }
		}
		return new Datos(comido, obj == null ? null : obj.macros(), cfg.modo() == Modo.AUTOMATICO,
				toca.isEmpty() ? null : String.join(" · ", toca), proxima, n.hoy());
	}

	private void pintar(Datos d) {
		Macros o = d.objetivo() == null ? Macros.CERO : d.objetivo();
		kcal.valores(d.comido().kcal(), o.kcal());
		prot.valores(d.comido().proteinas(), o.proteinas());
		carb.valores(d.comido().carbohidratos(), o.carbohidratos());
		gras.valores(d.comido().grasas(), o.grasas());
		TextView datos = ver(R.id.texto_aviso_datos);
		datos.setText(R.string.sin_datos_personales);
		datos.setVisibility(d.objetivo() == null ? View.VISIBLE : View.GONE);
		((TextView) ver(R.id.texto_modo)).setText(d.automatico() ? R.string.modo_automatico : R.string.modo_manual);
		((TextView) ver(R.id.texto_entrenamiento)).setText(d.entrenamiento() == null ? getContext().getString(R.string.descanso) : d.entrenamiento());
		proximaComida = d.proximaComida();
		((Button) ver(R.id.boton_agregar)).setText("Agregar a " + proximaComida.toLowerCase());
	}
}
