package io.github.marcocipriani01.roverbluetooth;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, BluetoothHelper.BluetoothCallback {

    public static final String TAG = "RoverBluetooth";

    //Bluetooth
    static BluetoothHelper bluetoothHelper = new BluetoothHelper();
    //Settings
    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        //Settings
        preferences = getSharedPreferences(SettingsActivity.ROVER_SETTINGS, MODE_PRIVATE);

        //Bluetooth
        bluetoothHelper.setBluetoothCallback(this);
        if (getSetting("BT_auto_on", true)) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        if (getSetting("BT_auto_connect", true)) {
            bluetoothHelper.connectToAddress(getSetting("last_device", "null"));
        }

        //Views
        steeringWheel = (SeekBar) findViewById(R.id.steering_wheel);
        forwardsSpeed = (SeekBar) findViewById(R.id.forwards_speed);
        backwardsSpeed = (SeekBar) findViewById(R.id.backwards_speed);
        if (getSetting("save_speed", true)) {
            forwardsSpeed.setProgress(getSetting("forwards_speed", 205));
            backwardsSpeed.setProgress(getSetting("backwards_speed", 185));
        }
        distanceView = (TextView) findViewById(R.id.rover_distance);
        distance = String.valueOf(getApplicationContext().getText(R.string.distance));

        //listDialogs
        listDialog = new AlertDialog.Builder(this);
        listDialog.setCancelable(true);
        listDialog.setTitle(R.string.app_name);
        listDialog.setIcon(R.drawable.launcher_icon);
        listDialog.setNegativeButton(getApplicationContext().getText(R.string.dialog_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //errorDialog
        errorDialog = new AlertDialog.Builder(this);
        errorDialog.setCancelable(false);
        errorDialog.setTitle(R.string.app_name);
        errorDialog.setIcon(R.drawable.error_material);
        errorDialog.setPositiveButton(getApplicationContext().getText(R.string.dialog_OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //Set the full screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Accelerometer initializer
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (getSetting("BT_auto_off", false)) {
            bluetoothHelper.disableBluetooth();
        }

        editSetting("forwards_speed", forwardsSpeed.getProgress());
        editSetting("backwards_speed", backwardsSpeed.getProgress());
    }

    /**
     * void editSetting(String tag, boolean value) {
     * preferencesEditor = preferences.edit();
     * preferencesEditor.putBoolean(tag, value);
     * preferencesEditor.apply();
     * }
     */

    void editSetting(String tag, int value) {
        preferencesEditor = preferences.edit();
        preferencesEditor.putInt(tag, value);
        preferencesEditor.apply();
    }

    void editSetting(String tag, String value) {
        preferencesEditor = preferences.edit();
        preferencesEditor.putString(tag, value);
        preferencesEditor.apply();
    }

    boolean getSetting(String tag, boolean defValue) {
        return preferences.getBoolean(tag, defValue);
    }

    int getSetting(String tag, int defValue) {
        return preferences.getInt(tag, defValue);
    }

    String getSetting(String tag, String defValue) {
        return preferences.getString(tag, defValue);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause");
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        int sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;
        switch (getSetting("sensor_speed", 3)) {
            case 3: {
                sensorSpeed = SensorManager.SENSOR_DELAY_FASTEST;
                break;
            }

            case 2: {
                sensorSpeed = SensorManager.SENSOR_DELAY_GAME;
                break;
            }

            case 1: {
                sensorSpeed = SensorManager.SENSOR_DELAY_UI;
                break;
            }

            case 0: {
                sensorSpeed = SensorManager.SENSOR_DELAY_NORMAL;
                break;
            }
        }
        sensorManager.registerListener(this, accelerometer, sensorSpeed);

        if (getSetting("keep_on", true)) {
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
        switch (item.getItemId()) {
            case R.id.toolbar_connect: {
                //Connect
                if (!bluetoothHelper.getIsConnected()) {
                    initializeBT();

                } else {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.already_connected), Toast.LENGTH_LONG).show();
                }
                return true;
            }

            case R.id.toolbar_settings: {
                //Settings activity.
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.toolbar_disconnect: {
                if (bluetoothHelper.getIsConnected()) {
                    //If connected -> disconnect
                    bluetoothHelper.disconnect();

                } else {
                    //If disconnected -> disable Bluetooth
                    bluetoothHelper.disableBluetooth();
                    Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.BT_disabled), Toast.LENGTH_LONG).show();
                }
                return true;
            }
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
        Log.e(TAG, String.valueOf(bluetoothHelper.getIsConnected()));

        if (bluetoothHelper.getIsConnected()) {
            switch (view.getId()) {
                case R.id.rover_forward: {
                    bluetoothHelper.send(String.valueOf(forwardsSpeed.getProgress() + 1000));
                    break;
                }

                case R.id.rover_stop: {
                    bluetoothHelper.send("21");
                    break;
                }

                case R.id.rover_ackwards: {
                    bluetoothHelper.send(String.valueOf(backwardsSpeed.getProgress() + 1500));
                    break;
                }

                case R.id.rover_light_on: {
                    bluetoothHelper.send("22");
                    break;
                }

                case R.id.rover_light_off: {
                    bluetoothHelper.send("23");
                    break;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public void onSensorChanged(SensorEvent event) {
        //Get the axes values
        //float X = event.values[0];
        float Y = event.values[1];
        //float Z = event.values[2];

        //Set the steering wheel position
        steeringWheel.setProgress((int) ((Y + 10) * 10));

        if (bluetoothHelper.getIsConnected()) {
            bluetoothHelper.send(String.valueOf((int) (Y + 10)));
            distanceView.setText(distance);

        } else {
            distanceView.setText(getApplicationContext().getText(R.string.distance) + " -1");
        }

        //I use onSensorChanged exactly like a timer to show logs (I can't do this outside the main thread, for example in onMessage):
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
                Toast.makeText(getApplicationContext(), getApplicationContext().getText(R.string.error_no_devices_found), Toast.LENGTH_LONG).show();
            }

            listDialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    bluetoothHelper.connectToDevice(pairedDevices.get(which));
                    editSetting("last_device", pairedDevices.get(which).getAddress());
                    dialog.dismiss();
                }
            });

            listDialog.show();
        }
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        //Called when connected to a device.
        Log.e(TAG, "Connected successfully.");
        Log.e(TAG, "Device: " + device.getName() + "; Address: " + device.getAddress());
        bluetoothLogs = String.valueOf(getApplicationContext().getText(R.string.connected));
    }

    @Override
    public void onDisconnect(BluetoothDevice device, String message) {
        //Called when disconnected from a device.
        Log.e(TAG, "Disconnected successfully.");
        bluetoothLogs = String.valueOf(getApplicationContext().getText(R.string.disconnected));
    }

    @Override
    public void onMessage(String message) {
        //Called when a message can be read.
        Log.e(TAG, "Received message: " + message);
        distance = String.valueOf(getApplicationContext().getText(R.string.distance)) + " " + message;
    }

    @Override
    public void onError(String message) {
        //An error occurred. Print the information.
        Log.e(TAG, "An error occurred: " + message);
        bluetoothLogs = String.valueOf(getApplicationContext().getText(R.string.error));
    }

    @Override
    public void onConnectError(BluetoothDevice device, String message) {
        //An error occurred during connection
        Log.e(TAG, "An error occurred during connection: " + message);
        bluetoothLogs = String.valueOf(getApplicationContext().getText(R.string.connection_error));
    }
}