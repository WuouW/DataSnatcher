package com.example.DataSnatcher.collector;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import com.example.DataSnatcher.collector.IInfoCollector;
import org.json.JSONException;
import org.json.JSONObject;

public class BatteryInfoCollector implements IInfoCollector {
    private final Context context;

    public BatteryInfoCollector(Context context) {
        this.context = context;
    }

    @Override
    public String getCategory() {
        return "电池信息";
    }

    @Override
    public JSONObject collect() {
        JSONObject batteryInfo = new JSONObject();
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            try {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level / (float) scale;
                batteryInfo.put("当前电量百分比", batteryPct * 100);

                int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                batteryInfo.put("电池温度", temperature / 10.0);

                int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                batteryInfo.put("电池电压", voltage);

                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;
                batteryInfo.put("是否正在充电", isCharging);

                int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
                boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
                batteryInfo.put("是否通过 USB 充电", usbCharge);
                batteryInfo.put("是否通过 AC 充电", acCharge);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return batteryInfo;
    }
}