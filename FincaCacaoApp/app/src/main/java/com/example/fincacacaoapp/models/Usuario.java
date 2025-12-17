package com.example.fincacacaoapp.models;

public class Usuario {
    private String idTelefono;
    private String name;
    private String correo;
    private String contraseña;

    public Usuario(String idTelefono, String name, String correo, String contraseña) {
        this.idTelefono = idTelefono;
        this.name = name;
        this.correo = correo;
        this.contraseña = contraseña;
    }

    // Getters y Setters
    public String getIdTelefono() { return idTelefono; }
    public void setIdTelefono(String idTelefono) { this.idTelefono = idTelefono; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getContraseña() { return contraseña; }
    public void setContraseña(String contraseña) { this.contraseña = contraseña; }
}