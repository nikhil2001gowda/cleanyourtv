// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 toolicious, customized for CleanMyTV
package io.github.toolicious.homeonfire;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Authentic Amazon Fire TV 2020 Boot Animation.
 * Replicates the OEM Fire TV boot transition (pure deep black background,
 * centered "fire tv" logotype with smile arrow, and the glowing warm amber/peach
 * light flare sweeping smoothly along the smile curve).
 *
 * Trajectory and timing are mapped with 1:1 fidelity from /system/media/bootanimation.zip
 * (part01 loop: 44 frames @ 24 fps = 1.833s cycle) running at 60 FPS in hardware Canvas.
 */
public class LoaderView extends View {

    // ── Stage geometry (1080p reference) ──────────────────────────────────────
    private static final float W = 1920f, H = 1080f;
    private static final float CX = W / 2f, CY = H / 2f;

    // Logo dimensions in 1080p
    private static final float LOGO_W = 531f;
    private static final float LOGO_H = 276f;
    private static final float LOGO_LEFT = CX - LOGO_W / 2f;
    private static final float LOGO_TOP = CY - LOGO_H / 2f - 28f;

    // Flare radius
    private static final float FLARE_RADIUS = 370f;

    // Flare cycle duration: 44 frames @ 24fps = 1.8333s
    private static final float CYCLE_DURATION = 1.8333f;

    // ── OEM Trajectory Lookups (44 frames mapped to 1080p) ─────────────────────
    private static final float[] FLARE_X = {
        0.0f, 0.0f, 691.7f, 687.5f, 716.1f, 715.9f, 720.5f, 728.6f,
        759.1f, 777.5f, 794.6f, 809.4f, 833.8f, 845.7f, 859.1f, 874.5f,
        910.0f, 928.8f, 946.3f, 960.9f, 993.3f, 1009.4f, 1024.3f, 1038.5f,
        1064.8f, 1076.5f, 1087.7f, 1097.9f, 1111.4f, 1117.7f, 1123.9f, 1130.0f,
        1142.1f, 1147.9f, 1153.6f, 1158.4f, 1168.9f, 1170.4f, 1185.7f, 1187.3f,
        1207.2f, 0.0f, 0.0f, 0.0f
    };

    private static final float[] FLARE_Y = {
        0.0f, 0.0f, 556.0f, 560.5f, 530.4f, 533.1f, 535.8f, 539.8f,
        550.6f, 556.2f, 561.8f, 567.0f, 577.3f, 582.1f, 586.4f, 589.7f,
        593.7f, 594.0f, 592.8f, 591.2f, 587.4f, 584.7f, 581.7f, 578.6f,
        572.2f, 569.0f, 565.9f, 562.8f, 559.7f, 558.1f, 556.2f, 554.3f,
        550.2f, 548.0f, 545.7f, 543.4f, 538.9f, 536.5f, 534.0f, 532.3f,
        525.8f, 0.0f, 0.0f, 0.0f
    };

    private static final float[] FLARE_INTENSITY = {
        0.0f, 0.0f, 0.006f, 0.012f, 0.053f, 0.076f, 0.111f, 0.162f,
        0.303f, 0.384f, 0.465f, 0.545f, 0.715f, 0.8f, 0.877f, 0.939f,
        1.0f, 0.934f, 0.863f, 0.786f, 0.616f, 0.544f, 0.484f, 0.432f,
        0.352f, 0.322f, 0.296f, 0.274f, 0.253f, 0.243f, 0.233f, 0.221f,
        0.194f, 0.18f, 0.165f, 0.129f, 0.085f, 0.061f, 0.036f, 0.021f,
        0.006f, 0.0f, 0.0f, 0.0f
    };

    // ── Timeline (seconds) ────────────────────────────────────────────────────
    private static final float T_HOLD = 6.0f;

    // ── Runtime ────────────────────────────────────────────────────────────────
    private boolean running = false;
    private boolean loop = false;
    private boolean blackScreen = false;
    private long startNanos = 0L;
    private OnEndListener onEndListener;
    private boolean endNotified = false;

    private final Paint flarePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private Drawable logoDrawable;

    public LoaderView(Context c) { this(c, null); }

