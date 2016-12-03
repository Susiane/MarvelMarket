package com.laboratory.android.marvelmarket;

import com.google.android.gms.maps.model.LatLng;

import java.io.Serializable;

/**
 * Created by Susiane on 02/12/2016.
 */

public class Banca implements Serializable {
    private int id;
    private int idMarvel;
    private String Banca;
    private Double Latitude;
    private Double Longitude;
    private Double preco;

    public Banca(int id, int idMarvel, Double preco, Double latitude, Double longitude,String banca) {
        this.id = id;
        this.idMarvel = idMarvel;
        this.Banca = banca;
        Latitude = latitude;
        Longitude = longitude;
        this.preco = preco;
    }
    public LatLng getCoordenadas() {
        LatLng latLng = new LatLng(this.Latitude,this.Longitude);
        return latLng;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdMarvel() {
        return idMarvel;
    }

    public void setIdMarvel(int idMarvel) {
        this.idMarvel = idMarvel;
    }

    public String getBanca() {
        return Banca;
    }

    public void setBanca(String banca) {
        this.Banca = banca;
    }

    public Double getLatitude() {
        return Latitude;
    }

    public void setLatitude(Double latitude) {
        Latitude = latitude;
    }

    public Double getLongitude() {
        return Longitude;
    }

    public void setLongitude(Double longitude) {
        Longitude = longitude;
    }

    public Double getPreco() {
        return preco;
    }

    public void setPreco(Double preco) {
        this.preco = preco;
    }
}
