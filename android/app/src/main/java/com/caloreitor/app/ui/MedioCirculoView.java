package com.caloreitor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.caloreitor.app.R;
import com.caloreitor.app.logica.Cuentas;

/** Arco de 180° con el porcentaje de lo consumido; lleno desde el 100 %, y el número muestra el exceso (sin rojo). */
public final class MedioCirculoView extends View {
	private final Paint pincel = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final RectF arco = new RectF();
	private String titulo = "";
	private String unidad = "";
	private int color = 0xFF1E5BD8;
	private double consumido;
	private double objetivo;

	public MedioCirculoView(Context contexto) {
		super(contexto);
	}

	public MedioCirculoView(Context contexto, AttributeSet atributos) {
		super(contexto, atributos);
	}

	/** Título, unidad del detalle y color del arco. */
	public MedioCirculoView configurar(String titulo, String unidad, int color) {
		this.titulo = titulo;
		this.unidad = unidad;
		this.color = color;
		invalidate();
		return this;
	}

	/** objetivo <= 0 significa sin objetivo: el arco queda vacío. */
	public void valores(double consumido, double objetivo) {
		this.consumido = consumido;
		this.objetivo = objetivo;
		setContentDescription(titulo + ": " + Cuentas.textoPorcentaje(consumido, objetivo));
		invalidate();
	}

	@Override
	protected void onMeasure(int anchoSpec, int altoSpec) {
		int ancho = MeasureSpec.getSize(anchoSpec);
		setMeasuredDimension(ancho, Math.round(ancho * 0.75f));
	}

	@Override
	protected void onDraw(Canvas c) {
		float w = getWidth();
		float grosor = w * 0.075f;
		float d = w - 2 * grosor - w * 0.04f;
		float x = (w - d) / 2, y = grosor;
		arco.set(x, y, x + d, y + d);
		pincel.setStyle(Paint.Style.STROKE);
		pincel.setStrokeWidth(grosor);
		pincel.setStrokeCap(Paint.Cap.BUTT);
		pincel.setColor(ContextCompat.getColor(getContext(), R.color.barra));
		c.drawArc(arco, 180, 180, false, pincel);
		pincel.setColor(color);
		c.drawArc(arco, 180, (float) (180 * Cuentas.llenado(consumido, objetivo)), false, pincel);

		float centroY = y + d / 2;
		pincel.setStyle(Paint.Style.FILL);
		pincel.setTextAlign(Paint.Align.CENTER);
		pincel.setColor(ContextCompat.getColor(getContext(), R.color.texto));
		pincel.setTypeface(Typeface.DEFAULT_BOLD);
		pincel.setTextSize(w * 0.12f);
		c.drawText(Cuentas.textoPorcentaje(consumido, objetivo), w / 2, centroY - w * 0.03f, pincel);
		pincel.setTextSize(w * 0.075f);
		c.drawText(titulo, w / 2, centroY + w * 0.07f, pincel);
		pincel.setTypeface(Typeface.DEFAULT);
		pincel.setColor(ContextCompat.getColor(getContext(), R.color.secundario));
		pincel.setTextSize(w * 0.07f);
		String detalle = Math.round(consumido) + (objetivo > 0 ? " / " + Math.round(objetivo) : "") + " " + unidad;
		c.drawText(detalle, w / 2, centroY + w * 0.16f, pincel);
	}
}
