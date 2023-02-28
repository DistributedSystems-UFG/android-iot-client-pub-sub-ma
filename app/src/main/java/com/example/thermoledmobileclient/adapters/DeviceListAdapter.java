package com.example.thermoledmobileclient.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.interfaces.RecyclerViewInterface;
import com.example.thermoledmobileclient.models.Device;
import com.example.thermoledmobileclient.models.StringUtils;
import com.example.thermoledmobileclient.viewholders.DeviceItemViewHolder;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceItemViewHolder> {

    private final RecyclerViewInterface recyclerViewInterface;
    public Context context;
    public List<Device> deviceList;

    public DeviceListAdapter(RecyclerViewInterface recyclerViewInterface, Context context, List<Device> devices) {
        this.recyclerViewInterface = recyclerViewInterface;
        this.context = context;
        this.deviceList = devices;
    }

    @NonNull
    @Override
    public DeviceItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.device_item_view, parent, false);
        return new DeviceItemViewHolder(view, recyclerViewInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceItemViewHolder holder, int position) {
        Device device = deviceList.get(position);
        holder.deviceName.setText(StringUtils.capitalizeEachWord(device.getName()));
        holder.deviceLocation.setText(StringUtils.capitalizeEachWord(device.getLocation()));
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }
}
