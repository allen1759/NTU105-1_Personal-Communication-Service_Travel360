package com.location.sms.smslocator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Executor;
import org.mcnlab.lib.smscommunicate.Recorder;

public class ExecutorEmergency implements Executor {
    @Override
    public JSONObject execute(Context context, int device_id, int count, JSONObject usr_json) {
        Log.d("EXECUTOR EMERGENCY", "Count=" + count);

        switch(count) {
            case 0:
                return usr_json;
            case 1:
                Recorder rec = Recorder.getSharedRecorder();
                SQLiteDatabase db = rec.getWritableDatabase();
                Object o[] = rec.getDeviceById(db, device_id);

                //Notification
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE); // 取得系統的通知服務
                Notification notification = null; // 建立通知
                try {
                    String [] problems = usr_json.getString("problem").split(",");
                    String problem = "";
                    for(int i = 0; i < problems.length; i++)
                        if(problems[i].equals("location"))
                            problem += "找不到地點 ";
                        else
                            problem += "遇到危險 ";
                    notification = new Notification.Builder(context)
                        .setSmallIcon(R.drawable.icon).setContentTitle("緊急通知")
                        .setContentText((String)(o[1]) + " " + problem).build();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                long[] tVibrate = {0, 100, 200, 300};
                notification.vibrate = tVibrate;
                notificationManager.notify(1, notification); // 發送通知
                return null;
            default:
                return null;
        }
    }
}
