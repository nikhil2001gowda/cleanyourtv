package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

/**
 * High-performance vector canvas rendering a 16:9 live mini TV launcher preview
 * of any theme, showing real-time background, top shelf, dock styling, and app columns.
 */
public final class ThemePreviewView extends View {
    private ThemePreviewData data;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final RectF clipRect = new RectF();
    private final Path clipPath = new Path();

    // App card colors for realistic dock mockup
    private static final int[] APP_COLORS = {
            0xFFE50914, // Netflix Red
            0xFF00A8E1, // Prime Video Blue
            0xFFFF0000, // YouTube Red
            0xFF333333, // Apple TV Slate
            0xFF0C80DF, // Disney/Hotstar Blue
            0xFF00D166, // Spotify Green
            0xFFFF6600  // Crunchyroll Orange
    };

    public ThemePreviewView(Context context) {
        super(context);
        init();
    }

    public ThemePreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemePreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        data = ThemePreviewData.get("glass");
    }

    public void setTheme(ThemePreviewData themeData) {
        if (themeData != null && this.data != themeData) {
            this.data = themeData;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        // Maintain 16:9 aspect ratio if height is not explicitly fixed
        int height = (width * 9) / 16;
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightSpecMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float radius = 12f * density;

        bounds.set(0, 0, w, h);
        clipRect.set(bounds);
        clipPath.reset();
        clipPath.addRoundRect(clipRect, radius, radius, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);

        // 1. Background Gradient
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, h, data.bgStart, data.bgEnd, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);
        paint.setShader(null);

        // 2. Theme Special Effects
        if ("aurora".equals(data.key)) {
            // Draw dual polar aurora luminous wave bands
            paint.setColor(0x3500E5FF);
            Path aurora1 = new Path();
            aurora1.moveTo(0, h * 0.15f);
            aurora1.cubicTo(w * 0.35f, h * 0.05f, w * 0.65f, h * 0.35f, w, h * 0.18f);
            aurora1.lineTo(w, h * 0.45f);
            aurora1.cubicTo(w * 0.65f, h * 0.55f, w * 0.35f, h * 0.25f, 0, h * 0.35f);
            aurora1.close();
            canvas.drawPath(aurora1, paint);

            paint.setColor(0x309C27B0);
            Path aurora2 = new Path();
            aurora2.moveTo(0, h * 0.28f);
            aurora2.cubicTo(w * 0.4f, h * 0.4f, w * 0.7f, h * 0.12f, w, h * 0.25f);
            aurora2.lineTo(w, h * 0.5f);
            aurora2.cubicTo(w * 0.7f, h * 0.35f, w * 0.4f, h * 0.6f, 0, h * 0.42f);
            aurora2.close();
            canvas.drawPath(aurora2, paint);
        } else if ("dussehra".equals(data.key)) {
            // Festive golden spotlight and garland arcs
            paint.setColor(0x30FFB23C);
            canvas.drawCircle(w * 0.5f, h * 0.2f, w * 0.28f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f * density);
            paint.setColor(0x77FFB23C);
            for (int i = 0; i < 6; i++) {
                float x1 = (w / 6f) * i;
                float x2 = (w / 6f) * (i + 1);
                Path toran = new Path();
                toran.moveTo(x1, 0);
                toran.quadTo((x1 + x2) / 2f, 14f * density, x2, 0);
                canvas.drawPath(toran, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        } else if ("glass".equals(data.key)) {
            // 16:9 Cinematic poster card on right half
            float posterW = w * 0.42f;
            float posterH = h * 0.50f;
            float posterX = w * 0.54f;
            float posterY = h * 0.14f;
            RectF posterRect = new RectF(posterX, posterY, posterX + posterW, posterY + posterH);

            paint.setColor(0x40000000);
            canvas.drawRoundRect(posterRect, 8f * density, 8f * density, paint);

            paint.setShader(new LinearGradient(posterX, posterY, posterX + posterW, posterY + posterH,
                    0x66FF0055, 0x44220033, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(posterRect, 8f * density, 8f * density, paint);
            paint.setShader(null);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f * density);
            paint.setColor(0x55FFFFFF);
            canvas.drawRoundRect(posterRect, 8f * density, 8f * density, paint);
            paint.setStyle(Paint.Style.FILL);

            // Studio lighting reflection
            paint.setColor(0x22FFFFFF);
            canvas.drawCircle(posterX + posterW * 0.75f, posterY + posterH * 0.3f, posterW * 0.35f, paint);
        }

        // 3. Mini Top Bar
        boolean darkText = "light".equals(data.key);
        int textColor = darkText ? 0xCC111620 : 0xEEFFFFFF;
        paint.setColor(textColor);
        paint.setTextSize(9f * density);
        paint.setFakeBoldText(true);
        canvas.drawText("10:24", 16f * density, 18f * density, paint);
        paint.setFakeBoldText(false);

        // Status icons (mini circles)
        paint.setColor(darkText ? 0x88111620 : 0x88FFFFFF);
        canvas.drawCircle(w - 32f * density, 14f * density, 2.5f * density, paint);
        canvas.drawCircle(w - 22f * density, 14f * density, 2.5f * density, paint);
        canvas.drawCircle(w - 12f * density, 14f * density, 2.5f * density, paint);

        // 4. Mini Top Shelf Content (left cards)
        float shelfW = w * 0.22f;
        float shelfH = h * 0.36f;
        for (int i = 0; i < 2; i++) {
            float sx = (16f * density) + (i * (shelfW + 8f * density));
            float sy = h * 0.18f;
            RectF card = new RectF(sx, sy, sx + shelfW, sy + shelfH);
            paint.setColor(darkText ? 0x1A000000 : 0x22FFFFFF);
            canvas.drawRoundRect(card, 6f * density, 6f * density, paint);
        }

        // 5. Dock Band
        float dockY = h * 0.65f;
        float dockH = h * 0.30f;
        float dockMargin = 10f * density;
        RectF dockRect = new RectF(dockMargin, dockY, w - dockMargin, dockY + dockH);

        int dockAlpha = (int) (255 * (data.dockOpacity / 100f));
        if (data.dockOpacity > 0) {
            paint.setColor(darkText ? Color.argb((int)(dockAlpha * 0.15f), 0, 0, 0)
                    : Color.argb((int)(dockAlpha * 0.28f), 255, 255, 255));
            canvas.drawRoundRect(dockRect, 10f * density, 10f * density, paint);

            if (data.dockGlass) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1f * density);
                paint.setColor(darkText ? 0x22000000 : 0x33FFFFFF);
                canvas.drawRoundRect(dockRect, 10f * density, 10f * density, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        // 6. Dock App Tiles
        int cols = Math.max(4, Math.min(6, data.columns));
        float totalGap = (cols + 1) * (6f * density);
        float tileW = (dockRect.width() - totalGap) / cols;
        float tileH = dockH * 0.72f;
        float tileY = dockY + (dockH - tileH) / 2f;

        for (int i = 0; i < cols; i++) {
            float tx = dockRect.left + (6f * density) + (i * (tileW + 6f * density));
            RectF tileRect = new RectF(tx, tileY, tx + tileW, tileY + tileH);

            // Focused tile is index 1
            boolean focused = (i == 1);
            if (focused) {
                // Glow beam underneath focused tile
                paint.setColor(data.glowColor != 0 ? data.glowColor : 0x884DAE6D);
                canvas.drawCircle(tx + tileW / 2f, tileY + tileH, tileW * 0.7f, paint);

                // Focused tile scale
                tileRect.inset(-1.5f * density, -1.5f * density);
                paint.setColor(APP_COLORS[i % APP_COLORS.length]);
                canvas.drawRoundRect(tileRect, 6f * density, 6f * density, paint);

                // Luminous focus outline
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2f * density);
                paint.setColor(0xFFFFFFFF);
                canvas.drawRoundRect(tileRect, 6f * density, 6f * density, paint);
                paint.setStyle(Paint.Style.FILL);
            } else {
                paint.setColor(APP_COLORS[i % APP_COLORS.length]);
                canvas.drawRoundRect(tileRect, 5f * density, 5f * density, paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(0.8f * density);
                paint.setColor(0x33FFFFFF);
                canvas.drawRoundRect(tileRect, 5f * density, 5f * density, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        // 7. Outer TV Bezel Stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(0x40FFFFFF);
        canvas.drawRoundRect(bounds, radius, radius, paint);

        canvas.restore();
    }
}
