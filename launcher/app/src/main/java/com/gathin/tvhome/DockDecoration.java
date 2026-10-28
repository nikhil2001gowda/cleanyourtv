package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the tvOS style lighter band behind the first (dock) row and adds a gap below it. */
public class DockDecoration extends RecyclerView.ItemDecoration {

    private final int dockSize;
    private final float density;
    private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    /** Extra band below the tiles for their names (always_show_names), in px. */
    private final float namesRoom;
    /** Light glass like the top bar's pills (config.xml dock_glass): a white tint that fades downwards. */
    private final boolean lightGlass;
    /** Padding all round the tiles equal to the gap between them (config.xml dock_even_padding). */
    private final boolean evenPadding;
    /** Draw the band and its edge (config.xml dock_band); false leaves the tiles floating on the wallpaper. */
    private final boolean bandVisible;
    /** single_row: the tiles after the dock share its row, so they take the same offsets to stay level with it. */
    private final boolean singleRow;
    /** dock_band_all: the band runs behind every app instead of the dock apps only. */
    private final boolean bandAll;
    private final boolean translucent;
    private final int opacity;

    /**
     * @param translucent the cinematic shelf's band: the artwork and the glow show through it.
     * @param names       the dock tiles show their names, so the band reaches down over them.
     * @param opacity     the plain band's opacity in percent (dock_opacity), so a wallpaper can show through it.
     * @param lightGlass  a light frosted band like the pills, for a sky or wallpaper behind it.
     * @param evenPadding the band's padding is the gap between the tiles on all four sides (dock_even_padding).
     * @param bandVisible false skips the band and its edge, so the tiles float on the wallpaper (dock_band).
     * @param singleRow   every app sits in the dock's row (single_row); the band covers the dock apps only, unless bandAll.
     * @param bandAll     the band runs behind every app instead of the dock apps only (dock_band_all).
     */
    DockDecoration(int dockSize, float density, boolean translucent, boolean names, int opacity, boolean lightGlass,
                   boolean evenPadding, boolean bandVisible, boolean singleRow, boolean bandAll) {
        this.dockSize = dockSize;
        this.singleRow = singleRow;
        this.bandAll = bandAll;
        this.translucent = translucent;
        this.density = density;
        this.namesRoom = names ? 22 * density : 0;
        this.lightGlass = lightGlass;
        this.evenPadding = evenPadding;
        this.bandVisible = bandVisible;
        this.opacity = opacity;
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 2.55f)));
        band.setColor((alpha << 24) | (translucent ? 0x121014 : 0x141416));
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(density);
        int edgeAlpha = Math.max(16, Math.min(255, Math.round(opacity * 0.7f)));
        edge.setColor((edgeAlpha << 24) | 0x00FFFFFF);
    }

    /** Concentric corners: the band's radius is the tiles' radius plus the band's padding around them. */
    static float radiusPx(float tileRadiusPx, float paddingPx) {
        return tileRadiusPx + paddingPx;
    }

    /**
     * The band's padding on every side for dock_even_padding: the gap between two dock tiles (a cell minus its tile, as
     * AppAdapter.tileWidthPx lays them out), capped by the room the grid leaves at its edges.
     */
    static float evenPaddingPx(int innerWidthPx, int columns, int tileWidthPx, float roomPx) {
        float gap = columns > 0 ? innerWidthPx / (float) columns - tileWidthPx : 0f;
        return Math.max(0f, Math.min(gap, roomPx));
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        int pos = parent.getChildAdapterPosition(view);
        if (pos >= 0 && (singleRow || pos < dockSize)) {
            outRect.top = (int) (6 * density);
            outRect.bottom = (int) ((bandAll && !singleRow ? 46 : 12) * density);
        } else if (bandAll) {
            outRect.bottom = (int) (10 * density);
        }
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (!bandVisible) return;
        RecyclerView.Adapter<?> a = parent.getAdapter();
        int totalApps = a instanceof AppAdapter ? ((AppAdapter) a).appCount() : dockSize;
        int underCount = totalApps - dockSize;
        int underRows = (underCount + dockSize - 1) / dockSize;

        boolean hasDock = false;
        boolean hasUnder = false;
        int cardWidth = 0, cardHeight = 0;
        float cardTop = 8 * density;
        int underRowH = 0;
        float anyUnderTop = 0;
        int anyUnderRow = 0;

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int pos = parent.getChildAdapterPosition(child);
            if (pos < 0) continue;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(child);
            View card = vh instanceof AppAdapter.Holder ? ((AppAdapter.Holder) vh).card : child.findViewById(R.id.card);
            if (card == null) continue;
            cardWidth = card.getWidth();
            cardHeight = card.getHeight();
            cardTop = card.getTop();
            float l = child.getLeft() + card.getLeft();
            float t = child.getTop() + card.getTop();
            float r = l + card.getWidth();
            float b = t + card.getHeight();

            if (pos < dockSize || singleRow) {
                if (!hasDock) {
                    rect.set(l, t, r, b);
                    hasDock = true;
                } else {
                    rect.union(l, t, r, b);
                }
            } else if (bandAll && pos < totalApps && !hasUnder) {
                underRowH = child.getHeight() + (int) (10 * density);
                anyUnderTop = child.getTop();
                anyUnderRow = (pos - dockSize) / dockSize;
                hasUnder = true;
            }
        }
        if (!hasDock && !hasUnder) return;

        int inner = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        int colW = dockSize > 0 ? inner / dockSize : inner;
        float col0Left = parent.getPaddingLeft() + (colW - cardWidth) / 2f;
        float colLastRight = col0Left + (dockSize - 1) * colW + cardWidth;
        float sideMargin = Math.min(col0Left, parent.getWidth() - colLastRight) - density;

        float room = singleRow ? (hasDock ? rect.top - density : 0) : sideMargin;
        float even = evenPadding ? evenPaddingPx(inner, dockSize, cardWidth, room) : 0f;
        float sidePad = 22 * density;
        float side = Math.max(0f, Math.min(sidePad, sideMargin));
        float pad = 14 * density;
        float radius = AppAdapter.cornerRadius(cardWidth, density);
        radius = evenPadding ? radiusPx(radius, even) : 26 * density;

        float opRatio = opacity / 100f;
        int topAlpha = Math.max(4, Math.round(0x38 * opRatio));
        int botAlpha = Math.max(2, Math.round(0x14 * opRatio));

        float dx = evenPadding ? even : side;
        float dy = evenPadding ? even : pad;

        if (hasDock) {
            rect.inset(singleRow && !evenPadding ? -sidePad : -dx, -dy);
            if (singleRow) {
                rect.left = Math.max(rect.left, density);
                rect.right = Math.min(rect.right, parent.getWidth() - density);
            }
            rect.bottom += namesRoom;
            drawBand(c, rect, radius, topAlpha, botAlpha);
        }

        if (hasUnder && underRows > 0 && underRowH > 0) {
            float firstTop = anyUnderTop - anyUnderRow * underRowH;
            rect.set(col0Left, firstTop + cardTop, colLastRight, firstTop + (underRows - 1) * underRowH + cardTop + cardHeight);
            rect.inset(-dx, -dy);
            rect.bottom += namesRoom;
            drawBand(c, rect, radius, topAlpha, botAlpha);
        }
    }

    private void drawBand(Canvas c, RectF r, float rad, int topAlpha, int botAlpha) {
        if (lightGlass) {
            band.setShader(new LinearGradient(0, r.top, 0, r.bottom,
                    (topAlpha << 24) | 0x00FFFFFF, (botAlpha << 24) | 0x00FFFFFF,
                    Shader.TileMode.CLAMP));
        }
        c.drawRoundRect(r, rad, rad, band);
        c.drawRoundRect(r, rad, rad, edge);
    }
}
