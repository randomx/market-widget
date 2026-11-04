package com.leox.marketwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 三个数据源并发抓取，任一失败就沿用该项上次的值并标记 stale。 */
final class QuoteFetcher {
    private static final String TAG = "MarketWidget";
    private static final String UA = "Mozilla/5.0 (Linux; Android) MarketWidget/1.0";
    private static final int TIMEOUT_MS = 6000;
    private static final long WAIT_SEC = 8L;
    private static final Pattern GOLD_PATTERN =
            Pattern.compile("var hq_str_gds_AU9999=\"([^\"]*)\"");

    private QuoteFetcher() {
    }

    static Quotes cached(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(Quotes.PREF_NAME, Context.MODE_PRIVATE);
        return Quotes.fromJson(p.getString(Quotes.KEY_JSON, ""));
    }

    static Quotes refresh(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(Quotes.PREF_NAME, Context.MODE_PRIVATE);
        Quotes cached = Quotes.fromJson(p.getString(Quotes.KEY_JSON, ""));
        Quotes q = cached != null ? cached.copy() : new Quotes();
        boolean anyFail = false;

        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<double[]> gold = pool.submit(new Callable<double[]>() {
                @Override
                public double[] call() throws Exception {
                    return fetchGold();
                }
            });
            Future<double[]> btc = pool.submit(new Callable<double[]>() {
                @Override
                public double[] call() throws Exception {
                    return fetchGate("BTC_USDT");
                }
            });
            Future<double[]> eth = pool.submit(new Callable<double[]>() {
                @Override
                public double[] call() throws Exception {
                    return fetchGate("ETH_USDT");
                }
            });

            try {
                double[] g = gold.get(WAIT_SEC, TimeUnit.SECONDS);
                q.gold = g[0];
                q.goldChange = g[1];
                q.hasGold = true;
            } catch (Exception e) {
                anyFail = true;
                Log.w(TAG, "gold fetch failed: " + e);
            }
            try {
                double[] b = btc.get(WAIT_SEC, TimeUnit.SECONDS);
                q.btc = b[0];
                q.btcChange = b[1];
                q.hasBtc = true;
            } catch (Exception e) {
                anyFail = true;
                Log.w(TAG, "btc fetch failed: " + e);
            }
            try {
                double[] v = eth.get(WAIT_SEC, TimeUnit.SECONDS);
                q.eth = v[0];
                q.ethChange = v[1];
                q.hasEth = true;
            } catch (Exception e) {
                anyFail = true;
                Log.w(TAG, "eth fetch failed: " + e);
            }
        } finally {
            pool.shutdownNow();
        }

        if (!anyFail) {
            q.updatedAt = System.currentTimeMillis();
            q.stale = false;
        } else {
            q.stale = true;
        }

        try {
            p.edit().putString(Quotes.KEY_JSON, q.toJson().toString()).apply();
        } catch (Exception ignored) {
        }
        return q;
    }

    /** 新浪贵金属行情：GBK 文本，字段 0=最新价、7=昨收。必须带 Referer。 */
    private static double[] fetchGold() throws Exception {
        String body = get("https://hq.sinajs.cn/list=gds_AU9999",
                "https://finance.sina.com.cn", "GBK");
        Matcher m = GOLD_PATTERN.matcher(body);
        if (!m.find()) {
            throw new IllegalStateException("gold payload missing");
        }
        String[] f = m.group(1).split(",", -1);
        if (f.length < 9) {
            throw new IllegalStateException("gold fields too short");
        }
        double last = parseDouble(f[0]);
        double prevClose = parseDouble(f[7]);
        if (last <= 0 || prevClose <= 0) {
            throw new IllegalStateException("gold values invalid");
        }
        return new double[]{last, (last - prevClose) / prevClose * 100.0};
    }

    /** Gate.io 现货 ticker：last + change_percentage(24h)。 */
    private static double[] fetchGate(String pair) throws Exception {
        String body = get("https://api.gateio.ws/api/v4/spot/tickers?currency_pair=" + pair,
                null, "UTF-8");
        JSONArray arr = new JSONArray(body);
        if (arr.length() == 0) {
            throw new IllegalStateException("empty ticker");
        }
        JSONObject o = arr.getJSONObject(0);
        double last = parseDouble(o.optString("last", ""));
        if (last <= 0) {
            throw new IllegalStateException("ticker price invalid");
        }
        double change;
        try {
            change = parseDouble(o.optString("change_percentage", ""));
        } catch (Exception ignored) {
            change = 0;
        }
        return new double[]{last, change};
    }

    private static double parseDouble(String s) throws NumberFormatException {
        return Double.parseDouble(s.trim());
    }

    private static String get(String url, String referer, String charset) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "*/*");
        if (referer != null) {
            conn.setRequestProperty("Referer", referer);
        }
        conn.setInstanceFollowRedirects(true);
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("HTTP " + code + " for " + url);
            }
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            in.close();
            return new String(out.toByteArray(), charset);
        } finally {
            conn.disconnect();
        }
    }
}
