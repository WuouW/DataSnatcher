package com.example.DataSnatcher.collector.SMS;

import android.icu.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static String formatDate(long timestamp) {
        // 设置想要的时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
