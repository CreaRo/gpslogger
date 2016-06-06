package com.crearo.gpslogger.senders.email;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.crearo.gpslogger.senders.FileSenderFactory;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

/**
 * Created by rish on 1/4/16.
 */
public class LastTimeSentChecker {

    public static final String TAG = LastTimeSentChecker.class.getSimpleName();

    public static void updateFileSentTime(Context context, String fileToSend) {

        Map<String, Long> timeHash = loadMap(context);

        int gmtOffset = TimeZone.getDefault().getRawOffset();
        long now = System.currentTimeMillis() + gmtOffset;

        Log.d(TAG, "updating file " + fileToSend + " to " + new Date(now).toGMTString());

        timeHash.put(fileToSend, now);
        saveMap(context, timeHash);
    }

    public static void checkAllFileSentTime(Context context) {
        Log.d(TAG, "Checking all files sent time hashmap");
        Map<String, Long> timeHash = loadMap(context);
        for (Map.Entry<String, Long> entry : timeHash.entrySet()) {
            Log.d(TAG, entry.getKey() + "/" + entry.getValue());
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                sdf.setTimeZone(TimeZone.getDefault());

                long fileMillis = sdf.parse(entry.getKey()).getTime();
                if (fileMillis >= entry.getValue()) {
                    /*Send file in if the fileMillis is more than when it was last sent*/
                    // Call method to send this file
                    Log.d(TAG, "Sending the " + entry.getKey());
                    FileSenderFactory.autoSendFiles(entry.getKey());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveMap(Context context, Map<String, Long> inputMap) {
        SharedPreferences pSharedPref = context.getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        if (pSharedPref != null) {
            JSONObject jsonObject = new JSONObject(inputMap);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = pSharedPref.edit();
            editor.remove("My_map").commit();
            editor.putString("My_map", jsonString);
            editor.commit();
        }
    }

    private static Map<String, Long> loadMap(Context context) {
        Map<String, Long> outputMap = new HashMap<String, Long>();
        SharedPreferences pSharedPref = context.getSharedPreferences("MyVariables", Context.MODE_PRIVATE);
        try {
            if (pSharedPref != null) {
                String jsonString = pSharedPref.getString("My_map", (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    Long value = (Long) jsonObject.get(key);
                    outputMap.put(key, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputMap;
    }
}
