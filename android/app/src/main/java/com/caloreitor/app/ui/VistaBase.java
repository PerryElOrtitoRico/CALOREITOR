package com.caloreitor.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;

/** Una pestaña: infla su layout y, sin carpeta elegida, muestra el aviso en lugar del contenido. */
abstract class VistaBase extends FrameLayout {
	protected final Anfitrion anfitrion;
	private final View contenido;
	private final View sinCarpeta;
	private final boolean soloConSesion;

	VistaBase(Context contexto, Anfitrion anfitrion, int layout, boolean soloConSesion) {
		super(contexto);
		this.anfitrion = anfitrion;
		this.soloConSesion = soloConSesion;
		contenido = LayoutInflater.from(contexto).inflate(layout, this, false);
		sinCarpeta = LayoutInflater.from(contexto).inflate(R.layout.view_sin_carpeta, this, false);
		addView(contenido);
		addView(sinCarpeta);
	}

	/** Redibuja con lo que hay en memoria (los cálculos van en el hilo de E/S). */
	final void refrescar() {
		Sesion s = anfitrion.sesion();
		boolean ocultar = soloConSesion && s == null;
		contenido.setVisibility(ocultar ? GONE : VISIBLE);
		sinCarpeta.setVisibility(ocultar ? VISIBLE : GONE);
		if (!ocultar) cargar(s);
	}

	/** s es null solo si la pestaña no exige sesión. */
	protected abstract void cargar(Sesion s);

	protected <T extends View> T ver(int id) {
		return contenido.findViewById(id);
	}
}
