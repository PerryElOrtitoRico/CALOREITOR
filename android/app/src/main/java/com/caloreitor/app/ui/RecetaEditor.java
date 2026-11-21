package com.caloreitor.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.logica.Formato;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Receta;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Diálogo para crear o editar una receta: nombre e ingredientes con gramos; trabaja sobre una copia hasta apretar Guardar. */
final class RecetaEditor {
	/** Lo que se dibuja, ya calculado en el hilo de E/S. */
	private record Vista(List<Alimento> todos, Macros total) {
	}

	private final Context c;
	private final Sesion s;
	private final Receta previa;
	private final Consumer<Receta> alGuardar;
	private final EditText nombre;
	private final LinearLayout filas;
	private final TextView total;
	private final List<ItemReceta> ingredientes = new ArrayList<>();

	private RecetaEditor(Context c, Sesion s, Receta previa, Consumer<Receta> alGuardar) {
		this.c = c;
		this.s = s;
		this.previa = previa;
		this.alGuardar = alGuardar;
		nombre = Ui.campo(c, previa == null ? "" : previa.nombre(), false);
		nombre.setHint("Nombre de la receta");
		filas = new LinearLayout(c);
		filas.setOrientation(LinearLayout.VERTICAL);
		total = Ui.texto(c, " ", true, false);
		if (previa != null) ingredientes.addAll(previa.ingredientes());
	}

	/** previa es null para una receta nueva; alGuardar corre en el hilo principal con la receta lista. */
	static void abrir(Context c, Sesion s, Receta previa, Consumer<Receta> alGuardar) {
		RecetaEditor e = new RecetaEditor(c, s, previa, alGuardar);
		LinearLayout cuerpo = new LinearLayout(c);
		cuerpo.setOrientation(LinearLayout.VERTICAL);
		cuerpo.addView(e.nombre);
		cuerpo.addView(e.filas);
		cuerpo.addView(e.total);
		cuerpo.addView(Ui.boton(c, "+ Ingrediente", v -> BuscadorRapidoView.abrir(c, s, s.nucleo().hoy(), "la receta", e::dibujar, e::agregar)));
		android.widget.ScrollView scroll = new android.widget.ScrollView(c);
		scroll.addView(cuerpo);
		AlertDialog d = new AlertDialog.Builder(c).setTitle(previa == null ? "Receta nueva" : "Editar receta").setView(Ui.conMargen(c, scroll))
				.setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create();
		d.show();
		// el listener va después de show() para que un nombre vacío no cierre el diálogo
		d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> e.guardar(d));
		e.dibujar();
	}

	/** Un resultado del buscador (alimento o receta ya desarmada en sus ítems) se suma como ingredientes. Corre en el hilo de E/S. */
	private void agregar(com.caloreitor.app.modelo.Consumo consumo) {
		for (Item i : consumo.items()) ingredientes.add(new ItemReceta(i.alimentoId(), i.gramos()));
	}

	/** Pide los alimentos y el total al hilo de E/S y los pinta. */
	private void dibujar() {
		List<ItemReceta> copia = List.copyOf(ingredientes);
		s.leer(() -> {
			List<Alimento> t = s.nucleo().todosLosAlimentos();
			return new Vista(t, Cuentas.totalReceta(copia, t));
		}, v -> pintar(v, copia));
	}

	private void pintar(Vista v, List<ItemReceta> lista) {
		filas.removeAllViews();
		for (int i = 0; i < lista.size(); i++) {
			final int pos = i;
			ItemReceta ing = lista.get(i);
			String nom = "(alimento borrado)";
			for (Alimento a : v.todos()) if (a.id().equals(ing.alimentoId())) nom = a.nombre();
			final String nombreIng = nom;
			LinearLayout fila = Ui.renglon(c);
			android.widget.TextView t = Ui.texto(c, nombreIng + " — " + Formato.gramos(ing.gramos()) + " g", false, false);
			Ui.pesar(t);
			fila.addView(t);
			fila.setOnClickListener(x -> Ui.pedirGramos(c, nombreIng, ing.gramos(), g -> { ingredientes.set(pos, new ItemReceta(ing.alimentoId(), g)); dibujar(); }));
			fila.addView(Ui.boton(c, "✕", x -> { ingredientes.remove(pos); dibujar(); }));
			filas.addView(fila);
		}
		total.setText("Total: " + Formato.macros(v.total()));
	}

	private void guardar(AlertDialog d) {
		String n = nombre.getText().toString().trim();
		if (n.isEmpty()) { Ui.aviso(c, "Escribí un nombre para la receta"); return; }
		if (ingredientes.isEmpty()) { Ui.aviso(c, "Agregá al menos un ingrediente"); return; }
		alGuardar.accept(new Receta(previa == null ? UUID.randomUUID().toString() : previa.id(), n, List.copyOf(ingredientes)));
		d.dismiss();
	}
}