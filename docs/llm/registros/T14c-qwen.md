// android/app/src/main/java/com/caloreitor/app/ui/PlanificadorView.java
package com.caloreitor.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.caloreitor.app.R;
import com.caloreitor.app.android.Sesion;
import com.caloreitor.app.logica.Cuentas;
import com.caloreitor.app.logica.Formato;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.logica.Planificador;
import com.caloreitor.app.modelo.Configuracion;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.Objetivo;
import com.caloreitor.app.modelo.PlanSemanal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

/** La semana lunes→domingo: objetivo fijado de cada día, comidas planeadas, falta/sobra y lista del súper. */
final class PlanificadorView extends VistaBase {
	private static final Locale ES = Locale.forLanguageTag("es");
	private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d/M", ES);
	private static final DateTimeFormatter CORTO = DateTimeFormatter.ofPattern("d/M", ES);

	/** Una comida de un día: lo planeado, y si ya se registró lo mismo en el día de hoy. */
	private record Comida(String nombre, List<Consumo> planeadas, boolean hecho) {
	}

	/** objetivo es null si faltan los datos personales. */
	private record Dia(LocalDate fecha, boolean esHoy, Macros objetivo, Macros planeado, List<Comida> comidas) {
	}

	/** hay es false si la semana todavía no está armada. */
	private record Datos(boolean hay, List<Dia> dias) {
	}

	private volatile LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
	private final TextView titulo;
	private final LinearLayout contenido;
	private final View recalcular, superLista;

