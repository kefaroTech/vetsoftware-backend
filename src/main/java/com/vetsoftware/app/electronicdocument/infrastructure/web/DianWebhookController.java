package com.vetsoftware.app.electronicdocument.infrastructure.web;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.ProcessProviderWebhookUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Recibe webhooks de los proveedores async (p. ej. MATIAS) en {@code POST /dian/webhooks/{provider}}.
 * Ruta PÚBLICA (en AuthFilter.PUBLIC_PATHS): no lleva JWT; su autenticidad es la firma HMAC que valida
 * el servicio. Se recibe el cuerpo CRUDO ({@code String}) para poder verificar el HMAC sobre los bytes
 * exactos enviados por el proveedor.
 */
@RestController
public class DianWebhookController {
    private final ProcessProviderWebhookUseCase useCase;

    public DianWebhookController(ProcessProviderWebhookUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/dian/webhooks/{provider}")
    @ResponseStatus(HttpStatus.OK)
    public void receive(@PathVariable String provider,
                        @RequestBody String rawBody,
                        @RequestHeader(value = "X-Webhook-Signature", required = false) String signature) {
        useCase.execute(new ProcessProviderWebhookCommand(provider, rawBody, signature));
    }
}
