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
import android.widget.TextView;
import android.widget.Toast;

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
import io.grpc.examples.iotservice.Dado;
import io.grpc.examples.iotservice.Parametros;
import io.grpc.examples.iotservice.SensorServiceGrpc;
import io.grpc.stub.MetadataUtils;

public class SensorActivity extends AppCompatActivity {

    private Device dispositivo;
    TextView tvDadoSensor, tvDataMedicao, tvUnidadeMedida;
    Button btnAtualizar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        tvDadoSensor = (TextView) findViewById(R.id.tv_data_sensor);
        tvDataMedicao = (TextView) findViewById(R.id.tv_data_atualizacao);
        tvUnidadeMedida = (TextView) findViewById(R.id.tv_data_metric);
        btnAtualizar = (Button) findViewById(R.id.bt_refresh_data);

        //buscar os dados do sensor que recebe pelo intent
        Intent intent = getIntent();
        String name = intent.getStringExtra("NOME_DISPOSITIVO");
        String location = intent.getStringExtra("LOCALIZACAO");
        int type = intent.getIntExtra("TIPO_DISPOSITIVO", 0);
        dispositivo = new Device(name,location,type);

        selecionaUnidadeDeMedida(dispositivo.getType());

        consultarDadoSensor();

        btnAtualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnAtualizar.setEnabled(false);
                consultarDadoSensor();
            }
        });
    }

    private void consultarDadoSensor() {
        //aplicarDadoSensorNaTela("20/02/2022 16:30|25.8");
        new GrpcTaskConsultarDadoSensor(this).execute(dispositivo.getLocation(), dispositivo.getName(), definirStringDeFuncionalidade());
    }

    private String definirStringDeFuncionalidade(){
        return "sensor|"+dispositivo.getLocation()+"|"+dispositivo.getName()+"|"+dispositivo.getType();
    }

    @SuppressLint("SetTextI18n")
    private void aplicarDadoSensorNaTela(String resultado){
        String[] dados = resultado.split("\\|");
        try {
            float valor = Float.parseFloat(dados[1]);
            tvDadoSensor.setText(Float.toString(valor));
            String msgDataAtualizacao = "Última atualização em "+ dados[0];
            tvDataMedicao.setText(msgDataAtualizacao);
            btnAtualizar.setEnabled(true);
        } catch (Exception e) {
            Toast.makeText(this, "Erro na requisição", Toast.LENGTH_LONG).show();
        }

    }

    @SuppressLint("SetTextI18n")
    private void selecionaUnidadeDeMedida (int tipoDeDispositivo){
        switch (tipoDeDispositivo){
            case 2:
                tvUnidadeMedida.setText("°C");
                break;
            case 3:
                tvUnidadeMedida.setText("%");
                break;
            case 4:
                tvUnidadeMedida.setText("lm");
                break;
            default:
                break;
        }

    }
    private static class GrpcTaskConsultarDadoSensor extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTaskConsultarDadoSensor(Activity activity) {
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
                Parametros parametros = Parametros.newBuilder().setLocalizacao(localizacao).setNomeDispositivo(nomeDispositivo).build();
                stub = MetadataUtils.attachHeaders(stub, metadata);
                Dado resposta = stub.consultarUltimaLeituraSensor(parametros);

                return resposta.getData()+ "|" + resposta.getValor();

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
            if (activity instanceof SensorActivity) {
                ((SensorActivity) activity).aplicarDadoSensorNaTela(result);
            }
        }
    }
}