	PlanificadorView(Context c, Anfitrion anfitrion) {
		super(c, anfitrion, R.layout.view_lista, true);
		LinearLayout contenedor = ver(R.id.contenedor);
		LinearLayout nav = Ui.renglon(c);
		nav.addView(Ui.boton(c, c.getString(R.string.anterior), v -> ir(lunes.minusWeeks(1))));
		nav.addView(Ui.boton(c, c.getString(R.string.siguiente), v -> ir(lunes.plusWeeks(1))));
		nav.addView(Ui.boton(c, c.getString(R.string.esta_semana), v -> ir(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))));
		contenedor.addView(nav);
		titulo = Ui.texto(c, " ", false, true);
		contenedor.addView(titulo);
		LinearLayout acciones = Ui.renglon(c);
		recalcular = Ui.boton(c, c.getString(R.string.recalcular), v -> mutar(n -> planificador(n).recalcular(plan(n))));
		superLista = Ui.boton(c, c.getString(R.string.lista_super), v -> listaDelSuper());
		acciones.addView(recalcular);
		acciones.addView(superLista);
		contenedor.addView(acciones);
		contenido = new LinearLayout(c);
		contenido.setOrientation(LinearLayout.VERTICAL);
		contenedor.addView(contenido);
	}

	private void ir(LocalDate nuevoLunes) {
		lunes = nuevoLunes;
		refrescar();
	}

	@Override
	protected void cargar(Sesion s) {
		LocalDate l = lunes;
		s.leer(() -> calcular(s.nucleo(), l), d -> pintar(l, d));
	}

	// ---- cableado de escritura: todo cambio pasa por el cerrojo de la Sesion ----

	private static Planificador planificador(Nucleo n) {
		return new Planificador(n.repo(), n.objetivos());
	}

	private PlanSemanal plan(Nucleo n) {
		return n.repo().planSemanal(lunes.toString()).orElseThrow();
	}

	private void mutar(Consumer<Nucleo> cambio) {
		Sesion s = anfitrion.sesion();
		if (s != null) s.escribir(() -> cambio.accept(s.nucleo()), anfitrion::cambio);
	}

	private void armar() {
		LocalDate l = lunes;
		mutar(n -> planificador(n).crear(l));
	}

	private void agregar(LocalDate fecha, String comida) {
		Sesion s = anfitrion.sesion();
		if (s != null) BuscadorRapidoView.abrir(getContext(), s, fecha, comida, anfitrion::cambio, c -> planificador(s.nucleo()).agregar(plan(s.nucleo()), c));
	}

	private void quitar(String consumoId) {
		mutar(n -> planificador(n).quitar(plan(n), consumoId));
	}

	private void comiLoPlaneado(List<Consumo> planeadas) {
		mutar(n -> {
			for (Consumo c : planeadas) planificador(n).comiLoPlaneado(plan(n), c.id());
		});
	}

	// ---- lo escribe qwen ----

	/** Corre en el hilo de E/S. */
	private static Datos calcular(Nucleo n, LocalDate lunes) {
		java.util.Optional<PlanSemanal> op = n.repo().planSemanal(lunes.toString());
		if (op.isEmpty()) return new Datos(false, List.of());
		PlanSemanal plan = op.get();
		Configuracion cfg = n.repo().configuracion();
		Planificador pl = planificador(n);
		List<Dia> dias = new ArrayList<>();
		for (int d = 0; d < 7; d++) {
			LocalDate f = lunes.plusDays(d);
			String fecha = f.toString();
			Objetivo obj = plan.objetivos().get(fecha);
			Macros planeado = pl.planeado(plan, fecha);
			Set<String> nombres = new LinkedHashSet<>(cfg.comidas());
			for (Consumo c : plan.comidas()) if (c.fecha().equals(fecha)) nombres.add(c.comida());
			List<Comida> comidas = new ArrayList<>();
			for (String nombre : nombres) {
				List<Consumo> deEsta = plan.comidas().stream().filter(c -> c.fecha().equals(fecha) && c.comida().equals(nombre)).collect(java.util.stream.Collectors.toList());
				boolean hecho = false;
				if (f.equals(n.hoy()) && !deEsta.isEmpty()) { List<Consumo> reales = n.repo().consumos(fecha).stream().filter(c -> c.comida().equals(nombre)).collect(java.util.stream.Collectors.toList()); hecho = Cuentas.yaComido(deEsta, reales); }
				comidas.add(new Comida(nombre, deEsta, hecho));
			}
			dias.add(new Dia(f, f.equals(n.hoy()), obj == null ? null : obj.macros(), planeado, comidas));
		}
		return new Datos(true, dias);
	}

	/** Hilo principal. */
	private void pintar(LocalDate lunesPintado, Datos d) {
		Context c = getContext();
		titulo.setText("Semana del " + CORTO.format(lunesPintado) + " al " + CORTO.format(lunesPintado.plusDays(6)));
		recalcular.setEnabled(d.hay());
		superLista.setEnabled(d.hay());
		contenido.removeAllViews();
		if (!d.hay()) {
			contenido.addView(Ui.texto(c, c.getString(R.string.semana_sin_armar), true, false));
			contenido.addView(Ui.boton(c, c.getString(R.string.armar_semana), v -> armar()));
			return;
		}
		for (Dia dia : d.dias()) {
			contenido.addView(Ui.texto(c, DIA.format(dia.fecha()) + (dia.esHoy() ? "  (hoy)" : ""), false, true));
			String resumen = dia.objetivo() == null ? c.getString(R.string.sin_datos_personales) : "Objetivo " + Formato.macros(dia.objetivo()) + "\nPlaneado " + Formato.macros(dia.planeado()) + "\n" + faltaSobra(Cuentas.faltaSobra(dia.objetivo(), dia.planeado()));
			contenido.addView(Ui.texto(c, resumen, true, false));
			for (Comida com : dia.comidas()) {
				LinearLayout r = Ui.renglon(c);
				TextView t = Ui.texto(c, com.nombre(), false, false);
				Ui.pesar(t);
				r.addView(t);
				r.addView(Ui.boton(c, c.getString(R.string.agregar), v -> agregar(dia.fecha(), com.nombre())));
				if (dia.esHoy() && !com.planeadas().isEmpty()) {
					android.widget.Button comi = Ui.boton(c, com.hecho() ? "Ya registrado" : "Comí lo planeado", v -> comiLoPlaneado(com.planeadas()));
					comi.setEnabled(!com.hecho());
					r.addView(comi);
				}
				contenido.addView(r);
				for (Consumo x : com.planeadas()) {
					StringBuilder items = new StringBuilder();
					for (Item i : x.items()) {
						if (items.length() > 0) items.append(", ");
						items.append(i.nombre()).append(' ').append(Formato.gramos(i.gramos())).append(" g");
					}
					String que = (x.recetaNombre() != null ? "Receta " + x.recetaNombre() + ": " : "") + items + " — " + Math.round(x.macros().kcal()) + " kcal";
					LinearLayout rx = Ui.renglon(c);
					TextView tx = Ui.texto(c, que, true, false);
					Ui.pesar(tx);
					rx.addView(tx);
					rx.addView(Ui.boton(c, "✕", v -> quitar(x.id())));
					contenido.addView(rx);
				}
			}
			contenido.addView(Ui.separador(c));
		}
	}

	private void listaDelSuper() {
		Sesion s = anfitrion.sesion();
		if (s == null) return;
		Context c = getContext();
		LocalDate l = lunes;
		TextView texto = Ui.texto(c, " ", false, false);
		texto.setTextIsSelectable(true);
		LinearLayout dias = Ui.renglon(c);
		List<CheckBox> marcas = new ArrayList<>();
		Runnable actualizar = () -> {
			Set<String> fechas = new TreeSet<>();
			for (int d = 0; d < 7; d++) if (marcas.get(d).isChecked()) fechas.add(l.plusDays(d).toString());
			s.leer(() -> {
				String t = Planificador.comoTexto(planificador(s.nucleo()).listaSuper(plan(s.nucleo()), fechas));
				return t.isEmpty() ? "(No hay nada planeado en los días elegidos)" : t;
			}, texto::setText);
		};
		for (int d = 0; d < 7; d++) {
			CheckBox m = new CheckBox(c);
			m.setText(l.plusDays(d).getDayOfWeek().getDisplayName(TextStyle.SHORT, ES));
			m.setChecked(true);
			m.setOnClickListener(v -> actualizar.run());
			marcas.add(m);
			dias.addView(m);
		}
		android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c);
		hs.addView(dias);
		LinearLayout cuerpo = new LinearLayout(c);
		cuerpo.setOrientation(LinearLayout.VERTICAL);
		cuerpo.addView(hs);
		cuerpo.addView(texto);
		android.widget.ScrollView scroll = new android.widget.ScrollView(c);
		scroll.addView(cuerpo);
		new AlertDialog.Builder(c).setTitle("Lista del súper").setView(Ui.conMargen(c, scroll)).setPositiveButton("Copiar", (dlg, i) -> {
			ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setPrimaryClip(ClipData.newPlainText("Lista del súper", texto.getText()));
			Ui.aviso(c, "Copiado");
		}).setNegativeButton("Cerrar", null).show();
		actualizar.run();
	}

	/** "falta 300", "sobra 12" o "justo" por macro. */
	static String faltaSobra(Macros m) {
		return "kcal " + fs(m.kcal()) + " · P " + fs(m.proteinas()) + " · C " + fs(m.carbohidratos()) + " · G " + fs(m.grasas());
	}

	private static String fs(double v) {
		long r = Math.round(v);
		return r > 0 ? "falta " + r : r < 0 ? "sobra " + (-r) : "justo";
	}
}