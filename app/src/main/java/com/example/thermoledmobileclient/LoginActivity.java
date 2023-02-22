package com.example.thermoledmobileclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity {
    private EditText userEdt;
    private EditText passwordEdt;
    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userEdt = (EditText) findViewById(R.id.et_user);
        passwordEdt = (EditText) findViewById(R.id.et_password);
        loginBtn = (Button) findViewById(R.id.bt_login);

        //loginBtn.setOnClickListener();
    }
}