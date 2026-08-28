package com.vetsoftware.app.accountingexport.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El rechazo del contador. Sin {@code id} —lo lleva la ruta— y sin fecha: la
 * pone el caso de uso con su {@code Clock} inyectado, porque un
 * {@code LocalDateTime.now()} en la capa web es una fecha que ningun test puede
 * fijar.
 *
 * @param rejectionReason
 *            obligatorio, espejo de la tercera rama de
 *            {@code chk_accounting_exports_lifecycle}: un rechazo sin motivo
 *            escrito obliga a rehacer el fichero a ciegas
 */
public record RejectAccountingExportRequest(
        @NotBlank(message = "Debes indicar por que se rechaza la exportacion.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String rejectionReason) {
}
