package com.example.multas.integration;

import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.infrastructure.pago.PagosUdesAdapter;
import com.example.multas.infrastructure.pago.WompiAdapter;
import com.example.multas.model.EstadoMulta;
import com.example.multas.model.Multa;
import com.example.multas.repository.MultaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PasarelaPagoIntegrationTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @DisplayName("Pruebas de Integración con Adaptador por Defecto (PagosUDES)")
    class PagosUdesIntegrationTests {

        @Autowired
        private PasarelaPagoPort pasarelaPagoPort;

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private MultaRepository multaRepository;

        @MockBean
        private RestTemplate restTemplate;

        @BeforeEach
        void setUp() {
            multaRepository.deleteAll();
        }

        @Test
        @DisplayName("Debe inyectar PagosUdesAdapter como implementación activa del puerto PasarelaPagoPort")
        void debeInyectarPagosUdesAdapterPorDefecto() {
            assertThat(pasarelaPagoPort)
                    .isNotNull()
                    .isInstanceOf(PagosUdesAdapter.class);
        }

        @Test
        @DisplayName("Debe retornar 402 Payment Required cuando la comunicación o cobro con PagosUDES falla")
        void debeRetornar402CuandoFallaComunicacionPagosUdes() throws Exception {
            Multa multa = multaRepository.save(new Multa("EST-500", "Libro de Redes II", 3));

            when(restTemplate.postForObject(anyString(), any(), eq(java.util.Map.class)))
                    .thenThrow(new RestClientException("Connection refused to PagosUDES"));

            mockMvc.perform(post("/api/multas/" + multa.getId() + "/pagar-en-linea"))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.status").value(402))
                    .andExpect(jsonPath("$.error").value("Payment Required"))
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PAGOSUDES")));

            Multa multaActualizada = multaRepository.findById(multa.getId()).orElseThrow();
            assertThat(multaActualizada.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = "app.pagos.proveedor=wompi")
    @DisplayName("Pruebas de Inyección Dinámica con Wompi (app.pagos.proveedor=wompi)")
    class WompiIntegrationTests {

        @Autowired
        private PasarelaPagoPort pasarelaPagoPort;

        @Test
        @DisplayName("Debe inyectar WompiAdapter cuando app.pagos.proveedor=wompi")
        void debeInyectarWompiAdapter() {
            assertThat(pasarelaPagoPort)
                    .isNotNull()
                    .isInstanceOf(WompiAdapter.class);
        }
    }
}
