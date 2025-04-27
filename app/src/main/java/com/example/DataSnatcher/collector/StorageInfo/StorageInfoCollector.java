package com.example.DataSnatcher.collector.StorageInfo;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.example.DataSnatcher.collector.IInfoCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class StorageInfoCollector implements IInfoCollector {

    private static final String TAG = "StorageInfoCollector";
    private final Context context;

    public StorageInfoCollector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        // 使用 Application Context 防止 Activity 泄露
        this.context = context.getApplicationContext();
    }

    @Override
    public String getCategory() {
        return "StorageInfo"; // 信息类别名称
    }

    @Override
    public JSONObject collect() {
        JSONObject storageInfoJson = new JSONObject();

        try {
            // 1. 获取 RAM 信息
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();

            if (activityManager != null) {
                activityManager.getMemoryInfo(memoryInfo);
                storageInfoJson.put("ram_total_size", memoryInfo.totalMem); // 运行内存总大小 (byte)
                storageInfoJson.put("ram_usable_size", memoryInfo.availMem); // 运行内存可用大小 (byte)
            } else {
                Log.w(TAG, "ActivityManager service not available.");
                // 可以选择放入 0 或 JSONObject.NULL
                storageInfoJson.put("ram_total_size", 0L);
                storageInfoJson.put("ram_usable_size", 0L);
            }

            // 2. 获取内部存储信息
            File internalStoragePath = Environment.getDataDirectory(); // 通常是 /data 分区
            if (internalStoragePath != null) {
                try {
                    StatFs statInternal = new StatFs(internalStoragePath.getPath());
                    long internalTotalBytes = statInternal.getTotalBytes();
                    long internalAvailableBytes = statInternal.getAvailableBytes(); // 可用空间

                    storageInfoJson.put("internal_storage_total", internalTotalBytes); // 内部存储总空间 (byte)
                    storageInfoJson.put("internal_storage_usable", internalAvailableBytes); // 内部存储可用空间 (byte)
                } catch (IllegalArgumentException e) {
                    // StatFs 构造函数可能因路径无效抛出此异常，虽然对于 getDataDirectory 不太可能
                    Log.e(TAG, "Error getting internal storage stats: Invalid path?", e);
                    storageInfoJson.put("internal_storage_total", 0L);
                    storageInfoJson.put("internal_storage_usable", 0L);
                }
            } else {
                Log.w(TAG, "Could not get internal storage path (Environment.getDataDirectory() was null).");
                storageInfoJson.put("internal_storage_total", 0L);
                storageInfoJson.put("internal_storage_usable", 0L);
            }


            // 3. 获取外部存储 (SD Card) 信息
            long sdTotalBytes = 0L;
            long sdUsedBytes = 0L;

            // 检查外部存储状态
            String externalStorageState = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState)) {
                File externalStoragePath = Environment.getExternalStorageDirectory(); // 获取主外部存储目录
                if (externalStoragePath != null) {
                    try {
                        StatFs statExternal = new StatFs(externalStoragePath.getPath());
                        long externalTotalBytes = statExternal.getTotalBytes();
                        long externalAvailableBytes = statExternal.getAvailableBytes(); // 注意：这是可用空间

                        sdTotalBytes = externalTotalBytes;
                        sdUsedBytes = externalTotalBytes - externalAvailableBytes; // 已用空间 = 总空间 - 可用空间

                    } catch (IllegalArgumentException e) {
                        // StatFs 构造函数可能因路径无效或存储未准备好抛出异常
                        Log.e(TAG, "Error getting external storage stats: Invalid path or storage not ready?", e);
                        // 保持 sdTotalBytes 和 sdUsedBytes 为 0
                    }
                } else {
                    Log.w(TAG, "Could not get external storage path (Environment.getExternalStorageDirectory() was null), though state is mounted.");
                    // 保持 sdTotalBytes 和 sdUsedBytes 为 0
                }
            } else {
                Log.w(TAG, "External storage (SD card) not mounted. State: " + externalStorageState);
                // 外部存储不可用，保持大小为 0
            }

            storageInfoJson.put("memory_card_size", sdTotalBytes); // sd卡总空间 (byte)
            storageInfoJson.put("memory_card_size_use", sdUsedBytes); // sd卡已用空间 (byte)

        } catch (JSONException e) {
            Log.e(TAG, "Error constructing storage info JSON", e);
            // 发生 JSON 异常，可能返回部分填充或空的 JSON
            // 可以考虑返回一个包含错误的JSON对象
            // return new JSONObject().put("error", "JSON exception during storage info collection");
        } catch (Exception e) {
            // 捕获其他潜在的运行时异常
            Log.e(TAG, "Unexpected error during storage info collection", e);
            try {
                // 尝试放入错误信息，如果可能
                storageInfoJson.put("error", "Unexpected error: " + e.getMessage());
            } catch (JSONException je) {
                Log.e(TAG, "Error putting unexpected error message into JSON", je);
            }
        }

        return storageInfoJson;
    }
}