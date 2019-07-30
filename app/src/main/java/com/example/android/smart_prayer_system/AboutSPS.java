package com.example.android.smart_prayer_system;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class AboutSPS extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_sps);

        Toolbar toolbar = findViewById(R.id.about_sps_tool_bar);
        setSupportActionBar(toolbar);

        // to translate the action bar title when the language has changed
        setTitle(R.string.about_sps_activity);


    }


    // We need it to change the language of the app
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }


}
