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
                "Show 16:9 cinematic backdrop and dynamic top shelf above favorites.", true));
        list.add(new SwitchItem("cinematic_dots", "Shelf Position Indicator Dots", "LAYOUT & SHELF",
                "Display dock app position indicator dots beneath the cinematic shelf.", true));
        list.add(new SwitchItem("single_row", "Single Horizontal Scrolling Row", "LAYOUT & SHELF",
                "Display all apps in a single horizontal scrolling row without a lower grid.", false));

        // 2. Dock Styling
        list.add(new SwitchItem("dock_glass", "Frosted Glass Dock Band", "DOCK STYLING",
                "Render dock band with translucent frosted glass blur style instead of solid fill.", true));
        list.add(new SwitchItem("dock_band", "Show Dock Backing Band", "DOCK STYLING",
                "Draw background band behind dock apps. Turn off to let tiles float on wallpaper.", true));
        list.add(new SwitchItem("dock_title", "Dock Focused App Title", "DOCK STYLING",
                "Display the focused app's name in prominent typography directly above the dock.", false));

        // 3. Lighting & Glow
        list.add(new SwitchItem("app_glow", "App Brand Accent Focus Glow", "LIGHTING & GLOW",
                "Colorize tile focus highlight using the app's extracted brand accent color.", true));
        list.add(new SwitchItem("ambient_glow", "Ambient Dock Lighting", "LIGHTING & GLOW",
                "Cast dynamic ambient glow behind the dock in the focused app's accent color.", true));

        // 4. Tiles & App Cards
        list.add(new SwitchItem("always_show_names", "Always Show App Names", "TILES & CARDS",
                "Keep title labels permanently visible underneath all tiles instead of only when focused.", false));
        list.add(new SwitchItem("tile_menu", "App Tile Long-Press Menu", "TILES & CARDS",
                "Long-pressing OK on an app tile opens context menu (Move, Remove, App Info).", true));
        list.add(new SwitchItem("uniform_tiles", "Uniform Card Shapes", "TILES & CARDS",
                "Render all apps on uniform tinted cards instead of varying aspect banners.", false));

        // 5. Visual Effects & Motion
        list.add(new SwitchItem("fade_rows", "Fade Out Scrolling Rows", "VISUAL & MOTION",
                "Gradually fade out top grid rows as they scroll up beneath the clock and header.", true));
        list.add(new SwitchItem("focus_overshoot", "Spring Focus Overshoot", "VISUAL & MOTION",
                "Spring physics animation bounces slightly past card boundaries before settling on focus.", true));
        list.add(new SwitchItem("reduced_motion", "Reduced Motion Mode", "VISUAL & MOTION",
                "Use calm, minimal animations to optimize performance on low-end hardware.", false));

        // 6. Header & Status Bar
        list.add(new SwitchItem("show_network_pill", "Network Status Pill", "HEADER & STATUS",
                "Display Wi-Fi / Ethernet connectivity pill in the top header bar.", true));
        list.add(new SwitchItem("show_inputs_pill", "Inputs & HDMI Switcher Pill", "HEADER & STATUS",
                "Display HDMI and TV Inputs switcher pill in the top header bar.", true));
        list.add(new SwitchItem("icon_only_buttons", "Compact Header Buttons", "HEADER & STATUS",
                "Show Wi-Fi, Inputs, and Settings as round compact icon buttons in the top bar.", false));
        list.add(new SwitchItem("icon_only_inputs", "Icon-Only Inputs Pill", "HEADER & STATUS",
                "Render Inputs menu as a round icon pill matching other header buttons.", false));
        list.add(new SwitchItem("wifi_status_color", "Active Wi-Fi Color Indicator", "HEADER & STATUS",
                "Colorize Wi-Fi icon green when connected and yellow/red on weak/offline signals.", true));
        list.add(new SwitchItem("show_greeting", "Time-of-Day Greeting", "HEADER & STATUS",
                "Show personalized greeting ('Good morning', 'Good evening') above the clock.", true));
        list.add(new SwitchItem("date_below_clock", "Date Below Clock", "HEADER & STATUS",
                "Position date on its own dedicated line below the clock instead of beside it.", false));
        list.add(new SwitchItem("header_white", "Crisp White Header", "HEADER & STATUS",
                "Display date and clock in 100% bright white instead of subtle translucent gray.", true));

        // 7. Wallpaper & Art
        list.add(new SwitchItem("wallpaper_captions", "Wallpaper Location Captions", "WALLPAPER",
                "Show photo location and photographer attribution subtitle beneath top bar.", true));
        list.add(new SwitchItem("wallpaper_true_color", "32-bit Wallpaper Color Depth", "WALLPAPER",
                "Decode wallpapers in full 32-bit ARGB_8888 color instead of compressed RGB_565.", false));

        // 8. Screensaver
        list.add(new SwitchItem("clock_wallpaper", "Screensaver Wallpaper Backdrop", "SCREENSAVER",
                "Show launcher wallpaper behind clock screensaver instead of pure black background.", true));
        list.add(new SwitchItem("clock_glow", "Screensaver Atmospheric Glow", "SCREENSAVER",
                "Display ambient atmospheric color glow in the clock screensaver.", true));

        ALL = Collections.unmodifiableList(list);
    }
}
