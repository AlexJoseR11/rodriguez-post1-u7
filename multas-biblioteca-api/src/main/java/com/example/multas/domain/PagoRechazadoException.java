package com.example.multas.domain;

/**
 * Excepción de dominio lanzada cuando una pasarela de pago rechaza una transacción
 * o no es posible completar la operación financiera.
 */
public class PagoRechazadoException extends RuntimeException {

    public PagoRechazadoException(String message) {
        super(message);
    }

    public PagoRechazadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
