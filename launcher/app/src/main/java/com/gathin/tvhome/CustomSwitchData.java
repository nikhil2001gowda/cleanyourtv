package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata and catalog of all configurable boolean feature flags in the launcher,
 * providing descriptive categories and explanations for on-TV toggle customization.
 */
public final class CustomSwitchData {

    public static final class SwitchItem {
        public final String key;
        public final String title;
        public final String category;
        public final String description;
        public final boolean defaultVal;

        public SwitchItem(String key, String title, String category, String description, boolean defaultVal) {
            this.key = key;
            this.title = title;
            this.category = category;
            this.description = description;
            this.defaultVal = defaultVal;
        }
    }

    public static final List<SwitchItem> ALL;

    static {
        List<SwitchItem> list = new ArrayList<>(26);

        // 1. Layout & Shelf
        list.add(new SwitchItem("cinematic_home", "Cinematic Shelf & Posters", "LAYOUT & SHELF",
                "16:9 shelf above favorites.", true));
        list.add(new SwitchItem("cinematic_dots", "Shelf Position Indicator Dots", "LAYOUT & SHELF",
                "Indicator dots under shelf.", true));
        list.add(new SwitchItem("single_row", "Single Horizontal Scrolling Row", "LAYOUT & SHELF",
                "Single-row app layout.", false));

        // 2. Dock Styling
        list.add(new SwitchItem("dock_glass", "Frosted Glass Dock Band", "DOCK STYLING",
                "Frosted glass dock band.", true));
        list.add(new SwitchItem("dock_band", "Show Dock Backing Band", "DOCK STYLING",
                "Backing band behind dock.", true));
        list.add(new SwitchItem("dock_title", "Dock Focused App Title", "DOCK STYLING",
                "App title above dock.", false));

        // 3. Lighting & Glow
        list.add(new SwitchItem("app_glow", "App Brand Accent Focus Glow", "LIGHTING & GLOW",
                "Focus in app brand color.", true));
        list.add(new SwitchItem("ambient_glow", "Ambient Dock Lighting", "LIGHTING & GLOW",
                "Ambient glow behind dock.", true));

        // 4. Tiles & App Cards
        list.add(new SwitchItem("always_show_names", "Always Show App Names", "TILES & CARDS",
                "Always show titles under tiles.", false));
        list.add(new SwitchItem("tile_menu", "App Tile Long-Press Menu", "TILES & CARDS",
                "Context menu on long-press.", true));
        list.add(new SwitchItem("uniform_tiles", "Uniform Card Shapes", "TILES & CARDS",
                "Uniform tinted cards.", false));

        // 5. Visual Effects & Motion
        list.add(new SwitchItem("fade_rows", "Fade Out Scrolling Rows", "VISUAL & MOTION",
                "Fade upper rows on scroll.", true));
        list.add(new SwitchItem("focus_overshoot", "Spring Focus Overshoot", "VISUAL & MOTION",
                "Spring overshoot on focus.", true));
        list.add(new SwitchItem("reduced_motion", "Reduced Motion Mode", "VISUAL & MOTION",
                "Minimal animations.", false));

        // 6. Header & Status Bar
        list.add(new SwitchItem("show_network_pill", "Network Status Pill", "HEADER & STATUS",
                "Network pill in header.", true));
        list.add(new SwitchItem("show_inputs_pill", "Inputs & HDMI Switcher Pill", "HEADER & STATUS",
                "HDMI / Inputs in header.", true));
        list.add(new SwitchItem("icon_only_buttons", "Compact Header Buttons", "HEADER & STATUS",
                "Compact header buttons.", false));
        list.add(new SwitchItem("icon_only_inputs", "Icon-Only Inputs Pill", "HEADER & STATUS",
                "Compact Inputs pill.", false));
        list.add(new SwitchItem("wifi_status_color", "Active Wi-Fi Color Indicator", "HEADER & STATUS",
                "Network color indicator.", true));
        list.add(new SwitchItem("show_greeting", "Time-of-Day Greeting", "HEADER & STATUS",
                "Greeting above clock.", true));
        list.add(new SwitchItem("date_below_clock", "Date Below Clock", "HEADER & STATUS",
                "Date below clock.", false));
        list.add(new SwitchItem("header_white", "Crisp White Header", "HEADER & STATUS",
                "Bright white header.", true));

        // 7. Wallpaper & Art
        list.add(new SwitchItem("wallpaper_captions", "Wallpaper Location Captions", "WALLPAPER",
                "Wallpaper location captions.", true));
        list.add(new SwitchItem("wallpaper_true_color", "32-bit Wallpaper Color Depth", "WALLPAPER",
                "Full 32-bit color depth.", false));

        // 8. Screensaver
        list.add(new SwitchItem("clock_wallpaper", "Screensaver Wallpaper Backdrop", "SCREENSAVER",
                "Wallpaper behind clock.", true));
        list.add(new SwitchItem("clock_glow", "Screensaver Atmospheric Glow", "SCREENSAVER",
                "Atmospheric clock glow.", true));

        ALL = Collections.unmodifiableList(list);
    }
}
