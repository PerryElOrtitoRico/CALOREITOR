package com.caloreitor.app.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.caloreitor.app.red.ConexionPanel;

/** Ajustes de este celular, en SharedPreferences privadas (allowBackup=false). El token no se anota en ningún log. */
public final class Ajustes {
	private final SharedPreferences prefs;

	public Ajustes(Context contexto) {
		prefs = contexto.getApplicationContext().getSharedPreferences("ajustes", Context.MODE_PRIVATE);
	}

	/** Null si todavía no se eligió la carpeta. */
	public Uri carpeta() {
		String u = prefs.getString("carpeta", null);
		return u == null || u.isEmpty() ? null : Uri.parse(u);
	}

	public void elegirCarpeta(Uri arbol) {
		prefs.edit().putString("carpeta", arbol.toString()).commit();
	}

	public String dispositivo() {
		return prefs.getString("dispositivo", "celular");
	}

	public void elegirDispositivo(String nombre) {
		prefs.edit().putString("dispositivo", nombre).commit();
	}

	/** Sin dispositivo guardado usa el nombre de este celular. */
	public ConexionPanel conexionPanel() {
		return new ConexionPanel(prefs.getString("panelUrl", ""), prefs.getString("panelToken", ""),
				prefs.getString("panelDispositivo", dispositivo()), prefs.getBoolean("panelEnviar", false));
	}

	public boolean guardarConexionPanel(ConexionPanel c) {
		return prefs.edit().putString("panelUrl", c.url()).putString("panelToken", c.token())
				.putString("panelDispositivo", c.dispositivo()).putBoolean("panelEnviar", c.enviar()).commit();
	}
}
