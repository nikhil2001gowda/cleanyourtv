package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata and catalog of all configurable boolean feature flags in the launcher.
 */
public final class CustomSwitchData {

    public static final class SwitchItem {
        public final String key;
        public final String title;
        public final String category;
        public final String description;
        public final boolean defaultVal;

        public SwitchItem(String key, String title, String category, boolean defaultVal) {
            this.key = key;
            this.title = title;
            this.category = category;
            this.description = title;
            this.defaultVal = defaultVal;
        }
    }

    public static final List<SwitchItem> ALL;

    static {
        String[] data = {
            "cinematic_home", "Cinematic Shelf & Posters", "LAYOUT", "1",
            "cinematic_dots", "Shelf Position Indicator Dots", "LAYOUT", "1",
            "single_row", "Single Horizontal Scrolling Row", "LAYOUT", "0",
            "dock_glass", "Frosted Glass Dock Band", "DOCK", "1",
            "dock_band", "Show Dock Backing Band", "DOCK", "1",
            "dock_title", "Dock Focused App Title", "DOCK", "0",
            "app_glow", "App Brand Accent Focus Glow", "GLOW", "1",
            "ambient_glow", "Ambient Dock Lighting", "GLOW", "1",
            "always_show_names", "Always Show App Names", "TILES", "0",
            "tile_menu", "App Tile Long-Press Menu", "TILES", "1",
            "uniform_tiles", "Uniform Card Shapes", "TILES", "0",
            "show_inputs_pill", "Show Inputs Pill", "HEADER", "1",
            "show_status_clock", "Header Status Clock", "HEADER", "1",
            "show_status_badges", "Header Network & VPN Badges", "HEADER", "1",
            "show_status_pill", "Control Center Status Pill", "HEADER", "1",
            "show_settings_pill", "Launcher Settings Header Pill", "HEADER", "1",
            "show_sleep_pill", "Quick Sleep Screen Pill", "HEADER", "1",
            "show_bluetooth_pill", "Quick Bluetooth Header Pill", "HEADER", "1",
            "show_memory_pill", "Quick Memory Monitor Pill", "HEADER", "1",
            "screensaver", "Built-in Screensaver Clock", "SCREENSAVER", "1",
            "screensaver_fade", "Gentle Ambient Fading", "SCREENSAVER", "1",
            "wallpaper_zoom", "Smooth Ambient Motion", "MOTION", "1",
            "banner_bounce", "Spring Dynamics & Bounces", "MOTION", "1",
            "dpad_glow_ripple", "Interactive Directional Light Wave", "MOTION", "1",
            "wallpaper_dimming", "Dim Wallpaper Behind Focused Content", "WALLPAPER", "1",
            "wallpaper_scrim", "Header Protection Scrim Gradient", "WALLPAPER", "1"
        };
        List<SwitchItem> list = new ArrayList<>(26);
        for (int i = 0; i < data.length; i += 4) {
            list.add(new SwitchItem(data[i], data[i + 1], data[i + 2], "1".equals(data[i + 3])));
        }
        ALL = Collections.unmodifiableList(list);
    }
}
