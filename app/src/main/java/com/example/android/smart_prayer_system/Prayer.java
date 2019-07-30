package com.example.android.smart_prayer_system;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;


@Entity(tableName = "prayers_table")
public class Prayer {

    @PrimaryKey
    @ColumnInfo(name = "prayer_number")
    private int number;

    @ColumnInfo(name = "prayer_name")
    private String name;

    @ColumnInfo(name = "prayer_done")
    private boolean done;

    @ColumnInfo(name = "prayer_done_time")
    private String done_time;

    @Ignore
    public Prayer(){
        this.number = 0;
        this.name = "";
        this.done = false;
        this.done_time = "";
    }

    public Prayer(int number, String name, Boolean done, String done_time){
        this.number = number;
        this.name = name;
        this.done = done;
        this.done_time = done_time;
    }

    // Setters & Getters

    public void setNumber(int number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setDone_time(String done_time){ this.done_time = done_time; }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        return done;
    }

    public String getDone_time(){ return done_time; }
}
