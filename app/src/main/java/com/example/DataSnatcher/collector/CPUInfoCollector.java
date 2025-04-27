package com.example.DataSnatcher.collector;

import android.content.Context;
import com.example.DataSnatcher.collector.IInfoCollector;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CPUInfoCollector implements IInfoCollector {
    private final Context context;

    public CPUInfoCollector(Context context) {
        this.context = context;
    }

    @Override
    public String getCategory() {
        return "CPU 信息";
    }

    @Override
    public JSONObject collect() {
        JSONObject cpuInfo = new JSONObject();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
            StringBuilder cpuModel = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.contains("model name")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        cpuModel.append(parts[1].trim());
                    }
                    break;
                }
            }
            cpuInfo.put("CPU 型号", cpuModel.toString());

            int coreCount = Runtime.getRuntime().availableProcessors();
            cpuInfo.put("CPU 核心数", coreCount);

            BufferedReader maxFreqReader = new BufferedReader(new FileReader("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
            String maxFreqStr = maxFreqReader.readLine();
            long maxFreq = Long.parseLong(maxFreqStr);
            cpuInfo.put("CPU 最大频率（kHz）", maxFreq);

            try {
                BufferedReader tempReader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
                String tempStr = tempReader.readLine();
                long cpuTemp = Long.parseLong(tempStr);
                cpuInfo.put("CPU 温度", cpuTemp / 1000.0);
            } catch (IOException e) {
                cpuInfo.put("CPU 温度", "无法获取");
            }

            try {
                BufferedReader loadReader = new BufferedReader(new FileReader("/proc/loadavg"));
                String loadStr = loadReader.readLine();
                String[] loadParts = loadStr.split(" ");
                if (loadParts.length > 0) {
                    double load = Double.parseDouble(loadParts[0]);
                    cpuInfo.put("当前 CPU 负载", load);
                }
            } catch (IOException e) {
                cpuInfo.put("当前 CPU 负载", "无法获取");
            }

            br.close();
            maxFreqReader.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return cpuInfo;
    }
}