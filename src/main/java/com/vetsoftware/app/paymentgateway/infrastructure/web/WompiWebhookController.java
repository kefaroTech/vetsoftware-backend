package com.vetsoftware.app.paymentgateway.infrastructure.web;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import com.vetsoftware.app.paymentgateway.domain.PaymentGatewayNotConfiguredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recibe los eventos de Wompi en {@code POST /payment-gateway/wompi/events}.
 * Ruta PÚBLICA (en {@code PublicRoutes.BUSINESS}): no lleva JWT, su
 * autenticidad es el checksum que valida el caso de uso. Cuerpo crudo
 * ({@code String}) para poder calcular el checksum sobre los bytes exactos que
 * mandó Wompi.
 */
@RestController
public class WompiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WompiWebhookController.class);

    private final ProcessWompiEventUseCase processEventUseCase;

    public WompiWebhookController(ProcessWompiEventUseCase processEventUseCase) {
        this.processEventUseCase = processEventUseCase;
    }

    @PostMapping("/payment-gateway/wompi/events")
    @ResponseStatus(HttpStatus.OK)
    public void events(@RequestBody String rawBody,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        processEventUseCase.execute(new ProcessWompiEventCommand(rawBody, checksum));
    }

    /**
     * Handler local, no en {@code GlobalExceptionHandler}: esa misma excepción la
     * usa {@code WompiGatewayClient} para el checkout síncrono y allí se mapea a
     * 409. Un webhook es tráfico entrante de Wompi, no una petición de un cliente
     * propio, y 503 es la señal correcta para que reintente cuando la pasarela
     * quede configurada.
     *
     * <p>
     * <strong>El detalle que sale es genérico.</strong> {@code ex.getMessage()}
     * nombra la propiedad de configuración exacta
     * ({@code vetsoftware.payments.wompi.enabled}, {@code events-secret}), y esta
     * ruta es pública: cualquiera puede sondearla para averiguar cómo está
     * configurado el backend. El mensaje completo sí se registra en el log.
     */
    @ExceptionHandler(PaymentGatewayNotConfiguredException.class)
    public ProblemDetail handleNotConfigured(PaymentGatewayNotConfiguredException ex) {
        log.warn("Webhook de Wompi rechazado: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "La pasarela de pagos no está disponible.");
        pd.setTitle(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        pd.setProperty("code", "PAYMENT_GATEWAY_NOT_CONFIGURED");
        return pd;
    }
}
