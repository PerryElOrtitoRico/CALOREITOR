package com.caloreitor.app.android;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Un único hilo para todo lo que toca la carpeta, el repositorio y el historial (el repositorio no admite hilos), y el
 * hilo principal para la pantalla. Lo que se pide al hilo de E/S corre de a una, en el orden en que se pidió.
 */
public final class Hilos {
	private static final ExecutorService ES = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "caloreitor-es");
		t.setDaemon(true);
		return t;
	});

	private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

	private Hilos() {
	}

	/** Corre la tarea en el hilo de E/S. Una excepción que se escapa se anota (solo su clase) y no cierra la app. */
	public static void enEs(Runnable tarea) {
		ES.execute(() -> {
			try {
				tarea.run();
			} catch (RuntimeException e) {
				Log.w("Hilos", "Falló una tarea de E/S: " + e.getClass().getSimpleName());
			}
		});
	}

	/** Corre la tarea en el hilo principal. */
	public static void enPrincipal(Runnable tarea) {
		PRINCIPAL.post(tarea);
	}
}
