package com.vetsoftware.app.debtopenaccount.application.command;

/**
 * Borrado (logico) de un abono. Lleva actor, motivo y version esperada por el
 * mismo motivo que {@link VoidDebtOpenAccountCommand}: quitar un abono mueve
 * dinero —sube el saldo pendiente de la cuenta y hay que compensar la caja—, no
 * es una limpieza de datos.
 *
 * @param deletedById
 *            empleado que ejecuta la baja; su caja OPEN es la que recibe la
 *            compensacion (VOID_OUT)
 * @param reason
 *            motivo obligatorio, se persiste como motivo de anulacion para que
 *            la baja quede auditable
 * @param expectedVersion
 *            version de la CUENTA que vio el front (opt-in); null = sin chequeo
 */
public record DeleteDebtOpenAccountCommand(Long id, Long companyId, Long deletedById, String reason,
        Long expectedVersion) {
}
