package com.leox.marketwidget;

import org.json.JSONException;
import org.json.JSONObject;

/** 一次行情快照。价格是原始数值，涨跌是百分比。 */
final class Quotes {
    static final String PREF_NAME = "market_quotes";
    static final String KEY_JSON = "snapshot";

    double gold;
    double goldChange;
    double btc;
    double btcChange;
    double eth;
    double ethChange;
    long updatedAt;
    boolean stale;
    boolean hasGold;
    boolean hasBtc;
    boolean hasEth;

    Quotes copy() {
        Quotes q = new Quotes();
        q.gold = gold;
        q.goldChange = goldChange;
        q.btc = btc;
        q.btcChange = btcChange;
        q.eth = eth;
        q.ethChange = ethChange;
        q.updatedAt = updatedAt;
        q.stale = stale;
        q.hasGold = hasGold;
        q.hasBtc = hasBtc;
        q.hasEth = hasEth;
        return q;
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("gold", gold);
        o.put("goldChange", goldChange);
        o.put("btc", btc);
        o.put("btcChange", btcChange);
        o.put("eth", eth);
        o.put("ethChange", ethChange);
        o.put("updatedAt", updatedAt);
        o.put("hasGold", hasGold);
        o.put("hasBtc", hasBtc);
        o.put("hasEth", hasEth);
        return o;
    }

    static Quotes fromJson(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(s);
            Quotes q = new Quotes();
            q.gold = o.optDouble("gold", 0);
            q.goldChange = o.optDouble("goldChange", 0);
            q.btc = o.optDouble("btc", 0);
            q.btcChange = o.optDouble("btcChange", 0);
            q.eth = o.optDouble("eth", 0);
            q.ethChange = o.optDouble("ethChange", 0);
            q.updatedAt = o.optLong("updatedAt", 0);
            q.hasGold = o.optBoolean("hasGold", false);
            q.hasBtc = o.optBoolean("hasBtc", false);
            q.hasEth = o.optBoolean("hasEth", false);
            return q;
        } catch (JSONException e) {
            return null;
        }
    }
}
