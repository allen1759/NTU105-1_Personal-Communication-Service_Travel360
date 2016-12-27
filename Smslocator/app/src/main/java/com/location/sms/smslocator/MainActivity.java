package com.location.sms.smslocator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcnlab.lib.smscommunicate.CommandHandler;
import org.mcnlab.lib.smscommunicate.Recorder;
import org.mcnlab.lib.smscommunicate.UserDefined;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // ----------------------------------------------------------------         Default Activity Functions           ----------------------------------------------------------------------

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
        CommandHandler.getSharedCommandHandler().addExecutor("EMERGENCY", new ExecutorEmergency());

        createMainView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // When pressing "back" button
            if(customerMapOn)
            {
                destroyMap(R.id.targetmap);
                customerMapOn = false;
            }
            createMainView();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // ----------------------------------------------------------------         View Related Functions           ----------------------------------------------------------------------
    public static ArrayList<CustomerInfo> customer_data = new ArrayList<CustomerInfo>();
    public static ArrayList<CustomerLocationInfo> marker_data= new ArrayList<CustomerLocationInfo>();

    void createMainView() {
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

    void createGuiderView() {
        setContentView(R.layout.guider_view);

        Button broadcast_btn = (Button) findViewById(R.id.broadcast_btn);
        Button getloc_btn = (Button) findViewById(R.id.getloc_btn);
        ListView customer_listview = (ListView) findViewById(R.id.customer_list);

        broadcast_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater li = LayoutInflater.from(MainActivity.this);
                View promptsView = li.inflate(R.layout.selectpostition_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setTitle("選擇地點");
                alertDialogBuilder.setView(promptsView);

                setUpSelectPosMap();

                alertDialogBuilder
                        .setPositiveButton("確定",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if(selected_latitude != 0.0 && selected_longitude != 0.0) {
                                        JSONObject position_info = new JSONObject();
                                        try {
                                            position_info.put("lat", selected_latitude).put("lon", selected_longitude);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                                        for (int i = 1; i < customer_data.size(); i++)
                                            if (!customer_data.get(i).name.equals("GUIDER") && customer_data.get(i).selected)
                                                hdlr.execute("SENDPOSITION", Integer.parseInt(customer_data.get(i).device_id), 0, position_info);
                                    }
                                    else
                                    {
                                        Toast.makeText(MainActivity.this, "尚未選擇位置", Toast.LENGTH_LONG).show();
                                    }
                                }
                            })
                        .setNegativeButton("取消",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    destroyMap(R.id.selectposmap);
                                }
                            });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        });

        getloc_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                for (int i = 1; i < customer_data.size(); i++)
                    if (!customer_data.get(i).name.equals("GUIDER") && customer_data.get(i).selected)
                        hdlr.execute("WHERE", Integer.parseInt(customer_data.get(i).device_id), 0, null);

                createMapView(0);
            }
        });

        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] devices = rec.getAllDevices(db);

        customer_data.clear();
        CustomerInfo customer;
        customer = new CustomerInfo(String.valueOf(-1), "成員名稱", "聯絡電話");
        customer_data.add(customer);
        for (int i = 0; i < devices.length; i++) {
            customer = new CustomerInfo((String) String.valueOf(devices[i][0]), (String) devices[i][1], (String) devices[i][2]);
            if (!customer.name.equals("GUIDER"))
                customer_data.add(customer);
        }
        CustomerAdapter customerAdapter = new CustomerAdapter(MainActivity.this, R.layout.customer_list_row, customer_data);
        customer_listview.setAdapter(customerAdapter);
        customerAdapter.notifyDataSetChanged();
    }

    void createCustomerView() {
        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] devices = rec.getAllDevices(db);
        int i;
        for (i = 0; i < devices.length; i++)
            if (devices[i][1].equals("GUIDER"))
                break;
        if (i < devices.length) {
            setContentView(R.layout.customer_view);

            Button getguiderloc_btn = (Button) findViewById(R.id.getguiderloc_btn);
            Button emergency_btn = (Button) findViewById(R.id.emergency_btn);

            getguiderloc_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                    Recorder rec = Recorder.getSharedRecorder();
                    SQLiteDatabase db = rec.getWritableDatabase();
                    Object[][] devices = rec.getAllDevices(db);
                    int i;
                    for (i = 0; i < devices.length; i++)
                        if (devices[i][1].equals("GUIDER"))
                            break;

                    hdlr.execute("WHERE", (int)(devices[i][0]), 0, null);

                    createMapView(1);
                }
            });

            emergency_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String problem_list[] = {"", ""};
                    LayoutInflater li = LayoutInflater.from(MainActivity.this);
                    View promptsView = li.inflate(R.layout.emergency_dialog, null);
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    final CheckedTextView problem1 = (CheckedTextView) promptsView.findViewById(R.id.problem1);
                    final CheckedTextView problem2 = (CheckedTextView) promptsView.findViewById(R.id.problem2);
                    problem1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            problem1.setChecked(!problem1.isChecked());
                            problem_list[0] = problem1.isChecked() ? problem1.getText().toString() : "";
                        }
                    });
                    problem2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            problem2.setChecked(!problem2.isChecked());
                            problem_list[1] = problem2.isChecked() ? problem2.getText().toString() : "";
                        }
                    });
                    alertDialogBuilder.setView(promptsView);

                    alertDialogBuilder
                        .setPositiveButton("確定",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Recorder rec = Recorder.getSharedRecorder();
                                    CommandHandler hdlr = CommandHandler.getSharedCommandHandler();
                                    SQLiteDatabase db = rec.getWritableDatabase();
                                    Object[][] devices = rec.getAllDevices(db);
                                    int i;
                                    for (i = 0; i < devices.length; i++)
                                        if (devices[i][1].equals("GUIDER"))
                                            break;

                                    JSONObject emergency_info = new JSONObject();
                                    try {
                                        String problems = "";
                                        for(int j = 0; j < problem_list.length; j++)
                                            problems += (problem_list[j] + ", ");
                                        emergency_info.put("problem", problems);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    hdlr.execute("EMERGENCY", (int)(devices[i][0]), 0, emergency_info);
                                }
                            })
                        .setNegativeButton("取消",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });

            setUpTargetMap();
        } else {
            setContentView(R.layout.customer_register_view);

            Button regisrer_btn = (Button) findViewById(R.id.register_btn);

            regisrer_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String phonenumber = ((EditText) findViewById(R.id.phonenumber)).getText().toString();
                    String name = ((EditText) findViewById(R.id.name)).getText().toString();

                    if (name.equals("GUIDER")) {
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

    void createMapView(final int pre_state) {
        setContentView(R.layout.map_view);

        Button return_btn = (Button) findViewById(R.id.return_btn);
        return_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destroyMap(R.id.customersmap);
                switch (pre_state) {
                    case 0:
                        createGuiderView();
                        break;
                    case 1:
                        createCustomerView();
                        break;
                }
            }
        });
    }

    // ---------------------------------------------------------------         ListView Related Classes           ---------------------------------------------------------------------

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
            CustomerHolder holder;

            if (row == null) {
                LayoutInflater inflater = LayoutInflater.from(this.activity);

                row = inflater.inflate(layoutResourceId, parent, false);
                holder = new CustomerHolder();
                holder.name = (TextView) row.findViewById(R.id.name);
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

                    if (position != 0) {
                        customer = customer_data.get(position);
                        customer.selected = isChecked;
                        customer_data.set(position, customer);
                    } else {
                        for (int i = 0; i < customer_data.size(); i++) {
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

    // --------------------------------------------------------------         Permission Related Functions           --------------------------------------------------------------------

    void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    // ----------------------------------------------------------------         GPS Location Functions           ----------------------------------------------------------------------
    public static double mylatitude = 0.0, mylongitude = 0.0;

    void setUpLocationReceiver() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mylatitude = location.getLatitude();
                    mylongitude = location.getLongitude();
                    checkDistanceFromTarget(mylatitude, mylongitude, "你");
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

    void checkDistanceFromTarget(double latitude, double longitude, String name)
    {
        Recorder rec = Recorder.getSharedRecorder();
        SQLiteDatabase db = rec.getWritableDatabase();
        Object[][] commands = rec.getAllCommands(db);
        String content = null;
        int i;
        for (i = 0; i < commands.length; i++)
            if (commands[i][5].equals("SENDPOSITION"))
                content = (String) (commands[i][6]);

        if (content != null && latitude != 0.0 && longitude != 0.0) {
            try {
                JSONObject json = new JSONObject(content);
                float [] dist = new float[1];
                Location.distanceBetween(latitude, longitude, json.getDouble("lat"), json.getDouble("lon"), dist);
                if(dist[0] > 10000) //10000 meters
                {
                    //Notification
                    NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); // 取得系統的通知服務
                    Notification notification = null; // 建立通知
                    notification = new Notification.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.icon).setContentTitle("位置太遠警告")
                            .setContentText(name + "距離目的地太遠").build();
                    long[] tVibrate = {0, 100, 200, 300};
                    notification.vibrate = tVibrate;
                    notificationManager.notify(1, notification); // 發送通知
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // ----------------------------------------------------------------         Map Related Functions           ----------------------------------------------------------------------
    boolean customerMapOn = false;
    double selected_latitude = 0.0, selected_longitude = 0.0;

    public void setUpSelectPosMap () {
        ((MapFragment) getFragmentManager().findFragmentById(R.id.selectposmap)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap mMap) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                }

                mMap.setMyLocationEnabled(true);
                if (mylatitude != 0.0 && mylongitude != 0.0) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mylatitude, mylongitude), 12));
                }

                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        mMap.addMarker(new MarkerOptions().position(new LatLng(latLng.latitude, latLng.longitude))
                                .title("集合地點")).showInfoWindow();
                        selected_latitude = latLng.latitude;
                        selected_longitude = latLng.longitude;
                    }
                });
            }
        });
    }

    public void setUpTargetMap () {
        customerMapOn = true;
        ((MapFragment) getFragmentManager().findFragmentById(R.id.targetmap)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                Recorder rec = Recorder.getSharedRecorder();
                SQLiteDatabase db = rec.getWritableDatabase();
                Object[][] devices = rec.getAllDevices(db);
                int i;
                for (i = 0; i < devices.length; i++)
                    if (devices[i][1].equals("GUIDER"))
                        break;
                Object[][] commands = rec.getCommandsByDeviceId(db, i);
                String content = null;
                for (i = 0; i < commands.length; i++)
                    if (commands[i][5].equals("SENDPOSITION"))
                        content = (String) (commands[i][6]);

                if (content != null) {
                    try {
                        JSONObject json = new JSONObject(content);

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                        }

                        mMap.setMyLocationEnabled(true);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(json.getDouble("lat"), json.getDouble("lon")), 12));
                        mMap.addMarker(new MarkerOptions().position(new LatLng(json.getDouble("lat"), json.getDouble("lon")))
                                .title("集合地點")).showInfoWindow();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (mylatitude != 0.0 && mylongitude != 0.0) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                    }

                    mMap.setMyLocationEnabled(true);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mylatitude, mylongitude), 12));
                }
            }
        });
    }

    public void setUpCustomersMap() {
        ((MapFragment) getFragmentManager().findFragmentById(R.id.customersmap)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                }
                mMap.setMyLocationEnabled(true);
                if (mylatitude != 0.0 && mylongitude != 0.0)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mylatitude, mylongitude), 12));

                CustomerLocationInfo marker;
                for (int i = 0; i < marker_data.size(); i++) {
                    marker = marker_data.get(i);
                    checkDistanceFromTarget(Double.parseDouble(marker.latitude), Double.parseDouble(marker.longitude), marker.name);

                    mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(marker.latitude), Double.parseDouble(marker.longitude)))
                            .title(marker.name)
                            .snippet(marker.phonenumber)).showInfoWindow();
                }
            }
        });
    }

    public void destroyMap(int id) {
        Fragment fragment = (getFragmentManager().findFragmentById(id));
        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.remove(fragment);
        ft.commit();
    }
}