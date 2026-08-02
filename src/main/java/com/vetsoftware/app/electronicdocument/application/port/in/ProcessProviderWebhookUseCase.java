package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;

/**
 * Procesa un webhook entrante del proveedor (async). SIN @PreAuthorize: la ruta
 * es pública y la autenticidad se valida por HMAC dentro del servicio (no por
 * JWT).
 */
public interface ProcessProviderWebhookUseCase {
    void execute(ProcessProviderWebhookCommand command);
}
