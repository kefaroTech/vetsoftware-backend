package com.vetsoftware.app.paymentgateway.infrastructure.web;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.paymentgateway.application.port.in.ProcessWompiEventUseCase;
import org.springframework.http.HttpStatus;
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
}
