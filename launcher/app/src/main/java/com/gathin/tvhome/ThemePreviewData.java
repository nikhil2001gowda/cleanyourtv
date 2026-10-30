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

    private static final String[] B4 = new String[]{"4 COLUMNS"};
    private static final String[] B5 = new String[]{"5 COLUMNS"};
    private static final String[] B6 = new String[]{"6 COLUMNS"};

    public static ThemePreviewData get(String key) {
        if ("nature_calm".equals(key)) {
            return new ThemePreviewData(key, "Nature Calm",
                    0xFF05070C, 0xFF0A131C, 6, 40, true, 0x664DAE6D,
                    B6, "Calm landscape.");
        } else if ("retro".equals(key)) {
            return new ThemePreviewData(key, "Retro",
                    0xFF000000, 0xFF000000, 5, 0, false, 0x88FFFFFF,
                    B5, "OLED black canvas.");
        } else if ("ruska".equals(key)) {
            return new ThemePreviewData(key, "Ruska",
                    0xFF07080D, 0xFF180E12, 4, 70, false, 0x88FF9800,
                    B4, "Autumn dusk tones.");
        } else if ("aurora".equals(key)) {
            return new ThemePreviewData(key, "Aurora",
                    0xFF070A14, 0xFF0D1B2A, 4, 100, true, 0x8800E5FF,
                    B4, "Aurora night sky.");
        } else if ("sweden".equals(key)) {
            return new ThemePreviewData(key, "Sweden",
                    0xFF05070C, 0xFF091422, 6, 50, true, 0x663388FF,
                    B6, "Stockholm twilight.");
        } else if ("six_columns".equals(key)) {
            return new ThemePreviewData(key, "Six Columns",
                    0xFF000000, 0xFF0B0E14, 6, 100, false, 0x66FFFFFF,
                    B6, "Six tiles per row.");
        } else if ("dussehra".equals(key)) {
            return new ThemePreviewData(key, "Dussehra",
                    0xFF120809, 0xFF2A0E12, 4, 100, true, 0x99FFB23C,
                    B4, "Festive celebration.");
        } else if ("scenes".equals(key)) {
            return new ThemePreviewData(key, "Scenes",
                    0xFF0A0E1A, 0xFF121B2B, 4, 100, true, 0x664A90E2,
                    B4, "Landscape artwork.");
        } else if ("dark".equals(key)) {
            return new ThemePreviewData(key, "Dark Mode",
                    0xFF12141A, 0xFF0A0C10, 4, 100, false, 0x664A90E2,
                    B4, "Dark slate interface.");
        } else if ("light".equals(key)) {
            return new ThemePreviewData(key, "Light Mode",
                    0xFFF0F3F8, 0xFFDDE4EE, 4, 100, true, 0x33000000,
                    B4, "Pearl clean look.");
        } else if ("night".equals(key)) {
            return new ThemePreviewData(key, "Night Mode",
                    0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF,
                    B4, "Pitch-black OLED.");
        } else if ("default".equals(key)) {
            return new ThemePreviewData(key, "Default",
                    0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF,
                    B4, "Standard reference.");
        }
        return new ThemePreviewData("glass", "tvOS Glass",
                0xFF0A0E1A, 0xFF06080F, 5, 100, true, 0x887B1FA2,
                B5, "Dark navy frosted.");
    }
}
