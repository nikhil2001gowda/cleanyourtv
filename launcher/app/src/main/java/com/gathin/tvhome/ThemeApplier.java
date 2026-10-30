package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Directly applies a built-in theme on the device by writing its official config.json overlay,
 * maintaining custom dock/hidden app lists, updating the theme revision stamp, and clearing
 * conflicting user switch overrides so that the theme immediately takes full effect.
 */
public final class ThemeApplier {
    private static final String TAG = "TvHome.ThemeApplier";

    private ThemeApplier() {}

    public static void applyTheme(Context ctx, String themeKey) {
        if (ctx == null || themeKey == null) return;
        try {
            File dir = Config.dir(ctx);
            if (dir == null) return;
            if (!dir.exists()) dir.mkdirs();

            File configFile = new File(dir, Config.FILE_NAME);
            JSONArray existingDockApps = null;
            JSONArray existingHiddenApps = null;
            if (configFile.exists() && configFile.length() > 0) {
                try (FileInputStream in = new FileInputStream(configFile)) {
                    byte[] bytes = new byte[(int) configFile.length()];
                    int read = in.read(bytes);
                    if (read > 0) {
                        JSONObject old = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
                        existingDockApps = old.optJSONArray("dock_apps");
                        existingHiddenApps = old.optJSONArray("hidden_apps");
                    }
                } catch (Exception ignored) {}
            }

            JSONObject json = buildThemeJson(themeKey);
            if (existingDockApps != null && existingDockApps.length() > 0) {
                json.put("dock_apps", existingDockApps);
            }
            if (existingHiddenApps != null && existingHiddenApps.length() > 0) {
                json.put("hidden_apps", existingHiddenApps);
            }

            // Write config.json
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                out.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                out.flush();
            }

            // Write theme_revision token
            String revToken = UUID.randomUUID().toString().replace("-", "");
            File revFile = new File(dir, "theme_revision");
            try (FileOutputStream out = new FileOutputStream(revFile)) {
                out.write(revToken.getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }

            // Synchronize choices
            SharedPreferences choices = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = choices.edit();
            editor.putString(Config.THEME_PRESET_KEY, themeKey);
            editor.remove("columns");
            editor.remove(Config.ROWS_BELOW_DOCK_KEY);
            editor.remove(Config.DOCK_OPACITY_KEY);
            editor.remove("animations");
            editor.remove("advanced_effects");
            for (CustomSwitchData.SwitchItem item : CustomSwitchData.ALL) {
                editor.remove(item.key);
            }
            String signature = configFile.length() + "@" + configFile.lastModified() + ":" + revToken;
            editor.putString("theme_signature", signature);
            long rev = choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1;
            editor.putLong(Config.SETTINGS_REVISION_KEY, rev);
            editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to apply theme: " + themeKey, e);
        }
    }

    private static JSONObject buildThemeJson(String key) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("columns", 4);
        obj.put("rows_below_dock", 0);
        obj.put("header_white", true);
        obj.put("cinematic_home", false);
        obj.put("dock_opacity", 100);

        if ("nature_calm".equals(key) || "sweden".equals(key)) {
            obj.put("columns", 6);
            obj.put("rows_below_dock", 1);
            obj.put("background_color", "#05070C");
            obj.put("wallpaper", true);
            obj.put("dock_glass", true);
            obj.put("dock_opacity", "sweden".equals(key) ? 50 : 40);
            obj.put("icon_only_buttons", true);
            obj.put("icon_only_inputs", true);
            obj.put("advanced_effects", true);
            obj.put("fade_rows", true);
            obj.put("glass_style", "frosted");
        } else if ("retro".equals(key)) {
            obj.put("columns", 5);
            obj.put("background_color", "#000000");
            obj.put("glass_style", "rim");
            obj.put("icon_only_buttons", true);
            obj.put("icon_only_inputs", true);
            obj.put("wifi_status_color", true);
            obj.put("tile_menu", true);
            obj.put("reduced_motion", true);
            obj.put("sky_intensity", 0);
        } else if ("ruska".equals(key)) {
            obj.put("background_color", "#07080D");
            obj.put("wallpaper", true);
            obj.put("dock_opacity", 70);
            obj.put("dock_band", true);
            obj.put("app_glow", true);
            obj.put("glass_style", "frosted");
        } else if ("aurora".equals(key)) {
            obj.put("background_color", "#070A14");
            obj.put("wallpaper", true);
            obj.put("dock_glass", true);
            obj.put("dock_band", true);
            obj.put("app_glow", true);
            obj.put("glow_style", "horizon");
            obj.put("sky_intensity", 45);
            obj.put("glass_style", "frosted");
        } else if ("six_columns".equals(key)) {
            obj.put("columns", 6);
            obj.put("rows_below_dock", 1);
            obj.put("background_color", "#000000");
            obj.put("dock_band", true);
        } else if ("dussehra".equals(key)) {
            obj.put("festival", "dussehra");
            obj.put("accent_color", "#FFB23C");
            obj.put("background_color", "#120809");
            obj.put("wallpaper", true);
            obj.put("dock_glass", true);
        } else if ("scenes".equals(key)) {
            obj.put("background_color", "#0A0E1A");
            obj.put("wallpaper", true);
            obj.put("dock_glass", true);
        } else if ("dark".equals(key) || "night".equals(key)) {
            obj.put("background_color", "dark".equals(key) ? "#12141A" : "#000000");
            obj.put("glass_style", "flat");
            obj.put("app_glow", true);
            obj.put("glow_style", "horizon");
        } else if ("light".equals(key)) {
            obj.put("background_color", "#F0F3F8");
            obj.put("header_white", false);
            obj.put("dock_glass", true);
            obj.put("glass_style", "light");
            obj.put("app_glow", false);
        } else if ("default".equals(key)) {
            obj.put("background_color", "#000000");
            obj.put("dock_band", true);
        } else { // "glass"
            obj.put("columns", 5);
            obj.put("background_color", "#090C14");
            obj.put("wallpaper", true);
            obj.put("dock_glass", true);
            obj.put("dock_band", true);
            obj.put("dock_even_padding", true);
            obj.put("icon_only_buttons", true);
            obj.put("app_glow", true);
            obj.put("glow_style", "horizon");
            obj.put("advanced_effects", true);
            obj.put("fade_rows", true);
            obj.put("glass_style", "frosted");
        }
        return obj;
    }
}
