package com.location.sms.smslocator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.Executor;
import org.mcnlab.lib.smscommunicate.Recorder;

public class ExecutorSendPosition implements Executor {
    @Override
    public JSONObject execute(Context context, int device_id, int count, JSONObject usr_json) {
        Log.d("EXECUTOR SENDPOSITION", "Count=" + count);

        switch(count) {
            case 0:
                return usr_json;
            default:
                return null;
        }
    }
}
