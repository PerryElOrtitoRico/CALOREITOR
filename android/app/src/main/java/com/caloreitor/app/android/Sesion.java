package com.caloreitor.app.android;

import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;
import com.caloreitor.app.datos.ArchivoEventos;
import com.caloreitor.app.logica.BaseAlimentos;
import com.caloreitor.app.logica.Nucleo;
import com.caloreitor.app.modelo.Alimento;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * El núcleo común más lo de Android: cómo se abre la carpeta, y cómo se lee y se escribe sin trabar la pantalla. Toda
 * lectura o cambio al repositorio va por el hilo de E/S; la pantalla solo ve los resultados ya calculados.
 */
public final class Sesion {
	/** Cómo está la carpeta de sincronización. SIN_ELEGIR y SIN_PERMISO no tienen sesión; VACIA sí. */
	public enum Estado { LISTA, SIN_ELEGIR, SIN_PERMISO, VACIA }

	/** sesion es null salvo en LISTA y VACIA. */
	public record Apertura(Estado estado, Sesion sesion) {
	}

	private final Context contexto;
	private final Nucleo nucleo;

	private Sesion(Context contexto, Nucleo nucleo) {
		this.contexto = contexto;
		this.nucleo = nucleo;
	}

	/** Lee los eventos y corre la depuración: llamar en el hilo de E/S. Una carpeta que no se puede leer cuenta como permiso perdido. */
	public static Apertura abrir(Context contexto, Ajustes ajustes) {
		Context app = contexto.getApplicationContext();
		Uri arbol = ajustes.carpeta();
		if (arbol == null) return new Apertura(Estado.SIN_ELEGIR, null);
		boolean permiso = false;
		for (UriPermission p : app.getContentResolver().getPersistedUriPermissions()) {
			if (p.getUri().equals(arbol) && p.isReadPermission() && p.isWritePermission()) permiso = true;
		}
		if (!permiso) return new Apertura(Estado.SIN_PERMISO, null);
		try {
			CarpetaSaf carpeta = new CarpetaSaf(app, arbol);
			boolean vacia = carpeta.archivos().isEmpty();
			Nucleo n = new Nucleo(new ArchivoEventos(carpeta, ajustes.dispositivo()), new HistorialSqlite(app), baseDeAlimentos());
			return new Apertura(vacia ? Estado.VACIA : Estado.LISTA, new Sesion(app, n));
		} catch (RuntimeException e) {
			Log.w("Sesion", "No se pudo leer la carpeta: " + e.getClass().getSimpleName());
			return new Apertura(Estado.SIN_PERMISO, null);
		}
	}

	private static List<Alimento> baseDeAlimentos() {
		try (InputStream in = Sesion.class.getResourceAsStream("/alimentos-base.csv")) {
			return BaseAlimentos.cargar(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public Nucleo nucleo() {
		return nucleo;
	}

	/** Calcula en el hilo de E/S (acá se puede tocar el repositorio) y entrega el resultado en el hilo principal. */
	public <T> void leer(Supplier<T> calcular, Consumer<T> pintar) {
		Hilos.enEs(() -> {
			T r;
			try {
				r = calcular.get();
			} catch (RuntimeException e) {
				Log.w("Sesion", "Falló una lectura: " + e.getClass().getSimpleName());
				return;
			}
			Hilos.enPrincipal(() -> pintar.accept(r));
		});
	}

	/**
	 * Llamar desde el hilo principal. Corre el cambio en el hilo de E/S. Si se está releyendo no lo hace y avisa (no se
	 * pierde en silencio); si falla avisa el error. alTerminar corre en el hilo principal solo si se guardó.
	 */
	public void escribir(Runnable cambio, Runnable alTerminar) {
		if (nucleo.leyendo()) {
			Toast.makeText(contexto, "Se está actualizando: el cambio no se guardó, repetilo.", Toast.LENGTH_LONG).show();
			return;
		}
		Hilos.enEs(() -> {
			try {
				cambio.run();
			} catch (RuntimeException e) {
				Hilos.enPrincipal(() -> Toast.makeText(contexto, "No se pudo guardar el cambio: " + e, Toast.LENGTH_LONG).show());
				return;
			}
			Hilos.enPrincipal(alTerminar);
		});
	}

	/** Llamar desde el hilo principal. Relee y depura en el hilo de E/S; alTerminar(true) si anduvo, en el hilo principal. */
	public void releer(Consumer<Boolean> alTerminar) {
		if (nucleo.leyendo()) return;
		nucleo.leyendo(true);
		Hilos.enEs(() -> {
			boolean ok = true;
			try {
				nucleo.recargar();
			} catch (RuntimeException e) {
				ok = false;
				Log.w("Sesion", "No se pudo releer: " + e.getClass().getSimpleName());
			}
			boolean anduvo = ok;
			Hilos.enPrincipal(() -> {
				nucleo.leyendo(false);
				alTerminar.accept(anduvo);
			});
		});
	}
}
