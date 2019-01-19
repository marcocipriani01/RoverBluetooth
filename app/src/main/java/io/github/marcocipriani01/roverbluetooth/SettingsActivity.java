package io.github.marcocipriani01.roverbluetooth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    public static final String ROVER_SETTINGS = "RoverBluetooth_settings";
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;

    ArrayList<Switch> switches = new ArrayList<>();
    SeekBar sensorSpeedBar;


    @Override
    @SuppressWarnings("all")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //Settings
        preferences = getSharedPreferences(ROVER_SETTINGS, MODE_PRIVATE);

        switches.add((Switch) findViewById(R.id.Save_speed_switch));
        switches.get(0).setChecked(getSetting("save_speed", true));
        switches.add((Switch) findViewById(R.id.Keep_screen_on_switch));
        switches.get(1).setChecked(getSetting("keep_on", true));
        switches.add((Switch) findViewById(R.id.BT_auto_on_switch));
        switches.get(2).setChecked(getSetting("BT_auto_on", true));
        switches.add((Switch) findViewById(R.id.BT_auto_off_switch));
        switches.get(3).setChecked(getSetting("BT_auto_off", false));
        switches.add((Switch) findViewById(R.id.BT_auto_connect));
        switches.get(4).setChecked(getSetting("BT_auto_connect", true));

        sensorSpeedBar = (SeekBar) findViewById(R.id.sensor_speed_seekbar);
        sensorSpeedBar.setProgress(getSetting("sensor_speed", 3));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        editSetting("save_speed", switches.get(0).isChecked());
        editSetting("keep_on", switches.get(1).isChecked());
        editSetting("BT_auto_on", switches.get(2).isChecked());
        editSetting("BT_auto_off", switches.get(3).isChecked());
        editSetting("BT_auto_connect", switches.get(4).isChecked());

        editSetting("sensor_speed", sensorSpeedBar.getProgress());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //You can use this like an onClickListener, but for menu items
        switch (item.getItemId()) {
            case android.R.id.home:
                //Back button
                this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    public void visitMySite(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getApplicationContext().getString(R.string.website)));
        startActivity(browserIntent);
    }

    void editSetting(String tag, boolean value) {
        preferencesEditor = preferences.edit();
        preferencesEditor.putBoolean(tag, value);
        preferencesEditor.apply();
    }

    void editSetting(String tag, int value) {
        preferencesEditor = preferences.edit();
        preferencesEditor.putInt(tag, value);
        preferencesEditor.apply();
    }

    boolean getSetting(String tag, boolean defValue) {
        return preferences.getBoolean(tag, defValue);
    }

    int getSetting(String tag, int defValue) {
        return preferences.getInt(tag, defValue);
    }

    public void shareAction(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getApplicationContext().getText(R.string.share_text));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}