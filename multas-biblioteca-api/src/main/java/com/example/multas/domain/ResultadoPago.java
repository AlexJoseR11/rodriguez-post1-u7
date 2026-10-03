package com.example.multas.domain;

/**
 * Representa el resultado neutral de un intento de pago procesado por una pasarela.
 * Modelo de dominio puro, agnóstico de tecnologías HTTP o frameworks externos.
 */
public record ResultadoPago(
        String proveedor,
        boolean exitoso,
        String referenciaExterna,
        String mensaje
) {}
