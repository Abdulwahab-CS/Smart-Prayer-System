package com.example.android.smart_prayer_system;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface myDAO {

    @Insert
    public void add_prayer(Prayer prayer);

    @Query("select * from prayers_table")
    public List<Prayer> get_all_prayers();

    @Query("delete from prayers_table where 1")
    public void delete_all_prayers();

    @Delete
    public void delete_prayer(Prayer prayer);

    @Update
    public void update_prayer(Prayer prayer);

    @Query("select prayer_done_time from prayers_table where prayer_number LIKE :prayerNum")
    public String get_prayer_done_time(int prayerNum);

}
