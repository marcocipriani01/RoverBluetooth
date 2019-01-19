package io.github.marcocipriani01.roverbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class BluetoothHelper {

    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private boolean isConnected = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private BluetoothCallback listener = null;
    private BufferedReader input;
    private OutputStream output;


    public BluetoothHelper() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void enableBluetooth() {
        //Enable the Bluetooth without user prompt (avoid using this)
        if ((bluetoothAdapter != null) && (!bluetoothAdapter.isEnabled())) {
            bluetoothAdapter.enable();
        }
    }

    public void disableBluetooth() {
        //Disable the Bluetooth without user prompt
        if ((bluetoothAdapter != null) && (bluetoothAdapter.isEnabled())) {
            bluetoothAdapter.disable();
        }
    }

    public void connectToAddress(String address) {
        //Connect to a device from the address (a code like 34:FG:SR:95:45:62)
        if (!address.equals("null")) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            new ConnectThread(device).start();
        }
    }

    public void connectToName(String name) {
        for (BluetoothDevice blueDevice : bluetoothAdapter.getBondedDevices()) {
            if (blueDevice.getName().equals(name)) {
                connectToAddress(blueDevice.getAddress());
                return;
            }
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        new ConnectThread(device).start();
    }

    public void disconnect() {
        try {
            bluetoothSocket.close();

        } catch (IOException e) {
            if (listener != null) {
                listener.onError(e.getMessage());
            }
        }
    }

    public boolean getIsConnected() {
        return isConnected;
    }

    public void send(String message) {
        //Add a line feed code
        message = message + "\n";

        //Send the message
        try {
            output.write(message.getBytes());

        } catch (IOException e) {
            isConnected = false;

            if (listener != null) {
                listener.onDisconnect(bluetoothDevice, e.getMessage());
            }
        }
    }

    public List<BluetoothDevice> getPairedDevices() {
        //Get a list with all the paired devices
        List<BluetoothDevice> devices = new ArrayList<>();
        for (BluetoothDevice bluetoothDevice : bluetoothAdapter.getBondedDevices()) {
            devices.add(bluetoothDevice);
        }

        return devices;
    }

    public BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void setBluetoothCallback(BluetoothCallback listener) {
        this.listener = listener;
    }

    public void removeBluetoothCallback() {
        this.listener = null;
    }

    public interface BluetoothCallback {
        //You can use BluetoothCallback to put voids into your Activity (@Override them)
        void onConnect(BluetoothDevice device);

        void onDisconnect(BluetoothDevice device, String message);

        void onMessage(String message);

        void onError(String message);

        void onConnectError(BluetoothDevice device, String message);
    }

    private class ReceiveThread extends Thread implements Runnable {
        public void run() {
            String message;

            try {
                while ((message = input.readLine()) != null) {
                    if (listener != null)
                        listener.onMessage(message);
                }

            } catch (IOException e) {
                isConnected = false;

                if (listener != null) {
                    listener.onDisconnect(bluetoothDevice, e.getMessage());
                }
            }
        }
    }

    private class ConnectThread extends Thread {
        public ConnectThread(BluetoothDevice device) {
            BluetoothHelper.this.bluetoothDevice = device;

            try {
                BluetoothHelper.this.bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);

            } catch (IOException e) {
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
                output = bluetoothSocket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                isConnected = true;

                new ReceiveThread().start();

                if (listener != null) {
                    listener.onConnect(bluetoothDevice);
                }

            } catch (IOException e) {
                if (listener != null) {
                    listener.onConnectError(bluetoothDevice, e.getMessage());
                }

                try {
                    bluetoothSocket.close();

                } catch (IOException closeException) {

                    if (listener != null) {
                        listener.onError(closeException.getMessage());
                    }
                }
            }
        }
    }
}

