package com.caloreitor.app.ui;

import android.content.Context;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;

/** Provisorio: lo completa su pieza. */
final class PlanificadorView extends VistaBase {
	PlanificadorView(Context c, Anfitrion anfitrion) {
		super(c, anfitrion, R.layout.view_lista, true);
	}

	@Override
	protected void cargar(Sesion s) {
	}
}
