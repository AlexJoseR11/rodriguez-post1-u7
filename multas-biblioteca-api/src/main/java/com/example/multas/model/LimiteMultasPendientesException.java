package com.example.multas.model;

public class LimiteMultasPendientesException extends RuntimeException {

    public LimiteMultasPendientesException(String estudianteId) {
        super("El estudiante con ID '" + estudianteId + "' ha alcanzado el límite máximo de multas pendientes (3).");
    }

    public LimiteMultasPendientesException(String estudianteId, int limite) {
        super("El estudiante con ID '" + estudianteId + "' ha alcanzado el límite máximo de " + limite + " multas pendientes.");
    }
}
