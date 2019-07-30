package com.example.android.smart_prayer_system;

public class Device {

    private String name;
    private String address;

    // Constructor

    public Device(String name, String address){
        this.name = name;
        this.address = address;
    }

    //  Setters & Getters

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
