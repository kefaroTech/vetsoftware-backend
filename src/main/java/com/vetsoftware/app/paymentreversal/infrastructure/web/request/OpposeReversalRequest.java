package com.vetsoftware.app.paymentreversal.infrastructure.web.request;

import com.vetsoftware.app.paymentreversal.domain.OppositionGround;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Oponerse exige constancia: los dos campos son obligatorios, no uno. Sin
 * {@code companyId} en el cuerpo ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}).
 */
public record OpposeReversalRequest(
        @NotNull(message = "Debes indicar el motivo de la oposicion.") OppositionGround ground,
        @NotBlank(message = "Debes adjuntar la constancia de la oposicion.") @Size(max = 255, message = "La constancia no puede superar los 255 caracteres.") String oppositionEvidenceRef) {
}
