package com.gathin.tvhome;

/** Metadata and visual parameters for live theme previews. */
public final class ThemePreviewData {
    public final String key;
    public final String title;
    public final int bgStart;
    public final int bgEnd;
    public final int columns;
    public final int dockOpacity;
    public final boolean dockGlass;
    public final int glowColor;
    public final String[] badges;
    public final String description;

    public ThemePreviewData(String key, String title, int bgStart, int bgEnd, int columns,
                            int dockOpacity, boolean dockGlass, int glowColor,
                            String[] badges, String description) {
        this.key = key;
        this.title = title;
        this.bgStart = bgStart;
        this.bgEnd = bgEnd;
        this.columns = columns;
        this.dockOpacity = dockOpacity;
        this.dockGlass = dockGlass;
        this.glowColor = glowColor;
        this.badges = badges;
        this.description = description;
    }

    public static ThemePreviewData get(String key) {
        if ("nature_calm".equals(key)) {
            return new ThemePreviewData(key, "Nature Calm",
                    0xFF05070C, 0xFF0A131C, 6, 40, true, 0x664DAE6D,
                    new String[]{"6 COLUMNS", "40% DOCK GLASS", "CALM AESTHETIC", "NATURE"},
                    "Six compact tiles in a 40% translucent frosted-glass dock, calm natural atmosphere with serene landscape styling.");
        } else if ("retro".equals(key)) {
            return new ThemePreviewData(key, "Retro",
                    0xFF000000, 0xFF000000, 5, 0, false, 0x88FFFFFF,
                    new String[]{"5 COLUMNS", "OLED BLACK", "RIM GLASS", "MINIMAL MOTION"},
                    "Pure deep black canvas with rim-only focus highlights, no background gradient, icon-only top bar, and zero distractions.");
        } else if ("ruska".equals(key)) {
            return new ThemePreviewData(key, "Ruska (Autumn)",
                    0xFF07080D, 0xFF180E12, 4, 70, false, 0x88FF9800,
                    new String[]{"4 COLUMNS", "70% DOCK BAND", "AUTUMN DUSK", "APP GLOW"},
                    "Finnish autumn dusk over a calm lake. 70% opaque dock lets warm dusk tones shine through, with app-reactive glow.");
        } else if ("aurora".equals(key)) {
            return new ThemePreviewData(key, "Aurora",
                    0xFF070A14, 0xFF0D1B2A, 4, 100, true, 0x8800E5FF,
                    new String[]{"4 COLUMNS", "AURORA SKY", "HORIZON GLOW", "FROSTED DOCK"},
                    "Luminous teal and violet aurora borealis shimmering across a deep midnight sky, with matching horizon lighting.");
        } else if ("sweden".equals(key)) {
            return new ThemePreviewData(key, "Sweden",
                    0xFF05070C, 0xFF091422, 6, 50, true, 0x663388FF,
                    new String[]{"6 COLUMNS", "50% DOCK GLASS", "NORDIC TWILIGHT", "ICON PILLS"},
                    "Stockholm dusk aesthetic with six small tiles per row, 50% frosted-glass dock, round icon-only top controls.");
        } else if ("six_columns".equals(key)) {
            return new ThemePreviewData(key, "Six Columns",
                    0xFF000000, 0xFF0B0E14, 6, 100, false, 0x66FFFFFF,
                    new String[]{"6 COLUMNS", "1 ROW BELOW DOCK", "HIGH DENSITY", "DOCK BAND"},
                    "High-density layout showing six tiles per row and one full grid row visible under the dock for maximum app visibility.");
        } else if ("dussehra".equals(key)) {
            return new ThemePreviewData(key, "Dussehra (Festive)",
                    0xFF120809, 0xFF2A0E12, 4, 100, true, 0x99FFB23C,
                    new String[]{"FESTIVE SKIN", "GOLDEN ACCENT", "DIYA & BELL", "MARIGOLD TORAN"},
                    "Festive celebration skin featuring marigold floral torans, diya lamp Wi-Fi indicator, sacred bell Inputs icon, and petal shower.");
        } else if ("scenes".equals(key)) {
            return new ThemePreviewData(key, "Scenes",
                    0xFF0A0E1A, 0xFF121B2B, 4, 100, true, 0x664A90E2,
                    new String[]{"4 COLUMNS", "ILLUSTRATED SCENES", "ROTATING ART", "CINEMATIC"},
                    "Dark illustrated landscape scenes rotating on your home screen with frosted glass dock and ambient horizon glow.");
        } else if ("dark".equals(key)) {
            return new ThemePreviewData(key, "Dark Mode",
                    0xFF12141A, 0xFF0A0C10, 4, 100, false, 0x664A90E2,
                    new String[]{"4 COLUMNS", "DARK SLATE", "HORIZON GLOW", "FLAT GLASS"},
                    "Classic modern dark slate interface with comfortable contrast, flat glass dock, and reactive horizon focus lighting.");
        } else if ("light".equals(key)) {
            return new ThemePreviewData(key, "Light Mode",
                    0xFFF0F3F8, 0xFFDDE4EE, 4, 100, true, 0x33000000,
                    new String[]{"4 COLUMNS", "LIGHT PEARL", "HIGH CONTRAST", "CRISP TYPOGRAPHY"},
                    "Bright daytime look with pearl background, dark crisp typography, and frosted light-glass elements.");
        } else if ("night".equals(key)) {
            return new ThemePreviewData(key, "Night Mode (OLED)",
                    0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF,
                    new String[]{"4 COLUMNS", "OLED BLACK", "HORIZON GLOW", "ZERO EYE STRAIN"},
                    "True pitch-black background optimized for OLED displays, eliminating backlight bleed and eye fatigue in dark rooms.");
        } else if ("default".equals(key)) {
            return new ThemePreviewData(key, "Default",
                    0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF,
                    new String[]{"4 COLUMNS", "STOCK REFERENCE", "DOCK BAND", "SOLID BLACK"},
                    "The original clean reference look from config.xml with standard 4-column dock band and classic Android TV simplicity.");
        }
        return new ThemePreviewData("glass", "Apple tvOS Glass",
                0xFF0A0E1A, 0xFF06080F, 5, 100, true, 0x887B1FA2,
                new String[]{"5 COLUMNS", "FROSTED GLASS", "HORIZON GLOW", "CINEMATIC ART"},
                "Dark navy aesthetic with soft teal and violet lighting, cinematic top shelf artwork, frosted-glass dock, and horizon focus glow.");
    }
}
