package com.example.DataSnatcher;

import android.app.Activity;
import android.content.Context;

import com.example.DataSnatcher.collector.AudioInfo.AudioInfoCollector;
import com.example.DataSnatcher.collector.DeviceIdentifierInfoCollection;
import com.example.DataSnatcher.collector.IInfoCollector;
import com.example.DataSnatcher.collector.SensorInfo.SensorInfoCollector;

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
    }

    public interface CollectAllCallback {
        void onFinished(Map<String, JSONObject> allInfo);
    }

    public void collectAllAsync(CollectAllCallback callback) {
        new Thread(() -> {
            for (IInfoCollector collector : collectors) {
                allInfo.put(collector.getCategory(), collector.collect());
            }
            callback.onFinished(allInfo);
        }).start();
    }

    public Map<String, JSONObject> getAllInfo(){
        return allInfo;
    }
}
