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
 * Adaptador de infraestructura para la pasarela comercial Wompi.
 * Activo condicionalmente cuando app.pagos.proveedor=wompi.
 */
@Component
@ConditionalOnProperty(prefix = "app.pagos", name = "proveedor", havingValue = "wompi")
public class WompiAdapter implements PasarelaPagoPort {

    private final RestTemplate restTemplate;
    private final String url;

    public WompiAdapter(RestTemplate restTemplate,
                        @Value("${app.pagos.wompi.url}") String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    @Override
    public ResultadoPago procesar(Multa multa) {
        // Conversión del monto de la multa a centavos requerida por el estándar de Wompi
        long amountInCents = multa.getMonto() != null
                ? multa.getMonto().multiply(BigDecimal.valueOf(100)).longValue()
                : 0L;

        String reference = "WOMPI-MULTA-" + (multa.getId() != null ? multa.getId() : "NEW") + "-" + System.currentTimeMillis();

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("amount_in_cents", amountInCents);
        requestPayload.put("currency", "COP");
        requestPayload.put("reference", reference);
        requestPayload.put("customer_email", multa.getEstudianteId() + "@correo.udes.edu.co");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestPayload, Map.class);

            if (response != null) {
                String refResponse = String.valueOf(response.getOrDefault("reference", reference));
                String status = String.valueOf(response.getOrDefault("status", ""));
                String message = String.valueOf(response.getOrDefault("message", "Respuesta de pasarela Wompi"));

                boolean exitoso = "APPROVED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
                return new ResultadoPago("WOMPI", exitoso, refResponse, message);
            }

            return new ResultadoPago("WOMPI", false, null, "Respuesta nula desde Wompi");
        } catch (RestClientException ex) {
            return new ResultadoPago("WOMPI", false, null, "Fallo de comunicación con Wompi: " + ex.getMessage());
        }
    }
}
