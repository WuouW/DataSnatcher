package com.example.DataSnatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    Map<String, JSONObject> allInfo = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermission();

        InfoCollectionManager icm = new InfoCollectionManager(this, this);

        // 介绍
        TextView textViewIntro = findViewById(R.id.introduction);
        textViewIntro.setText(
                "信息窃取者"
        );

        // 信息获取与展示
        Button buttonGetInfo = findViewById(R.id.buttonGetInfo);
        TextView textViewGetInfo = findViewById(R.id.textViewGetInfo);
        /*buttonGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                icm.collectAllAsync((res) -> {
                    runOnUiThread(() -> {
                        allInfo = icm.getAllInfo();
                    });
                });

            }
        });*/
        buttonGetInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                icm.collectAllAsync((res) -> {
                    runOnUiThread(() -> {
                        allInfo = icm.getAllInfo();

                        SpannableStringBuilder builder = new SpannableStringBuilder();

                        for (Map.Entry<String, JSONObject> entry : allInfo.entrySet()) {
                            String category = entry.getKey();
                            JSONObject details = entry.getValue();

                            // 加粗 + 大字显示分类名
                            SpannableString categorySpan = new SpannableString(category + "\n");
                            categorySpan.setSpan(new StyleSpan(Typeface.BOLD), 0, categorySpan.length(), 0);
                            categorySpan.setSpan(new RelativeSizeSpan(1.2f), 0, categorySpan.length(), 0);
                            builder.append(categorySpan);

                            Iterator<String> keys = details.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                String value = details.optString(key);
                                builder.append("  ").append(key).append(": ").append(value).append("\n");
                            }

                            builder.append("\n"); // 类之间空一行
                        }

                        textViewGetInfo.setText(builder);
                        textViewGetInfo.setVisibility(View.VISIBLE);
                    });
                });
            }
        });
    }

    private void requestPermission(){
        List<String> permissionsToRequest = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_SMS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
        }
    }
}