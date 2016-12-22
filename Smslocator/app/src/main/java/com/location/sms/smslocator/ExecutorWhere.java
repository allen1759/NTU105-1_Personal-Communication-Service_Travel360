package com.location.sms.smslocator;

import android.content.Context;
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

public class ExecutorWhere implements Executor {
    @Override
    public JSONObject execute(Context context, int device_id, int count, JSONObject usr_json) {
        Log.d("EXECUTOR", "Count=" + count);

        switch(count) {
            case 0:
                return (new JSONObject());
            case 1:
                final int device_id_closure = device_id;

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                try {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            JSONObject new_usr_json = new JSONObject();
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            try {
                                new_usr_json.put("lat", latitude).put("lon", longitude);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            CommandHandler.getSharedCommandHandler().execute("WHERE", device_id_closure, 2, new_usr_json);
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
                    }, null);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }

                return null;
            case 2:
                return usr_json;
            default:
                try {
                    Log.d("Location", "lat=" + usr_json.getDouble("lat") + ",lon=" + usr_json.getDouble("lon"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
        }
    }
}
