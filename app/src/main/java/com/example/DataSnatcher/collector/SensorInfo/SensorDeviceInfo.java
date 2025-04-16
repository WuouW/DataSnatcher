package com.example.DataSnatcher.collector.SensorInfo;

import android.content.Context;
import android.util.Log;

import com.example.DataSnatcher.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


public class SensorDeviceInfo {
    private Context context;
    private static final String TAG = "gyroscope sensor";
    private CountDownLatch mLatch;

    public void setContext(Context context) {
        this.context = context;
    }

    public void setLatch(CountDownLatch latch) {
        mLatch = latch;
    }

    public JSONObject getInfo() {
        // 每一个Map中存储三个数据（一次传感器输出的xyz轴数据），而List中存储多次获取的数据
        List<Map<String, Double>> gyroscopeInfo = new ArrayList<>();
        List<Map<String, Double>> magnetometerInfo = new ArrayList<>();
        // 特征
        JSONObject gyroscopeFeature = new JSONObject();
        JSONObject magnetometerFeature = new JSONObject();


        // 收集陀螺仪数据
        Gyroscope gyroscope = new Gyroscope(context);
        gyroscope.startCollecting(new Gyroscope.GyroscopeDataCallback() {
            @Override
            public void onGyroscopeDataChanged(float x, float y, float z) {
                Map<String, Double> oneGyroscopeInfo = new HashMap<>();
                oneGyroscopeInfo.put("x", (double) x);
                oneGyroscopeInfo.put("y", (double) y);
                oneGyroscopeInfo.put("z", (double) z);
                gyroscopeInfo.add(oneGyroscopeInfo);
                Log.d(TAG, gyroscopeInfo.toString());
            }

            @Override
            public void onDataCollectionFinished() {
                gyroscope.stopCollecting();
                mLatch.countDown();
            }
        });


        // 收集磁力计数据
        Magnetometer magnetometer = new Magnetometer(context);
        magnetometer.startCollecting(new Magnetometer.MagnetometerDataCallback() {

            @Override
            public void onMagnetometerDataChanged(float x, float y, float z) {
                Map<String, Double> oneMagnetometerInfo = new HashMap<>();
                oneMagnetometerInfo.put("x", (double) x);
                oneMagnetometerInfo.put("y", (double) y);
                oneMagnetometerInfo.put("z", (double) z);
                magnetometerInfo.add(oneMagnetometerInfo);
                Log.d("magnetometer", magnetometerInfo.toString());
            }

            @Override
            public void onDataCollectionFinished() {
                magnetometer.stopCollecting();
                mLatch.countDown();
            }
        });


        // 处理陀螺仪数据并生成特征
        try {
            gyroscopeFeature = extractFeatures(gyroscopeInfo, "gyroscope");

        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        //处理磁力计数据并生成特征
        try {
            magnetometerFeature = extractFeatures(magnetometerInfo, "magnetometer");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject sensorInfo = new JSONObject();
        try {
            sensorInfo = genSensorInfo(gyroscopeInfo, magnetometerInfo, gyroscopeFeature, magnetometerFeature);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG + " return", sensorInfo.toString());
        return sensorInfo;
    }

    private JSONObject extractFeatures(List<Map<String, Double>> info, String name) throws JSONException {
        if (info == null) {
            return null;
        }
        JSONObject features = new JSONObject();

        // mean
        double meanX = Features.mean(info, "x");
        double meanY = Features.mean(info, "y");
        double meanZ = Features.mean(info, "z");
        List<Double> mean = new ArrayList<>();
        mean.add(meanX);
        mean.add(meanY);
        mean.add(meanZ);
        features.put(name + "Mean", mean);

        // stddev
        double stddevX = Features.stddev(info, "x", meanX);
        double stddevY = Features.stddev(info, "y", meanY);
        double stddevZ = Features.stddev(info, "z", meanZ);
        List<Double> stddev = new ArrayList<>();
        stddev.add(stddevX);
        stddev.add(stddevY);
        stddev.add(stddevZ);
        features.put(name + "Stddev", stddev);

        // avgdev
        List<Double> avgdev = new ArrayList<>();
        avgdev.add(Features.avgdev(info, "x", meanX));
        avgdev.add(Features.avgdev(info, "y", meanY));
        avgdev.add(Features.avgdev(info, "z", meanZ));
        features.put(name + "Avgdev", avgdev);

        // skewness
        List<Double> skewness = new ArrayList<>();
        skewness.add(Features.skewness(info, "x", meanX, stddevX));
        skewness.add(Features.skewness(info, "y", meanY, stddevY));
        skewness.add(Features.skewness(info, "z", meanZ, stddevZ));
        features.put(name + "Skewness", skewness);

        // kurtosis
        List<Double> kurtosis = new ArrayList<>();
        kurtosis.add(Features.kurtosis(info, "x", meanX, stddevX));
        kurtosis.add(Features.kurtosis(info, "y", meanY, stddevY));
        kurtosis.add(Features.kurtosis(info, "z", meanZ, stddevZ));
        features.put(name + "Kurtosis", kurtosis);

        // rmsamplitude
        List<Double> rmsamplitude = new ArrayList<>();
        rmsamplitude.add(Features.rmsamplitude(info, "x"));
        rmsamplitude.add(Features.rmsamplitude(info, "y"));
        rmsamplitude.add(Features.rmsamplitude(info, "z"));
        features.put(name + "Rmsamplitude", rmsamplitude);

        // lowest
        List<Double> lowest = new ArrayList<>();
        lowest.add(Features.lowest(info, "x"));
        lowest.add(Features.lowest(info, "y"));
        lowest.add(Features.lowest(info, "z"));
        features.put(name + "Lowest", lowest);

        // highest
        List<Double> highest = new ArrayList<>();
        highest.add(Features.highest(info, "x"));
        highest.add(Features.highest(info, "y"));
        highest.add(Features.highest(info, "z"));
        features.put(name + "Highest", highest);

        return null;
    }

    private JSONObject genSensorInfo(
            List<Map<String, Double>> gyroscopeInfo,
            List<Map<String, Double>> magnetometerInfo,
            JSONObject gyroscopeFeature,
            JSONObject magnetometerFeature
    ) throws JSONException {
        // TODO
        JSONObject sensorInfo = new JSONObject();
        sensorInfo.put("gyroscopeData", gyroscopeInfo);
        sensorInfo.put("magnetometerData", magnetometerInfo);
        sensorInfo = Util.merge(sensorInfo, gyroscopeFeature);
        sensorInfo = Util.merge(sensorInfo, magnetometerFeature);
        return sensorInfo;
    }
}
