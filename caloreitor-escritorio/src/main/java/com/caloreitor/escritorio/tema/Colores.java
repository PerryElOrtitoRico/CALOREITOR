package com.caloreitor.escritorio.tema;

import java.awt.Color;
import java.util.Map;

/** Paleta de la app del celular (claro y oscuro), recortada a lo que usa CALOREITOR. */
public final class Colores {

    public static final Map<String, Color> CLARO = Map.ofEntries(
            Map.entry("fondo", new Color(0xF2F2F2)),
            Map.entry("barra", new Color(0xE4E4E4)),
            Map.entry("campo", new Color(0xFFFFFF)),
            Map.entry("borde", new Color(0xBDBDBD)),
            Map.entry("texto", new Color(0x222222)),
            Map.entry("secundario", new Color(0x555555)),
            Map.entry("foco", new Color(0x1E5BD8)),
            Map.entry("error", new Color(0xC62828)),
            Map.entry("disco_amarillo", new Color(0xFDD835)),
            Map.entry("disco_azul", new Color(0x1E5BD8)),
            Map.entry("disco_verde", new Color(0x43A047)),
            Map.entry("disco_violeta", new Color(0x7E57C2)),
            Map.entry("objetivo", new Color(0xB58900)));

    public static final Map<String, Color> OSCURO = Map.ofEntries(
            Map.entry("fondo", new Color(0x2B2D30)),
            Map.entry("barra", new Color(0x3C3F41)),
            Map.entry("campo", new Color(0x46494B)),
            Map.entry("borde", new Color(0x5E6060)),
            Map.entry("texto", new Color(0xDCDCDC)),
            Map.entry("secundario", new Color(0xA9A9A9)),
            Map.entry("foco", new Color(0x4A88F0)),
            Map.entry("error", new Color(0xEF5350)),
            Map.entry("disco_amarillo", new Color(0xFDD835)),
            Map.entry("disco_azul", new Color(0x1E5BD8)),
            Map.entry("disco_verde", new Color(0x43A047)),
            Map.entry("disco_violeta", new Color(0x7E57C2)),
            Map.entry("objetivo", new Color(0xFDD835)));

    private static volatile Map<String, Color> activo = CLARO;

    private Colores() {
    }

    /** Elige la paleta (lo llama Tema.aplicar). */
    static void usar(boolean oscuro) {
        activo = oscuro ? OSCURO : CLARO;
    }

    /** El color de esa clave en la paleta activa; IllegalArgumentException si la clave no existe. */
    public static Color de(String clave) {
        Color c = activo.get(clave);
        if (c == null) {
            throw new IllegalArgumentException("Color desconocido: " + clave);
        }
        return c;
    }
}
