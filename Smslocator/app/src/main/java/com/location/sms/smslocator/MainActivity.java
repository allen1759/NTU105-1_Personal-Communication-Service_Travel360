package com.location.sms.smslocator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
    public static double mylatitude = 0.0, mylongitude = 0.0;

    //For Guider View
    public static ArrayList<CustomerInfo> customer_data = new ArrayList<CustomerInfo>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLocationPermission();
        setUpLocationReceiver();

        UserDefined.filter = "$FINDME$";
        Recorder.init(this, "MainActivity");
        CommandHandler.init(this);
        CommandHandler.getSharedCommandHandler().addExecutor("WHERE", new ExecutorWhere());
        CommandHandler.getSharedCommandHandler().addExecutor("REGISTER", new ExecutorRegister());
        CommandHandler.getSharedCommandHandler().addExecutor("SENDPOSITION", new ExecutorSendPosition());

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

    void setUpLocationReceiver() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mylatitude = location.getLatitude();
                    mylongitude = location.getLongitude();
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            }, getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
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

        Button broadcast_btn = (Button) findViewById(R.id.broadcast_btn);
        Button getloc_btn = (Button) findViewById(R.id.getloc_btn);
        ListView customer_listview = (ListView) findViewById(R.id.customer_list);

        broadcast_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject position_info = new JSONObject();
                try {
                    position_info.put("lat", mylatitude).put("lon", mylongitude);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                for(int i = 1; i < customer_data.size(); i++)
                    if(!customer_data.get(i).name.equals("GUIDER"))
                        hdlr.execute("SENDPOSITION", Integer.parseInt(customer_data.get(i).device_id), 0, position_info);
            }
        });

        getloc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] devices = rec.getAllDevices(db);

        customer_data.clear();
        CustomerInfo customer;
        customer = new CustomerInfo(String.valueOf(-1), "成員名稱", "聯絡電話");
        customer_data.add(customer);
        for(int i = 0; i < devices.length; i++) {
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
            holder.selected.setChecked(customer.selected);

            holder.selected.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = (int) buttonView.getTag();
                    CustomerInfo customer;

                    if(position != 0) {
                        customer = customer_data.get(position);
                        customer.selected = isChecked;
                        customer_data.set(position, customer);
                    }
                    else {
                        for(int i = 0; i < customer_data.size(); i++) {
                            customer = customer_data.get(i);
                            customer.selected = isChecked;
                            customer_data.set(i, customer);
                        }
                        ListView customer_listview = (ListView) findViewById(R.id.customer_list);
                        CustomerAdapter customerAdapter = new CustomerAdapter(MainActivity.this, R.layout.customer_list_row, customer_data);
                        customer_listview.setAdapter(customerAdapter);
                        customerAdapter.notifyDataSetChanged();
                    }
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