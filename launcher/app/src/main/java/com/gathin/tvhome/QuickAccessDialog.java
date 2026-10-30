package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

final class QuickAccessDialog implements View.OnClickListener, View.OnFocusChangeListener,
        View.OnLongClickListener, PopupWindow.OnDismissListener, Runnable {

    private static final int ID_POWER = 101;
    private static final int ID_AUDIO = 102;
    private static final int ID_WIFI = 103;
    private static final int ID_DND = 104;
    private static final int ID_TIMER = 105;
    private static final int ID_PREV = 106;
    private static final int ID_PLAY = 107;
    private static final int ID_NEXT = 108;
    private static final int ID_INPUTS = 109;
    private static final int ID_STATS = 110;
    private static final int ID_SETTINGS = 111;
    private static final int ID_APPS = 112;

    private static final Handler TIMER_HANDLER = new Handler(Looper.getMainLooper());
    private static int activeSleepMinutes = 0;

    private final MainActivity activity;
    private final float density;
    private final boolean reducedMotion;
    private final boolean rtl;
    private final PopupWindow popup;
    private final LinearLayout panel;

    private TextView dndStatusText;
    private TextView timerStatusText;
    private TextView mediaSubtitleText;
    private View mediaPlayBtn;

    QuickAccessDialog(MainActivity activity, boolean reducedMotion) {
        this.activity = activity;
        this.reducedMotion = reducedMotion;
        this.rtl = activity.getResources().getBoolean(R.bool.rtl_menu_bar);
        this.density = activity.getResources().getDisplayMetrics().density;

        panel = new LinearLayout(activity);
        panel.setLayoutDirection(rtl ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(surface(0xEB131722, 28, 0x33FFFFFF, 1));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);

        buildContent();

        popup = new PopupWindow(panel, dp(336), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setAnimationStyle(0);
        popup.setElevation(0f);
        popup.setOnDismissListener(this);
    }

    void show() {
        popup.showAtLocation(activity.getWindow().getDecorView(),
                (rtl ? Gravity.START : Gravity.END) | Gravity.TOP, dp(24), dp(18));

        View container = panel.getRootView();
        if (container.getLayoutParams() instanceof WindowManager.LayoutParams) {
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) container.getLayoutParams();
            params.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            params.dimAmount = 0.40f;
            activity.getWindowManager().updateViewLayout(container, params);
        }

        panel.setAlpha(0f);
        panel.setTranslationX(rtl ? -dp(18) : dp(18));
        panel.animate().alpha(1f).translationX(0f)
                .setDuration(reducedMotion ? Motion.REDUCED_MS : 200).start();

        panel.post(this);
    }

    @Override
    public void run() {
        if (popup.isShowing()) {
            View power = panel.findViewById(ID_POWER);
            if (power != null) power.requestFocus();
        } else {
            performSleep();
        }
    }

    @Override
    public void onDismiss() {
        activity.onQuickAccessDismissed();
    }

    void close() {
        if (popup.isShowing()) popup.dismiss();
    }

    private void buildContent() {
        // 1. Header: TextClock & Control Center title
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(4), 0, dp(4), dp(10));

        TextClock clock = new TextClock(activity);
        clock.setTextColor(0xFFFFFFFF);
        clock.setTextSize(14);
        clock.setTypeface(UiFont.get(activity, R.string.font_medium));
        header.addView(clock, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tag = new TextView(activity);
        tag.setText("CONTROL CENTER");
        tag.setTextColor(0x80FFFFFF);
        tag.setTextSize(10);
        tag.setLetterSpacing(0.08f);
        tag.setTypeface(UiFont.get(activity, R.string.font_medium));
        header.addView(tag, new LinearLayout.LayoutParams(-2, -2));
        panel.addView(header);

        // 2. Grid: Left col (Power + Audio), Right col (Wi-Fi, DND, Timer)
        LinearLayout grid = new LinearLayout(activity);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);

        // Left Column
        LinearLayout leftCol = new LinearLayout(activity);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setClipChildren(false);
        leftCol.setClipToPadding(false);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, -2, 1f);
        leftParams.setMarginEnd(dp(5));
        leftCol.setLayoutParams(leftParams);

        leftCol.addView(createTile(ID_POWER, R.drawable.ic_power, 0xFFFFFFFF, "Sleep", "Hold: Restart", true, 0));
        leftCol.addView(createTile(ID_AUDIO, R.drawable.ic_bluetooth, 0xFF0A84FF, "Audio", "Bluetooth", false, 8));
        grid.addView(leftCol);

        // Right Column
        LinearLayout rightCol = new LinearLayout(activity);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setClipChildren(false);
        rightCol.setClipToPadding(false);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, -2, 1f);
        rightParams.setMarginStart(dp(5));
        rightCol.setLayoutParams(rightParams);

        String ssid = getConnectedSsid();
        rightCol.addView(createTile(ID_WIFI, R.drawable.ic_wifi, 0xFF0A84FF, "Wi-Fi", !TextUtils.isEmpty(ssid) ? ssid : "Connected", false, 0));

        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        boolean isMuted = am != null && (am.isStreamMute(AudioManager.STREAM_MUSIC) || am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0);
        View dnd = createTile(ID_DND, R.drawable.ic_moon, 0xFFBF5AF2, "Do Not Disturb", isMuted ? "Muted" : "Sound On", false, 6);
        dndStatusText = (TextView) ((ViewGroup) ((ViewGroup) dnd).getChildAt(1)).getChildAt(1);
        rightCol.addView(dnd);

        View timer = createTile(ID_TIMER, R.drawable.ic_timer, 0xFFFF9F0A, "Sleep Timer", activeSleepMinutes > 0 ? (activeSleepMinutes + " min") : "Off", false, 6);
        timerStatusText = (TextView) ((ViewGroup) ((ViewGroup) timer).getChildAt(1)).getChildAt(1);
        rightCol.addView(timer);

        grid.addView(rightCol);
        panel.addView(grid);

        // 3. Media Player Widget
        buildMediaWidget();

        // 4. Bottom Action Row
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        row.setLayoutParams(lp);

        row.addView(createButton(ID_INPUTS, R.drawable.ic_input, null, 44));
        row.addView(createButton(ID_STATS, R.drawable.ic_memory, null, 44));
        row.addView(createButton(ID_SETTINGS, R.drawable.ic_settings, null, 44));
        row.addView(createButton(ID_APPS, R.drawable.ic_apps, null, 44));
        panel.addView(row);
    }

    private View createTile(int id, int iconRes, int iconTint, String titleText, String subText, boolean vertical, int topMarginDp) {
        LinearLayout card = new LinearLayout(activity);
        card.setId(id);
        card.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        card.setGravity(vertical ? Gravity.CENTER : Gravity.CENTER_VERTICAL);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setBackground(surface(0x24FFFFFF, vertical ? 20 : 16, 0x1AFFFFFF, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(vertical ? 112 : 52));
        if (topMarginDp > 0) lp.topMargin = dp(topMarginDp);
        card.setLayoutParams(lp);
        card.setOnClickListener(this);
        if (vertical) card.setOnLongClickListener(this);
        card.setOnFocusChangeListener(this);

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setColorFilter(iconTint);

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(vertical ? 13 : 12);
        title.setTypeface(UiFont.get(activity, R.string.font_medium));

        TextView sub = new TextView(activity);
        sub.setText(subText);
        sub.setTextColor(0x8AFFFFFF);
        sub.setTextSize(10);

        if (vertical) {
            FrameLayout iconCircle = new FrameLayout(activity);
            iconCircle.setBackground(surface(0x30FFFFFF, 21));
            iconCircle.addView(icon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
            card.addView(iconCircle, new LinearLayout.LayoutParams(dp(42), dp(42)));

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
            tlp.topMargin = dp(6);
            card.addView(title, tlp);
            card.addView(sub);
        } else {
            card.setPadding(dp(10), dp(6), dp(10), dp(6));
            card.addView(icon, new LinearLayout.LayoutParams(dp(20), dp(20)));

            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(8), 0, 0, 0);
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(title);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(sub);
            card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        return card;
    }

    private void buildMediaWidget() {
        LinearLayout widget = new LinearLayout(activity);
        widget.setOrientation(LinearLayout.HORIZONTAL);
        widget.setGravity(Gravity.CENTER_VERTICAL);
        widget.setPadding(dp(12), dp(10), dp(12), dp(10));
        widget.setBackground(surface(0x28FFFFFF, 18, 0x24FFFFFF, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64));
        lp.topMargin = dp(10);
        widget.setLayoutParams(lp);

        FrameLayout iconBox = new FrameLayout(activity);
        iconBox.setBackground(surface(0x33FFFFFF, 12));
        ImageView note = new ImageView(activity);
        note.setImageResource(R.drawable.ic_music_note);
        note.setColorFilter(0xFFFF2D55);
        iconBox.addView(note, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        widget.addView(iconBox, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout texts = new LinearLayout(activity);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(10), 0, dp(6), 0);

        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        boolean musicActive = am != null && am.isMusicActive();

        TextView title = new TextView(activity);
        title.setText("Now Playing");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(12);
        title.setTypeface(UiFont.get(activity, R.string.font_medium));
        texts.addView(title);

        mediaSubtitleText = new TextView(activity);
        mediaSubtitleText.setText(musicActive ? "Audio Playing" : "Background Music");
        mediaSubtitleText.setTextColor(0x8AFFFFFF);
        mediaSubtitleText.setTextSize(10);
        texts.addView(mediaSubtitleText);
        widget.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));

        LinearLayout controls = new LinearLayout(activity);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.addView(createButton(ID_PREV, 0, "⏮", 28));

        mediaPlayBtn = createButton(ID_PLAY, 0, musicActive ? "❚❚" : "▶", 34);
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        playLp.setMargins(dp(6), 0, dp(6), 0);
        mediaPlayBtn.setLayoutParams(playLp);
        controls.addView(mediaPlayBtn);

        controls.addView(createButton(ID_NEXT, 0, "⏭", 28));
        widget.addView(controls);
        panel.addView(widget);
    }

    private View createButton(int id, int iconRes, String text, int sizeDp) {
        FrameLayout btn = new FrameLayout(activity);
        btn.setId(id);
        btn.setFocusable(true);
        btn.setFocusableInTouchMode(true);
        btn.setClickable(true);
        btn.setBackground(surface(0x28FFFFFF, sizeDp / 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp));
        if (iconRes != 0) lp.setMargins(dp(10), 0, dp(10), 0);
        btn.setLayoutParams(lp);

        if (iconRes != 0) {
            ImageView icon = new ImageView(activity);
            icon.setImageResource(iconRes);
            icon.setColorFilter(0xFFFFFFFF);
            btn.addView(icon, new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        } else {
            TextView tv = new TextView(activity);
            tv.setText(text);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(13);
            tv.setGravity(Gravity.CENTER);
            btn.addView(tv, new FrameLayout.LayoutParams(-1, -1));
        }

        btn.setOnClickListener(this);
        btn.setOnFocusChangeListener(this);
        return btn;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == ID_DND) {
            AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                boolean muted = am.isStreamMute(AudioManager.STREAM_MUSIC) || am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
                if (dndStatusText != null) dndStatusText.setText(muted ? "Muted" : "Sound On");
            }
            return;
        }
        if (id == ID_TIMER) {
            cycleSleepTimer();
            return;
        }
        if (id == ID_PREV || id == ID_NEXT) {
            sendMediaKey(id == ID_PREV ? KeyEvent.KEYCODE_MEDIA_PREVIOUS : KeyEvent.KEYCODE_MEDIA_NEXT);
            return;
        }
        if (id == ID_PLAY) {
            sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            boolean active = am != null && !am.isMusicActive();
            TextView tv = (TextView) ((ViewGroup) mediaPlayBtn).getChildAt(0);
            if (tv != null) tv.setText(active ? "❚❚" : "▶");
            if (mediaSubtitleText != null) mediaSubtitleText.setText(active ? "Audio Playing" : "Paused");
            return;
        }
        close();
        if (id == ID_POWER) performSleep();
        else if (id == ID_AUDIO) activity.showBluetoothPanel();
        else if (id == ID_WIFI) activity.showNetworkPanel();
        else if (id == ID_INPUTS) activity.showInputs();
        else if (id == ID_STATS) activity.optimizeRam(true);
        else if (id == ID_SETTINGS) activity.openNativeSettings();
        else if (id == ID_APPS) activity.onLibraryRequested();
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == ID_POWER) {
            close();
            performRestart();
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        v.animate().scaleX(hasFocus ? 1.05f : 1f).scaleY(hasFocus ? 1.05f : 1f)
                .setDuration(reducedMotion ? Motion.REDUCED_MS : 140).start();

        int id = v.getId();
        int radius = (id == ID_POWER) ? 20 : (id == ID_AUDIO || id == ID_WIFI || id == ID_DND || id == ID_TIMER) ? 16 : 22;
        v.setBackground(surface(hasFocus ? 0x48FFFFFF : 0x24FFFFFF, radius,
                hasFocus ? 0xFFFFFFFF : 0x1AFFFFFF, hasFocus ? 2 : 1));
    }

    private void performSleep() {
        if (HomeKeyService.sleep()) return;
        try {
            activity.startActivity(new Intent(activity, ClockActivity.class));
        } catch (Exception ignored) {}
    }

    private void performRestart() {
        try {
            activity.sendBroadcast(new Intent("android.intent.action.REBOOT").putExtra("nowait", 1));
        } catch (Exception ignored) {}
        try {
            activity.startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
        } catch (Exception ignored) {}
    }

    private void cycleSleepTimer() {
        int[] steps = {0, 15, 30, 60, 120};
        int next = 0;
        for (int s : steps) {
            if (s > activeSleepMinutes) { next = s; break; }
        }
        activeSleepMinutes = next;
        TIMER_HANDLER.removeCallbacksAndMessages(null);
        if (activeSleepMinutes > 0) {
            if (timerStatusText != null) timerStatusText.setText(activeSleepMinutes + " min");
            Toast.makeText(activity, "Sleep in " + activeSleepMinutes + "m", Toast.LENGTH_SHORT).show();
            TIMER_HANDLER.postDelayed(this, (long) activeSleepMinutes * 60000L);
        } else if (timerStatusText != null) {
            timerStatusText.setText("Off");
        }
    }

    private void sendMediaKey(int keyCode) {
        AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;
        long eventTime = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0);
        am.dispatchMediaKeyEvent(down);
        am.dispatchMediaKeyEvent(up);
    }

    private String getConnectedSsid() {
        try {
            WifiManager wm = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wm != null ? wm.getConnectionInfo() : null;
            String ssid = info != null ? info.getSSID() : null;
            if (ssid != null && !ssid.equals("<unknown ssid>")) {
                return ssid.replace("\"", "").trim();
            }
        } catch (Exception ignored) {}
        return "Connected";
    }

    private GradientDrawable surface(int color, int radius) {
        return surface(color, radius, 0x18FFFFFF, 1);
    }

    private GradientDrawable surface(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * density);
    }
}
