package com.caloreitor.escritorio.infra;

import com.caloreitor.app.datos.HistorialDias;
import com.caloreitor.app.modelo.Macros;
import com.caloreitor.app.modelo.TotalDia;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Historial en una base SQLite propia de este dispositivo (nunca dentro de la carpeta de Syncthing). */
public final class HistorialSqlite implements HistorialDias {
	private static final String COLS = "fecha, kcal, proteinas, carbohidratos, grasas, obj_kcal, obj_proteinas, obj_carbohidratos, obj_grasas";
	private final String url;

	public static Path rutaPorDefecto() {
		return Path.of(System.getProperty("user.home"), ".config", "caloreitor", "historial.db");
	}

	public HistorialSqlite(Path archivo) {
		try {
			Files.createDirectories(archivo.toAbsolutePath().getParent());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		url = "jdbc:sqlite:" + archivo.toAbsolutePath();
		try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
			s.execute("CREATE TABLE IF NOT EXISTS dias(fecha TEXT PRIMARY KEY, kcal REAL, proteinas REAL, carbohidratos REAL, "
					+ "grasas REAL, obj_kcal REAL, obj_proteinas REAL, obj_carbohidratos REAL, obj_grasas REAL)");
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public void guardar(TotalDia t) {
		try (Connection c = DriverManager.getConnection(url);
				PreparedStatement p = c.prepareStatement("INSERT OR REPLACE INTO dias(" + COLS + ") VALUES (?,?,?,?,?,?,?,?,?)")) {
			p.setString(1, t.fecha());
			poner(p, 2, t.consumido());
			Macros o = t.objetivo();
			if (o == null) {
				for (int i = 6; i <= 9; i++) p.setNull(i, java.sql.Types.REAL);
			} else {
				poner(p, 6, o);
			}
			p.executeUpdate();
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public Optional<TotalDia> leer(String fecha) {
		return entre(fecha, fecha).stream().findFirst();
	}

	public List<TotalDia> entre(String desde, String hasta) {
		try (Connection c = DriverManager.getConnection(url);
				PreparedStatement p = c.prepareStatement("SELECT " + COLS + " FROM dias WHERE fecha BETWEEN ? AND ? ORDER BY fecha")) {
			p.setString(1, desde);
			p.setString(2, hasta);
			List<TotalDia> r = new ArrayList<>();
			try (ResultSet rs = p.executeQuery()) {
				while (rs.next()) {
					rs.getDouble(6);
					Macros obj = rs.wasNull() ? null : new Macros(rs.getDouble(6), rs.getDouble(7), rs.getDouble(8), rs.getDouble(9));
					r.add(new TotalDia(rs.getString(1), new Macros(rs.getDouble(2), rs.getDouble(3), rs.getDouble(4), rs.getDouble(5)), obj));
				}
			}
			return r;
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	public boolean existe(String fecha) {
		return leer(fecha).isPresent();
	}

	private static void poner(PreparedStatement p, int desde, Macros m) throws SQLException {
		p.setDouble(desde, m.kcal());
		p.setDouble(desde + 1, m.proteinas());
		p.setDouble(desde + 2, m.carbohidratos());
		p.setDouble(desde + 3, m.grasas());
	}
}
