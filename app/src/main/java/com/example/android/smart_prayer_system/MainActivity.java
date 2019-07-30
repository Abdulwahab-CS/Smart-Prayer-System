package com.example.android.smart_prayer_system;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.arch.persistence.room.Room;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// for converting numbers from arabic to english and vise-versa

public class MainActivity extends AppCompatActivity {

    // --------------------------------------------------------------------------------------------- start data members

    // Access the Bluetooth in the device
    BluetoothAdapter bluetoothAdapter;

    // Used when you are pairing the app with SPS device
    private final static String SPS_device_name = "SPS";
    String SPS_address = null;

    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;

    // means the bluetooth is turned on & the device is paired
    private boolean readyToConnectToBT = false;
    private Set<BluetoothDevice> pairedDevices;
    private ProgressDialog progress;

    // for updating the prayers list
    public static boolean updateList = false;
    public static int prayerNumber = 0;

    // to send a permission request to the user to turn the bluetooth on
    private final static int REQUEST_ENABLE_BT = 1;

    // components from XML file
    private Button connectBtn;
    private TextView status, day, date;
    private ImageView connectedIcon;
    private CheckBox Fajr_checkBox, Dhuhr_checkBox, Asr_checkBox, Maghrib_checkBox, Esha_checkBox;
    private TextView Fajr_textView, Dhuhr_textView, Asr_textView, Maghrib_textView, Esha_textView;
    private TextView Fajr_doneTime, Dhuhr_doneTime, Asr_doneTime, Maghrib_doneTime, Esha_doneTime;
    private Button Fajr_startBtn, Dhuhr_startBtn, Asr_startBtn, Maghrib_startBtn, Esha_startBtn;
    private Button Fajr_undoBtn, Dhuhr_undoBtn, Asr_undoBtn, Maghrib_undoBtn, Esha_undoBtn;

    // define the database
    public static SPS_database sps_database;

    // a flag to to reset all prayers in 11:59 pm everyday
    public static boolean resetAllPrayers;

    // --------------------------------------------------------------------------------------------- end data members

    // --------------------------------------------------------------------------------------------- start onCreate function
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // to add the custom Tool bar to the app
        Toolbar toolbar =  findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        // to translate the action bar title when the language has changed
        setTitle(R.string.app_name);

        // defining the database
        sps_database = Room.databaseBuilder(getApplicationContext(), SPS_database.class, "SPS_db")
                .allowMainThreadQueries()
                .build();


        // if you open the app after 11:59 pm, the app must reset the prayers list
        if(resetAllPrayers){
            __reset_all_prayers_in_SPS_db_to_not_done();
            resetAllPrayers = false;
        }



        // Now you can access the mobile device Bluetooth, i.e. check the paired devices etc.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // check if the device NOT supports the Bluetooth technology
        if (bluetoothAdapter == null) {   msg("Bluetooth NOT supported in your device !!");    finish();  }

        // match the components needed ( match XML with Java )
        matchComponents();

        // display the current day & date
        day.setText(getCurrentDay());
        date.setText(getCurrentDate());

        // to ensure that the time format in the database same as the current display language
        adjustDoneTimesInDatabase_toBeSameDisplayLanguage();

        // Reflect the prayers information from the database to prayers list on UI
        reflect_SPS_db_to_UI_prayers_list();


