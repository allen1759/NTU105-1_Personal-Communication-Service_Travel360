package com.location.sms.smslocator;

public class CustomerLocationInfo {
    String device_id;
    String name;
    String phonenumber;
    String latitude;
    String longitude;

    CustomerLocationInfo(String device_id, String name, String phonenumber, String latitude, String longitude)
    {
        this.device_id = device_id;
        this.name = name;
        this.phonenumber = phonenumber;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
