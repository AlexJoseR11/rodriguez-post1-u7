package com.example.multas.controller;

import com.example.multas.domain.PagoRechazadoException;
import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaNotFoundException;
import com.example.multas.model.MultaYaPagadaException;
import com.example.multas.service.MultaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MultaController.class)
class MultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MultaService multaService;

    @Test
    @DisplayName("GET /api/multas debe retornar 200 OK y la lista de multas")
    void debeListarTodasLasMultas() throws Exception {
        Multa multa = new Multa("EST-1", "Libro de Redes", 3);
        multa.setId(1L);
        when(multaService.listarTodas()).thenReturn(List.of(multa));

        mockMvc.perform(get("/api/multas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estudianteId").value("EST-1"))
                .andExpect(jsonPath("$[0].monto").value(1500));
    }

    @Test
    @DisplayName("GET /api/multas/{id} debe retornar 200 OK cuando la multa existe")
    void debeBuscarMultaPorIdExistente() throws Exception {
        Multa multa = new Multa("EST-1", "Libro de Redes", 3);
        multa.setId(1L);
        when(multaService.buscarPorId(1L)).thenReturn(multa);

        mockMvc.perform(get("/api/multas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estudianteId").value("EST-1"));
    }

    @Test
    @DisplayName("GET /api/multas/{id} debe retornar 404 Not Found cuando la multa no existe")
    void debeRetornar404CuandoMultaNoExiste() throws Exception {
        when(multaService.buscarPorId(99L)).thenThrow(new MultaNotFoundException(99L));

        mockMvc.perform(get("/api/multas/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Multa no encontrada con ID: 99"));
    }

    @Test
    @DisplayName("GET /api/multas/estudiante/{id} debe retornar 200 OK")
    void debeListarMultasPorEstudiante() throws Exception {
        Multa multa = new Multa("EST-100", "Libro de Base de Datos", 2);
        multa.setId(10L);
        when(multaService.listarPorEstudiante("EST-100")).thenReturn(List.of(multa));

        mockMvc.perform(get("/api/multas/estudiante/EST-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estudianteId").value("EST-100"));
    }

    @Test
    @DisplayName("POST /api/multas debe retornar 201 Created al generar multa con payload válido")
    void debeGenerarMultaRetornando201() throws Exception {
        GenerarMultaRequest request = new GenerarMultaRequest("EST-200", "Devolución tardía de laptop", 4);
        Multa multaCreada = new Multa("EST-200", "Devolución tardía de laptop", 4);
        multaCreada.setId(5L);

        when(multaService.generar(eq("EST-200"), eq("Devolución tardía de laptop"), eq(4))).thenReturn(multaCreada);

        mockMvc.perform(post("/api/multas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.estudianteId").value("EST-200"))
                .andExpect(jsonPath("$.monto").value(2000))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST /api/multas debe retornar 400 Bad Request cuando el payload es inválido")
    void debeRetornar400CuandoPayloadInvalido() throws Exception {
        GenerarMultaRequest requestInvalido = new GenerarMultaRequest("", "", 0);

        mockMvc.perform(post("/api/multas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/multas debe retornar 409 Conflict cuando el estudiante excede el límite de multas pendientes")
    void debeRetornar409CuandoExcedeLimitePendientes() throws Exception {
        GenerarMultaRequest request = new GenerarMultaRequest("EST-300", "Libro de Física", 2);
        when(multaService.generar(anyString(), anyString(), anyInt()))
                .thenThrow(new LimiteMultasPendientesException("EST-300", 3));

        mockMvc.perform(post("/api/multas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("El estudiante con ID 'EST-300' ha alcanzado el límite máximo de 3 multas pendientes."));
    }

    @Test
    @DisplayName("PATCH /api/multas/{id}/pagar debe retornar 200 OK tras pagar en ventanilla")
    void debePagarEnVentanillaRetornando200() throws Exception {
        Multa multaPagada = new Multa("EST-1", "Libro de Algoritmos", 2);
        multaPagada.setId(1L);
        multaPagada.marcarComoPagada("VENTANILLA");

        when(multaService.pagarEnVentanilla(1L)).thenReturn(multaPagada);

        mockMvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.metodoPago").value("VENTANILLA"));
    }

    @Test
    @DisplayName("PATCH /api/multas/{id}/pagar debe retornar 409 Conflict si la multa ya estaba pagada")
    void debeRetornar409SiMultaYaPagadaEnVentanilla() throws Exception {
        when(multaService.pagarEnVentanilla(1L))
                .thenThrow(new MultaYaPagadaException(1L));

        mockMvc.perform(patch("/api/multas/1/pagar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("La multa con ID 1 ya se encuentra pagada."));
    }

    @Test
    @DisplayName("POST /api/multas/{id}/pagar-en-linea debe retornar 200 OK cuando el pago en pasarela es exitoso")
    void debePagarEnLineaRetornando200() throws Exception {
        Multa multaPagada = new Multa("EST-1", "Libro de Algoritmos", 2);
        multaPagada.setId(1L);
        multaPagada.marcarComoPagada("PAGOSUDES");

        when(multaService.pagarConPasarela(1L)).thenReturn(multaPagada);

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.metodoPago").value("PAGOSUDES"));
    }

    @Test
    @DisplayName("POST /api/multas/{id}/pagar-en-linea debe retornar 402 Payment Required cuando la pasarela rechaza la transacción")
    void debeRetornar402CuandoPasarelaRechaza() throws Exception {
        when(multaService.pagarConPasarela(1L))
                .thenThrow(new PagoRechazadoException("Pago rechazado por pasarela [WOMPI]: Fondos insuficientes"));

        mockMvc.perform(post("/api/multas/1/pagar-en-linea"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.error").value("Payment Required"))
                .andExpect(jsonPath("$.message").value("Pago rechazado por pasarela [WOMPI]: Fondos insuficientes"));
    }
}
