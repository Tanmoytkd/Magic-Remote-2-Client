package com.example.tanmoykrishnadas.magicremoteclient;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tanmoy Krishna Das on 2/22/2018.
 */

class DeviceHolder extends RecyclerView.ViewHolder {
    View view;
    ImageView icon;
    TextView deviceName, deviceAddress;


    public DeviceHolder(View itemView) {
        super(itemView);
        view = itemView;
        icon = itemView.findViewById(R.id.icon);
        deviceName = itemView.findViewById(R.id.device_Name);
        deviceAddress = itemView.findViewById(R.id.device_address);
    }
}

class DeviceAdapter extends RecyclerView.Adapter<DeviceHolder> {
    ArrayList<BluetoothDevice> devices;
    MainActivity activity;

    public DeviceAdapter(ArrayList<BluetoothDevice> devices, MainActivity activity) {
        this.devices = devices;
        this.activity = activity;
    }


    @Override
    public DeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.each_device, parent, false);
        return new DeviceHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceHolder holder, int position) {
        BluetoothDevice device = devices.get(position);

        holder.deviceName.setText(device.getName());
        holder.deviceAddress.setText(device.getAddress());

        holder.view.setOnClickListener(e->{
            Log.d(activity.TAG, "Start pairing");
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(activity.TAG, "Starting to pair...");
                if(device.getBondState()!=BluetoothDevice.BOND_BONDED) {
                    device.createBond();
                } else {
                    activity.startConnection(device);
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                activity.registerReceiver(activity.bondingReceiver, filter);
                activity.bondingReceiverFlag=true;
            }
        });

    }

    @Override
    public int getItemCount() {
        return devices.size();
    }
}
