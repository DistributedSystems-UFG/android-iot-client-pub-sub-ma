package com.example.thermoledmobileclient.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.models.Device;
import com.example.thermoledmobileclient.viewholders.DeviceItemViewHolder;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceItemViewHolder> {

    Context context;
    List<Device> deviceList;

    public DeviceListAdapter(Context context, List<Device> devices) {
        this.context = context;
        this.deviceList = devices;
    }

    @NonNull
    @Override
    public DeviceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DeviceItemViewHolder(LayoutInflater.from(context).inflate(R.layout.device_item_view,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceItemViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceLocation.setText(device.getLocation());
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}
