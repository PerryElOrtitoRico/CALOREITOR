package com.caloreitor.escritorio.tema;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import javax.swing.UIManager;

/**
 * FlatLaf con los colores de la app. El modo oscuro sigue al del sistema al arrancar: oscuro si gsettings dice
 * color-scheme 'prefer-dark' o si ~/.config/kdeglobals tiene en [General] un ColorScheme con «Dark»; si no, claro.
 */
public final class Tema {

    private static volatile boolean oscuro;

    private Tema() {
    }

    /** JScrollPane con la rueda más rápida: los paneles comunes avanzan 1 px por paso si no se ajusta. */
    public static javax.swing.JScrollPane desplazable(java.awt.Component vista) {
        javax.swing.JScrollPane s = new javax.swing.JScrollPane(vista);
        s.getVerticalScrollBar().setUnitIncrement(32);
        s.getHorizontalScrollBar().setUnitIncrement(32);
        return s;
    }

    public static boolean oscuro() {
        return oscuro;
    }

    /** Decide el modo, elige la paleta de Colores, instala FlatLaf y ajusta UIManager. Se llama una vez, antes de crear ventanas. */
    public static void aplicar() {
        oscuro = oscuroDe(leerGsettings(), leerKdeglobals());
        Colores.usar(oscuro);
        if (oscuro) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("Panel.background", Colores.de("fondo"));
        UIManager.put("TextField.background", Colores.de("campo"));
        UIManager.put("Component.borderColor", Colores.de("borde"));
        UIManager.put("Component.focusColor", Colores.de("foco"));
        UIManager.put("Label.foreground", Colores.de("texto"));
        UIManager.put("TabbedPane.selectedBackground", Colores.de("barra"));
        UIManager.put("TabbedPane.underlineColor", Colores.de("foco"));
    }

    /** True si gsettings dice 'prefer-dark' o si kdeglobals tiene en [General] un ColorScheme con «Dark» (cualquiera de los dos puede ser null). */
    static boolean oscuroDe(String gsettings, String kdeglobals) {
        if (gsettings != null && gsettings.contains("prefer-dark")) {
            return true;
        }
        if (kdeglobals == null) {
            return false;
        }
        boolean enGeneral = false;
        for (String linea : kdeglobals.split("\n")) {
            String l = linea.trim();
            if (l.startsWith("[")) {
                enGeneral = l.equals("[General]");
            } else if (enGeneral && l.startsWith("ColorScheme=")) {
                return l.contains("Dark");
            }
        }
        return false;
    }

    /** Salida de `gsettings get org.gnome.desktop.interface color-scheme` (sin espacios al borde), o null si falla o tarda más de 2 s. */
    static String leerGsettings() {
        try {
            Process p = new ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme").redirectErrorStream(true).start();
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) {
                return null;
            }
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Contenido de ~/.config/kdeglobals, o null si no existe o no se puede leer. */
    static String leerKdeglobals() {
        Path archivo = Path.of(System.getProperty("user.home"), ".config", "kdeglobals");
        try {
            return Files.readString(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
