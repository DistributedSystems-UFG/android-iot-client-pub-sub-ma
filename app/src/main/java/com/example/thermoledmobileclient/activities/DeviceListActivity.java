package com.example.thermoledmobileclient.activities;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import com.example.thermoledmobileclient.R;
import com.example.thermoledmobileclient.adapters.DeviceListAdapter;
import com.example.thermoledmobileclient.interfaces.RecyclerViewInterface;
import com.example.thermoledmobileclient.models.Device;
import com.example.thermoledmobileclient.models.GrpcConfig;
import com.example.thermoledmobileclient.models.SessaoClient;
import com.example.thermoledmobileclient.models.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.examples.iotservice.EmptyMessage;
import io.grpc.examples.iotservice.ListaDispositivos;
import io.grpc.examples.iotservice.SensorServiceGrpc;
import io.grpc.examples.iotservice.Sessao;
import io.grpc.stub.MetadataUtils;

public class DeviceListActivity extends AppCompatActivity implements RecyclerViewInterface {

    public ArrayList<Device> deviceList;
    private RecyclerView recyclerViewDeviceList;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        recyclerViewDeviceList = (RecyclerView) findViewById(R.id.devices_recycler_view);
        //mockDeviceList();
        consultarListaDeDispositivos();

        handler = new Handler();
        runnable = () -> consultarSessao();
        handler.postDelayed(runnable, 15000); // inicializa a consulta após 15 segundos
    }

    private void consultarSessao(){
        new GrpcTaskConsultarSessao(this).execute(SessaoClient.getToken());
    }

    private void consultarListaDeDispositivos() {
        new GrpcTaskListarDispositivos(this).execute(definirStringDeFuncionalidade());
    }

    private String definirStringDeFuncionalidade(){
        return "home";
    }

    public void mostarListaDeDispositivosNaTela(String result) {
        if(!result.equals("sucesso")) {
            Toast.makeText(this, "Erro ao carregar lista de dispositivos", Toast.LENGTH_SHORT).show();
            recyclerViewDeviceList.setVisibility(View.GONE);
        }
        recyclerViewDeviceList.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDeviceList.setAdapter(new DeviceListAdapter(this, this, deviceList));
    }

    @Override
    public void onRecyclerViewItemClick(int position) {
        if(deviceList.get(position).getType() == 1) {
            abrirActivityDeAtuador(position);
        } else {
            abrirActivityDeSensor(position);
        }
    }

    private void abrirActivityDeAtuador(int position) {
        Intent intent = new Intent(DeviceListActivity.this, ActuatorActivity.class);
        intent.putExtra("NOME_DISPOSITIVO", deviceList.get(position).getName());
        intent.putExtra("LOCALIZACAO", deviceList.get(position).getLocation());
        intent.putExtra("TIPO_DISPOSITIVO", deviceList.get(position).getType());
        startActivity(intent);
    }

    private void abrirActivityDeSensor(int position) {
        Intent intent = new Intent(DeviceListActivity.this, SensorActivity.class);
        intent.putExtra("NOME_DISPOSITIVO", deviceList.get(position).getName());
        intent.putExtra("LOCALIZACAO", deviceList.get(position).getLocation());
        intent.putExtra("TIPO_DISPOSITIVO", deviceList.get(position).getType());
        startActivity(intent);
    }

    private static class GrpcTaskListarDispositivos extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;
        ArrayList<Device> listaDeDispositivos;


        private GrpcTaskListarDispositivos(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String funcionalidade = params[0];

            //Adicionando os headers da requisicao
            Metadata metadata = new Metadata();
            metadata.put(Metadata.Key.of("token", ASCII_STRING_MARSHALLER), SessaoClient.getToken());
            metadata.put(Metadata.Key.of("funcionalidade", ASCII_STRING_MARSHALLER), funcionalidade);

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                EmptyMessage emptyMessage = EmptyMessage.newBuilder().build();
                stub = MetadataUtils.attachHeaders(stub, metadata);
                ListaDispositivos resposta = stub.listarDispositivos(emptyMessage);


                listaDeDispositivos = new ArrayList<>();
                resposta.getDispositivosList().forEach(dispositivo -> {
                    String nomeDispositivo = dispositivo.getNomeDispositivo();
                    String localizacaoDispositivo = dispositivo.getLocalizacao();
                    int tipoDispositivo = dispositivo.getTipoDispositivo();
                    listaDeDispositivos.add(new Device(nomeDispositivo, localizacaoDispositivo, tipoDispositivo));
                });

                Activity activity = activityReference.get();
                if (activity == null) {
                    return "erro";
                }
                if (activity instanceof DeviceListActivity) {
                    ((DeviceListActivity) activity).deviceList = listaDeDispositivos;
                }
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
            if (activity instanceof DeviceListActivity) {
                ((DeviceListActivity) activity).mostarListaDeDispositivosNaTela(result);
            }
        }
    }
    private static class GrpcTaskConsultarSessao extends AsyncTask<String, Void, String> {
        private final WeakReference<Activity> activityReference;
        private ManagedChannel channel;


        private GrpcTaskConsultarSessao(Activity activity) {
            this.activityReference = new WeakReference<Activity>(activity);
        }

        @Override
        protected String doInBackground(String... params) {
            String token = params[0];

            try {
                channel = ManagedChannelBuilder.forAddress(GrpcConfig.host, GrpcConfig.port).usePlaintext().build();
                SensorServiceGrpc.SensorServiceBlockingStub stub = SensorServiceGrpc.newBlockingStub(channel);
                Sessao sessao = Sessao.newBuilder().setToken(token).build();
                Sessao resposta = stub.consultarFuncionalidade(sessao);

                return resposta.getToken();

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
            if (activity instanceof DeviceListActivity) {
                ((DeviceListActivity) activity).atualizarparaFuncionalidadeSeDiferente(result);
            }
        }
    }

    private void atualizarparaFuncionalidadeSeDiferente(String result){

        if(result.contains("Failed")){
            Toast.makeText(this, "Erro no login", Toast.LENGTH_LONG).show();
            return;
        }

        String[] dados = SessaoClient.getFuncionalidade().split("\\|");
        switch (dados[0].toLowerCase()){
            case "home":
            case "listardispositivos":
                break;
            case "atuador":
                abrirActivityDeAtuador(dados[1], dados[2], Integer.parseInt(dados[3]));
                break;
            case "sensor":
                abrirActivityDeSensor(dados[1], dados[2], Integer.parseInt(dados[3]));
                break;
            default:
                break;
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable); // remove a tarefa quando a activity é destruída
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
}