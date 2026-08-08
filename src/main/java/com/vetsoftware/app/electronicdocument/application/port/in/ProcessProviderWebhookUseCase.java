package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Procesa un webhook entrante del proveedor (async). SIN @PreAuthorize: la ruta
 * es pública y la autenticidad se valida por HMAC dentro del servicio (no por
 * JWT).
 */
@NoAuthorizationRequired(reason = "Webhook del proveedor: se autentica con la firma que envía el proveedor, no con un JWT de empleado.")
public interface ProcessProviderWebhookUseCase {
    void execute(ProcessProviderWebhookCommand command);
}
