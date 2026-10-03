package com.example.multas.domain.port;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;

/**
 * Puerto de Dominio para el procesamiento de pagos.
 * Define el contrato abstracto e independiente de tecnologías externas (HTTP, REST, SDKs de pasarelas).
 * Satisface el Principio de Inversión de Dependencias (DIP).
 */
public interface PasarelaPagoPort {

    /**
     * Procesa el cobro de una multa a través del proveedor de pagos activo.
     *
     * @param multa Entidad Multa a pagar.
     * @return ResultadoPago con el estado, referencia externa y mensaje de la transacción.
     */
    ResultadoPago procesar(Multa multa);
}
