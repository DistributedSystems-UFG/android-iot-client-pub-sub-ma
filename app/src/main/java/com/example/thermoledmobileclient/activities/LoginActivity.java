package com.example.thermoledmobileclient.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.models.SessaoClient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.sensorservice.ParamLogin;
import io.grpc.examples.sensorservice.SensorServiceGrpc;
import io.grpc.examples.sensorservice.Sessao;

public class LoginActivity extends AppCompatActivity {
    private static final String HOST = "146.148.42.190";
    private static final int PORT = 50051;
    private EditText userEdt;
    private EditText passwordEdt;
    private Button loginBtn;
    private ProgressBar progressBar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        userEdt = (EditText) findViewById(R.id.et_user);
        passwordEdt = (EditText) findViewById(R.id.et_password);
        loginBtn = (Button) findViewById(R.id.bt_login);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar_login);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendAuthenticationRequest();
                changeToLastFunctionalityUsed();
            }
        });
    }

    private void sendAuthenticationRequest(){
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(passwordEdt.getWindowToken(), 0);
        loginBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new GrpcTask(this).execute(userEdt.getText().toString(), passwordEdt.getText().toString());
    }

    private void changeToLastFunctionalityUsed(){
        progressBar.setVisibility(View.GONE);
        //TODO: implementar metodo para decidir qual activitya acessar após o login
        Intent intent = new Intent(this, DeviceListActivity.class);
        //String message = editText1.getText().toString() + " " + editText2.getText().toString();
        //intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    private static class GrpcTask extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTask(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            try {
                channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                ParamLogin paramLogin = ParamLogin.newBuilder().setUsuario(username).setSenha(password).build();
                //stub = MetadataUtils.attachHeaders(stub, new Metadata());
                //stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor())
                Sessao resposta = stub.autenticarUsuario(paramLogin);

                SessaoClient.setToken(resposta.getToken());
                SessaoClient.setFuncionalidade(resposta.getFuncionalidade());

                return "sucesso";

            } catch (Exception e){
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            if (activity instanceof LoginActivity) {
                ((LoginActivity) activity).changeToLastFunctionalityUsed();
            }
        }
    }
}