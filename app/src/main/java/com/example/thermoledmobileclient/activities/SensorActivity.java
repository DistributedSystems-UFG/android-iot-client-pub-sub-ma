package com.example.thermoledmobileclient.activities;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
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
import io.grpc.examples.sensorservice.Dado;
import io.grpc.examples.sensorservice.LedStatus;
import io.grpc.examples.sensorservice.ListaLedStatus;
import io.grpc.examples.sensorservice.Parametros;
import io.grpc.examples.sensorservice.SensorServiceGrpc;
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
        Bundle extras = getIntent().getExtras();
        String name = extras.getString("NOME_DISPOSITIVO");
        int type = Integer.parseInt(extras.getString("TIPO_DISPOSITIVO"));
        String location = extras.getString("LOCALIZACAO_DISPOSITIVO");
        dispositivo = new Device(name,location,type);

        selecionaUnidadeDeMedida(dispositivo.getType());

        consultarDadoSensor();
    }

    private void consultarDadoSensor() {
        new GrpcTaskConsultarDadoSensor(this).execute(dispositivo.getLocation(), dispositivo.getName());
    }

    private void aplicarDadoSensorNaTela(String resultado){
        String[] dados = resultado.split(":");

        tvDadoSensor.setText(dados[1]);
        String msgDataAtualizacao = "Última atualização em "+ dados[2];
        tvDataMedicao.setText(msgDataAtualizacao);
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

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), "sensor|" + localizacao + "|" + nomeDispositivo);

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                Parametros parametros = Parametros.newBuilder().setLocalizacao(localizacao).setNomeDispositivo(nomeDispositivo).build();
                stub = MetadataUtils.attachHeaders(stub, metadata);
                Dado resposta = stub.consultarUltimaLeituraSensor(parametros);

                return resposta.getData()+ ":" + resposta.getValor();

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