        // Handle the connect to BT button
        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!bluetoothAdapter.isEnabled()) {

                    // sending direct request msg to turn the bluetooth on
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    msg(getResources().getString(R.string.turnBT_on_msg));

                } else if (bluetoothAdapter.isEnabled()) {

                    if (checkPairing()) {

                        SPS_address = getSPS_device().getAddress();
                        readyToConnectToBT = true;

                        if(isBtConnected){
                            msg(getResources().getString(R.string.alreadyConnected_msg));
                        }else {
                            createBT_socket_andStartCommunication();
                        }

                    } else {
                        msg(getResources().getString(R.string.pair_yourDevice_with_SPS_msg));
                    }
                }

            }
        });


        // add an event listener to shapredPereferences so that in case the language is changed remove remove the MainActivity with the old language
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // Implementation
                finish();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(listener);


        setTimeToResetAllPrayers();
    }
    // --------------------------------------------------------------------------------------------- end onCreate function

    // when the time is 11:59 pm, reset all prayers
    private void setTimeToResetAllPrayers() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(getApplicationContext(), myAlarm.class);

        // PendingIntent specifies an action to take in future.
        // The main differences between a pendingIntent and regular intent is pendingIntent will perform at a later time where Normal/Regular intent starts immediately.
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, i, 0);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    // for creating the three dots as menu on the custom action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // to handle when the user selects from the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.settings:
                Intent i = new Intent(MainActivity.this, Settings.class);
                startActivity(i);
                return true;

            case R.id.about_sps:
                Intent j = new Intent(MainActivity.this, AboutSPS.class);
                startActivity(j);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    // We need it to change the language of the app
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }


    public void start_prayer(View view){

        if(isBtConnected){

            int resID = view.getId();
            String clickedBtnID = getResources().getResourceEntryName(resID);

            int prayer_number = Integer.parseInt(clickedBtnID.substring(1,2));

            switch (prayer_number){
                case 1:
                    send_prayerSymbol_toSPS("F");
                    break;

                case 2:
                    send_prayerSymbol_toSPS("D");
                    break;

                case 3:
                    send_prayerSymbol_toSPS("A");
                    break;

                case 4:
                    send_prayerSymbol_toSPS("M");
                    break;

                case 5:
                    send_prayerSymbol_toSPS("E");
                    break;
            }

        } else {

            msg(getResources().getString(R.string.connect_toSPS_first_msg));
        }


    }

    public void undo_prayer(View view){

        final int prayer_number;
        String clickedCheckBoxID = getResources().getResourceEntryName(view.getId());
        prayer_number = Integer.parseInt(clickedCheckBoxID.substring(1,2));

        final int the_prayer_number = prayer_number;


        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(get_confirm_undo_msg(prayer_number))
                .setIcon(android.R.drawable.ic_dialog_alert)

                // if the user clicks Yes
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {


                        set_prayer_as_NOT_done_on_database(the_prayer_number);
                        set_prayer_as_NOT_done_on_UI(the_prayer_number);

                        // show undo prayer msg
                        show_undo_prayer_mag(prayer_number);
                    }
                })

                // if the user clicks no
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                }).show();


    }

    private void createBT_socket_andStartCommunication(){

        if(readyToConnectToBT){

            new ConnectBT().execute();

                // make timer to read from the input stream each 1 second
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                reading_from_BT_socket();
                            }
                        });
                    }
                },0, 1000);

        }

    }

    private void matchComponents(){

        Fajr_textView = findViewById(R.id._1_textView);
        Dhuhr_textView = findViewById(R.id._2_textView);
        Asr_textView = findViewById(R.id._3_textView);
        Maghrib_textView = findViewById(R.id._4_textView);
        Esha_textView = findViewById(R.id._5_textView);

        Fajr_checkBox = findViewById(R.id._1_checkBox);
        Dhuhr_checkBox = findViewById(R.id._2_checkBox);
        Asr_checkBox = findViewById(R.id._3_checkBox);
        Maghrib_checkBox = findViewById(R.id._4_checkBox);
        Esha_checkBox = findViewById(R.id._5_checkBox);

        Fajr_doneTime = findViewById(R.id._1_prayerDoneTime);
        Dhuhr_doneTime = findViewById(R.id._2_prayerDoneTime);
        Asr_doneTime = findViewById(R.id._3_prayerDoneTime);
        Maghrib_doneTime = findViewById(R.id._4_prayerDoneTime);
        Esha_doneTime = findViewById(R.id._5_prayerDoneTime);

        Fajr_startBtn = findViewById(R.id._1_startBtn);
        Dhuhr_startBtn = findViewById(R.id._2_startBtn);
        Asr_startBtn = findViewById(R.id._3_startBtn);
        Maghrib_startBtn = findViewById(R.id._4_startBtn);
        Esha_startBtn = findViewById(R.id._5_startBtn);

        Fajr_undoBtn = findViewById(R.id._1_undoBtn);
        Dhuhr_undoBtn = findViewById(R.id._2_undoBtn);
        Asr_undoBtn = findViewById(R.id._3_undoBtn);
        Maghrib_undoBtn = findViewById(R.id._4_undoBtn);
        Esha_undoBtn = findViewById(R.id._5_undoBtn);

        day = findViewById(R.id.day);
        date = findViewById(R.id.date);

        status = findViewById(R.id.status);

        // connection status icon. i.e. "!" for disconnected status
        connectedIcon = findViewById(R.id.connected_icon);
        connectBtn = findViewById(R.id.connectBtn);
    }

    private void send_prayerSymbol_toSPS(String prayer_symbol){

        if(btSocket != null){ // or we can use (isBtConnected == true)
            try{
                btSocket.getOutputStream().write(prayer_symbol.getBytes());
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    private void update_database(int prayer_number){


            Prayer p = new Prayer();

            switch (prayer_number) {
                case 1:
                    p.setNumber(1);
                    p.setName("Fajr");
                    break;

                case 2:
                    p.setNumber(2);
                    p.setName("Dhuhr");
                    break;

                case 3:
                    p.setNumber(3);
                    p.setName("Asr");
                    break;

                case 4:
                    p.setNumber(4);
                    p.setName("Maghrib");
                    break;

                case 5:
                    p.setNumber(5);
                    p.setName("Esha");
                    break;
            }

            p.setDone(true);

            String time = getCurrentTime();
            p.setDone_time(time);

            // now insert the new object in the database
            sps_database.myDAO().update_prayer(p);

            // show done save prayer msg
            show_done_save_prayer_mag(prayer_number);

    }

    private void show_done_save_prayer_mag(int prayer_number) {

        switch (prayer_number) {
            case 1:
                msg(getResources().getString(R.string.done_save_prayer_1));
                break;
            case 2:
                msg(getResources().getString(R.string.done_save_prayer_2));
                break;
            case 3:
                msg(getResources().getString(R.string.done_save_prayer_3));
                break;
            case 4:
                msg(getResources().getString(R.string.done_save_prayer_4));
                break;
            case 5:
                msg(getResources().getString(R.string.done_save_prayer_5));
                break;
        }
    }

    private void show_undo_prayer_mag(int prayer_number) {

        switch (prayer_number) {
            case 1:
                msg(getResources().getString(R.string.done_undo_prayer_1));
                break;
            case 2:
                msg(getResources().getString(R.string.done_undo_prayer_2));
                break;
            case 3:
                msg(getResources().getString(R.string.done_undo_prayer_3));
                break;
            case 4:
                msg(getResources().getString(R.string.done_undo_prayer_4));
                break;
            case 5:
                msg(getResources().getString(R.string.done_undo_prayer_5));
                break;
        }
    }

    private void __reset_all_prayers_in_SPS_db_to_not_done(){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        for(Prayer p : allPrayers){

            p.setDone(false);
            p.setDone_time("");
            sps_database.myDAO().update_prayer(p);
        }

    }

    private boolean __is_all_prayers_NOT_done(){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        for(Prayer p : allPrayers){

            if(p.isDone() == true){
                return false;

            }
        }
        return true;
    }

    private void reflect_SPS_db_to_UI_prayers_list(){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        updateList = true;

        for(Prayer p : allPrayers){

            if(p.isDone() == true){
                set_prayer_as_done_on_UI(p.getNumber());

            }else if(p.isDone() == false){
                set_prayer_as_NOT_done_on_UI(p.getNumber());
            }
        }

    }

    private void set_prayer_as_done_on_database(int prayer_number){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        for(Prayer p : allPrayers){
            if(p.getNumber() == prayer_number){
                p.setDone(true);
                p.setDone_time(getCurrentTime());
                sps_database.myDAO().update_prayer(p);
            }
        }
    }

    private void set_prayer_as_NOT_done_on_database(int prayer_number){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        for(Prayer p : allPrayers){
            if(p.getNumber() == prayer_number){
                p.setDone(false);
                p.setDone_time("");
                sps_database.myDAO().update_prayer(p);
            }
        }
    }

    private void set_prayer_as_done_on_UI(int prayer_number){

        LinearLayout linearLayout;
        String prayerDoneTimeValue;

        switch (prayer_number) {
            case 1:
                Fajr_checkBox.setChecked(true);
                Fajr_checkBox.setEnabled(false);
                Fajr_startBtn.setEnabled(false);
                Fajr_startBtn.setText(R.string.prayer_btn_done);

                // to change the background color of the prayer
                linearLayout = (LinearLayout) Fajr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.done_prayer_background_color));

                // get the prayer done time from the data base
                prayerDoneTimeValue = sps_database.myDAO().get_prayer_done_time(1);
                Fajr_doneTime.setText(prayerDoneTimeValue);
                Fajr_doneTime.setVisibility(View.VISIBLE);

                Fajr_undoBtn.setVisibility(View.VISIBLE);

                break;
            case 2:
                Dhuhr_checkBox.setChecked(true);
                Dhuhr_checkBox.setEnabled(false);
                Dhuhr_startBtn.setEnabled(false);
                Dhuhr_startBtn.setText(R.string.prayer_btn_done);
                linearLayout = (LinearLayout) Dhuhr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.done_prayer_background_color));

                // get the prayer done time from the data base
                prayerDoneTimeValue = sps_database.myDAO().get_prayer_done_time(2);
                Dhuhr_doneTime.setText(prayerDoneTimeValue);
                Dhuhr_doneTime.setVisibility(View.VISIBLE);

                Dhuhr_undoBtn.setVisibility(View.VISIBLE);

                break;

            case 3:
                Asr_checkBox.setChecked(true);
                Asr_checkBox.setEnabled(false);
                Asr_startBtn.setEnabled(false);
                Asr_startBtn.setText(R.string.prayer_btn_done);
                linearLayout = (LinearLayout) Asr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.done_prayer_background_color));

                // get the prayer done time from the data base
                prayerDoneTimeValue = sps_database.myDAO().get_prayer_done_time(3);
                Asr_doneTime.setText(prayerDoneTimeValue);
                Asr_doneTime.setVisibility(View.VISIBLE);

                Asr_undoBtn.setVisibility(View.VISIBLE);

                break;

            case 4:
                Maghrib_checkBox.setChecked(true);
                Maghrib_checkBox.setEnabled(false);
                Maghrib_startBtn.setEnabled(false);
                Maghrib_startBtn.setText(R.string.prayer_btn_done);
                linearLayout = (LinearLayout) Maghrib_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.done_prayer_background_color));

                // get the prayer done time from the data base
                prayerDoneTimeValue = sps_database.myDAO().get_prayer_done_time(4);
                Maghrib_doneTime.setText(prayerDoneTimeValue);
                Maghrib_doneTime.setVisibility(View.VISIBLE);

                Maghrib_undoBtn.setVisibility(View.VISIBLE);

                break;

            case 5:
                Esha_checkBox.setChecked(true);
                Esha_checkBox.setEnabled(false);
                Esha_startBtn.setEnabled(false);
                Esha_startBtn.setText(R.string.prayer_btn_done);
                linearLayout = (LinearLayout) Esha_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.done_prayer_background_color));

                // get the prayer done time from the data base
                prayerDoneTimeValue = sps_database.myDAO().get_prayer_done_time(5);
                Esha_doneTime.setText(prayerDoneTimeValue);
                Esha_doneTime.setVisibility(View.VISIBLE);

                Esha_undoBtn.setVisibility(View.VISIBLE);

                break;
        }

    }

    private void set_prayer_as_NOT_done_on_UI(int prayer_number){

        LinearLayout linearLayout;


        switch (prayer_number) {
            case 1:
                Fajr_checkBox.setChecked(false);
                Fajr_checkBox.setEnabled(true);
                Fajr_startBtn.setEnabled(true);
                Fajr_startBtn.setText(R.string.prayer_btn_not_done);

                // to change the background color of the prayer
                linearLayout = (LinearLayout) Fajr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.not_done_prayer_background_color));

                Fajr_doneTime.setText("");
                Fajr_doneTime.setVisibility(View.INVISIBLE);

                Fajr_undoBtn.setVisibility(View.INVISIBLE);

                break;
            case 2:
                Dhuhr_checkBox.setChecked(false);
                Dhuhr_checkBox.setEnabled(true);
                Dhuhr_startBtn.setEnabled(true);
                Dhuhr_startBtn.setText(R.string.prayer_btn_not_done);
                linearLayout = (LinearLayout) Dhuhr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.not_done_prayer_background_color));

                Dhuhr_doneTime.setText("");
                Dhuhr_doneTime.setVisibility(View.INVISIBLE);

                Dhuhr_undoBtn.setVisibility(View.INVISIBLE);

                break;

            case 3:
                Asr_checkBox.setChecked(false);
                Asr_checkBox.setEnabled(true);
                Asr_startBtn.setEnabled(true);
                Asr_startBtn.setText(R.string.prayer_btn_not_done);
                linearLayout = (LinearLayout) Asr_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.not_done_prayer_background_color));

                Asr_doneTime.setText("");
                Asr_doneTime.setVisibility(View.INVISIBLE);

                Asr_undoBtn.setVisibility(View.INVISIBLE);

                break;

            case 4:
                Maghrib_checkBox.setChecked(false);
                Maghrib_checkBox.setEnabled(true);
                Maghrib_startBtn.setEnabled(true);
                Maghrib_startBtn.setText(R.string.prayer_btn_not_done);
                linearLayout = (LinearLayout) Maghrib_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.not_done_prayer_background_color));

                Maghrib_doneTime.setText("");
                Maghrib_doneTime.setVisibility(View.INVISIBLE);

                Maghrib_undoBtn.setVisibility(View.INVISIBLE);

                break;

            case 5:
                Esha_checkBox.setChecked(false);
                Esha_checkBox.setEnabled(true);
                Esha_startBtn.setEnabled(true);
                Esha_startBtn.setText(R.string.prayer_btn_not_done);
                linearLayout = (LinearLayout) Esha_checkBox.getParent();
                linearLayout.setBackgroundColor(getResources().getColor(R.color.not_done_prayer_background_color));

                Esha_doneTime.setText("");
                Esha_doneTime.setVisibility(View.INVISIBLE);

                Esha_undoBtn.setVisibility(View.INVISIBLE);

                break;
        }

    }

    private void __fill_SPS_db_initialy_with_all_prayers(){

        // Remembr : you have to clear the database first and removing all prayers tubles, then complete

        Prayer p1 = new Prayer(1, "Fajr", false, "");
        Prayer p2 = new Prayer(2, "Dhuhr", false, "");
        Prayer p3 = new Prayer(3, "Asr", false, "");
        Prayer p4 = new Prayer(4, "Maghrib", false, "");
        Prayer p5 = new Prayer(5, "Esha", false, "");

        sps_database.myDAO().add_prayer(p1);
        sps_database.myDAO().add_prayer(p2);
        sps_database.myDAO().add_prayer(p3);
        sps_database.myDAO().add_prayer(p4);
        sps_database.myDAO().add_prayer(p5);

    }

    private void adjustDoneTimesInDatabase_toBeSameDisplayLanguage(){

        List<Prayer> allPrayers = sps_database.myDAO().get_all_prayers();

        String currentLanguage = Locale.getDefault().getDisplayLanguage();

        DateFormat formatArabic = new SimpleDateFormat("h:mm a", new Locale("ar"));
        DateFormat formatEnglish = new SimpleDateFormat("h:mm a", Locale.ENGLISH);


        for(Prayer p : allPrayers){

            if(!p.getDone_time().equals("")){

                switch (currentLanguage){
                    case "English":
                        if(isTime_inArabic(p.getDone_time())){

                            Date tempTime = null;
                            try {
                                tempTime = formatArabic.parse(p.getDone_time());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            String time = formatEnglish.format(tempTime);

                            p.setDone_time(time);
                            sps_database.myDAO().update_prayer(p);
                        }
                        break;

                    case "العربية":
                        if(!isTime_inArabic(p.getDone_time())){

                            Date tempTime = null;
                            try {
                                tempTime = formatEnglish.parse(p.getDone_time());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            String time = formatArabic.format(tempTime);

                            p.setDone_time(time);
                            sps_database.myDAO().update_prayer(p);
                        }
                        break;
                }

            }
        }



    }

    private boolean isTime_inArabic(String time){

        if( time.contains("AM") || time.contains("PM") ){
            return false;
        }else
            return true;
    }

    private String getCurrentDate(){
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("yyyy - MM - dd");
        String formattedDate = df.format(calendar.getTime());

        return formattedDate;
    }

    private String getCurrentDay() {
        String today = "";

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek){
            case 1:
                today = getResources().getString(R.string.day1);
                break;
            case 2:
                today = getResources().getString(R.string.day2);
                break;
            case 3:
                today = getResources().getString(R.string.day3);
                break;
            case 4:
                today = getResources().getString(R.string.day4);
                break;
            case 5:
                today = getResources().getString(R.string.day5);
                break;
            case 6:
                today = getResources().getString(R.string.day6);
                break;
            case 7:
                today = getResources().getString(R.string.day7);
                break;
        }

        return today;
    }

    private boolean checkPairing() {

        // to get all paired devices
        pairedDevices = bluetoothAdapter.getBondedDevices();

        boolean pairingSuccess = false;
        if (pairedDevices.size() > 0) {

            for (BluetoothDevice bt : pairedDevices) {

                if (bt.getName().equals(SPS_device_name)) {
                    pairingSuccess = true;
                }
            }
        }

        return pairingSuccess;
    }

    private Device getSPS_device() {

        pairedDevices = bluetoothAdapter.getBondedDevices();
        Device sps_device = null;

        for (BluetoothDevice bt : pairedDevices) {
            if (bt.getName().equals(SPS_device_name)) {
                sps_device = new Device(bt.getName(), bt.getAddress());
            }
        }

        return sps_device;
    }

    public void confirmation_dialog_1(View view) {

        int prayer_number;
        String clickedCheckBoxID = getResources().getResourceEntryName(view.getId());
        prayer_number = Integer.parseInt(clickedCheckBoxID.substring(1,2));

        final int the_prayer_number = prayer_number;



        // get the clicked checkBox object (to make it disabled) - (i don't know a way to get it directly from 'view' param)
        // ---
        LinearLayout list_item = (LinearLayout) view.getParent();
        CheckBox temp_c = null;
        for(int i=0; i<list_item.getChildCount(); i++){
            int resID = list_item.getChildAt(i).getId();
            String childID = getResources().getResourceEntryName(resID);
            String testKeyword_in_childID = childID.substring(childID.length() - 8);
            if(testKeyword_in_childID.equals("checkBox")){
                temp_c = (CheckBox) list_item.getChildAt(i);

            }
        }
        final CheckBox c = temp_c;
        // ---


        // get the undo button object to the this prayer (to make it visible) - (i don't know a way to get it directly from 'view' param)
        // ---
        Button temp_undoBtn = null;
        for(int i=0; i<list_item.getChildCount(); i++){
            int resID = list_item.getChildAt(i).getId();
            String childID = getResources().getResourceEntryName(resID);
            String testKeyword_in_childID = childID.substring(childID.length() - 7);
            if(testKeyword_in_childID.equals("undoBtn")){
                temp_undoBtn = (Button) list_item.getChildAt(i);

            }
        }
        final Button undoBtn = temp_undoBtn;
        // ---





        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(get_confirm_msg(prayer_number))
                .setIcon(android.R.drawable.ic_dialog_alert)

                // if the user clicks Yes
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        // update the database
                        set_prayer_as_done_on_database(the_prayer_number);

                        // reflect the update to the UI
                        reflect_SPS_db_to_UI_prayers_list();

                        // show done save msg
                        show_done_save_prayer_mag(the_prayer_number);

                        // make undoBtn is visiable
                         undoBtn.setVisibility(View.VISIBLE);
                    }
                })

                // if the user clicks no
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                         c.setChecked(false);
                    }
                }).show();

    }

    private String get_confirm_msg(int prayer_number){

        String the_confirm_msg = "";
        switch (prayer_number) {
            case 1:
                the_confirm_msg = getResources().getString(R.string.confirm_done_prayer_1);
                break;
            case 2:
                the_confirm_msg = getResources().getString(R.string.confirm_done_prayer_2);
                break;
            case 3:
                the_confirm_msg = getResources().getString(R.string.confirm_done_prayer_3);
                break;
            case 4:
                the_confirm_msg = getResources().getString(R.string.confirm_done_prayer_4);
                break;
            case 5:
                the_confirm_msg = getResources().getString(R.string.confirm_done_prayer_5);
                break;
        }

        return the_confirm_msg;
    }

    private String get_confirm_undo_msg(int prayer_number){

        String the_confirm_msg = "";
        switch (prayer_number) {
            case 1:
                the_confirm_msg = getResources().getString(R.string.confirm_undo_prayer_1);
                break;
            case 2:
                the_confirm_msg = getResources().getString(R.string.confirm_undo_prayer_2);
                break;
            case 3:
                the_confirm_msg = getResources().getString(R.string.confirm_undo_prayer_3);
                break;
            case 4:
                the_confirm_msg = getResources().getString(R.string.confirm_undo_prayer_4);
                break;
            case 5:
                the_confirm_msg = getResources().getString(R.string.confirm_undo_prayer_5);
                break;
        }

        return the_confirm_msg;
    }

    public void donePrayer(View view) {

        LinearLayout list_item = (LinearLayout) view.getParent();
        CheckBox c = null;

        for(int i=0; i<list_item.getChildCount(); i++){
            int resID = list_item.getChildAt(i).getId();
            String childID = getResources().getResourceEntryName(resID);
            String testKeyword_in_childID = childID.substring(childID.length() - 8);
            if(testKeyword_in_childID.equals("checkBox")){
                c = (CheckBox) list_item.getChildAt(i);

            }
        }


        if (c.isChecked()) {
            confirmation_dialog_1(view);
        }

    }

    public void msg(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    public String getCurrentTime(){

        String time = "";
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat df = new SimpleDateFormat("H:mm");
        String _24HourTime = df.format(calendar.getTime());


        try {
            SimpleDateFormat _24HourSDF = new SimpleDateFormat("H:mm");
            SimpleDateFormat _12HourSDF = new SimpleDateFormat("h:mm a");
            Date _24HourDt = _24HourSDF.parse(_24HourTime);
            time = _12HourSDF.format(_24HourDt);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return time;
    }

    public void reading_from_BT_socket(){

        String out = "";

        try {

            InputStream in = btSocket.getInputStream();
            int readable_char = in.available();

            for(int i11=0;i11<readable_char;i11++){
                int i=in.read();
                if(i!=-1)
                    out += (char)i;
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        try{
            if( out != null && out.trim().length() != 0 ){

                // remove the extra spaces at the end.
                out = out.replaceAll("\\D+","");

                try {
                    prayerNumber = Integer.parseInt(out);

                }catch(Exception e){
                     msg(e.getMessage());
                }

                if(prayerNumber == 1 || prayerNumber == 2 || prayerNumber == 3 || prayerNumber == 4 || prayerNumber == 5){

                    // update the database
                    update_database(prayerNumber);

                    // update the UI
                    set_prayer_as_done_on_UI(prayerNumber);
                    // update_UI(prayerSymbol);
                }

            }

        }catch (Exception e){

            e.printStackTrace();
        }
    }



    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            if (btSocket == null || !isBtConnected){
                String title = getResources().getString(R.string.connecting);
                String body = getResources().getString(R.string.please_wait);

                //show a progress dialog
                progress = ProgressDialog.show(MainActivity.this, title, body);
            }
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    BluetoothDevice SPS_device = bluetoothAdapter.getRemoteDevice(SPS_address);

                    // Java Reflection code
                    Method m = SPS_device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});

                    // (first parameter of invoke method) is SPS_device is the object that we are invoking the method on
                    // (second parameter of invoke method) is the port number to create the socket on
                    btSocket = (BluetoothSocket) m.invoke(SPS_device, 1);

                    // start connection
                    btSocket.connect();
                }
            }
            catch (Exception e)
            {
                ConnectSuccess = false;
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            // after the doInBackground, it checks if everything went fine
            if (!ConnectSuccess)
            {
                status.setText(getResources().getString(R.string.BTstatus_disconnected));
                connectedIcon.setImageResource(R.mipmap.ic_launcher_foreground);
                msg(getResources().getString(R.string.connectionFaild_msg));
                isBtConnected = false;
                status.setBackgroundColor(getResources().getColor(R.color.disconnected_color));
            }
            else
            {
                status.setText(getResources().getString(R.string.BTstatus_connected));
                connectedIcon.setImageResource(R.drawable.connected_icon);
                msg(getResources().getString(R.string.BTstatus_connected));
                isBtConnected = true;
                status.setBackgroundColor(getResources().getColor(R.color.connected_color));
            }

            if(progress != null)
                progress.dismiss();
        }

    }


}