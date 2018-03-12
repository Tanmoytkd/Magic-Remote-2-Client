package com.example.tanmoykrishnadas.magicremoteclient.backend;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by Tanmoy Krishna Das on 2/16/2018.
 * This piece of code is part of the project "Magic Remote 2 Client"
 */

public class BluetoothConnectionService {

    private static BluetoothConnectionService instance = new BluetoothConnectionService();

    private static final String TAG = "BluetoothService";
    //    private static final UUID MY_UUID_INSECURE = UUID.fromString(Constants.UUID);
    private String bluetoothStatus = "disconnected";

    private final BluetoothAdapter bluetoothAdapter;
//    private Context context;

    private startConnectionThread startConnectionThread;
    private ReadWriteThread readWriteThread;
    private BluetoothDevice RemoteDevice;
    private UUID deviceUUID;
    private long lastMessageReceiveTime = Calendar.getInstance().getTimeInMillis();
//    private ProgressDialog progressDialogBox;

    public boolean isConnected() {
        return bluetoothStatus.equals("connected");
    }

    public long getLastMessageReceiveTime() {
        return lastMessageReceiveTime;
    }

    class startConnectionThread extends Thread {
        private BluetoothSocket clientSocket;

        startConnectionThread(BluetoothDevice RemoteDevice, UUID uuid) {
            BluetoothConnectionService.this.RemoteDevice = RemoteDevice;
            BluetoothConnectionService.this.deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp;
//            Log.i(TAG, "Run Connect Thread");
            setBluetoothStatus("connecting");
            try {
                //create a client clientSocket
                tmp = RemoteDevice.createRfcommSocketToServiceRecord(deviceUUID);

                clientSocket = tmp; //the main clientSocket is client clientSocket now
                bluetoothAdapter.cancelDiscovery(); //because client is not needed to be discovered

                try {
                    clientSocket.connect(); //connect clientSocket
                    setBluetoothStatus("connected");
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close(); //close clientSocket on failure to connect
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        setBluetoothStatus("disconnected");
                    }
                }
                onConnected(clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
                setBluetoothStatus("disconnected");
            }
        }

        void disconnect() {
            //Log.d(TAG, "Closing Client Socket");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to close Client Socket");
            } finally {
                setBluetoothStatus("disconnected");
            }
        }
    }

    class ReadWriteThread extends Thread {

        private final BluetoothSocket clientBluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        ReadWriteThread(BluetoothSocket socket) {
            //Log.d(TAG, "ReadWriteThread Started");
            clientBluetoothSocket = socket;
            InputStream tempI = null;
            OutputStream tempO = null;

            /// dismiss progressDialogBox
            //progressDialogBox.dismiss();

            //((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Connection Successful", Toast.LENGTH_LONG).show());

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

            while (bluetoothStatus.equals("connected")) {
                try {
                    Log.e(TAG, "Trying to read input stream");
                    bytes = inputStream.read(buffer);

                    lastMessageReceiveTime = Calendar.getInstance().getTimeInMillis();
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.e(TAG, "Input Stream: " + incomingMessage);
                } catch (IOException e) {
                    setBluetoothStatus("disconnected");
                    stopClient();
                    cancel();
                    e.printStackTrace();
                    break;
                }
            }

            Log.e(TAG, "Input Aborted");
        }

        void write(byte[] bytes) {
//            String text = new String(bytes, Charset.defaultCharset());
            //Log.d(TAG, "write: writing to outputStream " + text);
            try {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error writing to outputStream");
            }
        }


        void cancel() {
            try {
                clientBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBluetoothStatus(String bluetoothStatus) {
        this.bluetoothStatus = bluetoothStatus;
    }

    public String getBluetoothStatus() {
        return bluetoothStatus;
    }

    private BluetoothConnectionService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

//    public void setContext(Context context) {
//        instance.context = context;
//    }

    public static BluetoothConnectionService getInstance() {
        //instance.setContext(context);
        return instance;
    }


    public void startClient(BluetoothDevice device, UUID uuid) {
        //progressDialogBox = ProgressDialog.show(context, "Connecting Bluetooth", "Please wait...", true);
        startConnectionThread = new startConnectionThread(device, uuid); //create a new thread for connection
        startConnectionThread.start();
    }

    public void stopClient() {
        setBluetoothStatus("disconncted");
        if (startConnectionThread != null) {
            startConnectionThread.disconnect();
        }
    }

    private void onConnected(BluetoothSocket clientSocket) {
        readWriteThread = new ReadWriteThread(clientSocket);
        readWriteThread.start();
    }

    public void write(byte[] msg) {
        readWriteThread.write(msg);
    }
}