package com.example.tanmoykrishnadas.magicremoteclient;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Tanmoy Krishna Das on 2/16/2018.
 */

public class BluetoothConnectionService {
    public static final String TAG = "BluetoothService";

    private static final UUID MY_UUID_INSECURE = UUID.fromString(Constants.UUID);

    private final BluetoothAdapter bluetoothAdapter;
    Context context;

    private ClientConnectThread clientConnectThread;
    private ClientConnectedThread clientConnectedThread;
    private BluetoothDevice device;
    private UUID deviceUUID;
    private ProgressDialog progressDialogeBox;

    public BluetoothConnectionService(Context context) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
    }

    private class ClientConnectThread extends Thread {
        private BluetoothSocket clientSocket;

        public ClientConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "Connect Thread initialized...");
            BluetoothConnectionService.this.device = device;
            BluetoothConnectionService.this.deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "Run Connect Thread");
            try {
                //create a client clientSocket
                tmp = device.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            clientSocket = tmp; //the main clientSocket is client clientSocket now
            bluetoothAdapter.cancelDiscovery(); //because client is not needed to be discovered

            try {
                clientSocket.connect(); //connect clientSocket
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    clientSocket.close(); //close clientSocket on failure to connect
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            connected(clientSocket, device);
        }

        public void cancel() {
            Log.d(TAG, "Closing Client Socket");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to close Client Socket");
            }
        }
    }

//    public synchronized void start() {
//        Log.d(TAG, "start");
//        if (clientConnectThread != null) {
//            clientConnectThread.cancel();
//            clientConnectThread = null;
//        }
//
//    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "statClient: Started");
        progressDialogeBox = ProgressDialog.show(context, "Connecting Bluetooth", "Please wait...", true);
        clientConnectThread = new ClientConnectThread(device, uuid); //create a new thread for connection
        clientConnectThread.start();
    }

    private class ClientConnectedThread extends Thread {

        private final BluetoothSocket clientBluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ClientConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ClientConnectedThread Started");
            clientBluetoothSocket = socket;
            InputStream tempI = null;
            OutputStream tempO = null;

            /// dismiss progressDialogBox
            progressDialogeBox.dismiss();

            try {
                tempI = clientBluetoothSocket.getInputStream();
                tempO = clientBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempI;
            outputStream = tempO;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "Input Stream: " + incomingMessage);
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: writing to outputStream " + text);
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to outputStream");
            }
        }

        public void cancel() {
            try {
                clientBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket clientSocket, BluetoothDevice device) {
        Log.d(TAG, "Connected method starting...");

        clientConnectedThread = new ClientConnectedThread(clientSocket);
        clientConnectedThread.start();
    }

    public void write(byte[] out) {
        Log.d(TAG, "Write called");
        clientConnectedThread.write(out);
    }
}