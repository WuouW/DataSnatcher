package com.example.DataSnatcher.collector;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiInfoCollector implements IInfoCollector {
    private final Context context;

    public WifiInfoCollector(Context context) {
        this.context = context;
    }

    @Override
    public String getCategory() {
        return "Wi-Fi 信息";
    }

    @Override
    public JSONObject collect() {
        JSONObject wifiInfo = new JSONObject();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            try {
                wifiInfo.put("SSID", info.getSSID());
                wifiInfo.put("BSSID", info.getBSSID());
                wifiInfo.put("信号强度", info.getRssi());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return wifiInfo;
    }
}