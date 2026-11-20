package com.caloreitor.app.logica;

import com.caloreitor.app.modelo.Alimento;
import com.caloreitor.app.modelo.Macros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class BaseAlimentos {
    private BaseAlimentos() {
    }

    /** Lee el CSV de la base (UTF-8, separador ';', primera línea de encabezado). */
    public static List<Alimento> cargar(InputStream csv) throws IOException {
        List<Alimento> r = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8));
        br.readLine();
        for (String linea; (linea = br.readLine()) != null; ) {
            if (linea.isBlank()) continue;
            String[] c = linea.split(";", -1);
            Macros m = new Macros(Double.parseDouble(c[2]), Double.parseDouble(c[3]), Double.parseDouble(c[4]),
                    Double.parseDouble(c[5]));
            boolean porcion = !c[6].isBlank() && !c[7].isBlank();
            r.add(new Alimento(c[0], c[1], m, porcion ? c[6] : null, porcion ? Double.valueOf(c[7]) : null, true));
        }
        return r;
    }
}
