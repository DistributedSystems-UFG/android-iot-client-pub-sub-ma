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
import android.widget.Toast;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.models.GrpcConfig;
import com.example.thermoledmobileclient.models.SessaoClient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.iotservice.ParamLogin;
import io.grpc.examples.iotservice.SensorServiceGrpc;
import io.grpc.examples.iotservice.Sessao;

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
                /*
                changeToLastFunctionalityUsed();
                SessaoClient.setToken("asdqwdas");
                SessaoClient.setFuncionalidade("home");
                 */

                sendAuthenticationRequest();
            }
        });
    }

    private void sendAuthenticationRequest(){
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(passwordEdt.getWindowToken(), 0);
        loginBtn.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        new GrpcTaskAutenticar(this).execute(userEdt.getText().toString(), passwordEdt.getText().toString());
    }

    private void decidirProximaDelaComBaseNaUltimaAcessada(String result){
        progressBar.setVisibility(View.GONE);
        loginBtn.setEnabled(true);
        if(result.contains("Failed")){
            Toast.makeText(this, "Erro no login", Toast.LENGTH_LONG).show();
            return;
        }
        String[] dados = SessaoClient.getFuncionalidade().split("\\|");
        switch (dados[0].toLowerCase()){
            case "home":
            case "listardispositivos":
                abrirActivityListagemDispositivos();
                break;
            case "atuador":
                abrirActivityDeAtuador(dados[1], dados[2], Integer.parseInt(dados[3]));
                break;
            case "sensor":
                abrirActivityDeSensor(dados[1], dados[2], Integer.parseInt(dados[3]));
                break;
        }
    }

    private void abrirActivityListagemDispositivos(){
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivity(intent);
    }

    private void abrirActivityDeAtuador(String localizacao, String nome, int tipo) {
        Intent intent = new Intent(this, ActuatorActivity.class);
        intent.putExtra("NOME_DISPOSITIVO", nome);
        intent.putExtra("LOCALIZACAO", localizacao);
        intent.putExtra("TIPO_DISPOSITIVO", tipo);
        startActivity(intent);
    }

    private void abrirActivityDeSensor(String localizacao, String nome, int tipo) {
        Intent intent = new Intent(this, SensorActivity.class);
        intent.putExtra("NOME_DISPOSITIVO", nome);
        intent.putExtra("LOCALIZACAO", localizacao);
        intent.putExtra("TIPO_DISPOSITIVO", tipo);
        startActivity(intent);
    }

    private static class GrpcTaskAutenticar extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTaskAutenticar(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                ParamLogin paramLogin = ParamLogin.newBuilder().setUsuario(username).setSenha(password).build();
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
                ((LoginActivity) activity).decidirProximaDelaComBaseNaUltimaAcessada(result);
            }
        }
    }
}