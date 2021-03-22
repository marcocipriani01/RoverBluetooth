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

public class BluetoothHelper {

    private static final UUID DEFAULT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter bluetoothAdapter;
    private boolean isConnected = false;
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
        if ((bluetoothAdapter != null) && (!bluetoothAdapter.isEnabled()))
            bluetoothAdapter.enable();
    }

    public void disableBluetooth() {
        //Disable the Bluetooth without user prompt
        if ((bluetoothAdapter != null) && (bluetoothAdapter.isEnabled()))
            bluetoothAdapter.disable();
    }

    public void connectToAddress(String address) {
        //Connect to a device from the address (a code like 34:FG:SR:95:45:62)
        if (!address.equals("null")) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            try {
                new ConnectThread(device).start();
            } catch (IOException e) {
                if (listener != null) listener.onBTError(e.getMessage());
            }
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
        try {
            new ConnectThread(device).start();
        } catch (IOException e) {
            if (listener != null) listener.onBTError(e.getMessage());
        }
    }

    public void disconnect() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            if (listener != null) {
                listener.onBTError(e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void send(String message) {
        try {
            output.write((message + "\n").getBytes());
        } catch (IOException e) {
            isConnected = false;
            if (listener != null) listener.onBTDisconnected(bluetoothDevice, e.getMessage());
        }
    }

    public List<BluetoothDevice> getPairedDevices() {
        //Get a list with all the paired devices
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
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

        void onBTConnected(BluetoothDevice device);

        void onBTDisconnected(BluetoothDevice device, String message);

        void onBTMessage(String message);

        void onBTError(String message);

        void onBTConnectError(BluetoothDevice device, String message);
    }

    private class ReceiveThread extends Thread {

        @Override
        public void run() {
            String message;
            try {
                while ((message = input.readLine()) != null) {
                    if (listener != null) listener.onBTMessage(message);
                }
            } catch (IOException e) {
                isConnected = false;
                if (listener != null)
                    listener.onBTDisconnected(bluetoothDevice, e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) throws IOException {
            BluetoothHelper.this.bluetoothDevice = device;
            BluetoothHelper.this.bluetoothSocket = device.createRfcommSocketToServiceRecord(DEFAULT_UUID);
        }

        @Override
        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();
                output = bluetoothSocket.getOutputStream();
                input = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                isConnected = true;
                new ReceiveThread().start();
                if (listener != null)
                    listener.onBTConnected(bluetoothDevice);
            } catch (IOException e) {
                if (listener != null)
                    listener.onBTConnectError(bluetoothDevice, e.getMessage());
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {

                    if (listener != null) {
                        listener.onBTError(closeException.getMessage());
                    }
                }
            }
        }
    }
}