package com.example.android.smart_prayer_system;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Prayer.class}, version = 2)
public abstract class SPS_database extends RoomDatabase {

    public abstract myDAO myDAO();

}
