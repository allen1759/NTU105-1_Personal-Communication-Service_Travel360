package com.location.sms.smslocator;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Recorder;
import org.mcnlab.lib.smscommunicate.UserDefined;

public class MainActivity extends Activity {
    public final static String LOG_TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button findme = (Button) findViewById(R.id.findme);
        findme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phonenumber = ((EditText) findViewById(R.id.phonenumber)).getText().toString();
                Recorder rec = Recorder.getSharedRecorder();
                CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                SQLiteDatabase db = rec.getWritableDatabase();
                int device_id = rec.getDeviceIdByPhonenumberOrCreate(db, phonenumber);
                db.close();
                hdlr.execute("WHERE", device_id, 0, null);
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 0);
        }

        UserDefined.filter="$FINDME$";
        Recorder.init(this, "MainActivity");
        CommandHandler.init(this);
        CommandHandler.getSharedCommandHandler().addExecutor("WHERE", new ExecutorWhere());
    }
}
