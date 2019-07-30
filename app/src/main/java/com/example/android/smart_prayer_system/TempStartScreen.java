package com.example.android.smart_prayer_system;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class TempStartScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.temp_start_screen);


        Thread timerThread = new Thread(){
            public void run(){
                try{
                    sleep(1000);

                }catch(InterruptedException e){
                    e.printStackTrace();

                }finally{

                    Intent intent = new Intent(TempStartScreen.this,MainActivity.class);
                    startActivity(intent);
                }
            }
        };
        timerThread.start();


    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    // We need it to change the language of the app
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

}
