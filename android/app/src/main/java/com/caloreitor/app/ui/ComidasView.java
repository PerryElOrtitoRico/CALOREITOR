package com.caloreitor.app.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Formato;
import com.caloreitor.app.logica.Textos;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.TotalDia;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Las comidas de un día, navegable: agregar, editar gramos tocando el renglón, borrar y repetir de otro día. */
final class ComidasView extends VistaBase {
	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale.forLanguageTag("es"));

	/** archivado: más de 31 días, solo quedan los totales (igual que Depuracion). total es null si no hay ni eso. */
	private record Datos(LocalDate dia, LocalDate hoy, boolean archivado, TotalDia total, List<String> comidas, List<String> comidasConfig,
			List<Consumo> consumos) {
	}

	private final LinearLayout contenedor;
	private final Button repetir;
	private LocalDate dia;
	private List<String> comidasConfig = List.of();

	ComidasView(Context c, Anfitrion anfitrion) {
		super(c, anfitrion, R.layout.view_comidas, true);
		contenedor = ver(R.id.contenedor);
		repetir = ver(R.id.boton_repetir);
		ver(R.id.boton_anterior).setOnClickListener(v -> ir(dia.minusDays(1)));
		ver(R.id.boton_siguiente).setOnClickListener(v -> ir(dia.plusDays(1)));
		ver(R.id.boton_hoy).setOnClickListener(v -> ir(anfitrion.sesion().nucleo().hoy()));
		repetir.setOnClickListener(v -> elegirOrigen());
	}

	private void ir(LocalDate d) {
		dia = d;
		refrescar();
	}

	@Override
	protected void cargar(Sesion s) {
		LocalDate pedido = dia; // null la primera vez: se muestra hoy
		s.leer(() -> {
			var n = s.nucleo();
			LocalDate d = pedido == null ? n.hoy() : pedido;
			boolean archivado = d.isBefore(n.hoy().minusDays(31));
			var cfg = n.repo().configuracion();
			if (archivado) return new Datos(d, n.hoy(), true, n.historial().leer(d.toString()).orElse(null), List.of(), cfg.comidas(), List.of());
			List<Consumo> consumos = n.repo().consumos(d.toString());
			// las comidas configuradas y, al final, las que tienen consumos pero ya no están en la lista
			Set<String> comidas = new LinkedHashSet<>(cfg.comidas());
			for (Consumo c : consumos) comidas.add(c.comida());
			return new Datos(d, n.hoy(), false, null, new ArrayList<>(comidas), cfg.comidas(), consumos);
		}, d -> {
			dia = d.dia();
			pintar(d);
		});
	}

	private void pintar(Datos d) {
		Context c = getContext();
		comidasConfig = d.comidasConfig();
		((TextView) ver(R.id.titulo_dia)).setText(FECHA.format(d.dia()));
		repetir.setEnabled(!d.archivado());
		contenedor.removeAllViews();
		if (d.archivado()) {
			if (d.total() == null) contenedor.addView(Ui.texto(c, "Sin datos", false, false));
			else {
				contenedor.addView(Ui.texto(c, "Consumido: " + Formato.macros(d.total().consumido()), false, false));
				Macros o = d.total().objetivo();
				contenedor.addView(Ui.texto(c, o == null ? "Sin objetivo ese día" : "Objetivo: " + Formato.macros(o), false, false));
				contenedor.addView(Ui.texto(c, Textos.SOLO_TOTALES, true, false));
			}
			return;
		}
		for (String comida : d.comidas()) {
			List<Consumo> deEsta = d.consumos().stream().filter(x -> comida.equals(x.comida())).toList();
			Macros total = Macros.CERO;
			for (Consumo x : deEsta) total = total.mas(x.macros());
			LinearLayout cab = Ui.renglon(c);
			TextView nombre = Ui.texto(c, comida + " — " + Formato.macros(total), false, true);
			Ui.pesar(nombre);
			cab.addView(nombre);
			cab.addView(Ui.boton(c, c.getString(R.string.agregar), v -> {
				Sesion s = anfitrion.sesion();
				BuscadorRapidoView.abrir(c, s, d.dia(), comida, anfitrion::cambio, x -> s.nucleo().repo().guardar(x));
			}));
			cab.setPadding(0, Ui.dp(c, 8), 0, 0);
			contenedor.addView(cab);
			contenedor.addView(Ui.separador(c));
			for (Consumo x : deEsta) dibujarConsumo(c, x);
		}
	}

	private void dibujarConsumo(Context c, Consumo x) {
		if (x.recetaNombre() != null) {
			LinearLayout p = Ui.renglon(c);
			TextView t = Ui.texto(c, "Receta: " + x.recetaNombre(), true, false);
			Ui.pesar(t);
			p.addView(t);
			p.addView(Ui.boton(c, "✕", v -> new AlertDialog.Builder(c).setMessage("¿Borrar la receta «" + x.recetaNombre() + "» entera de este día?")
					.setPositiveButton("Borrar", (a, i) -> escribir(() -> anfitrion.sesion().nucleo().repo().borrarConsumo(x.id())))
					.setNegativeButton("Cancelar", null).show()));
			contenedor.addView(p);
		}
		for (int pos = 0; pos < x.items().size(); pos++) contenedor.addView(renglon(c, x, pos));
	}

	/** Tocar el renglón edita los gramos; ✕ saca el alimento (si era el último, el consumo entero). */
	private View renglon(Context c, Consumo x, int pos) {
		Item it = x.items().get(pos);
		LinearLayout p = Ui.renglon(c);
		p.setPadding(Ui.dp(c, x.recetaNombre() != null ? 24 : 8), 0, 0, 0);
		TextView t = Ui.texto(c, it.nombre() + " · " + Formato.gramos(it.gramos()) + " g · " + Math.round(it.macros().kcal()) + " kcal", false, false);
		Ui.pesar(t);
		p.addView(t);
		p.setClickable(true);
		p.setOnClickListener(v -> Ui.pedirGramos(c, it.nombre(), it.gramos(), g -> {
			List<Item> items = new ArrayList<>(x.items());
			items.set(pos, new Item(it.alimentoId(), it.nombre(), it.por100(), g));
			escribir(() -> anfitrion.sesion().nucleo().repo().guardar(new Consumo(x.id(), x.fecha(), x.comida(), x.recetaId(), x.recetaNombre(), items)));
		}));
		p.addView(Ui.boton(c, "✕", v -> {
			List<Item> items = new ArrayList<>(x.items());
			items.remove(pos);
			escribir(() -> {
				var repo = anfitrion.sesion().nucleo().repo();
				if (items.isEmpty()) repo.borrarConsumo(x.id());
				else repo.guardar(new Consumo(x.id(), x.fecha(), x.comida(), x.recetaId(), x.recetaNombre(), items));
			});
		}));
		return p;
	}

	private void escribir(Runnable cambio) {
		anfitrion.sesion().escribir(cambio, anfitrion::cambio);
	}

	/** Primero el día de origen (calendario) y después la comida; copia con ids nuevos a este día. */
	private void elegirOrigen() {
		Context c = getContext();
		LocalDate ini = dia.minusDays(1);
		new DatePickerDialog(c, (picker, a, m, d) -> elegirComida(LocalDate.of(a, m + 1, d)), ini.getYear(), ini.getMonthValue() - 1, ini.getDayOfMonth()).show();
	}

	private void elegirComida(LocalDate origen) {
		Context c = getContext();
		String[] nombres = comidasConfig.toArray(new String[0]);
		new AlertDialog.Builder(c).setTitle("Repetir de " + origen).setItems(nombres, (d, i) -> repetir(origen, nombres[i])).show();
	}

	private void repetir(LocalDate origen, String comida) {
		Sesion s = anfitrion.sesion();
		LocalDate destino = dia;
		s.leer(() -> s.nucleo().repo().consumos(origen.toString()).stream().filter(x -> x.comida().equals(comida)).toList(), fuente -> {
			if (fuente.isEmpty()) {
				Ui.aviso(getContext(), "Ese día no tiene consumos en " + comida);
				return;
			}
			s.escribir(() -> {
				for (Consumo x : fuente) {
					s.nucleo().repo().guardar(new Consumo(UUID.randomUUID().toString(), destino.toString(), comida, x.recetaId(), x.recetaNombre(), x.items()));
				}
			}, anfitrion::cambio);
		});
	}
}
