package com.location.sms.smslocator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Executor;
import org.mcnlab.lib.smscommunicate.Recorder;

public class ExecutorRegister implements Executor {
    @Override
    public JSONObject execute(Context context, int device_id, int count, JSONObject usr_json) {
        Log.d("EXECUTOR REGISTER", "Count=" + count);

        switch(count) {
            case 0:
                return usr_json;
            case 1:
                Recorder rec = Recorder.getSharedRecorder();
                SQLiteDatabase db = rec.getWritableDatabase();
                try {
                    rec.changeDeviceNameById(db, device_id, usr_json.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return null;
            default:
                return null;
        }
    }
}
