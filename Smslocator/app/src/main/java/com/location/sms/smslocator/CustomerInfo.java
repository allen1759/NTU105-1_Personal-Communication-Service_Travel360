package com.location.sms.smslocator;

public class CustomerInfo {
    String device_id;
    String name;
    String phonenumber;
    boolean selected;

    CustomerInfo(String device_id, String name, String phonenumber)
    {
        this.device_id = device_id;
        this.name = name;
        this.phonenumber = phonenumber;
        this.selected = false;
    }
}
