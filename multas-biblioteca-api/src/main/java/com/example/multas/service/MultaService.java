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
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MultaService {

    public static final int LIMITE_MULTAS_PENDIENTES = 3;

    private final MultaRepository multaRepository;
    private final PasarelaPagoPort pasarelaPagoPort;

    public MultaService(MultaRepository multaRepository, PasarelaPagoPort pasarelaPagoPort) {
        this.multaRepository = multaRepository;
        this.pasarelaPagoPort = pasarelaPagoPort;
    }

    @Transactional(readOnly = true)
    public List<Multa> listarTodas() {
        return multaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Multa> listarPorEstudiante(String estudianteId) {
        return multaRepository.findByEstudianteId(estudianteId);
    }

    @Transactional(readOnly = true)
    public Multa buscarPorId(Long id) {
        return multaRepository.findById(id)
                .orElseThrow(() -> new MultaNotFoundException(id));
    }

    /**
     * Genera una nueva multa validando que el estudiante no exceda el límite de multas pendientes.
     * Delega el cálculo de monto a la lógica de dominio en la entidad Multa.
     *
     * @param estudianteId Identificador del estudiante.
     * @param concepto Motivo de la sanción.
     * @param diasAtraso Días transcurridos tras la fecha de devolución.
     * @return Multa creada y persistida.
     */
    public Multa generar(String estudianteId, String concepto, int diasAtraso) {
        long multasPendientes = multaRepository.countByEstudianteIdAndEstado(estudianteId, EstadoMulta.PENDIENTE);
        if (multasPendientes >= LIMITE_MULTAS_PENDIENTES) {
            throw new LimiteMultasPendientesException(estudianteId, LIMITE_MULTAS_PENDIENTES);
        }

        Multa nuevaMulta = new Multa(estudianteId, concepto, diasAtraso);
        return multaRepository.save(nuevaMulta);
    }

    /**
     * Procesa el pago de una multa en ventanilla física.
     *
     * @param id Identificador de la multa.
     * @return Multa actualizada con estado PAGADA.
     */
    public Multa pagarEnVentanilla(Long id) {
        Multa multa = buscarPorId(id);
        multa.marcarComoPagada("VENTANILLA");
        return multaRepository.save(multa);
    }

    /**
     * Procesa el pago en línea comunicándose con el puerto de dominio de pasarela de pago.
     * Si la pasarela rechaza la transacción, se lanza PagoRechazadoException y no se muta el estado a PAGADA.
     *
     * @param id Identificador de la multa.
     * @return Multa liquidada exitosamente.
     */
    public Multa pagarConPasarela(Long id) {
        Multa multa = buscarPorId(id);

        if (multa.getEstado() == EstadoMulta.PAGADA) {
            throw new MultaYaPagadaException(id);
        }

        ResultadoPago resultado = pasarelaPagoPort.procesar(multa);

        if (!resultado.exitoso()) {
            throw new PagoRechazadoException("Pago rechazado por pasarela [" + resultado.proveedor() + "]: " + resultado.mensaje());
        }

        multa.marcarComoPagada(resultado.proveedor());
        return multaRepository.save(multa);
    }
}