    public LoaderView(Context c, AttributeSet a) {
        super(c, a);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setCaptions(String loading, String ready) {
        // Retained for compatibility with HijackService API; authentic boot animation uses pure logo
    }

    public void setLooping(boolean l) { loop = l; }

    public void setBlackScreen(boolean b) { blackScreen = b; }

    public interface OnEndListener { void onEnd(); }

    public void setOnEndListener(OnEndListener l) { onEndListener = l; }

    public void start() {
        if (running) return;
        running = true;
        endNotified = false;
        startNanos = System.nanoTime();
        postInvalidateOnAnimation();
    }

    public void stop() { running = false; }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        start();
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) start();
        else stop();
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int withAlpha(int color, float op) {
        int a = (int) (clamp(op, 0f, 1f) * 255f + 0.5f);
        return (color & 0x00FFFFFF) | (a << 24);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Deep pure black background
        canvas.drawColor(0xFF000000);

        if (blackScreen) {
            return;
        }

        float t = currentTime();

        int vw = getWidth(), vh = getHeight();
        float s = Math.min(vw / W, vh / H);
        float ox = (vw - W * s) / 2f, oy = (vh - H * s) / 2f;

        canvas.save();
        canvas.translate(ox, oy);
        canvas.scale(s, s);

        // 1. Draw animated warm amber flare
        drawFlare(canvas, t);

        // 2. Draw crisp authentic Fire TV logo
        drawLogo(canvas, t);

        canvas.restore();

        if (running && (loop || t < T_HOLD)) {
            postInvalidateOnAnimation();
        } else if (running && !loop && !endNotified) {
            endNotified = true;
            if (onEndListener != null) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (onEndListener != null) onEndListener.onEnd();
                    }
                });
            }
        }
    }

    private float currentTime() {
        float t = (System.nanoTime() - startNanos) / 1_000_000_000f;
        if (!loop && t > T_HOLD) {
            t = T_HOLD;
        }
        return t;
    }

    private void drawFlare(Canvas canvas, float t) {
        float phase = (t % CYCLE_DURATION) / CYCLE_DURATION;
        float frameFloat = phase * 44f;
        int f0 = (int) frameFloat;
        int f1 = (f0 + 1) % 44;
        float frac = frameFloat - f0;

        // Smooth 60 FPS interpolation between discrete frames
        float fx = FLARE_X[f0] + frac * (FLARE_X[f1] - FLARE_X[f0]);
        float fy = FLARE_Y[f0] + frac * (FLARE_Y[f1] - FLARE_Y[f0]);
        float intensity = FLARE_INTENSITY[f0] + frac * (FLARE_INTENSITY[f1] - FLARE_INTENSITY[f0]);

        if (intensity <= 0.005f || fx <= 0f) return;

        // Multi-stop radial gradient matching authentic 2020 OEM boot flare:
        // core: warm peach/pink [225, 175, 180], mid: amber [245, 144, 48], outer: deep ember [200, 69, 16]
        int cCore  = withAlpha(0xFFFFC69C, 0.95f * intensity);
        int cAmber = withAlpha(0xFFF59030, 0.82f * intensity);
        int cEmber = withAlpha(0xFFC84510, 0.60f * intensity);
        int cDeep  = withAlpha(0xFF651505, 0.28f * intensity);
        int cOuter = 0x00000000;

        RadialGradient shader = new RadialGradient(
                fx, fy, FLARE_RADIUS,
                new int[]{cCore, cAmber, cEmber, cDeep, cOuter},
                new float[]{0.0f, 0.22f, 0.48f, 0.72f, 1.0f},
                Shader.TileMode.CLAMP);

        flarePaint.setShader(shader);
        canvas.drawCircle(fx, fy, FLARE_RADIUS, flarePaint);
        flarePaint.setShader(null);
    }

    private void drawLogo(Canvas canvas, float t) {
        if (logoDrawable == null) {
            Drawable d = getContext().getDrawable(R.drawable.firetv_boot_logo);
            if (d != null) logoDrawable = d.mutate();
        }
        if (logoDrawable == null) return;

        logoDrawable.setBounds((int) LOGO_LEFT, (int) LOGO_TOP,
                (int) (LOGO_LEFT + LOGO_W), (int) (LOGO_TOP + LOGO_H));

        // Smooth subtle fade-in on first appearance (150ms)
        float alpha = clamp(t * 6.5f, 0f, 1f);
        logoDrawable.setAlpha((int) (alpha * 255f));
        logoDrawable.draw(canvas);
    }
}
