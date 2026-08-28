package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El cierre del expediente.
 *
 * <p>
 * <strong>Sin {@code resolvedAt}</strong>: el instante lo pone el reloj
 * inyectado del servidor. Una fecha de resolucion escrita por quien resuelve se
 * puede antedatar a un periodo ya cerrado, que es justo lo que este par de
 * campos existe para impedir.
 *
 * @param postingPeriod
 *            periodo contable {@code YYYY-MM}. El {@code @Pattern} es el mismo
 *            que el {@code REGEXP} de {@code chk_eir_resolved}, con el mes
 *            acotado a {@code 01..12}: sin esa mitad, {@code 2026-13} pasaria
 *            por bueno. <strong>No tiene clave foranea contra
 *            {@code accounting_periods} y es una carencia declarada</strong> —
 *            esa tabla es de otra capa y no existe en el arbol de changesets—,
 *            asi que este formato es toda la comprobacion que hay
 */
public record ResolveExternalInvoiceReconciliationRequest(
        @NotNull(message = "Debes indicar quien resuelve la conciliacion.") Long resolvedBySystemUserId,
        @NotBlank(message = "Debes explicar como se resolvio el descuadre.") @Size(max = 255, message = "La nota de resolucion no puede superar los 255 caracteres.") String resolutionNote,
        @NotBlank(message = "Debes indicar el periodo contable.") @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "El periodo contable debe tener el formato AAAA-MM con el mes entre 01 y 12.") String postingPeriod) {
}
