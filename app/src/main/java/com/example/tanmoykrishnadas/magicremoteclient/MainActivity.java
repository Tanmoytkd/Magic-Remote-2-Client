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
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();

    public static BluetoothConnectionService bluetoothConnection;

    RecyclerView deviceList;
    public volatile boolean bondingReceiverFlag;
    private volatile boolean deviceFoundReceiverFlag;
    private volatile boolean BTStatusReceiverFlag;
    private volatile boolean previouslyEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = findViewById(R.id.deviceList);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        previouslyEnabled = bluetoothAdapter.isEnabled();
        bluetoothConnection = BluetoothConnectionService.getInstance(MainActivity.this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothConnection != null) bluetoothConnection.setContext(MainActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BTStatusReceiverFlag) unregisterReceiver(BTStatusReceiver);
        if (deviceFoundReceiverFlag) unregisterReceiver(deviceInfoReceiver);
        if (bondingReceiverFlag) unregisterReceiver(bondingReceiver);
        if (!previouslyEnabled) bluetoothAdapter.disable();
    }

    public void goToKeyBoard(View v) {
        startActivity(new Intent(MainActivity.this, KeyboardActivity.class));
    }

    public void goToMouse(View v) {
        startActivity(new Intent(MainActivity.this, MouseActivity.class));
    }

    public void startConnection(BluetoothDevice remoteDevice) {
        bluetoothConnection.startClient(remoteDevice, UUID.fromString(Constants.UUID));
    }

    public final BroadcastReceiver bondingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");

                    startConnection(mDevice);
                }
                //case2: creating a bone
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
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
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
                devices.add(device);
                deviceList.setAdapter(new DeviceAdapter(devices, MainActivity.this));

//                device.fetchUuidsWithSdp();
                Log.d(TAG, "Device Found: " + device.getName() + " : " + device.getAddress());
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("connected");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            } else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("disconnected");
                bluetoothConnection.stopClient();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                bluetoothConnection.setBluetoothStatus("disconnected");
            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
//                BluetoothDevice deviceExtra = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
//                Log.d( TAG,"Current Device: "+ deviceExtra.getName() + "(" + deviceExtra.getAddress() + ")");
//                if (uuidExtra != null) {
//                    for (Parcelable p : uuidExtra) {
//                        String currentDeviceUUID = p.toString();
//                        Log.d(TAG, "uuidExtra - " + p);
//                        if (currentDeviceUUID.contains(Constants.foreignUUID)) {
//                            devices.add(deviceExtra);
//                            Set<BluetoothDevice> allDevices = new TreeSet<>(new Comparator<BluetoothDevice>() {
//                                @Override
//                                public int compare(BluetoothDevice device, BluetoothDevice t1) {
//                                    String s1 = device.getName() + " " + device.getAddress();
//                                    String s2 = t1.getName() + " " + t1.getAddress();
//                                    return s1.compareTo(s2);
//                                }
//                            });
//                            allDevices.addAll(devices);
//                            devices = new ArrayList<>(allDevices);
//
//                            deviceList.setAdapter(new DeviceAdapter(devices, MainActivity.this));
//                            break;
//                        }
//                    }
//                } else {
//                    System.out.println("uuidExtra is still null");
//                }
            }

        }
    };

    public void disconnect(View view) {
        bluetoothConnection.stopClient();
    }

    public void enableDisableBT(View view) {
        Log.d(TAG, "Toggle Bluetooth");
        enableDisableBT();
    }

    public void enableDisableBT() {
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

    public void scanDevices(View view) {
        Log.d(TAG, "Device Scan started...");
        scanDevices();
    }

    public void scanDevices() {
        if (bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();

        devices = new ArrayList<>();
        if (deviceList.getAdapter() != null) deviceList.getAdapter().notifyDataSetChanged();
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
