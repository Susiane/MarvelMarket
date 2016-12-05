package com.laboratory.android.marvelmarket;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Susiane on 02/12/2016.
 */

public class RevistaDetalhes extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback {
    private Revista revista = new Revista();
    private ImageView img_CollapsingToolBar;
    private ImageView img_capaRevista;
    private TextView titulo;
    private TextView txt_published;
    private TextView txt_price;
    private TextView txt_pages;
    private TextView txt_description;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private Banca[] arrayBancas;
    private Marker marker;
    private static final String REVISTA_SELECIONADA_BUNDLE = "revistaSelecionada";
    private CameraUpdate update;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setEnterTransition(TransitionInflater.from(this).inflateTransition(R.transition.grid_exit));
            getWindow().setSharedElementExitTransition(TransitionInflater.from(this).inflateTransition(R.transition.capa_transition));
        }

        if (servicesOK()) {
            setContentView(R.layout.activity_revista_detalhes);

            if (initMap()) {

                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .addConnectionCallbacks(this).addOnConnectionFailedListener(this)
                        .addApi(LocationServices.API).build();
                mGoogleApiClient.connect();
            } else {
                Toast.makeText(this, "Map not connected!", Toast.LENGTH_SHORT).show();
            }
        } else {
            setContentView(R.layout.activity_revista_detalhes_sem_mapa);
        }

        //->  Layout pattern
        img_capaRevista = (ImageView) findViewById(R.id.imagemCapaDetalhes);
        titulo = (TextView) findViewById(R.id.tituloPrincipalRevista);
        txt_description = (TextView) findViewById(R.id.descricaoRevista);
        txt_published = (TextView) findViewById(R.id.dataPublicacao);
        txt_price = (TextView) findViewById(R.id.price);
        txt_pages = (TextView) findViewById(R.id.pages);
        //<-


        Intent intent = getIntent();
        revista = (Revista) intent.getSerializableExtra(REVISTA_SELECIONADA_BUNDLE);
        //<-

        //->  Preenchendo dados na tela
        titulo.setText(revista.getTitle());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            txt_description.setText(Html.fromHtml(revista.getDescription(),Html.FROM_HTML_MODE_LEGACY));
        } else {
            txt_description.setText(Html.fromHtml(revista.getDescription()));
        }

        txt_published.setText(revista.getDates().toString());

        // Checando se a revista possui versão digital (além da versão em papel)
        if (revista.getPrices("digital")!=0.0){
            txt_price.setText("$ "+revista.getPrices("paper")+" (Paperback)"+ "\n$ "+
                    revista.getPrices("digital")+ " (Digital Version)");
        } else {
            txt_price.setText("$ "+revista.getPrices("paper")+" (Paperback)");
        }
        txt_pages.setText(revista.getPageCount());
        Picasso.with(this).load(revista.getThumbnail("pequena")).into(img_capaRevista);
        //<-


        final Toolbar toolbar = (Toolbar) findViewById(R.id.MyToolbar);
        toolbar.setTitle("");
        toolbar.setSubtitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        CollapsingToolbarLayout collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapse_toolbar);
        collapsingToolbarLayout.setTitleEnabled(false);

        // Conectando ao repositório e pegando informações da DB (Online)
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWiFi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWiFi.isConnected() || getNetworkClass(this) != "2G"){
            SecondThread secondThread = new SecondThread();
            secondThread.execute();
        } else {
            Toast.makeText(this, "Prezado usuário, sua conexão não está adequada! Favor tentar mais tarde.", Toast.LENGTH_SHORT).show();
            finish();
        }


    }

    //-> Atribuindo função no clique da Capa da Revista (Exibindo ela maior):

    public void showFullImage(View v) {

        Intent intent = new Intent(this, RevistaCapaInteira.class);
        intent.putExtra(REVISTA_SELECIONADA_BUNDLE, revista);
        intent.setAction(Intent.ACTION_VIEW);


        if (Build.VERSION.SDK_INT >= 21) {

            ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(this, img_capaRevista, "imagemCompartilhada");
            startActivity(intent,optionsCompat.toBundle());


        } else {
            startActivity(intent);
        }

    }


    public void goBack(View v){
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (Build.VERSION.SDK_INT >= 21) {
            finishAfterTransition();
        } else {
            finish();
        }
    }
    /**
     * Verifica conexão com Google Play Services
     * @return
     */
    public boolean servicesOK(){
        int isAvaliable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if(isAvaliable == ConnectionResult.SUCCESS){
            return true;
        } else if(GooglePlayServicesUtil.isUserRecoverableError(isAvaliable)){
            Dialog dialog =
                    GooglePlayServicesUtil.getErrorDialog(isAvaliable,this,ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to mapping service",Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /**
     * Faz carregamento do Mapa
     * @return
     */
    private boolean initMap(){

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        map = mapFragment.getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        return(map != null);

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(RevistaDetalhes.this, "Conexão intererrompida!", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(RevistaDetalhes.this, "Erro ao conectar: "+ connectionResult, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.map = googleMap;
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Desconecta do Google Play Services:
        mGoogleApiClient.disconnect();
    }

    /**
     * Método para testar a conexão do usuário */
    public static String getNetworkClass(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    class SecondThread extends AsyncTask<String, String, String> {

        private OkHttpClient client = new OkHttpClient();
        private String saida;


        @Override
        protected String doInBackground(String... params) {
            try {
                Request request = new Request.Builder()
                        .url("http://www.virtualdatabase.com.br/db_info/bancas_revistas/").build();


                Response response = client.newCall(request).execute();
                String json = response.body().string();
                saida = json;
                Log.e("saida", saida);
                // Pegando o GSON e transformando em objetos:
                Banca banca;
                Gson gson = new Gson();
                arrayBancas = gson.fromJson(json, Banca[].class);


            } catch (IOException e) {
                e.printStackTrace();
            }

            return saida.toString();
        }

        @Override
        protected void onPostExecute(String s) {


            // Incluindo localidades no Mapa:

            try {
                for (Banca banca : arrayBancas) {

                    adicionarMarcador(banca);
                    Log.e("JSON", "" + banca.getBanca());

                    /* Código para Mostrar banca relacionda a revista selecionada
                    if(revista.getId() == banca.getIdMarvel()){
                        adicionarMarcador(banca);
                        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(banca.getCoordenadas(), 12);
                        map.moveCamera(update);
                    }*/

                }

            } catch (Exception e){}

            // Move a camera para o primeiro item do Array de Bancas
            update = CameraUpdateFactory.newLatLngZoom(arrayBancas[0].getCoordenadas(), 12);
            map.moveCamera(update);

        }
    }

    /**
     * Método que adiciona um Marcador no mapa
     * @param  -  Dados geográficos de determinada localidade (Lontitude e Latitude LatLng)
     */
    public void adicionarMarcador(Banca banca){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(banca.getCoordenadas()).title(banca.getBanca())
                .snippet(banca.getBanca());
        marker = map.addMarker(markerOptions);

    }

    public void Teste(){

    }




}
