package com.example.fincacacaoapp.models;

public class CarruselItem {
    private int imageResId;
    private String titulo;
    private String descripcion;

    public CarruselItem(int imageResId, String titulo, String descripcion) {
        this.imageResId = imageResId;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public int getImageResId() { return imageResId; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
}