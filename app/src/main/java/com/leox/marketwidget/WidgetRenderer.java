package com.leox.marketwidget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.Locale;

/** 把整块 4×1 小部件画成一张 Bitmap，绕开 RemoteViews 的排版限制。 */
final class WidgetRenderer {
    private static final float DESIGN_W = 300f;
    private static final float DESIGN_H = 48f;
    private static final float PAD = 10f;
    private static final float INSET = 5f;
    private static final float LABEL_Y = 22f;
    private static final float PRICE_Y = 40f;

    private WidgetRenderer() {
    }

    static Bitmap render(Context ctx, Quotes q, int widthDp, int heightDp) {
        boolean dark = isDark(ctx);
        float density = ctx.getResources().getDisplayMetrics().density;
        // 按真实 dp 比例出图，避免拉伸变形；2 倍 density 做超采样
        int w = Math.max(300, Math.round(widthDp * density * 2f));
        int h = Math.max(48, Math.round(heightDp * density * 2f));
        float s = Math.min(w / DESIGN_W, h / DESIGN_H);

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        int bgColor = dark ? 0xFF17171B : 0xFFF7F6F2;
        int borderColor = dark ? 0x29FFFFFF : 0x14000000;
        int primary = dark ? 0xFFF4F3EF : 0xFF2C2C2A;
        int secondary = dark ? 0xFF9A9AA4 : 0xFF6B6A63;
        int hint = dark ? 0xFF8C8C96 : 0xFF7A7972;
        int divider = dark ? 0x1AFFFFFF : 0x17000000;
        int up = dark ? 0xFFF09595 : 0xFFA32D2D;
        int down = dark ? 0xFF97C459 : 0xFF3B6D11;

        float radius = 11f * s;
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(bgColor);
        fill.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(0, 0, w, h), radius, radius, fill);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setColor(borderColor);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, 0.7f * s));
        float half = stroke.getStrokeWidth() / 2f;
        c.drawRoundRect(new RectF(half, half, w - half, h - half),
                radius - half, radius - half, stroke);

        c.save();
        c.translate((w - DESIGN_W * s) / 2f, (h - DESIGN_H * s) / 2f);
        c.scale(s, s);

        Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
        Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        if (medium == null) {
            medium = regular;
        }

        float colW = (DESIGN_W - 2f * PAD) / 3f;

        Paint vline = new Paint(Paint.ANTI_ALIAS_FLAG);
        vline.setColor(divider);
        vline.setStrokeWidth(0.5f);
        c.drawLine(PAD + colW, 11f, PAD + colW, 41f, vline);
        c.drawLine(PAD + 2f * colW, 11f, PAD + 2f * colW, 41f, vline);

        for (int i = 0; i < 3; i++) {
            float left = PAD + i * colW + INSET;
            float right = PAD + (i + 1) * colW - INSET;
            if (i == 0) {
                drawColumn(c, left, right, "黄金", "¥", q.hasGold,
                        q.gold, q.goldChange, regular, medium,
                        primary, secondary, up, down);
            } else if (i == 1) {
                drawColumn(c, left, right, "BTC", "", q.hasBtc,
                        q.btc, q.btcChange, regular, medium,
                        primary, secondary, up, down);
            } else {
                drawColumn(c, left, right, "ETH", "", q.hasEth,
                        q.eth, q.ethChange, regular, medium,
                        primary, secondary, up, down);
            }
        }

        // 平时不占任何空间；只有刷新失败、正在用旧值时才出现
        if (q.stale) {
            Paint badge = textPaint(hint, 9f, regular);
            badge.setTextAlign(Paint.Align.RIGHT);
            c.drawText("缓存", DESIGN_W - PAD, 9f, badge);
        }

        c.restore();
        return bmp;
    }

    private static void drawColumn(Canvas c, float left, float right, String name,
                                   String prefix, boolean has, double price, double change,
                                   Typeface regular, Typeface medium,
                                   int primary, int secondary, int up, int down) {
        c.drawText(name, left, LABEL_Y, textPaint(secondary, 10f, regular));
        drawChange(c, right, LABEL_Y, has, change, 10f, medium,
                up, down, Paint.Align.RIGHT);
        c.drawText(has ? prefix + money(price) : "—", left, PRICE_Y,
                textPaint(primary, 14f, medium));
    }

    private static void drawChange(Canvas c, float x, float y, boolean has, double change,
                                   float size, Typeface tf, int up, int down,
                                   Paint.Align align) {
        Paint p = textPaint(change >= 0 ? up : down, size, tf);
        p.setTextAlign(align);
        c.drawText(has ? percent(change) : "—", x, y, p);
    }

    private static Paint textPaint(int color, float size, Typeface tf) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTextSize(size);
        p.setTypeface(tf);
        return p;
    }

    private static String money(double v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String percent(double v) {
        return (v >= 0 ? "+" : "\u2212") + String.format(Locale.US, "%.2f", Math.abs(v)) + "%";
    }

    static boolean isDark(Context ctx) {
        int mask = ctx.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }
}
