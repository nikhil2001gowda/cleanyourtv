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
                            int dockOpacity, boolean dockGlass, int glowColor) {
        this.key = key;
        this.title = title;
        this.bgStart = bgStart;
        this.bgEnd = bgEnd;
        this.columns = columns;
        this.dockOpacity = dockOpacity;
        this.dockGlass = dockGlass;
        this.glowColor = glowColor;
        this.badges = new String[]{columns + " COLUMNS"};
        this.description = title;
    }

    private static final ThemePreviewData[] ALL = {
        new ThemePreviewData("nature_calm", "Nature Calm", 0xFF05070C, 0xFF0A131C, 6, 40, true, 0x664DAE6D),
        new ThemePreviewData("retro", "Retro", 0xFF000000, 0xFF000000, 5, 0, false, 0x88FFFFFF),
        new ThemePreviewData("ruska", "Ruska", 0xFF07080D, 0xFF180E12, 4, 70, false, 0x88FF9800),
        new ThemePreviewData("aurora", "Aurora", 0xFF070A14, 0xFF0D1B2A, 4, 100, true, 0x8800E5FF),
        new ThemePreviewData("sweden", "Sweden", 0xFF05070C, 0xFF091422, 6, 50, true, 0x663388FF),
        new ThemePreviewData("six_columns", "Six Columns", 0xFF000000, 0xFF0B0E14, 6, 100, false, 0x66FFFFFF),
        new ThemePreviewData("dussehra", "Dussehra", 0xFF120809, 0xFF2A0E12, 4, 100, true, 0x99FFB23C),
        new ThemePreviewData("scenes", "Scenes", 0xFF0A0E1A, 0xFF121B2B, 4, 100, true, 0x664A90E2),
        new ThemePreviewData("dark", "Dark Mode", 0xFF12141A, 0xFF0A0C10, 4, 100, false, 0x664A90E2),
        new ThemePreviewData("light", "Light Mode", 0xFFF0F3F8, 0xFFDDE4EE, 4, 100, true, 0x33000000),
        new ThemePreviewData("night", "Night Mode", 0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF),
        new ThemePreviewData("default", "Default", 0xFF000000, 0xFF000000, 4, 100, false, 0x66FFFFFF),
        new ThemePreviewData("glass", "Frosted Glass", 0xFF0B0E14, 0xFF151922, 4, 100, true, 0x66389BFF)
    };

    public static ThemePreviewData get(String key) {
        if (key != null) {
            for (ThemePreviewData d : ALL) {
                if (key.equals(d.key)) return d;
            }
        }
        return ALL[ALL.length - 1];
    }
}
