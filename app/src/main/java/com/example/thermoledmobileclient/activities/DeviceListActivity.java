package com.example.thermoledmobileclient.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.adapters.DeviceListAdapter;
import com.example.thermoledmobileclient.interfaces.RecyclerViewInterface;
import com.example.thermoledmobileclient.models.Device;

import java.util.ArrayList;

public class DeviceListActivity extends AppCompatActivity implements RecyclerViewInterface {

    ArrayList<Device> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        RecyclerView recyclerViewDeviceList = findViewById(R.id.devices_recycler_view);
        mockDeviceList();
        recyclerViewDeviceList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDeviceList.setAdapter(new DeviceListAdapter(this, this, deviceList));
    }

    private void mockDeviceList() {
        deviceList = new ArrayList<>();
        deviceList.add(new Device("Lampada", "Sala", 1));
        deviceList.add(new Device("Termostato", "Sala", 2));
        deviceList.add(new Device("Lampada", "Varanda", 1));
        deviceList.add(new Device("Temperatura", "Varanda", 2));
        deviceList.add(new Device("Umidade", "Varanda", 3));
        deviceList.add(new Device("Luminosidade", "Varanda", 4));
        deviceList.add(new Device("Lampada", "Quarto", 1));
        deviceList.add(new Device("Abajur", "Quarto", 1));
        deviceList.add(new Device("Temperatura", "Quarto", 2));
    }

    @Override
    public void onRecyclerViewItemClick(int position) {
        Intent intent = new Intent(DeviceListActivity.this, ActuatorActivity.class);
        intent.putExtra("NOME_DISPOSITIVO", deviceList.get(position).getName());
        intent.putExtra("LOCALIZACAO", deviceList.get(position).getLocation());
        intent.putExtra("TIPO_DISPOSITIVO", Integer.toString(deviceList.get(position).getType()));
        startActivity(intent);
    }
}