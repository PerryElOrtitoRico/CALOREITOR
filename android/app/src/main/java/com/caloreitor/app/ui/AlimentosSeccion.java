package com.caloreitor.app.ui;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Busqueda;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.logica.Formato;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.modelo.Alimento;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Alimentos propios y de la base: buscar, crear, editar y borrar (los de la base no se borran). */
final class AlimentosSeccion extends LinearLayout {
	/** Cuántos renglones se dibujan como máximo (la base tiene cientos). */
	private static final int MAXIMO = 40;

	private final Anfitrion anfitrion;
	private final EditText buscar;
	private final LinearLayout lista;

	AlimentosSeccion(Context c, Anfitrion anfitrion) {
		super(c);
		this.anfitrion = anfitrion;
		setOrientation(VERTICAL);
		addView(Ui.texto(c, "Alimentos", false, true));
		buscar = Ui.campo(c, "", false);
		buscar.setHint("Buscar alimento");
		buscar.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
			@Override public void onTextChanged(CharSequence s, int a, int b, int d) { refrescar(); }
			@Override public void afterTextChanged(Editable s) { }
		});
		addView(buscar);
		addView(Ui.boton(c, "Nuevo alimento…", v -> Ui.pedirAlimento(c, "", null, this::guardar)));
		lista = new LinearLayout(c);
		lista.setOrientation(VERTICAL);
		addView(lista);
	}

	void refrescar() {
		Sesion s = anfitrion.sesion();
		if (s == null) return;
		String texto = buscar.getText().toString();
		s.leer(() -> calcular(s.nucleo(), texto), this::pintar);
	}

	private void guardar(Alimento a) {
		Sesion s = anfitrion.sesion();
		if (s != null) s.escribir(() -> s.nucleo().repo().guardar(a), anfitrion::cambio);
	}

	private void editar(Alimento previo) {
		Ui.pedirAlimento(getContext(), previo.nombre(), previo, this::guardar);
	}

	private void borrar(Alimento a) {
		Sesion s = anfitrion.sesion();
		if (s == null) return;
		new AlertDialog.Builder(getContext()).setTitle("Borrar alimento").setMessage("¿Borrar «" + a.nombre() + "»? Lo ya registrado no cambia.")
				.setPositiveButton("Borrar", (d, i) -> s.escribir(() -> s.nucleo().repo().borrarAlimento(a.id()), anfitrion::cambio))
				.setNegativeButton("Cancelar", null).show();
	}

	/** Corre en el hilo de E/S. Devuelve a lo sumo MAXIMO alimentos. */
	private static List<Alimento> calcular(Nucleo n, String texto) {
		// 1. Obtener alimentos visibles
		List<Alimento> visibles = Cuentas.visibles(n.todosLosAlimentos());
		// 2. Inicializar lista de resultados
		List<Alimento> mostrar = new ArrayList<>();
		// 3. Lógica de filtrado/orden
		if (texto.isBlank()) {
			// Si no hay texto, mostrar todos los visibles ordenados por nombre
			mostrar.addAll(visibles);
			mostrar.sort(Comparator.comparing(a -> a.nombre().toLowerCase()));
		} else {
			// Si hay texto, buscar en los visibles
			Map<String, Alimento> porId = new HashMap<>();
			for (Alimento a : visibles) {
				porId.putIfAbsent(a.id(), a);
			}
			// Realizar la búsqueda
			List<Busqueda.Resultado> resultadosBusqueda = new Busqueda(visibles, List.of(), List.of()).buscar(texto, "", LocalDate.now());
			for (Busqueda.Resultado r : resultadosBusqueda) {
				Alimento a = porId.get(r.id());
				if (a != null) {
					mostrar.add(a);
				}
			}
		}
		// 6. Limitar el resultado al máximo permitido
		return mostrar.size() > MAXIMO ? new ArrayList<>(mostrar.subList(0, MAXIMO)) : mostrar;
	}

	private void pintar(List<Alimento> alimentos) {
		// 1. Limpiar la lista de visualización
		Context c = getContext();
		lista.removeAllViews();

		// 2. Iterar sobre los alimentos recibidos
		for (Alimento a : alimentos) {
			// Crear el renglón principal
			LinearLayout r = Ui.renglon(c);
			// Contenedor para los datos del alimento
			LinearLayout datos = new LinearLayout(c);
			datos.setOrientation(LinearLayout.VERTICAL);
			Ui.pesar(datos);

			// 3. Añadir nombre y tipo (base o no)
			datos.addView(Ui.texto(c, a.nombre() + (a.base() ? "  (base)" : ""), false, true));
			// 4. Añadir información de macros
			datos.addView(Ui.texto(c, Formato.macros(a.por100()) + "  (por 100 g)", true, false));

			// 5. Añadir el contenedor de datos al renglón y configurar el click para editar
			r.addView(datos);
			r.setOnClickListener(v -> editar(a));

			// 6. Añadir botón de borrar si el alimento no es de la base
			if (!a.base()) {
				r.addView(Ui.boton(c, "Borrar", v -> borrar(a)));
			}

			// 7. Añadir el renglón completo y un separador
			lista.addView(r);
			lista.addView(Ui.separador(c));
		}

		// 8. Mostrar mensaje si se alcanzó el límite
		if (alimentos.size() >= MAXIMO) {
			lista.addView(Ui.texto(c, "Se muestran los primeros " + MAXIMO + ": escribí para buscar.", true, false));
		}
	}
}
