package com.example.DataSnatcher.collector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DeviceIdentifierInfoCollection implements IInfoCollector {
    private final Context context;
    private final Activity activity;

    public DeviceIdentifierInfoCollection(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    @Override
    public String getCategory() {
        return "设备标识符";
    }

    @Override
    public JSONObject collect() {
        JSONObject DI = new JSONObject();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    1
            );
            // 检查权限是否授予
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
                    PackageManager.PERMISSION_GRANTED) {
                Log.e("DeviceIdentifierInfo", "未授予 READ_PHONE_STATE 权限");
                return DI;
            }
        }

        TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Class<?> telephonyManagerClass = telephonyManager.getClass();

        try {
            // IMEI
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    Method getImeiMethod = telephonyManagerClass.getMethod("getImei");
                    String IMEI = (String) getImeiMethod.invoke(telephonyManager);
                    DI.put("IMEI", IMEI);
                } catch (Exception e) {
                    Log.e("DeviceIdentifierInfo", "获取 IMEI 时出错: " + e.getMessage());
                }
            }

            // MEID
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    Method getMeidMethod = telephonyManagerClass.getMethod("getMeid");
                    String MEID = (String) getMeidMethod.invoke(telephonyManager);
                    DI.put("MEID", MEID);
                } catch (Exception e) {
                    Log.e("DeviceIdentifierInfo", "获取 MEID 时出错: " + e.getMessage());
                }
            }

            // Serial Number
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    Method getSerialNumberMethod = telephonyManagerClass.getMethod("getSimSerialNumber");
                    String SerialNumber = (String) getSerialNumberMethod.invoke(telephonyManager);
                    DI.put("Serial Number", SerialNumber);
                } catch (Exception e) {
                    Log.e("DeviceIdentifierInfo", "获取 Serial Number 时出错: " + e.getMessage());
                }
            }

            // Subscriber Id
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    Method getSubscriberIdMethod = telephonyManagerClass.getMethod("getSubscriberId");
                    String SubscriberId = (String) getSubscriberIdMethod.invoke(telephonyManager);
                    DI.put("Subscriber Id", SubscriberId);
                } catch (Exception e) {
                    Log.e("DeviceIdentifierInfo", "获取 Subscriber Id 时出错: " + e.getMessage());
                }
            }

            // Device Id
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    Method getDeviceIdMethod = telephonyManagerClass.getMethod("getDeviceId");
                    String DeviceId = (String) getDeviceIdMethod.invoke(telephonyManager);
                    DI.put("Device Id", DeviceId);
                } catch (Exception e) {
                    Log.e("DeviceIdentifierInfo", "获取 Device Id 时出错: " + e.getMessage());
                }
            }

            // Android ID
            @SuppressLint("HardwareIds")
            String Android_ID = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
            DI.put("Android ID", Android_ID);
        } catch (JSONException e) {
            Log.e("DeviceIdentifierInfo", "JSON 处理出错: " + e.getMessage());
        }
        return DI;
    }
}