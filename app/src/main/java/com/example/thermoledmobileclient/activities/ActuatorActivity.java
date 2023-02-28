package com.example.thermoledmobileclient.activities;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.models.Device;
import com.example.thermoledmobileclient.models.GrpcConfig;
import com.example.thermoledmobileclient.models.SessaoClient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.examples.sensorservice.LedStatus;
import io.grpc.examples.sensorservice.ListaLedStatus;
import io.grpc.examples.sensorservice.ParamLogin;
import io.grpc.examples.sensorservice.SensorServiceGrpc;
import io.grpc.examples.sensorservice.Sessao;
import io.grpc.stub.MetadataUtils;

public class ActuatorActivity extends AppCompatActivity {

    private Device dispositivo;
    private ImageView imagenAtuador;
    private Button btnAtuador;
    private int ultimoEstadoAtuador;

    @SuppressLint({"MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_actuator);

        TextView tituloAtuador = (TextView) findViewById(R.id.tv_atuador_titulo);
        imagenAtuador = (ImageView) findViewById(R.id.iv_lightbulb);
        btnAtuador = (Button) findViewById(R.id.bt_on_off_lightbulb);

        //buscar os dados do sensor que recebe pelo intent
        Intent intent = getIntent();

        String name = intent.getStringExtra("NOME_DISPOSITIVO");
        String location = intent.getStringExtra("LOCALIZACAO_DISPOSITIVO");
        String type = intent.getStringExtra("TIPO_DISPOSITIVO");

        dispositivo = new Device(name,location,Integer.parseInt(type));
        String titulo= dispositivo.getLocation() + ": " + dispositivo.getName();
        tituloAtuador.setText(titulo);

        consultarEstadoAtuador();

        btnAtuador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ultimoEstadoAtuador == 1) {
                    alterarEstadoAtuador(0);
                } else {
                    alterarEstadoAtuador(1);
                }
            }
        });
    }

    private void consultarEstadoAtuador() {
        new GrpcTaskConsultaEstadoAtuador(this).execute(dispositivo.getLocation(), dispositivo.getName());
    }

    private void alterarEstadoAtuador(int novoEstado) {
        new GrpcTaskAlterarEstadoAtuador(this).execute(dispositivo.getLocation(), dispositivo.getName(), Integer.toString(novoEstado));
    }

    @SuppressLint("SetTextI18n")
    public void aplicarEstadoAoAtuador(int estado) {
        if (estado == 1) {
            btnAtuador.setBackgroundColor(getResources().getColor(R.color.red));
            btnAtuador.setText("Turn Off");
            imagenAtuador.setImageResource(R.drawable.ic_baseline_lightbulb_240_on);
        } else {
            btnAtuador.setBackgroundColor(getResources().getColor(R.color.green));
            btnAtuador.setText("Turn On");
            imagenAtuador.setImageResource(R.drawable.ic_baseline_lightbulb_240_off);
        }

        ultimoEstadoAtuador = estado;
    }

    private static class GrpcTaskConsultaEstadoAtuador extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTaskConsultaEstadoAtuador(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String localizacao = params[0];
            String nomeDispositivo = params[1];

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), "atuador|" + localizacao + "|" + nomeDispositivo);

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                LedStatus ledStatus = LedStatus.newBuilder().setLocalizacao(localizacao).setNomeDispositivo(nomeDispositivo).build();
                stub = MetadataUtils.attachHeaders(stub, metadata);
                ListaLedStatus resposta = stub.listarLeds(ledStatus);

                return Integer.toString(resposta.getStatus(0).getEstado());

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
            if (activity instanceof ActuatorActivity) {
                ((ActuatorActivity) activity).aplicarEstadoAoAtuador(Integer.parseInt(result));
            }
        }
    }

    private static class GrpcTaskAlterarEstadoAtuador extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTaskAlterarEstadoAtuador(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String localizacao = params[0];
            String nomeDispositivo = params[1];

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), "atuador|" + localizacao + "|" + nomeDispositivo);

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                LedStatus ledStatus = LedStatus.newBuilder().setLocalizacao(localizacao).setNomeDispositivo(nomeDispositivo).build();
                stub = MetadataUtils.attachHeaders(stub, metadata);
                LedStatus resposta = stub.acionarLed(ledStatus);

                return Integer.toString(resposta.getEstado());

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
            if (activity instanceof ActuatorActivity) {
                ((ActuatorActivity) activity).aplicarEstadoAoAtuador(Integer.parseInt(result));
            }
        }
    }
}
