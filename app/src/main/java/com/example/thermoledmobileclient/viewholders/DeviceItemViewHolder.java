package com.example.thermoledmobileclient.viewholders;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.interfaces.RecyclerViewInterface;

public class DeviceItemViewHolder extends RecyclerView.ViewHolder {
    public TextView deviceName;
    public TextView deviceLocation;

    public DeviceItemViewHolder(@NonNull View itemView, RecyclerViewInterface recyclerViewInterface) {
        super(itemView);
        deviceName = itemView.findViewById(R.id.tv_device_name);
        deviceLocation = itemView.findViewById(R.id.tv_device_location);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = getBindingAdapterPosition();

                if (position!= RecyclerView.NO_POSITION) {
                    recyclerViewInterface.onRecyclerViewItemClick(position);
                }
            }
        });
    }
}
