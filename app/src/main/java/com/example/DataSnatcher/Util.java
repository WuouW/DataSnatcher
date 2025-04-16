package com.example.DataSnatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class Util {
    public static JSONObject merge(JSONObject o1, JSONObject o2) throws JSONException {
        if(o2 == null)
            return o1;
        Iterator<String> keys = o2.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            o1.put(key, o2.get(key));
        }
        return o1;
    }

}
