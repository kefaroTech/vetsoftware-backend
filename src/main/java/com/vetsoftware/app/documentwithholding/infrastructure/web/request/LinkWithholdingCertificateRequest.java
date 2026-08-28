package com.vetsoftware.app.documentwithholding.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;

/**
 * Un solo campo, y aun asi es un {@code @RequestBody} y no un
 * {@code @RequestParam}.
 *
 * <p>
 * <strong>Sin {@code companyId} ni {@code id}</strong>: la retencion la
 * identifica la ruta y la empresa viaja como {@code @RequestParam}
 * ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}, que mira todo cuerpo sin mirar la
 * ruta ni el rol). Lo unico que este cuerpo aporta es a que certificado se
 * apunta.
 */
public record LinkWithholdingCertificateRequest(
        @NotNull(message = "Debes indicar el certificado que respalda la retencion.") Long certificateId) {
}
