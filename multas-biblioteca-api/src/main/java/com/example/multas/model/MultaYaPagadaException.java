package com.example.multas.model;

public class MultaYaPagadaException extends RuntimeException {

    public MultaYaPagadaException(Long id) {
        super("La multa con ID " + id + " ya se encuentra pagada.");
    }

    public MultaYaPagadaException(String message) {
        super(message);
    }
}
