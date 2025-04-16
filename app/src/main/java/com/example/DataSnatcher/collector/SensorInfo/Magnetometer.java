package com.example.DataSnatcher.collector.SensorInfo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;

public class Magnetometer {
    private SensorManager sensorManager;
    private Sensor magnetometerSensor;
    private SensorEventListener magnetometerListener;
    private float[] magnetometerData = new float[3];
    private boolean isCollecting = false;

    public Magnetometer(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void startCollecting(Magnetometer.MagnetometerDataCallback callback) {
        if (isCollecting) {
            return;
        }

        magnetometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magnetometerData[0] = event.values[0];
                    magnetometerData[1] = event.values[1];
                    magnetometerData[2] = event.values[2];

                    callback.onMagnetometerDataChanged(magnetometerData[0], magnetometerData[1], magnetometerData[2]);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                return;
            }
        };

        sensorManager.registerListener(magnetometerListener, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME);
        isCollecting = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopCollecting();
            callback.onDataCollectionFinished();
        }, 5000);

//        new Thread(() -> {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            stopCollecting();
//            callback.onDataCollectionFinished();
//        }).start();
    }

    public void stopCollecting() {
        if (!isCollecting) {
            return;
        }
        sensorManager.unregisterListener(magnetometerListener);
        isCollecting = false;
    }

    public interface MagnetometerDataCallback {
        void onMagnetometerDataChanged(float x, float y, float z);

        void onDataCollectionFinished();
    }
}
