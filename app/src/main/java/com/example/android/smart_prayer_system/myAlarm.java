package com.example.android.smart_prayer_system;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class myAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

            MainActivity.resetAllPrayers = true;

            Toast.makeText(context.getApplicationContext(), "Reset all prayers", Toast.LENGTH_LONG).show();

    }

}
