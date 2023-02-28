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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.models.Device;
import com.example.thermoledmobileclient.models.GrpcConfig;
import com.example.thermoledmobileclient.models.SessaoClient;
import com.example.thermoledmobileclient.models.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.examples.iotservice.LedStatus;
import io.grpc.examples.iotservice.ListaLedStatus;
import io.grpc.examples.iotservice.SensorServiceGrpc;
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
        String location = intent.getStringExtra("LOCALIZACAO");
        int type = intent.getIntExtra("TIPO_DISPOSITIVO", 0);
        dispositivo = new Device(name,location,type);

        String titulo= dispositivo.getLocation() + ": " + dispositivo.getName();
        tituloAtuador.setText(StringUtils.capitalizeEachWord(titulo));

        consultarEstadoAtuador();

        btnAtuador.setOnClickListener(v -> {
            if (ultimoEstadoAtuador == 1) {
                alterarEstadoAtuador(0);
            } else {
                alterarEstadoAtuador(1);
            }
        });

        ImageButton btnVoltar = findViewById(R.id.btn_home_atuadores);
        btnVoltar.setOnClickListener(v -> abrirActivityListagemDispositivos());
    }

    private void consultarEstadoAtuador() {
        //if(ultimoEstadoAtuador != 0) ultimoEstadoAtuador = 0;
        //aplicarEstadoAoAtuador(Integer.toString(ultimoEstadoAtuador));
        new GrpcTaskConsultaEstadoAtuador(this).execute(dispositivo.getLocation(), dispositivo.getName(), definirStringDeFuncionalidade());
    }

    private void alterarEstadoAtuador(int novoEstado) {
        //aplicarEstadoAoAtuador(Integer.toString(novoEstado));
        new GrpcTaskAlterarEstadoAtuador(this).execute(dispositivo.getLocation(), dispositivo.getName(), definirStringDeFuncionalidade(), Integer.toString(novoEstado));
    }

    private String definirStringDeFuncionalidade(){
        return "atuador|"+dispositivo.getLocation()+"|"+dispositivo.getName()+"|"+dispositivo.getType();
    }

    @SuppressLint("SetTextI18n")
    public void aplicarEstadoAoAtuador(String estado) {
        try {
            if (estado.equals("1")) {
                btnAtuador.setBackgroundColor(getResources().getColor(R.color.red));
                btnAtuador.setText("Desligar");
                imagenAtuador.setImageResource(R.drawable.ic_baseline_lightbulb_240_on);
            } else if (estado.equals("0")) {
                btnAtuador.setBackgroundColor(getResources().getColor(R.color.green));
                btnAtuador.setText("Ligar");
                imagenAtuador.setImageResource(R.drawable.ic_baseline_lightbulb_240_off);
            }
            ultimoEstadoAtuador = Integer.parseInt(estado);
        }catch (Exception e){
            Toast.makeText(this, "Erro na requisição", Toast.LENGTH_LONG).show();
        }
    }

    private void abrirActivityListagemDispositivos(){
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivity(intent);
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
            String funcionalidade = params[2];

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), funcionalidade);

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
                ((ActuatorActivity) activity).aplicarEstadoAoAtuador(result);
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
            String funcionalidade = params[2];
            int estado = Integer.parseInt(params[3]);

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), funcionalidade);

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                LedStatus ledStatus = LedStatus.newBuilder().setLocalizacao(localizacao).setNomeDispositivo(nomeDispositivo).setEstado(estado).build();
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
                ((ActuatorActivity) activity).aplicarEstadoAoAtuador(result);
            }
        }
    }
}
