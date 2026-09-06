package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.command.ProcessWompiEventCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Procesa un webhook de Wompi. SIN {@code @PreAuthorize}: la ruta es pública y
 * la autenticidad se valida por el checksum SHA-256 firmado con el secreto de
 * eventos, no por JWT.
 */
@NoAuthorizationRequired(reason = "Webhook de Wompi: se autentica con el checksum SHA-256 firmado con el secreto de eventos, no con un JWT.")
public interface ProcessWompiEventUseCase {
    void execute(ProcessWompiEventCommand command);
}
