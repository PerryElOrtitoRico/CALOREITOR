package com.caloreitor.app.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.caloreitor.app.datos.HistorialDias;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.TotalDia;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Historial de totales por día en una base SQLite de este celular: misma tabla y columnas que la del escritorio. */
public final class HistorialSqlite extends SQLiteOpenHelper implements HistorialDias {
	private static final String COLS = "fecha, kcal, proteinas, carbohidratos, grasas, obj_kcal, obj_proteinas, obj_carbohidratos, obj_grasas";

	public HistorialSqlite(Context contexto) {
		super(contexto.getApplicationContext(), "historial.db", null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS dias(fecha TEXT PRIMARY KEY, kcal REAL, proteinas REAL, carbohidratos REAL, "
				+ "grasas REAL, obj_kcal REAL, obj_proteinas REAL, obj_carbohidratos REAL, obj_grasas REAL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int vieja, int nueva) {
	}

	@Override
	public void guardar(TotalDia t) {
		ContentValues v = new ContentValues();
		v.put("fecha", t.fecha());
		v.put("kcal", t.consumido().kcal());
		v.put("proteinas", t.consumido().proteinas());
		v.put("carbohidratos", t.consumido().carbohidratos());
		v.put("grasas", t.consumido().grasas());
		Macros o = t.objetivo();
		v.put("obj_kcal", o == null ? null : o.kcal());
		v.put("obj_proteinas", o == null ? null : o.proteinas());
		v.put("obj_carbohidratos", o == null ? null : o.carbohidratos());
		v.put("obj_grasas", o == null ? null : o.grasas());
		if (getWritableDatabase().replace("dias", null, v) < 0) throw new IllegalStateException("No se pudo guardar el historial");
	}

	@Override
	public Optional<TotalDia> leer(String fecha) {
		return entre(fecha, fecha).stream().findFirst();
	}

	@Override
	public List<TotalDia> entre(String desde, String hasta) {
		List<TotalDia> r = new ArrayList<>();
		try (Cursor c = getReadableDatabase().rawQuery("SELECT " + COLS + " FROM dias WHERE fecha BETWEEN ? AND ? ORDER BY fecha",
				new String[] {desde, hasta})) {
			while (c.moveToNext()) {
				Macros obj = c.isNull(5) ? null : new Macros(c.getDouble(5), c.getDouble(6), c.getDouble(7), c.getDouble(8));
				r.add(new TotalDia(c.getString(0), new Macros(c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getDouble(4)), obj));
			}
		}
		return r;
	}

	@Override
	public boolean existe(String fecha) {
		return leer(fecha).isPresent();
	}
}
