package com.example.DataSnatcher;

import android.app.Activity;
import android.content.Context;

import com.example.DataSnatcher.collector.AudioInfo.AudioInfoCollector;
import com.example.DataSnatcher.collector.BatteryInfoCollector;
import com.example.DataSnatcher.collector.CPUInfoCollector;
import com.example.DataSnatcher.collector.DeviceIdentifierInfoCollection;
import com.example.DataSnatcher.collector.IInfoCollector;
import com.example.DataSnatcher.collector.SensorInfo.SensorInfoCollector;
import com.example.DataSnatcher.collector.WifiInfoCollector;
import com.example.DataSnatcher.collector.BatteryInfoCollector;
import com.example.DataSnatcher.collector.CPUInfoCollector;
import com.example.DataSnatcher.collector.SimCardInfo.SimCardInfoCollector;
import com.example.DataSnatcher.collector.StorageInfo.StorageInfoCollector;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoCollectionManager {
    private Map<String, JSONObject> allInfo = new HashMap<>();
    private List<IInfoCollector> collectors;
    private final Context context;
    private final Activity activity;

    public InfoCollectionManager(Context context, Activity activity){
        this.context = context;
        this.activity = activity;
        collectors = new ArrayList<>();

        // 在此处加
        collectors.add(new AudioInfoCollector());
        collectors.add(new SensorInfoCollector(context));
        collectors.add(new DeviceIdentifierInfoCollection(context, activity));
        collectors.add(new BatteryInfoCollector(context));
        collectors.add(new CPUInfoCollector(context));
        collectors.add(new WifiInfoCollector(context));
        collectors.add(new SimCardInfoCollector(context));
        collectors.add(new StorageInfoCollector(context));
    }

    public interface CollectAllCallback {
        void onFinished(Map<String, JSONObject> allInfo);
    }

    public void collectAllAsync(CollectAllCallback callback) {
        new Thread(() -> {
            for (IInfoCollector collector : collectors) {
                allInfo.put(collector.getCategory(), collector.collect());
            }

            for (Map.Entry<String, JSONObject> entry : allInfo.entrySet()) {
                String category = entry.getKey();
                JSONObject details = entry.getValue();
                System.out.println("Category: " + category);
                System.out.println("Details: " + details.toString());
            }

            callback.onFinished(allInfo);
        }).start();
    }

    public Map<String, JSONObject> getAllInfo(){
        return allInfo;
    }
}
