package com.example.tanmoykrishnadas.magicremoteclient;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tanmoykrishnadas.magicremoteclient.backend.BluetoothConnectionService;
import com.example.tanmoykrishnadas.magicremoteclient.backend.Constants;

import java.util.ArrayList;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class HomeActivity extends AppCompatActivity {
    public static String TAG = "HomeActivity";
    public static boolean searching = false;

    public static void setSearching(boolean searching) {
        HomeActivity.searching = searching;
    }

    public static boolean isSearching() {
        return searching;
    }

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();

    public BluetoothConnectionService bluetoothConnection;

    RecyclerView deviceList;
    public volatile boolean bondingReceiverFlag;
    private volatile boolean deviceFoundReceiverFlag;
    private volatile boolean BTStatusReceiverFlag;
    private volatile boolean bluetoothPreviouslyEnabled;

    Animation ringRotate;
    FrameLayout connectButtonFrame;
    TextView connectionStatus;
    ImageView ring;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        deviceList = findViewById(R.id.deviceList);
        connectButtonFrame = findViewById(R.id.connectButtonFrame);
        connectionStatus = findViewById(R.id.connectionStatus);
        ring = findViewById(R.id.ring);
        ringRotate = AnimationUtils.loadAnimation(HomeActivity.this, R.anim.rotate);

        doBluetoothAdapterSetup();
        enableBT();

        connectButtonFrame.setOnClickListener(v -> {
            if (!searching && !bluetoothConnection.getBluetoothStatus().equals("connected")) {
                scanDevices();
                searching = true;
                Thread searchingNotifier = new Thread(() -> {
                    while (searching) {
                        try {
                            if (bluetoothConnection.getBluetoothStatus().equals("connected")) {
                                searching = false;
                            }
                            sleep(60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //when searching completed
                    runOnUiThread(()->{
                        if(ringRotate!=null) {
                            if(ring!=null) ring.clearAnimation();
                            ringRotate.cancel();
                            ringRotate.reset();
                        }
                    });

                });
                searchingNotifier.start();

                connectionStatus.setText("Searching");
                ring.startAnimation(ringRotate);
            } else {
                connectionStatus.setText("Connect");
                searching = false;
                disconnect();
                stopScanning();
            }
        });
    }

    private void doBluetoothAdapterSetup() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            bluetoothPreviouslyEnabled = bluetoothAdapter.isEnabled();
            bluetoothConnection = BluetoothConnectionService.getInstance(HomeActivity.this);
        } else {
            AlertDialog noBluetoothDialog = new AlertDialog.Builder(this).setTitle("Bluetooth Unavailable").setMessage("Sorry, your device does not support bluetooth. Our app can not work here").setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            }).create();
            noBluetoothDialog.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothConnection != null) bluetoothConnection.setContext(HomeActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
        searching = false;
        if (BTStatusReceiverFlag) unregisterReceiver(BTStatusReceiver);
        if (deviceFoundReceiverFlag) unregisterReceiver(deviceInfoReceiver);
        if (bondingReceiverFlag) unregisterReceiver(bondingReceiver);
        if (!bluetoothPreviouslyEnabled && bluetoothAdapter != null) bluetoothAdapter.disable();
    }

    public void startConnection(BluetoothDevice remoteDevice) {
        bluetoothConnection.startClient(remoteDevice, UUID.fromString(Constants.UUID));
    }

    public final BroadcastReceiver bondingReceiver = new BroadcastReceiver() {
        String action;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();

            if (action != null && action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    startConnection(mDevice);
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    private final BroadcastReceiver BTStatusReceiver = new BroadcastReceiver() {
        String action;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "Bluetooth OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "Bluetooth Turning Off");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "Bluetooth On");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "Bluetooth Turning On");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver deviceInfoReceiver = new BroadcastReceiver() {
        String action;
        BluetoothDevice device;

        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(HomeActivity.this,
                        "Device Found: " + device.getName() + " : " + device.getAddress(), Toast.LENGTH_LONG).show();

                devices.add(device);
                deviceList.setAdapter(new DeviceAdapter(devices, HomeActivity.this));
                Log.d(TAG, "Device Found: " + device.getName() + " : " + device.getAddress());
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("connected");
                searching = false;
                connectionStatus.setText("Disconnect");
                devices = new ArrayList<>();
                deviceList.setAdapter(new DeviceAdapter(devices, HomeActivity.this));
                stopScanning();

                new Thread(()->{
                    while(bluetoothConnection.getBluetoothStatus().equals("connected")) {
                        try {
                            sleep(90);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(()->connectionStatus.setText("Connect"));
                }).start();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "Search Completed");
                searching = false;
                if(!connectionStatus.getText().toString().equals("Disconnect")) {
                    connectionStatus.setText("Connect");
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("disconnected");
                bluetoothConnection.stopClient();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("disconnected");
            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {

            }
        }
    };

    public void disconnect() {
        bluetoothConnection.stopClient();
    }

    public void enableBT() {
        if (!bluetoothAdapter.isEnabled()) {
            toogleBluetooth();
        }
    }

    public void toogleBluetooth() {
        if (bluetoothAdapter == null) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(this);
            }
            builder.setTitle("Bluetooth unavailable...")
                    .setMessage("Bluetooth is not available in this device. This app will not be able to work here.")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })

                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableIntent);

            IntentFilter BTIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(BTStatusReceiver, BTIntentFilter);
            BTStatusReceiverFlag = true;
        } else if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();

            IntentFilter BTIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(BTStatusReceiver, BTIntentFilter);
            BTStatusReceiverFlag = true;
        }
    }

    public void stopScanning() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
    }

    public void scanDevices() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

        devices = new ArrayList<>();
        deviceList.setAdapter(new DeviceAdapter(devices, HomeActivity.this));
        bluetoothAdapter.startDiscovery();

        checkBluetoothPermission();

        IntentFilter deviceFoundFilter = new IntentFilter();
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_FOUND);
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        deviceFoundFilter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(deviceInfoReceiver, deviceFoundFilter);
        deviceFoundReceiverFlag = true;
    }

    public void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT > 23) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 218);
            }

        } else {
            Log.d(TAG, "Bluetooth Permission Check: No need to check permission, SDK version < LOLLIPOP");
        }
    }
}
