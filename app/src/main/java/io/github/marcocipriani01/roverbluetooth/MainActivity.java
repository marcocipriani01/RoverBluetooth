package io.github.marcocipriani01.roverbluetooth;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, BluetoothHelper.BluetoothCallback {

    private static final String TAG = "RoverBluetooth";
    //Bluetooth
    private static final BluetoothHelper bluetoothHelper = new BluetoothHelper();
    //Settings
    private SharedPreferences preferences;
    private String distance;
    private String bluetoothLogs = "null";
    //Full screen
    private boolean FullScreenState = true;
    //Views
    private SeekBar steeringWheel;
    private SeekBar forwardsSpeed;
    private SeekBar backwardsSpeed;
    private TextView distanceView;
    //Dialogs
    private AlertDialog.Builder errorDialog;
    private AlertDialog.Builder listDialog;
    //Accelerometer managers
    private Sensor accelerometer;
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar
        setSupportActionBar(findViewById(R.id.app_bar));
        //Settings
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Bluetooth
        bluetoothHelper.setBluetoothCallback(this);
        if (preferences.getBoolean("BT_auto_on", true)) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        if (preferences.getBoolean("BT_auto_connect", true))
            bluetoothHelper.connectToAddress(preferences.getString("last_device", "null"));

        //Views
        steeringWheel = findViewById(R.id.steering_wheel);
        forwardsSpeed = findViewById(R.id.forwards_speed);
        backwardsSpeed = findViewById(R.id.backwards_speed);
        if (preferences.getBoolean("save_speed", true)) {
            forwardsSpeed.setProgress(preferences.getInt("forwards_speed", 205));
            backwardsSpeed.setProgress(preferences.getInt("backwards_speed", 185));
        }
        distanceView = findViewById(R.id.rover_distance);
        distance = String.valueOf(getString(R.string.distance));

        //listDialogs
        listDialog = new AlertDialog.Builder(this);
        listDialog.setCancelable(true);
        listDialog.setTitle(R.string.app_name);
        listDialog.setIcon(R.drawable.launcher_icon);
        listDialog.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> dialog.dismiss());

        //errorDialog
        errorDialog = new AlertDialog.Builder(this);
        errorDialog.setCancelable(false);
        errorDialog.setTitle(R.string.app_name);
        errorDialog.setIcon(R.drawable.error_material);
        errorDialog.setPositiveButton(getString(R.string.dialog_OK), (dialog, which) -> dialog.dismiss());

        //Set the full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Accelerometer initializer
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        preferences.edit()
                .putInt("forwards_speed", forwardsSpeed.getProgress())
                .putInt("backwards_speed", backwardsSpeed.getProgress()).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preferences.getBoolean("BT_auto_off", false)) {
            bluetoothHelper.disableBluetooth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        int sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;
        switch (preferences.getInt("sensor_speed", 3)) {
            case 0:
                sensorSpeed = SensorManager.SENSOR_DELAY_NORMAL;
                break;
            case 1:
                sensorSpeed = SensorManager.SENSOR_DELAY_UI;
                break;
            case 2:
                sensorSpeed = SensorManager.SENSOR_DELAY_GAME;
                break;
            case 3:
                sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;
                break;
        }
        sensorManager.registerListener(this, accelerometer, sensorSpeed);

        if (preferences.getBoolean("keep_on", true)) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Create buttons on the toolbar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.toolbar_connect) {//Connect
            if (!bluetoothHelper.isConnected()) {
                initializeBT();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.already_connected), Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (itemId == R.id.toolbar_settings) {//Settings activity.
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.toolbar_disconnect) {
            if (bluetoothHelper.isConnected()) {
                //If connected -> disconnect
                bluetoothHelper.disconnect();
            } else {
                //If disconnected -> disable Bluetooth
                bluetoothHelper.disableBluetooth();
                Toast.makeText(getApplicationContext(), getString(R.string.BT_disabled), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void switchToFullScreen(View v) {
        if (FullScreenState) {
            //Exit full screen by removing flags
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            FullScreenState = false;
        } else {
            //Enter full screen by adding flags
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            FullScreenState = true;
        }
    }

    public void buttonsActions(View view) {
        Log.e(TAG, String.valueOf(bluetoothHelper.isConnected()));
        if (bluetoothHelper.isConnected()) {
            int id = view.getId();
            if (id == R.id.rover_forward) {
                bluetoothHelper.send(String.valueOf(forwardsSpeed.getProgress() + 1000));
            } else if (id == R.id.rover_stop) {
                bluetoothHelper.send("21");
            } else if (id == R.id.rover_ackwards) {
                bluetoothHelper.send(String.valueOf(backwardsSpeed.getProgress() + 1500));
            } else if (id == R.id.rover_light_on) {
                bluetoothHelper.send("22");
            } else if (id == R.id.rover_light_off) {
                bluetoothHelper.send("23");
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Get the axes values
        float Y = event.values[1];
        //Set the steering wheel position
        steeringWheel.setProgress((int) ((Y + 10) * 10));
        if (bluetoothHelper.isConnected()) {
            bluetoothHelper.send(String.valueOf((int) (Y + 10)));
            distanceView.setText(distance);
        } else {
            distanceView.setText(getString(R.string.distance) + " -1");
        }
        //I use onSensorChanged like a timer to show logs (I can't do this outside the main thread, for example in onMessage):
        if (!bluetoothLogs.equals("null")) {
            Toast.makeText(getApplicationContext(), bluetoothLogs, Toast.LENGTH_SHORT).show();
            bluetoothLogs = "null";
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    void initializeBT() {
        //Check for Bluetooth support:
        if (bluetoothHelper.getBluetoothAdapter() == null) {
            //"Bluetooth is unsupported!"
            errorDialog.setMessage(getApplicationContext().getString(R.string.error_unsupported));
            errorDialog.show();
        } else if (!bluetoothHelper.getBluetoothAdapter().isEnabled()) {
            //Bluetooth disabled -> enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        } else {
            final List<BluetoothDevice> pairedDevices;
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            pairedDevices = bluetoothHelper.getPairedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice bluetoothDevice : pairedDevices) {
                    //Get the device's name and the address
                    adapter.add(bluetoothDevice.getName());
                }
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.error_no_devices_found), Toast.LENGTH_LONG).show();
            }
            listDialog.setAdapter(adapter, (dialog, which) -> {
                bluetoothHelper.connectToDevice(pairedDevices.get(which));
                preferences.edit()
                        .putString("last_device", pairedDevices.get(which).getAddress()).apply();
                dialog.dismiss();
            });
            listDialog.show();
        }
    }

    @Override
    public void onBTConnected(BluetoothDevice device) {
        //Called when connected to a device.
        Log.e(TAG, "Connected successfully.");
        Log.e(TAG, "Device: " + device.getName() + "; Address: " + device.getAddress());
        bluetoothLogs = getString(R.string.connected);
    }

    @Override
    public void onBTDisconnected(BluetoothDevice device, String message) {
        //Called when disconnected from a device.
        Log.e(TAG, "Disconnected successfully.");
        bluetoothLogs = getString(R.string.disconnected);
    }

    @Override
    public void onBTMessage(String message) {
        //Called when a message can be read.
        Log.e(TAG, "Received message: " + message);
        distance = getString(R.string.distance) + " " + message;
    }

    @Override
    public void onBTError(String message) {
        //An error occurred. Print the information.
        Log.e(TAG, "An error occurred: " + message);
        bluetoothLogs = getString(R.string.error);
    }

    @Override
    public void onBTConnectError(BluetoothDevice device, String message) {
        //An error occurred during connection
        Log.e(TAG, "An error occurred during connection: " + message);
        bluetoothLogs = getString(R.string.connection_error);
    }
}