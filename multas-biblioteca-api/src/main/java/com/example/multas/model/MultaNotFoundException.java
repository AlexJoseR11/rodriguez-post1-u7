package com.example.multas.model;

public class MultaNotFoundException extends RuntimeException {

    public MultaNotFoundException(Long id) {
        super("Multa no encontrada con ID: " + id);
    }

    public MultaNotFoundException(String message) {
        super(message);
    }
}
