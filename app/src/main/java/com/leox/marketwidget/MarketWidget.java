package com.leox.marketwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class MarketWidget extends AppWidgetProvider {
    private static final String TAG = "MarketWidget";
    static final String ACTION_REFRESH = "com.leox.marketwidget.REFRESH";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            final PendingResult pending = goAsync();
            final Context app = ctx.getApplicationContext();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        updateAll(app);
                    } catch (Throwable t) {
                        Log.e(TAG, "refresh failed", t);
                    } finally {
                        pending.finish();
                    }
                }
            }, "market-widget-refresh").start();
        }
    }

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        // 先用缓存画一版，避免首屏空白；真实数据在 onReceive 的线程里刷新
        Quotes cached = QuoteFetcher.cached(ctx);
        Quotes q = cached != null ? cached : new Quotes();
        for (int id : ids) {
            renderOne(ctx, mgr, id, q);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr,
                                          int id, Bundle options) {
        Quotes cached = QuoteFetcher.cached(ctx);
        renderOne(ctx, mgr, id, cached != null ? cached : new Quotes(), options);
    }

    private static void updateAll(Context ctx) {
        Quotes q = QuoteFetcher.refresh(ctx);
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, MarketWidget.class));
        for (int id : ids) {
            renderOne(ctx, mgr, id, q);
        }
    }

    private static void renderOne(Context ctx, AppWidgetManager mgr, int id, Quotes q) {
        renderOne(ctx, mgr, id, q, mgr.getAppWidgetOptions(id));
    }

    private static void renderOne(Context ctx, AppWidgetManager mgr, int id,
                                  Quotes q, Bundle options) {
        int widthDp = options != null
                ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) : 250;
        int heightDp = options != null
                ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) : 110;

        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_view);
        rv.setImageViewBitmap(R.id.widget_image,
                WidgetRenderer.render(ctx, q, widthDp, heightDp));

        Intent refresh = new Intent(ctx, MarketWidget.class).setAction(ACTION_REFRESH);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, id, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_image, pi);

        mgr.updateAppWidget(id, rv);
    }
}
