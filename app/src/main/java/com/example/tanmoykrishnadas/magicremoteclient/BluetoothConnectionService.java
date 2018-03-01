package com.example.tanmoykrishnadas.magicremoteclient;

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
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by Tanmoy Krishna Das on 2/16/2018.
 */

public class BluetoothConnectionService {
    class startConnectionThread extends Thread {
        private BluetoothSocket clientSocket;

        public startConnectionThread(BluetoothDevice RemoteDevice, UUID uuid) {
            BluetoothConnectionService.this.Remotedevice = RemoteDevice;
            BluetoothConnectionService.this.deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
//            Log.i(TAG, "Run Connect Thread");
            bluetoothStatus = "connecting";
            try {
                //create a client clientSocket
                tmp = Remotedevice.createRfcommSocketToServiceRecord(deviceUUID);

                clientSocket = tmp; //the main clientSocket is client clientSocket now
                bluetoothAdapter.cancelDiscovery(); //because client is not needed to be discovered

                try {
                    clientSocket.connect(); //connect clientSocket
                    bluetoothStatus = "connected";
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        clientSocket.close(); //close clientSocket on failure to connect
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        bluetoothStatus = "disconnected";
                    }
                }
                onConnected(clientSocket, Remotedevice);
            } catch (IOException e) {
                e.printStackTrace();
                bluetoothStatus = "disconnected";
            }
        }

        public void disconnect() {
            //Log.d(TAG, "Closing Client Socket");
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to close Client Socket");
            } finally {
                bluetoothStatus = "disconnected";
            }
        }
    }

    class ReadWriteThread extends Thread {

        private final BluetoothSocket clientBluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ReadWriteThread(BluetoothSocket socket) {
            //Log.d(TAG, "ReadWriteThread Started");
            clientBluetoothSocket = socket;
            InputStream tempI = null;
            OutputStream tempO = null;

            /// dismiss progressDialogBox
            progressDialogeBox.dismiss();

            ((Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Connection Successful", Toast.LENGTH_LONG).show();
            });

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

                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.e(TAG, "Input Stream: " + incomingMessage);
                } catch (IOException e) {
                    bluetoothStatus = "disconnected";
                    stopClient();
                    cancel();
                    e.printStackTrace();
                    break;
                }
            }

            Log.e(TAG, "Input Aborted");
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            //Log.d(TAG, "write: writing to outputStream " + text);
            try {
                outputStream.write(bytes);
                outputStream.flush();
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

    private static BluetoothConnectionService instance = new BluetoothConnectionService();

    public static final String TAG = "BluetoothService";
    private static final UUID MY_UUID_INSECURE = UUID.fromString(Constants.UUID);
    private static String bluetoothStatus = "disconnected";

    public static void setBluetoothStatus(String bluetoothStatus) {
        BluetoothConnectionService.bluetoothStatus = bluetoothStatus;
    }

    public static String getBluetoothStatus() {
        return bluetoothStatus;
    }

    private final BluetoothAdapter bluetoothAdapter;
    Context context;

    private startConnectionThread startConnectionThread;
    private ReadWriteThread readWriteThread;
    private BluetoothDevice Remotedevice;
    private UUID deviceUUID;
    private ProgressDialog progressDialogeBox;

    private BluetoothConnectionService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static void setContext(Context context) {
        instance.context = context;
    }

    public static BluetoothConnectionService getInstance(Context context) {
        setContext(context);
        return instance;
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        progressDialogeBox = ProgressDialog.show(context, "Connecting Bluetooth", "Please wait...", true);
        startConnectionThread = new startConnectionThread(device, uuid); //create a new thread for connection
        startConnectionThread.start();
    }

    public void stopClient() {
        bluetoothStatus = "disconncted";
        if(startConnectionThread!=null) {
            startConnectionThread.disconnect();
        }
    }

    private void onConnected(BluetoothSocket clientSocket, BluetoothDevice device) {
        readWriteThread = new ReadWriteThread(clientSocket);
        readWriteThread.start();
    }
    
    public void write(byte[] msg) {
        readWriteThread.write(msg);
    }
}