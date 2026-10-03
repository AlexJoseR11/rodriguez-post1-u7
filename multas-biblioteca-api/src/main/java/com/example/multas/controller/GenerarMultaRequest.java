package com.example.multas.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerarMultaRequest(
        @NotBlank(message = "El ID del estudiante no puede estar vacío")
        String estudianteId,

        @NotBlank(message = "El concepto no puede estar vacío")
        String concepto,

        @NotNull(message = "Los días de atraso son obligatorios")
        @Min(value = 1, message = "Los días de atraso deben ser al menos 1")
        Integer diasAtraso
) {}
