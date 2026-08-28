package com.vetsoftware.app.supplierwithholding.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * La emision del certificado. Sin {@code id} —lo lleva la ruta— y sin fecha: la
 * pone el caso de uso con su {@code Clock} inyectado, porque es un dato
 * probatorio y aceptarlo por HTTP dejaria antedatar la emision.
 */
public record IssueSupplierWithholdingCertificateRequest(
        @NotBlank(message = "Debes indicar el numero del certificado.") @Size(max = 100, message = "El numero del certificado no puede superar los 100 caracteres.") String certificateRef) {
}
