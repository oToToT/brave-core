/** Copyright (c) 2020 The Brave Authors. All rights reserved.
  * This Source Code Form is subject to the terms of the Mozilla Public
  * License, v. 2.0. If a copy of the MPL was not distributed with this file,
  * You can obtain one at http://mozilla.org/MPL/2.0/.
  */
package org.chromium.chrome.browser.widget.crypto.binance;

import androidx.annotation.VisibleForTesting;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class BinanceAccountBalance {
    private double mTotalUSD = 0;
    private double mTotalBTC = 0;

    public BinanceAccountBalance (String json_balance) throws JSONException {
        fromJson (json_balance);
    }

    private void fromJson(String json_balance) throws JSONException {
        JSONObject jsonroot = new JSONObject(json_balance);
        Iterator<String> keys = jsonroot.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            JSONArray data = jsonroot.getJSONArray(key);
            mTotalBTC = mTotalBTC + data.getDouble(1);
            mTotalUSD = mTotalUSD + data.getDouble(2);
        }
    }

    @VisibleForTesting
    @Override
    public String toString() {
        return "BinanceAccountBalance{" +
                "mTotalUSD=" + mTotalUSD +
                ", mTotalBTC=" + mTotalBTC +'}';
    }

    public double getTotalUSD() {
        return mTotalUSD;
    }

    public double getTotalBTC() {
        return mTotalBTC;
    }
}