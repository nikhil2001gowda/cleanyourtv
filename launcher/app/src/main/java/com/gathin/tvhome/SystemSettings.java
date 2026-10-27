package com.gathin.tvhome;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;

/** The standard settings intent, or, when nothing usable answers it, the first vendor settings entry
 *  from settings_fallback_components that is installed (Xiaomi China, TCL / FFALCON, Fire OS). */
final class SystemSettings {
    private SystemSettings() {}

    static Intent intent(Context context, String action) {
        PackageManager pm = context.getPackageManager();
        Intent standard = new Intent(action);
        ResolveInfo resolved = pm.resolveActivity(standard, PackageManager.MATCH_DEFAULT_ONLY);
        // Fire OS resolves the standard actions to a CTS compliance stub that opens nothing;
        // treat it as missing so the vendor fallback runs.
        if (resolved != null && !resolved.activityInfo.name.contains("CTSDummy")) return standard;
        Intent vendor = vendorIntent(context);
        return vendor != null ? vendor : standard;
    }

    /** The first vendor settings entry from settings_fallback_components that is installed, or null
     *  when none is. China-market firmware keeps pages the AOSP TV settings has no row for -
     *  Xiaomi's Account & Security, where the ADB switch lives - in the vendor's own settings app;
     *  MainActivity's Settings menu offers it as "Native settings" while one is installed. */
    static Intent vendorIntent(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String value : context.getResources().getStringArray(R.array.settings_fallback_components)) {
            ComponentName component = ComponentName.unflattenFromString(value);
            if (component == null) continue;
            // Explicit resolveActivity() always succeeds, so check the package exists instead.
            try {
                pm.getPackageInfo(component.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            return new Intent().setComponent(component);
        }
        return null;
    }

    /** The TV's App info page for pkg, or null where the firmware has none (CVTE's settings app, #1202). */
    static Intent appInfo(Context context, String pkg) {
        Intent info = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg));
        return context.getPackageManager().resolveActivity(info, PackageManager.MATCH_DEFAULT_ONLY) != null ? info : null;
    }

    /** True when pkg is installed and not a non-updated system app. */
    static boolean canUninstall(Context context, String pkg) {
        if (pkg == null || pkg.equals(context.getPackageName())) return false;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(pkg, 0);
            return (ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    || (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** Prompts to uninstall pkg using ACTION_DELETE, ACTION_UNINSTALL_PACKAGE, or App info fallback. */
    static boolean uninstall(Context context, String pkg) {
        if (pkg == null) return false;
        Uri uri = Uri.parse("package:" + pkg);

        // 1. Primary: ACTION_DELETE with package URI (standard on Android 8+)
        Intent delete = new Intent(Intent.ACTION_DELETE, uri);
        delete.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(delete);
            return true;
        } catch (ActivityNotFoundException | SecurityException ignored) {}

        // 2. Fallback: ACTION_UNINSTALL_PACKAGE
        Intent uninstallPkg = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri);
        uninstallPkg.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        uninstallPkg.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(uninstallPkg);
            return true;
        } catch (ActivityNotFoundException | SecurityException ignored) {}

        // 3. Fallback for restricted/OEM TVs: open App info where user can manage/uninstall
        Intent info = appInfo(context, pkg);
        if (info != null) {
            info.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(info);
                return true;
            } catch (ActivityNotFoundException | SecurityException ignored) {}
        }
        return false;
    }
}
