package com.example.android.smart_prayer_system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Locale;

public class Settings extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar =  findViewById(R.id.settings_tool_bar);
        setSupportActionBar(toolbar);

        // to translate the action bar title when the language has changed
        setTitle(R.string.settings_activity);


        // Set the spinner of display languages
        final Spinner langOption = findViewById(R.id.spinner);

        ArrayAdapter mAdapter = new ArrayAdapter<String>(Settings.this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.language_options));
        langOption.setAdapter(mAdapter);

        // to set initially the device language as the selected one
        if(Locale.getDefault().getDisplayLanguage().equals("العربية")) {
            langOption.setSelection(0, true);
        }else {
            langOption.setSelection(1, true);
        }



        // add event listener to the spinner, when the user select a language
        langOption.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {


                if(Locale.getDefault().getDisplayLanguage().equals("العربية")){

                    if(i == 1) {
                        LocaleHelper.setLocale(getApplicationContext(), "en");
                        finish();

                        Intent x = new Intent(Settings.this, TempStartScreen.class);
                        startActivity(x);

                    }
                }

                if(Locale.getDefault().getDisplayLanguage().equals("English")){

                    if(i == 0){
                        LocaleHelper.setLocale(getApplicationContext(), "ar");
                        finish();

                        Intent x = new Intent(Settings.this, TempStartScreen.class);
                        startActivity(x);
                    }

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }


    // We need it to change the language of the app
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }


}
