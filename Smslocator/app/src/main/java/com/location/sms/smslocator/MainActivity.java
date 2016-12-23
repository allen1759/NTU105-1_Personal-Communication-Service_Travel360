package com.location.sms.smslocator;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Recorder;
import org.mcnlab.lib.smscommunicate.UserDefined;

import java.util.ArrayList;

public class MainActivity extends Activity {
    public final static String LOG_TAG = "MainActivity";

    //For Guider View
    public static ArrayList<CustomerInfo> customer_data = new ArrayList<CustomerInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLocationPermission();

        UserDefined.filter = "$FINDME$";
        Recorder.init(this, "MainActivity");
        CommandHandler.init(this);
        CommandHandler.getSharedCommandHandler().addExecutor("WHERE", new ExecutorWhere());
        CommandHandler.getSharedCommandHandler().addExecutor("REGISTER", new ExecutorRegister());

        createMainView();
    }

    void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    void getSMSPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 0);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            // When pressing "back" button
            createMainView();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    void createMainView()
    {
        setContentView(R.layout.activity_main);

        Button guider_btn = (Button) findViewById(R.id.guider_btn);
        Button customer_btn = (Button) findViewById(R.id.customer_btn);

        guider_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSMSPermission();
                createGuiderView();
            }
        });

        customer_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSMSPermission();
                createCustomerView();
            }
        });
    }

    void createGuiderView()
    {
        setContentView(R.layout.guider_view);

        ListView customer_listview = (ListView) findViewById(R.id.customer_list);

        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] devices = rec.getAllDevices(db);

        customer_data.clear();
        for(int i = 0; i < devices.length; i++) {
            CustomerInfo customer;
            customer = new CustomerInfo((String) String.valueOf(devices[i][0]), (String) devices[i][1], (String) devices[i][2]);
            if(!customer.name.equals("GUIDER"))
                customer_data.add(customer);
        }
        CustomerAdapter customerAdapter = new CustomerAdapter(MainActivity.this, R.layout.customer_list_row, customer_data);
        customer_listview.setAdapter(customerAdapter);
        customerAdapter.notifyDataSetChanged();
    }

    public class CustomerAdapter extends ArrayAdapter<CustomerInfo> {
        Activity activity;
        int layoutResourceId;
        CustomerInfo customer;
        ArrayList<CustomerInfo> data = new ArrayList<CustomerInfo>();

        public CustomerAdapter(Activity act, int layoutResourceId,
                              ArrayList<CustomerInfo> data) {
            super(act, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.activity = act;
            this.data = data;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, final ViewGroup parent) {
            View row = convertView;
            CustomerHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(this.activity);

                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new CustomerHolder();
                holder.name= (TextView) row.findViewById(R.id.name);
                holder.phonenumber = (TextView) row.findViewById(R.id.phonenumber);
                holder.selected = (CheckBox) row.findViewById(R.id.selected);

                row.setTag(holder);
            } else {
                holder = (CustomerHolder) row.getTag();
            }
            customer = data.get(position);
            holder.name.setText(customer.name);
            holder.phonenumber.setText(customer.phonenumber);
            holder.selected.setTag(position);
            holder.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = (int) buttonView.getTag();
                    CustomerInfo customer = customer_data.get(position);
                    customer.selected = isChecked;
                    customer_data.set(position, customer);
                }
            });

            return row;
        }

        class CustomerHolder {
            TextView name;
            TextView phonenumber;
            CheckBox selected;
        }
    }

    void createCustomerView()
    {
        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] devices = rec.getAllDevices(db);
        int i;
        for(i = 0; i < devices.length; i++)
            if(devices[i][1].equals("GUIDER"))
                break;
        if(i < devices.length)
        {
            setContentView(R.layout.customer_view);

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
        }
        else
        {
            setContentView(R.layout.customer_register_view);

            Button regisrer_btn = (Button) findViewById(R.id.register_btn);
            regisrer_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String phonenumber = ((EditText) findViewById(R.id.phonenumber)).getText().toString();
                    String name = ((EditText) findViewById(R.id.name)).getText().toString();

                    if(name.equals("GUIDER"))
                    {
                        Toast.makeText(MainActivity.this, "這個名字不合法", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Recorder rec = Recorder.getSharedRecorder();
                    CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                    SQLiteDatabase db = rec.getWritableDatabase();
                    int device_id = rec.getDeviceIdByPhonenumberOrCreate(db, phonenumber);
                    rec.changeDeviceNameById(db, device_id, "GUIDER");

                    JSONObject registration_info = new JSONObject();
                    try {
                        registration_info.put("name", name);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    hdlr.execute("REGISTER", device_id, 0, registration_info);
                    createCustomerView();
                }
            });
        }
    }
}