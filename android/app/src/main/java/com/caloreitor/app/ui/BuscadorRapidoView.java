package com.caloreitor.app.ui;

import android.content.Context;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDialog;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Busqueda;
import com.caloreitor.app.logica.Busqueda.Resultado;
import com.caloreitor.app.logica.Formato;
import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.ItemReceta;
import com.caloreitor.app.modelo.Receta;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Agregar comida rápido: el teclado abre solo, se escribe y se toca el resultado, que se agrega con los gramos sugeridos
 * (los gramos se editan después tocando el renglón). La ventana queda abierta para seguir agregando.
 */
final class BuscadorRapidoView extends LinearLayout {
	/** Un renglón de la lista: un resultado o la opción de crear. */
	private record Opcion(Resultado resultado, String crear) {
		@Override
		public String toString() {
			if (resultado == null) return "Crear «" + crear + "»…";
			return resultado.nombre() + (resultado.esReceta() ? "  (receta)" : "");
		}
	}

	/** Lo que se lee del repositorio en el hilo de E/S; la búsqueda en sí es cuenta pura y corre en la pantalla. */
	private record Datos(Busqueda busqueda, Map<String, Alimento> alimentos, Map<String, Receta> recetas, LocalDate hoy) {
	}

	private final Sesion sesion;
	private final LocalDate fecha;
	private final String comida;
	private final Runnable alCambiar;
	// qué se hace con cada alta (en el hilo de E/S): el registro real, el plan de la semana o los ingredientes de una receta
	private final Consumer<Consumo> destino;
	private final EditText campo;
	private final TextView estado;
	private final ArrayAdapter<Opcion> adaptador;
	private final ListView lista;
	private Datos datos;
	private long ultimaAlta;

	private BuscadorRapidoView(Context c, Sesion sesion, LocalDate fecha, String comida, Runnable alCambiar, Consumer<Consumo> destino,
			Runnable cerrar) {
		super(c);
		this.sesion = sesion;
		this.fecha = fecha;
		this.comida = comida;
		this.alCambiar = alCambiar;
		this.destino = destino;
		setOrientation(VERTICAL);
		setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 8));

		LinearLayout cabecera = Ui.renglon(c);
		TextView titulo = Ui.texto(c, "Agregar a " + comida.toLowerCase(), false, true);
		Ui.pesar(titulo);
		cabecera.addView(titulo);
		cabecera.addView(Ui.boton(c, "Listo", v -> cerrar.run()));
		addView(cabecera);

		campo = Ui.campo(c, "", false);
		campo.setHint("Buscar alimento o receta");
		campo.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
		addView(campo, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		estado = Ui.texto(c, " ", true, false);
		estado.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));
		addView(estado);
		adaptador = new ArrayAdapter<>(c, android.R.layout.simple_list_item_1);
		lista = new ListView(c);
		lista.setAdapter(adaptador);
		lista.setOnItemClickListener((p, v, i, id) -> agregar(adaptador.getItem(i)));
		addView(lista, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		campo.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
			public void onTextChanged(CharSequence s, int a, int b, int d) { }
			public void afterTextChanged(Editable e) { buscar(); }
		});
		cargarDatos();
	}

	/** Abre la ventana. alCambiar corre (en el hilo principal) después de cada alta; destino, en el hilo de E/S. */
	static void abrir(Context c, Sesion sesion, LocalDate fecha, String comida, Runnable alCambiar, Consumer<Consumo> destino) {
		AppCompatDialog d = new AppCompatDialog(c, R.style.Dialogo_Pantalla);
		BuscadorRapidoView v = new BuscadorRapidoView(c, sesion, fecha, comida, alCambiar, destino, d::dismiss);
		d.setContentView(v);
		d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		d.show();
		v.campo.requestFocus();
	}

	private void cargarDatos() {
		sesion.leer(() -> {
			var n = sesion.nucleo();
			List<Alimento> todos = n.todosLosAlimentos();
			List<Receta> recetas = n.repo().recetas();
			Map<String, Alimento> a = new HashMap<>();
			for (Alimento x : todos) a.put(x.id(), x);
			Map<String, Receta> r = new HashMap<>();
			for (Receta x : recetas) r.put(x.id(), x);
			return new Datos(new Busqueda(todos, recetas, n.consumosRecientes()), a, r, n.hoy());
		}, d -> {
			datos = d;
			buscar();
		});
	}

	private void buscar() {
		adaptador.clear();
		if (datos == null) return;
		String texto = campo.getText().toString();
		for (Resultado r : datos.busqueda().buscar(texto, comida, datos.hoy())) adaptador.add(new Opcion(r, null));
		if (!texto.isBlank()) adaptador.add(new Opcion(null, texto.trim()));
	}

	private void agregar(Opcion o) {
		// un doble toque no agrega dos veces
		if (o == null || datos == null || SystemClock.elapsedRealtime() - ultimaAlta < 500) return;
		ultimaAlta = SystemClock.elapsedRealtime();
		if (o.resultado() != null && o.resultado().esReceta()) {
			Consumo c = deReceta(o.resultado().id());
			if (c != null) guardar(c, null);
		} else if (o.resultado() == null) {
			Ui.pedirAlimento(getContext(), o.crear(), null, a -> guardar(unico(a, 100), a));
		} else {
			Alimento a = datos.alimentos().get(o.resultado().id());
			if (a != null) guardar(unico(a, o.resultado().gramosSugeridos()), null);
		}
	}

	private Consumo unico(Alimento a, double gramos) {
		return new Consumo(UUID.randomUUID().toString(), fecha.toString(), comida, null, null,
				List.of(new Item(a.id(), a.nombre(), a.por100(), gramos)));
	}

	/** Un solo consumo con los ingredientes como ítems (los que ya no existen como alimento se saltean). */
	private Consumo deReceta(String id) {
		Receta r = datos.recetas().get(id);
		if (r == null) return null;
		List<Item> items = new ArrayList<>();
		for (ItemReceta ing : r.ingredientes()) {
			Alimento a = datos.alimentos().get(ing.alimentoId());
			if (a != null) items.add(new Item(a.id(), a.nombre(), a.por100(), ing.gramos()));
		}
		if (items.isEmpty()) {
			estado.setText("La receta no tiene ingredientes disponibles");
			return null;
		}
		return new Consumo(UUID.randomUUID().toString(), fecha.toString(), comida, r.id(), r.nombre(), items);
	}

	/** El alimento nuevo (si hay) se guarda junto con su consumo: si falla algo se avisa y no queda a medias en silencio. */
	private void guardar(Consumo c, Alimento nuevo) {
		sesion.escribir(() -> {
			if (nuevo != null) sesion.nucleo().repo().guardar(nuevo);
			destino.accept(c);
		}, () -> {
			alCambiar.run();
			estado.setText("Agregado: " + (c.recetaNombre() != null ? c.recetaNombre()
					: c.items().get(0).nombre() + " " + Formato.gramos(c.items().get(0).gramos()) + " g"));
			campo.setText("");
			cargarDatos(); // el alimento creado ahora también se encuentra
		});
	}
}
