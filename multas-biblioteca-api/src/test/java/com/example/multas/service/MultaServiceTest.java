package com.example.multas.service;

import com.example.multas.domain.PagoRechazadoException;
import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.EstadoMulta;
import com.example.multas.model.LimiteMultasPendientesException;
import com.example.multas.model.Multa;
import com.example.multas.model.MultaNotFoundException;
import com.example.multas.model.MultaYaPagadaException;
import com.example.multas.repository.MultaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultaServiceTest {

    @Mock
    private MultaRepository multaRepository;

    @Mock
    private PasarelaPagoPort pasarelaPagoPort;

    @InjectMocks
    private MultaService multaService;

    private Multa multaEjemplo;

    @BeforeEach
    void setUp() {
        multaEjemplo = new Multa("EST-101", "Libro de Algoritmos", 5);
        multaEjemplo.setId(1L);
    }

    @Nested
    @DisplayName("Punto 1: Pruebas de Regla de Negocio de Dominio (Cálculo de Monto)")
    class CalculoMontoTests {

        @Test
        @DisplayName("Debe calcular monto exacto sin superar el tope ($500 por día)")
        void debeCalcularMontoSinSuperarTope() {
            BigDecimal monto = Multa.calcularMonto(10);
            assertThat(monto).isEqualByComparingTo(new BigDecimal("5000"));
        }

        @Test
        @DisplayName("Debe topar el monto a $15.000 cuando los días superan el límite")
        void debeAplicarTopeMaximoDe15000() {
            BigDecimal montoExactoTope = Multa.calcularMonto(30);
            BigDecimal montoSuperiorTope = Multa.calcularMonto(60);

            assertThat(montoExactoTope).isEqualByComparingTo(new BigDecimal("15000"));
            assertThat(montoSuperiorTope).isEqualByComparingTo(new BigDecimal("15000"));
        }

        @Test
        @DisplayName("Debe retornar cero cuando los días de atraso son menores o iguales a cero")
        void debeRetornarCeroParaDiasMenoresOIgualesACero() {
            assertThat(Multa.calcularMonto(0)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(Multa.calcularMonto(-5)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Punto 2: Pruebas de Generación de Multas y Límite de Pendientes")
    class GeneracionMultasTests {

        @Test
        @DisplayName("Debe generar multa exitosamente si el estudiante tiene menos de 3 multas pendientes")
        void debeGenerarMultaExitosamente() {
            when(multaRepository.countByEstudianteIdAndEstado("EST-101", EstadoMulta.PENDIENTE)).thenReturn(2L);
            when(multaRepository.save(any(Multa.class))).thenAnswer(invocation -> {
                Multa m = invocation.getArgument(0);
                m.setId(2L);
                return m;
            });

            Multa generada = multaService.generar("EST-101", "Libro de Cálculo", 4);

            assertThat(generada).isNotNull();
            assertThat(generada.getEstudianteId()).isEqualTo("EST-101");
            assertThat(generada.getMonto()).isEqualByComparingTo(new BigDecimal("2000"));
            assertThat(generada.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
            assertThat(generada.getFechaGeneracion()).isEqualTo(LocalDate.now());
            verify(multaRepository).countByEstudianteIdAndEstado("EST-101", EstadoMulta.PENDIENTE);
            verify(multaRepository).save(any(Multa.class));
        }

        @Test
        @DisplayName("Debe lanzar LimiteMultasPendientesException si ya tiene 3 o más multas pendientes")
        void debeBloquearGeneracionAlAlcanzarLimite3() {
            when(multaRepository.countByEstudianteIdAndEstado("EST-101", EstadoMulta.PENDIENTE)).thenReturn(3L);

            assertThatThrownBy(() -> multaService.generar("EST-101", "Libro de Redes", 2))
                    .isInstanceOf(LimiteMultasPendientesException.class)
                    .hasMessageContaining("EST-101")
                    .hasMessageContaining("3");

            verify(multaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Pruebas de Consulta y Búsqueda")
    class ConsultasTests {

        @Test
        @DisplayName("Debe listar todas las multas")
        void debeListarTodasLasMultas() {
            when(multaRepository.findAll()).thenReturn(List.of(multaEjemplo));

            List<Multa> resultado = multaService.listarTodas();

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getEstudianteId()).isEqualTo("EST-101");
        }

        @Test
        @DisplayName("Debe listar multas por estudiante")
        void debeListarMultasPorEstudiante() {
            when(multaRepository.findByEstudianteId("EST-101")).thenReturn(List.of(multaEjemplo));

            List<Multa> resultado = multaService.listarPorEstudiante("EST-101");

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getConcepto()).isEqualTo("Libro de Algoritmos");
        }

        @Test
        @DisplayName("Debe buscar multa por ID exitosamente")
        void debeBuscarMultaPorIdExitosamente() {
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));

            Multa encontrada = multaService.buscarPorId(1L);

            assertThat(encontrada).isNotNull();
            assertThat(encontrada.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Debe lanzar MultaNotFoundException si no existe el ID")
        void debeLanzarExcepcionSiNoExiste() {
            when(multaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> multaService.buscarPorId(99L))
                    .isInstanceOf(MultaNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("Pruebas de Pago en Ventanilla")
    class PagoVentanillaTests {

        @Test
        @DisplayName("Debe pagar en ventanilla y actualizar estado, fecha y método")
        void debePagarEnVentanillaCorrectamente() {
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));
            when(multaRepository.save(any(Multa.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Multa pagada = multaService.pagarEnVentanilla(1L);

            assertThat(pagada.getEstado()).isEqualTo(EstadoMulta.PAGADA);
            assertThat(pagada.getFechaPago()).isEqualTo(LocalDate.now());
            assertThat(pagada.getMetodoPago()).isEqualTo("VENTANILLA");
            verify(multaRepository).save(multaEjemplo);
        }

        @Test
        @DisplayName("Debe lanzar MultaYaPagadaException si ya fue pagada en ventanilla previamente")
        void debeLanzarExcepcionSiYaEstabaPagadaEnVentanilla() {
            multaEjemplo.marcarComoPagada("VENTANILLA");
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));

            assertThatThrownBy(() -> multaService.pagarEnVentanilla(1L))
                    .isInstanceOf(MultaYaPagadaException.class);
        }
    }

    @Nested
    @DisplayName("Parte 2: Pruebas de Pago en Línea (Pasarela de Pago)")
    class PagoEnLineaTests {

        @Test
        @DisplayName("Debe procesar el pago en línea exitosamente a través del puerto de dominio")
        void debePagarConPasarelaExitosamente() {
            ResultadoPago resultadoExitoso = new ResultadoPago("PAGOSUDES", true, "TX-12345", "Aprobado");
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));
            when(pasarelaPagoPort.procesar(multaEjemplo)).thenReturn(resultadoExitoso);
            when(multaRepository.save(any(Multa.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Multa pagada = multaService.pagarConPasarela(1L);

            assertThat(pagada.getEstado()).isEqualTo(EstadoMulta.PAGADA);
            assertThat(pagada.getMetodoPago()).isEqualTo("PAGOSUDES");
            assertThat(pagada.getFechaPago()).isEqualTo(LocalDate.now());
            verify(pasarelaPagoPort).procesar(multaEjemplo);
            verify(multaRepository).save(multaEjemplo);
        }

        @Test
        @DisplayName("Debe lanzar PagoRechazadoException cuando la pasarela rechaza la transacción")
        void debeLanzarExcepcionSiPasarelaRechaza() {
            ResultadoPago resultadoRechazado = new ResultadoPago("WOMPI", false, null, "Fondos insuficientes");
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));
            when(pasarelaPagoPort.procesar(multaEjemplo)).thenReturn(resultadoRechazado);

            assertThatThrownBy(() -> multaService.pagarConPasarela(1L))
                    .isInstanceOf(PagoRechazadoException.class)
                    .hasMessageContaining("WOMPI")
                    .hasMessageContaining("Fondos insuficientes");

            assertThat(multaEjemplo.getEstado()).isEqualTo(EstadoMulta.PENDIENTE);
            verify(multaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Debe lanzar MultaYaPagadaException si se intenta pagar en línea una multa ya liquidada")
        void debeLanzarExcepcionSiYaEstabaPagadaEnLinea() {
            multaEjemplo.marcarComoPagada("VENTANILLA");
            when(multaRepository.findById(1L)).thenReturn(Optional.of(multaEjemplo));

            assertThatThrownBy(() -> multaService.pagarConPasarela(1L))
                    .isInstanceOf(MultaYaPagadaException.class);

            verify(pasarelaPagoPort, never()).procesar(any());
        }
    }
}
