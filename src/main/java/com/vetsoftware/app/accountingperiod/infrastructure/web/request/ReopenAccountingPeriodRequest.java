package com.vetsoftware.app.accountingperiod.infrastructure.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * <strong>Sin {@code reopenedBySystemUserId}: la firma la pone el controller
 * desde el principal autenticado.</strong> Aceptarla en el cuerpo dejaria que
 * quien reabre el mes escribiera el nombre de otro en la casilla que un auditor
 * mira primero, y la reapertura es exactamente la operacion sobre la que se
 * pregunta.
 *
 * @param reason
 *            <strong>el unico campo, y es obligatorio.</strong> El
 *            {@code @NotBlank} no es cosmetico: sin motivo,
 *            {@code reopened_reason} entraria vacio y
 *            {@code chk_accounting_periods_reopening} —que exige los tres
 *            campos de reapertura juntos— rechazaria la fila con un error de
 *            comprobacion que no nombra ni la columna. Con el, el binder
 *            contesta un error de campo que el front pinta bajo el textarea
 */
public record ReopenAccountingPeriodRequest(
        @NotBlank(message = "Debes escribir el motivo de la reapertura.") @Size(max = 255, message = "El motivo no puede superar los 255 caracteres.") String reason) {
}
