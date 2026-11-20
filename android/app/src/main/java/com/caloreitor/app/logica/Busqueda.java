package com.caloreitor.app.logica;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Consumo;
import com.caloreitor.app.modelo.Item;
import com.caloreitor.app.modelo.Receta;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Búsqueda de alimentos y recetas con sugerencias según el historial de consumos. */
public final class Busqueda {
    public record Resultado(String id, String nombre, boolean esReceta, double gramosSugeridos) {
    }

    private final List<Alimento> alimentos;
    private final List<Receta> recetas;
    private final List<Consumo> historial;

    public Busqueda(List<Alimento> todos, List<Receta> recetas, List<Consumo> historial) {
        // un propio con el mismo nombre que uno base lo reemplaza (editar = copia propia)
        Map<String, Alimento> porNombre = new LinkedHashMap<>();
        for (Alimento a : todos) {
            Alimento previo = porNombre.get(normalizar(a.nombre()));
            if (previo == null || (previo.base() && !a.base())) porNombre.put(normalizar(a.nombre()), a);
        }
        this.alimentos = new ArrayList<>(porNombre.values());
        this.recetas = recetas;
        this.historial = historial;
    }

    public List<Resultado> buscar(String texto, String comida, LocalDate hoy) {
        String[] palabras = palabras(texto == null ? "" : texto);
        boolean vacio = palabras.length == 0;
        Map<String, Integer> usos = usos(vacio ? comida : null, hoy);
        Map<String, Double> ultimos = ultimosGramos();

        List<Resultado> r = new ArrayList<>();
        for (Alimento a : alimentos) {
            if (vacio ? usos.containsKey(a.id()) : coincide(palabras, a.nombre())) {
                double sug = ultimos.containsKey(a.id()) ? ultimos.get(a.id())
                        : a.porcionGramos() != null ? a.porcionGramos() : 100;
                r.add(new Resultado(a.id(), a.nombre(), false, sug));
            }
        }
        for (Receta rc : recetas) {
            if (vacio ? usos.containsKey(rc.id()) : coincide(palabras, rc.nombre())) {
                r.add(new Resultado(rc.id(), rc.nombre(), true, 0));
            }
        }
        r.sort(Comparator.<Resultado>comparingInt(x -> -usos.getOrDefault(x.id(), 0))
                .thenComparing(x -> normalizar(x.nombre())));
        return vacio && r.size() > 15 ? new ArrayList<>(r.subList(0, 15)) : r;
    }

    /** Usos en los últimos 30 días (hoy incluido); si comida no es null, solo los de esa comida. */
    private Map<String, Integer> usos(String comida, LocalDate hoy) {
        LocalDate desde = hoy.minusDays(29);
        Map<String, Integer> u = new HashMap<>();
        for (Consumo c : historial) {
            LocalDate f = LocalDate.parse(c.fecha());
            if (f.isBefore(desde) || f.isAfter(hoy) || (comida != null && !comida.equals(c.comida()))) continue;
            if (c.recetaId() != null) u.merge(c.recetaId(), 1, Integer::sum);
            else for (Item i : c.items()) u.merge(i.alimentoId(), 1, Integer::sum);
        }
        return u;
    }

    private Map<String, Double> ultimosGramos() {
        Map<String, Double> gramos = new HashMap<>();
        Map<String, String> fechas = new HashMap<>();
        for (Consumo c : historial) {
            for (Item i : c.items()) {
                // >= para que, a igual fecha, gane el que viene después en la lista
                if (fechas.get(i.alimentoId()) == null || c.fecha().compareTo(fechas.get(i.alimentoId())) >= 0) {
                    fechas.put(i.alimentoId(), c.fecha());
                    gramos.put(i.alimentoId(), i.gramos());
                }
            }
        }
        return gramos;
    }

    private static boolean coincide(String[] palabras, String nombre) {
        String[] del = palabras(nombre);
        for (String p : palabras) {
            boolean ok = false;
            for (String d : del) if (d.startsWith(p)) { ok = true; break; }
            if (!ok) return false;
        }
        return true;
    }

    private static String[] palabras(String s) {
        String n = normalizar(s).trim();
        return n.isEmpty() ? new String[0] : n.split("[^\\p{L}\\p{N}]+");
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
