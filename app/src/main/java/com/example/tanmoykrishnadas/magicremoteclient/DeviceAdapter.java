package com.example.tanmoykrishnadas.magicremoteclient;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tanmoy Krishna Das on 2/22/2018.
 * This piece of code is part of the project "Magic Remote 2 Client"
 */

class DeviceHolder extends RecyclerView.ViewHolder {
    View view;
    private ImageView icon;
    TextView deviceName, deviceAddress;


    DeviceHolder(View itemView) {
        super(itemView);
        view = itemView;
        icon = itemView.findViewById(R.id.icon);
        deviceName = itemView.findViewById(R.id.device_Name);
        deviceAddress = itemView.findViewById(R.id.device_address);
    }
}

class DeviceAdapter extends RecyclerView.Adapter<DeviceHolder> {
    private static final String TAG = "DeviceAdapter";
    private ArrayList<BluetoothDevice> devices;
    private Activity activity;
    private MainActivity mainActivity;
    private HomeActivity homeActivity;

    DeviceAdapter(ArrayList<BluetoothDevice> devices, MainActivity activity) {
        this.devices = devices;
        this.activity = activity;
        this.mainActivity = activity;
    }

    DeviceAdapter(ArrayList<BluetoothDevice> devices, HomeActivity homeActivity) {
        this.devices = devices;
        this.activity = homeActivity;
        this.homeActivity = homeActivity;
    }


    @NonNull
    @Override
    public DeviceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.each_device, parent, false);
        return new DeviceHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceHolder holder, int position) {
        BluetoothDevice device = devices.get(position);

        holder.deviceName.setText(device.getName());
        holder.deviceAddress.setText(device.getAddress());

        holder.view.setOnClickListener(e -> {
            Log.d(TAG, "Start pairing");
            Log.d(TAG, "Starting to pair...");
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                device.createBond();
            } else {
                if (homeActivity != null) homeActivity.startConnection(device);
                else if (mainActivity != null) mainActivity.startConnection(device);
            }
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

            if (homeActivity != null) {
                activity.registerReceiver(homeActivity.bondingReceiver, filter);
                homeActivity.bondingReceiverFlag = true;
            } else if (mainActivity != null) {
                activity.registerReceiver(homeActivity.bondingReceiver, filter);
                homeActivity.bondingReceiverFlag = true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
