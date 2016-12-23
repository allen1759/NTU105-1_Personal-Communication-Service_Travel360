package com.location.sms.smslocator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Executor;
import org.mcnlab.lib.smscommunicate.Recorder;

public class ExecutorWhere implements Executor {
    @Override
    public JSONObject execute(Context context, int device_id, int count, JSONObject usr_json) {
        Log.d("EXECUTOR WHERE", "Count=" + count);

        switch(count) {
            case 0:
                return new JSONObject();
            case 1:
                JSONObject new_usr_json = new JSONObject();
                double latitude = MainActivity.mylatitude;
                double longitude = MainActivity.mylongitude;

                try {
                    new_usr_json.put("lat", latitude).put("lon", longitude);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                CommandHandler.getSharedCommandHandler().execute("WHERE", device_id, 2, new_usr_json);

                return null;
            case 2:
                return usr_json;
            case 3:
                try {
                    Recorder rec = Recorder.getSharedRecorder();
                    SQLiteDatabase db = rec.getWritableDatabase();
                    Object [] device_info = rec.getDeviceById(db, device_id);
                    MainActivity.marker_data.add(new CustomerLocationInfo((String) device_info[0], (String) device_info[1], (String) device_info[2],
                            String.valueOf(usr_json.getDouble("lat")), String.valueOf(usr_json.getDouble("lon"))));
                    ((MainActivity) context).setUpCustomersMap();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            default:
                return null;
        }
    }
}
