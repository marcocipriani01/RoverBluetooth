package io.github.marcocipriani01.roverbluetooth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SeekBar sensorSpeedBar;
    private SwitchCompat saveSpeed, keepScreenOn, btOn, btOff, autoConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        //Settings
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        saveSpeed = findViewById(R.id.Save_speed_switch);
        saveSpeed.setChecked(preferences.getBoolean("save_speed", true));
        keepScreenOn = findViewById(R.id.Keep_screen_on_switch);
        keepScreenOn.setChecked(preferences.getBoolean("keep_on", true));
        btOn = findViewById(R.id.BT_auto_on_switch);
        btOn.setChecked(preferences.getBoolean("BT_auto_on", true));
        btOff = findViewById(R.id.BT_auto_off_switch);
        btOff.setChecked(preferences.getBoolean("BT_auto_off", false));
        autoConnect = findViewById(R.id.BT_auto_connect);
        autoConnect.setChecked(preferences.getBoolean("BT_auto_connect", true));
        sensorSpeedBar = findViewById(R.id.sensor_speed_seekbar);
        sensorSpeedBar.setProgress(preferences.getInt("sensor_speed", 3));
    }

    @Override
    protected void onPause() {
        super.onPause();
        preferences.edit().putBoolean("save_speed", saveSpeed.isChecked())
                .putBoolean("keep_on", keepScreenOn.isChecked())
                .putBoolean("BT_auto_on", btOn.isChecked())
                .putBoolean("BT_auto_off", btOff.isChecked())
                .putBoolean("BT_auto_connect", autoConnect.isChecked())
                .putInt("sensor_speed", sensorSpeedBar.getProgress())
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) this.finish();
        return super.onOptionsItemSelected(item);
    }

    public void visitMySite(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getApplicationContext().getString(R.string.website)));
        startActivity(browserIntent);
    }

    public void shareAction(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, getApplicationContext().getText(R.string.share_text));
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}