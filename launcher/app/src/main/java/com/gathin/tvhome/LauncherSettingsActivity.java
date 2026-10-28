package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView themeChoice;
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;
    private Config config;

    private ThemePreviewView settingsThemePreview;
    private ImageView settingsWallpaperPreview;
    private TextView settingsPreviewTitle;
    private LinearLayout settingsPreviewBadges;
    private TextView settingsPreviewDescription;
    private TextView settingsPreviewHint;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        config = Config.load(this);

        settingsThemePreview = findViewById(R.id.settingsThemePreview);
        settingsWallpaperPreview = findViewById(R.id.settingsWallpaperPreview);
        settingsPreviewTitle = findViewById(R.id.settingsPreviewTitle);
        settingsPreviewBadges = findViewById(R.id.settingsPreviewBadges);
        settingsPreviewDescription = findViewById(R.id.settingsPreviewDescription);
        settingsPreviewHint = findViewById(R.id.settingsPreviewHint);

        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
            columnChoices[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    updateSettingsPreview(getString(R.string.columns_label) + ": " + value,
                            "Adjust how many apps are shown per row in the grid. 4 gives larger cards; 6-7 offers high density.",
                            new String[]{value + " COLUMNS", "GRID DENSITY"},
                            choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
                }
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
            dockChoices[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    updateSettingsPreview(getString(R.string.dock_opacity_label) + ": " + value + "%",
                            "Change the translucency of the dock band behind your favorite apps.",
                            new String[]{value + "% OPACITY", "DOCK BAND"},
                            choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
                }
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(dockChoices);
        }
        themeChoice = findViewById(R.id.themeChoice);
        themeChoice.setOnClickListener(v -> showThemePicker());
        themeChoice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String current = choices.getString(Config.THEME_PRESET_KEY, "glass");
                ThemePreviewData d = ThemePreviewData.get(current);
                updateSettingsPreview(d.title, d.description, d.badges, current, "Press OK to browse and change themes");
            }
        });
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        wallpaperChoice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String wpSource = choices.getString("wallpaper_source", "online");
                int interval = choices.getInt("wallpaper_rotation_minutes", 5);
                String order = choices.getString("wallpaper_order", "shuffle");
                String badge1 = "none".equals(wpSource) ? "OFF" : "theme".equals(wpSource) ? "THEME" : "ONLINE 8K";
                String badge2 = interval <= 0 ? "STATIC" : interval + " MIN";
                String badge3 = "sequential".equals(order) ? "SEQUENTIAL" : "SHUFFLE";
                updateSettingsPreview(getString(R.string.wallpaper_label),
                        "Configure online 8K wallpapers, auto-change rotation interval, and shuffle order.",
                        new String[]{badge1, badge2, badge3},
                        null, "Press OK to configure wallpapers");
            }
        });
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        animationsChoice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSettingsPreview(getString(R.string.animations_state, getString(animations ? R.string.on : R.string.off)),
                        "Control fluid spring physics, overshoot, and navigation animations.",
                        new String[]{animations ? "ENABLED" : "OFF", "MOTION"},
                        choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
            }
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        advancedChoice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSettingsPreview(getString(R.string.advanced_effects),
                        "Enable real-time horizon glow, lighting reflections, and elevated cards.",
                        new String[]{advancedEffects ? "ACTIVE" : "OFF", "HORIZON GLOW"},
                        choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
            }
        });
        TextView customSwitchesChoice = findViewById(R.id.customSwitchesChoice);
        if (customSwitchesChoice != null) {
            customSwitchesChoice.setOnClickListener(v -> showSwitchesPicker());
            customSwitchesChoice.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    updateSettingsPreview(getString(R.string.custom_switches_title),
                            getString(R.string.custom_switches_desc),
                            new String[]{"26 TOGGLES", "FEATURE SWITCHES"},
                            choices.getString(Config.THEME_PRESET_KEY, "glass"), "Press OK to configure all switches");
                }
            });
        }
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
            openOnStartChoice.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    updateSettingsPreview(getString(R.string.open_on_start_label),
                            "Choose an app to automatically open whenever the TV boots up.",
                            new String[]{"STARTUP", "AUTO LAUNCH"},
                            choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
                }
            });
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            SharedPreferences.Editor ed = choices.edit();
            for (CustomSwitchData.SwitchItem item : CustomSwitchData.ALL) ed.remove(item.key);
            ed.remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.THEME_PRESET_KEY).remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY)
                    .remove("wallpaper_source").remove("wallpaper_rotation_minutes").remove("wallpaper_order").commit();
            Config current = Config.load(this);
            columns = current.columns;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        followThemeChoice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSettingsPreview(getString(R.string.follow_theme),
                        getString(R.string.follow_theme_hint),
                        new String[]{"RESET", "THEME DEFAULT"},
                        choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
            }
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.defaultHomeChoice).setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSettingsPreview(getString(R.string.home_settings),
                        getString(R.string.home_settings_help),
                        new String[]{"ANDROID TV", "HOME APP"},
                        choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
            }
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        findViewById(R.id.tvSettingsChoice).setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateSettingsPreview(getString(R.string.tv_settings),
                        "Open the TV's native system settings panel for display, sound, network, and devices.",
                        new String[]{"SYSTEM", "DEVICE SETTINGS"},
                        choices.getString(Config.THEME_PRESET_KEY, "glass"), null);
            }
        });
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        Dialog dialog = new Dialog(this, R.style.Theme_TvHome);
        dialog.setContentView(R.layout.dialog_wallpaper_settings);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0xFA0B0E14));
        }

        LinearLayout listContainer = dialog.findViewById(R.id.wpSettingsListContainer);
        ImageView previewImage = dialog.findViewById(R.id.dialogWpPreviewImage);
        TextView previewTitle = dialog.findViewById(R.id.dialogWpPreviewTitle);
        LinearLayout previewBadges = dialog.findViewById(R.id.dialogWpPreviewBadges);
        TextView previewDesc = dialog.findViewById(R.id.dialogWpPreviewDescription);
        TextView applyPrompt = dialog.findViewById(R.id.dialogWpApplyPrompt);

        float density = getResources().getDisplayMetrics().density;

        final String[] sources = {"online", "theme", "none"};
        final int[] intervals = {1, 5, 15, 30, 60, 0};

        // Rows
        TextView sourceRow = createWpRow(density);
        listContainer.addView(sourceRow);

        TextView intervalRow = createWpRow(density);
        listContainer.addView(intervalRow);

        TextView orderRow = createWpRow(density);
        listContainer.addView(orderRow);

        TextView cycleRow = createWpRow(density);
        cycleRow.setText("Cycle to Next Wallpaper ↻");
        listContainer.addView(cycleRow);

        TextView themePicRow = null;
        if (!config.wallpapers.isEmpty()) {
            themePicRow = createWpRow(density);
            themePicRow.setText("Choose Specific Theme Picture...");
            listContainer.addView(themePicRow);
        }

        Runnable updateLabels = new Runnable() {
            @Override
            public void run() {
                String wpSource = choices.getString("wallpaper_source", "online");
                int interval = choices.getInt("wallpaper_rotation_minutes", 5);
                String order = choices.getString("wallpaper_order", "shuffle");

                String sLabel = "none".equals(wpSource) ? getString(R.string.wallpaper_none)
                        : "theme".equals(wpSource) ? "Theme Collection" : "Online 8K";
                sourceRow.setText("Wallpaper Source: " + sLabel);

                String iLabel = interval <= 0 ? "Static (Off)" : interval + " min";
                intervalRow.setText("Auto-Change: " + iLabel);
                intervalRow.setEnabled(!"none".equals(wpSource));
                intervalRow.setAlpha("none".equals(wpSource) ? 0.4f : 1f);

                String oLabel = "sequential".equals(order) ? "Sequential" : "Randomized";
                orderRow.setText("Shuffling Mode: " + oLabel);
                orderRow.setEnabled(!"none".equals(wpSource));
                orderRow.setAlpha("none".equals(wpSource) ? 0.4f : 1f);
            }
        };

        sourceRow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String wpSource = choices.getString("wallpaper_source", "online");
                String badge = "none".equals(wpSource) ? "OFF" : "theme".equals(wpSource) ? "THEME" : "ONLINE 8K";
                previewTitle.setText("Wallpaper Source");
                renderBadges(previewBadges, new String[]{badge, "WALLPAPERS"});
                if ("online".equals(wpSource)) {
                    previewDesc.setText("Stream curated ultra-HD 8K and 4K wallpapers from Unsplash in the background. Downloads are cached locally to reduce network usage.");
                } else if ("theme".equals(wpSource)) {
                    previewDesc.setText("Display scenic backgrounds bundled with your currently applied theme.");
                } else {
                    previewDesc.setText("Disables background wallpapers and keeps a sleek, distraction-free solid color backdrop.");
                }
                updateWpDialogPreview(previewImage, wpSource);
                applyPrompt.setText("Press OK to cycle source (Online 8K / Theme / Off)");
            }
        });
        sourceRow.setOnClickListener(v -> {
            String current = choices.getString("wallpaper_source", "online");
            int nextIdx = 0;
            for (int i = 0; i < sources.length; i++) {
                if (sources[i].equals(current)) {
                    nextIdx = (i + 1) % sources.length;
                    break;
                }
            }
            String nextSource = sources[nextIdx];
            SharedPreferences.Editor ed = choices.edit().putString("wallpaper_source", nextSource);
            if ("online".equals(nextSource)) ed.remove(Config.WALLPAPER_CHOICE_KEY);
            else if ("none".equals(nextSource)) ed.putString(Config.WALLPAPER_CHOICE_KEY, "none");
            ed.apply();
            updateLabels.run();
            updateWpDialogPreview(previewImage, nextSource);
            sourceRow.getOnFocusChangeListener().onFocusChange(sourceRow, true);
            changed();
            refresh();
        });

        intervalRow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                int interval = choices.getInt("wallpaper_rotation_minutes", 5);
                String badge = interval <= 0 ? "STATIC" : interval + " MIN";
                previewTitle.setText("Auto-Change Interval");
                renderBadges(previewBadges, new String[]{badge, "ROTATION"});
                previewDesc.setText("Controls how frequently the wallpaper cycles automatically in the background. Choose 1m for active variety or 30-60m / Static for calm focus.");
                updateWpDialogPreview(previewImage, choices.getString("wallpaper_source", "online"));
                applyPrompt.setText("Press OK to step interval (1m, 5m, 15m, 30m, 60m, Static)");
            }
        });
        intervalRow.setOnClickListener(v -> {
            int current = choices.getInt("wallpaper_rotation_minutes", 5);
            int nextIdx = 0;
            for (int i = 0; i < intervals.length; i++) {
                if (intervals[i] == current) {
                    nextIdx = (i + 1) % intervals.length;
                    break;
                }
            }
            int nextInterval = intervals[nextIdx];
            choices.edit().putInt("wallpaper_rotation_minutes", nextInterval).apply();
            updateLabels.run();
            intervalRow.getOnFocusChangeListener().onFocusChange(intervalRow, true);
            changed();
            refresh();
        });

        orderRow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                String order = choices.getString("wallpaper_order", "shuffle");
                String badge = "sequential".equals(order) ? "SEQUENTIAL" : "RANDOMIZED";
                previewTitle.setText("Shuffling Sequence");
                renderBadges(previewBadges, new String[]{badge, "ORDER"});
                previewDesc.setText("Randomized shuffling mixes wallpapers so every rotation is fresh. Sequential cycles wallpapers in numerical collection order.");
                updateWpDialogPreview(previewImage, choices.getString("wallpaper_source", "online"));
                applyPrompt.setText("Press OK to toggle order (Randomized / Sequential)");
            }
        });
        orderRow.setOnClickListener(v -> {
            String current = choices.getString("wallpaper_order", "shuffle");
            String nextOrder = "sequential".equals(current) ? "shuffle" : "sequential";
            choices.edit().putString("wallpaper_order", nextOrder).apply();
            updateLabels.run();
            orderRow.getOnFocusChangeListener().onFocusChange(orderRow, true);
            changed();
            refresh();
        });

        cycleRow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                previewTitle.setText("Cycle Wallpaper Now");
                renderBadges(previewBadges, new String[]{"INSTANT", "NEXT PICTURE"});
                previewDesc.setText("Immediately downloads and advances to the next wallpaper in the rotation.");
                updateWpDialogPreview(previewImage, choices.getString("wallpaper_source", "online"));
                applyPrompt.setText("Press OK to cycle immediately");
            }
        });
        cycleRow.setOnClickListener(v -> {
            changed();
            Toast.makeText(this, "Cycling to next wallpaper...", Toast.LENGTH_SHORT).show();
            new OnlineWallpaperFetcher(this, 1920, 1080, 0, true, new OnlineWallpaperFetcher.Callback() {
                @Override
                public void onWallpaperLoaded(Bitmap bitmap) {
                    previewImage.post(() -> previewImage.setImageBitmap(bitmap));
                }
                @Override
                public void onWallpaperFailed() {}
            }).start();
        });

        if (themePicRow != null) {
            themePicRow.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    previewTitle.setText("Theme Wallpapers");
                    renderBadges(previewBadges, new String[]{"GALLERY", "LOCAL"});
                    previewDesc.setText("Pick a specific scenic picture from the current theme's wallpaper pack.");
                    updateWpDialogPreview(previewImage, "theme");
                    applyPrompt.setText("Press OK to open picture gallery");
                }
            });
            themePicRow.setOnClickListener(v -> {
                dialog.dismiss();
                showThemePictureDialog();
            });
        }

        updateLabels.run();
        dialog.show();
        sourceRow.requestFocus();
    }

    private TextView createWpRow(float density) {
        TextView row = new TextView(this);
        row.setTextColor(0xFFFFFFFF);
        row.setTextSize(17);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding((int) (18 * density), (int) (14 * density), (int) (18 * density), (int) (14 * density));
        row.setBackgroundResource(R.drawable.settings_choice_bg);
        row.setFocusable(true);
        row.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (8 * density);
        row.setLayoutParams(lp);
        return row;
    }

    private void updateWpDialogPreview(ImageView previewImage, String source) {
        if ("none".equals(source)) {
            previewImage.setImageDrawable(null);
            previewImage.setBackgroundColor(0xFF0B0E14);
            return;
        }
        previewImage.setBackgroundColor(0xFF000000);
        if ("theme".equals(source) && !config.wallpapers.isEmpty()) {
            String choice = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
            File f = config.wallpapers.get(0);
            if (choice != null) {
                for (File file : config.wallpapers) {
                    if (file.getName().equals(choice)) { f = file; break; }
                }
            }
            try {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    previewImage.setImageBitmap(bmp);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        Bitmap bmp = OnlineWallpaperFetcher.latestBitmap;
        if (bmp != null && !bmp.isRecycled()) {
            previewImage.setImageBitmap(bmp);
            return;
        }
        File cacheDir = OnlineWallpaperFetcher.getCacheDirectory(this);
        File[] cached = cacheDir.listFiles((d, n) -> n.endsWith(".jpg") && new File(d, n).length() > 5000);
        if (cached != null && cached.length > 0) {
            try {
                Bitmap decoded = BitmapFactory.decodeFile(cached[0].getAbsolutePath());
                if (decoded != null) {
                    previewImage.setImageBitmap(decoded);
                    return;
                }
            } catch (Throwable ignored) {}
        }
        if (!config.wallpapers.isEmpty()) {
            try {
                Bitmap themeBmp = BitmapFactory.decodeFile(config.wallpapers.get(0).getAbsolutePath());
                if (themeBmp != null) previewImage.setImageBitmap(themeBmp);
            } catch (Throwable ignored) {}
        }
    }

    private void showThemePictureDialog() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_theme_default);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = current == null ? images.size() : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    SharedPreferences.Editor ed = choices.edit().putString("wallpaper_source", "theme");
                    if (which == images.size()) {
                        ed.remove(Config.WALLPAPER_CHOICE_KEY);
                    } else {
                        ed.putString(Config.WALLPAPER_CHOICE_KEY, images.get(which).getName());
                    }
                    ed.apply();
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private static final String[] THEME_KEYS = {
            "glass", "nature_calm", "retro", "ruska", "aurora", "sweden", "six_columns", "dussehra",
            "scenes", "dark", "light", "night", "default"
    };

    private void showThemePicker() {
        Dialog dialog = new Dialog(this, R.style.Theme_TvHome);
        dialog.setContentView(R.layout.dialog_theme_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0xFA0B0E14));
        }

        LinearLayout listContainer = dialog.findViewById(R.id.themeListContainer);
        ThemePreviewView previewView = dialog.findViewById(R.id.dialogThemePreview);
        TextView previewTitle = dialog.findViewById(R.id.dialogPreviewTitle);
        LinearLayout previewBadges = dialog.findViewById(R.id.dialogPreviewBadges);
        TextView previewDesc = dialog.findViewById(R.id.dialogPreviewDescription);
        TextView applyPrompt = dialog.findViewById(R.id.dialogApplyPrompt);

        String current = choices.getString(Config.THEME_PRESET_KEY, "glass");
        float density = getResources().getDisplayMetrics().density;
        View toFocus = null;

        for (String key : THEME_KEYS) {
            final String themeKey = key;
            final ThemePreviewData themeData = ThemePreviewData.get(themeKey);

            TextView row = new TextView(this);
            String title = themeData.title;
            if (themeKey.equals(current)) {
                title += "  [" + getString(R.string.theme_current_tag) + "]";
            }
            row.setText(title);
            row.setTextColor(0xFFFFFFFF);
            row.setTextSize(17);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding((int) (18 * density), (int) (14 * density), (int) (18 * density), (int) (14 * density));
            row.setBackgroundResource(R.drawable.settings_choice_bg);
            row.setFocusable(true);
            row.setClickable(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (8 * density);
            row.setLayoutParams(lp);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    previewView.setTheme(themeData);
                    previewTitle.setText(themeData.title);
                    renderBadges(previewBadges, themeData.badges);
                    previewDesc.setText(themeData.description);
                    applyPrompt.setText(themeKey.equals(current)
                            ? getString(R.string.theme_current_tag) + " (" + getString(R.string.theme_label) + ")"
                            : getString(R.string.theme_confirm_prompt));
                }
            });

            row.setOnClickListener(v -> {
                ThemeApplier.applyTheme(this, themeKey);
                Toast.makeText(this, getString(R.string.theme_applied) + ": " + themeData.title, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                setResult(RESULT_OK);
                finish();
            });

            listContainer.addView(row);
            if (themeKey.equals(current)) {
                toFocus = row;
                previewView.setTheme(themeData);
                previewTitle.setText(themeData.title);
                renderBadges(previewBadges, themeData.badges);
                previewDesc.setText(themeData.description);
            }
        }

        dialog.show();
        if (toFocus != null) {
            toFocus.requestFocus();
        }
    }

    private void updateSettingsPreview(String title, String desc, String[] badges, String themeKey, String hint) {
        if (settingsPreviewTitle == null) return;
        settingsPreviewTitle.setText(title);
        settingsPreviewDescription.setText(desc);
        renderBadges(settingsPreviewBadges, badges);
        if (themeKey == null) {
            if (settingsThemePreview != null) settingsThemePreview.setVisibility(View.GONE);
            if (settingsWallpaperPreview != null) {
                settingsWallpaperPreview.setVisibility(View.VISIBLE);
                updateWpDialogPreview(settingsWallpaperPreview, choices.getString("wallpaper_source", "online"));
            }
        } else {
            if (settingsWallpaperPreview != null) settingsWallpaperPreview.setVisibility(View.GONE);
            if (settingsThemePreview != null) {
                settingsThemePreview.setVisibility(View.VISIBLE);
                settingsThemePreview.setTheme(ThemePreviewData.get(themeKey));
            }
        }
        if (settingsPreviewHint != null) {
            if (hint != null && !hint.isEmpty()) {
                settingsPreviewHint.setText(hint);
                settingsPreviewHint.setVisibility(View.VISIBLE);
            } else {
                settingsPreviewHint.setVisibility(View.GONE);
            }
        }
    }

    private void renderBadges(LinearLayout container, String[] badges) {
        if (container == null) return;
        container.removeAllViews();
        if (badges == null) return;
        float density = getResources().getDisplayMetrics().density;
        int max = Math.min(badges.length, 3);
        for (int i = 0; i < max; i++) {
            String badge = badges[i];
            TextView chip = new TextView(this);
            chip.setText(badge);
            chip.setTextSize(10);
            chip.setTextColor(0xFFE5F5EC);
            chip.setSingleLine(true);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            chip.setPadding((int) (8 * density), (int) (3 * density), (int) (8 * density), (int) (3 * density));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(6 * density);
            bg.setColor(0x2AFFFFFF);
            bg.setStroke((int) (1 * density), 0x334DAE6D);
            chip.setBackground(bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int) (6 * density));
            chip.setLayoutParams(lp);
            container.addView(chip);
        }
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        String currentTheme = choices.getString(Config.THEME_PRESET_KEY, "glass");
        String themeName = getString(R.string.theme_glass);
        if ("nature_calm".equals(currentTheme)) themeName = getString(R.string.theme_nature_calm);
        else if ("retro".equals(currentTheme)) themeName = getString(R.string.theme_retro);
        else if ("ruska".equals(currentTheme)) themeName = getString(R.string.theme_ruska);
        else if ("aurora".equals(currentTheme)) themeName = getString(R.string.theme_aurora);
        else if ("sweden".equals(currentTheme)) themeName = getString(R.string.theme_sweden);
        else if ("six_columns".equals(currentTheme)) themeName = getString(R.string.theme_six_columns);
        else if ("dussehra".equals(currentTheme)) themeName = getString(R.string.theme_dussehra);
        else if ("scenes".equals(currentTheme)) themeName = getString(R.string.theme_scenes);
        else if ("dark".equals(currentTheme)) themeName = getString(R.string.theme_dark);
        else if ("light".equals(currentTheme)) themeName = getString(R.string.theme_light);
        else if ("night".equals(currentTheme)) themeName = getString(R.string.theme_night);
        else if ("default".equals(currentTheme)) themeName = getString(R.string.theme_default);
        themeChoice.setText(getString(R.string.theme_state, themeName));

        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wpSource = choices.getString("wallpaper_source", "online");
        int interval = choices.getInt("wallpaper_rotation_minutes", 5);
        String order = choices.getString("wallpaper_order", "shuffle");
        String summary;
        if ("none".equals(wpSource)) {
            summary = getString(R.string.wallpaper_none);
        } else if ("theme".equals(wpSource)) {
            String customPic = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
            summary = customPic != null ? "Theme (" + customPic + ")" : "Theme Collection";
        } else {
            summary = "Online 8K (" + (interval <= 0 ? "Static" : interval + "m") + ", "
                    + ("sequential".equals(order) ? "Sequential" : "Shuffle") + ")";
        }
        wallpaperChoice.setText(getString(R.string.wallpaper_state, summary));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());

        if (wallpaperChoice != null && wallpaperChoice.hasFocus()) {
            wallpaperChoice.getOnFocusChangeListener().onFocusChange(wallpaperChoice, true);
        } else if (themeChoice != null && themeChoice.hasFocus()) {
            ThemePreviewData currentData = ThemePreviewData.get(currentTheme);
            updateSettingsPreview(currentData.title, currentData.description, currentData.badges, currentTheme, "Press OK to browse and change themes");
        } else if (getCurrentFocus() == null) {
            ThemePreviewData currentData = ThemePreviewData.get(currentTheme);
            updateSettingsPreview(currentData.title, currentData.description, currentData.badges, currentTheme, "Press OK to browse and change themes");
        }
    }

    private boolean hasOnTvChoices() {
        for (CustomSwitchData.SwitchItem item : CustomSwitchData.ALL) {
            if (choices.contains(item.key)) return true;
        }
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(Config.THEME_PRESET_KEY) || choices.contains(OpenOnStart.KEY)
                || choices.contains("wallpaper_source") || choices.contains("wallpaper_rotation_minutes")
                || choices.contains("wallpaper_order");
    }

    private void showSwitchesPicker() {
        Dialog dialog = new Dialog(this, R.style.Theme_TvHome);
        dialog.setContentView(R.layout.dialog_switches_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0xFA0B0E14));
        }

        LinearLayout listContainer = dialog.findViewById(R.id.switchesListContainer);
        ThemePreviewView previewView = dialog.findViewById(R.id.dialogSwitchesPreview);
        TextView previewTitle = dialog.findViewById(R.id.dialogSwitchTitle);
        LinearLayout previewBadges = dialog.findViewById(R.id.dialogSwitchBadges);
        TextView previewDesc = dialog.findViewById(R.id.dialogSwitchDescription);
        TextView prompt = dialog.findViewById(R.id.dialogSwitchPrompt);

        String currentTheme = choices.getString(Config.THEME_PRESET_KEY, "glass");
        previewView.setTheme(ThemePreviewData.get(currentTheme));

        Config currentConfig = Config.load(this);
        float density = getResources().getDisplayMetrics().density;

        // Reset row
        TextView resetRow = new TextView(this);
        resetRow.setText(R.string.switch_reset_defaults);
        resetRow.setTextColor(0xFF389BFF);
        resetRow.setTextSize(15);
        resetRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        resetRow.setPadding((int) (18 * density), (int) (12 * density), (int) (18 * density), (int) (12 * density));
        resetRow.setBackgroundResource(R.drawable.settings_choice_bg);
        resetRow.setFocusable(true);
        resetRow.setClickable(true);

        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        resetLp.bottomMargin = (int) (12 * density);
        resetRow.setLayoutParams(resetLp);

        resetRow.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                previewTitle.setText(R.string.switch_reset_defaults);
                previewDesc.setText("Reset all 26 feature switches back to the currently selected theme preset defaults.");
                renderBadges(previewBadges, new String[]{"ACTION", "RESET"});
                prompt.setText("Press OK to reset all switches");
            }
        });

        resetRow.setOnClickListener(v -> {
            SharedPreferences.Editor ed = choices.edit();
            for (CustomSwitchData.SwitchItem item : CustomSwitchData.ALL) ed.remove(item.key);
            ed.apply();
            changed();
            Toast.makeText(this, R.string.switches_reset_applied, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            showSwitchesPicker();
        });

        listContainer.addView(resetRow);

        View toFocus = null;
        for (CustomSwitchData.SwitchItem item : CustomSwitchData.ALL) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setFocusable(true);
            row.setClickable(true);
            row.setBackgroundResource(R.drawable.settings_choice_bg);
            row.setPadding((int) (18 * density), (int) (10 * density), (int) (18 * density), (int) (10 * density));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (6 * density);
            row.setLayoutParams(lp);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            textCol.setLayoutParams(textLp);

            TextView titleView = new TextView(this);
            titleView.setText(item.title);
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTextSize(15);
            textCol.addView(titleView);

            TextView keyView = new TextView(this);
            keyView.setText(item.key);
            keyView.setTextColor(0x88ACB8C1);
            keyView.setTextSize(11);
            textCol.addView(keyView);

            row.addView(textCol);

            boolean currentValue = choices.contains(item.key)
                    ? choices.getBoolean(item.key, item.defaultVal)
                    : Config.getSwitchValue(currentConfig, item.key);

            TextView badgeView = new TextView(this);
            badgeView.setTextSize(12);
            badgeView.setPadding((int) (12 * density), (int) (4 * density), (int) (12 * density), (int) (4 * density));
            badgeView.setBackgroundResource(R.drawable.pill_bg);
            updateBadgeVisual(badgeView, currentValue);
            row.addView(badgeView);

            row.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    previewTitle.setText(item.title);
                    previewDesc.setText(item.description);
                    boolean cur = choices.contains(item.key)
                            ? choices.getBoolean(item.key, item.defaultVal)
                            : Config.getSwitchValue(Config.load(this), item.key);
                    renderBadges(previewBadges, new String[]{item.category, cur ? "STATUS: ON" : "STATUS: OFF"});
                    prompt.setText(getString(R.string.switch_toggle_hint));
                }
            });

            row.setOnClickListener(v -> {
                boolean cur = choices.contains(item.key)
                        ? choices.getBoolean(item.key, item.defaultVal)
                        : Config.getSwitchValue(Config.load(this), item.key);
                boolean next = !cur;
                choices.edit().putBoolean(item.key, next).apply();
                changed();
                updateBadgeVisual(badgeView, next);
                renderBadges(previewBadges, new String[]{item.category, next ? "STATUS: ON" : "STATUS: OFF"});
            });

            if (toFocus == null) toFocus = row;
            listContainer.addView(row);
        }

        dialog.show();
        if (toFocus != null) toFocus.requestFocus();
    }

    private void updateBadgeVisual(TextView badge, boolean on) {
        badge.setText(on ? R.string.switch_on : R.string.switch_off);
        badge.setTextColor(on ? 0xFF30D158 : 0xFF8E8E93);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
