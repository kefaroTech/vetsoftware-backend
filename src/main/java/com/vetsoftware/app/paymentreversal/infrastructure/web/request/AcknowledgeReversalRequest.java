package com.vetsoftware.app.paymentreversal.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Sin fecha: la del acuse la pone el servidor con el reloj inyectado. Es una
 * constancia probatoria, y dejar que quien llama elija su fecha vacia justo lo
 * que la constancia sirve para demostrar.
 *
 * <p>
 * Sin {@code companyId} tampoco: {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}. Viaja
 * como query param.
 */
public record AcknowledgeReversalRequest(
        @NotBlank(message = "Debes adjuntar la constancia del acuse.") @Size(max = 255, message = "La constancia no puede superar los 255 caracteres.") String acknowledgementRef) {
}
