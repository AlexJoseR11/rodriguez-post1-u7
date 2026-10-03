package com.example.multas.infrastructure.pago;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.domain.port.PasarelaPagoPort;
import com.example.multas.model.Multa;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Adaptador de infraestructura para la pasarela institucional PagosUDES.
 * Activo por defecto o cuando app.pagos.proveedor=pagosudes.
 */
@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "pagosudes", matchIfMissing = true)
public class PagosUdesAdapter implements PasarelaPagoPort {

    private final RestTemplate restTemplate;
    private final String url;

    public PagosUdesAdapter(RestTemplate restTemplate,
                             @Value("${app.pagos.pagosudes.url}") String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    @Override
    public ResultadoPago procesar(Multa multa) {
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("codigoEstudiante", multa.getEstudianteId());
        requestPayload.put("valor", multa.getMonto());
        requestPayload.put("descripcionMulta", multa.getConcepto());
        requestPayload.put("multaId", multa.getId());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestPayload, Map.class);

            if (response != null) {
                String idTransaccion = String.valueOf(response.getOrDefault("idTransaccion", "TX-UDES-" + multa.getId()));
                String estadoTransaccion = String.valueOf(response.getOrDefault("estadoTransaccion", ""));
                String mensaje = String.valueOf(response.getOrDefault("mensaje", "Respuesta de PagosUDES"));

                boolean exitoso = "APROBADO".equalsIgnoreCase(estadoTransaccion) || "SUCCESS".equalsIgnoreCase(estadoTransaccion);
                return new ResultadoPago("PAGOSUDES", exitoso, idTransaccion, mensaje);
            }

            return new ResultadoPago("PAGOSUDES", false, null, "Respuesta vacía desde pasarela PagosUDES");
        } catch (RestClientException ex) {
            return new ResultadoPago("PAGOSUDES", false, null, "Fallo de comunicación con PagosUDES: " + ex.getMessage());
        }
    }
}
