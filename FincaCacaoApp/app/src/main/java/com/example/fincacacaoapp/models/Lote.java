package com.example.fincacacaoapp.models;

import java.util.Date;

public class Lote {
    private int loteId;
    private String nombreLote;
    private int cantidadPlantas;
    private Date fechaSiembra;
    private String estado;
    private String descripcion;
    private String firestoreId; // ← AGREGAR ESTE

    public Lote(int loteId, String nombreLote, int cantidadPlantas, Date fechaSiembra, String estado, String descripcion) {
        this.loteId = loteId;
        this.nombreLote = nombreLote;
        this.cantidadPlantas = cantidadPlantas;
        this.fechaSiembra = fechaSiembra;
        this.estado = estado;
        this.descripcion = descripcion;
    }

    // GETTERS Y SETTERS
    public int getLoteId() { return loteId; }
    public String getNombre() { return nombreLote; }
    public int getCantidadPlantas() { return cantidadPlantas; }
    public Date getFechaSiembra() { return fechaSiembra; }
    public String getEstado() { return estado; }
    public String getDescripcion() { return descripcion; }

    // AGREGAR ESTOS:
    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }
}