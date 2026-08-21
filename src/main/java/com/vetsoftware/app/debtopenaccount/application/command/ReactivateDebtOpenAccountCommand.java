package com.vetsoftware.app.debtopenaccount.application.command;

/**
 * Reactivacion de un abono dado de baja. Lleva actor por el mismo motivo que
 * {@link DeleteDebtOpenAccountCommand}: devolver un abono a la vida <b>mueve
 * dinero</b> —vuelve a descontar del saldo pendiente de la cuenta y el ingreso
 * tiene que volver a entrar en caja—, y la caja que lo recibe es la del
 * empleado que reactiva, igual que la compensacion de la baja va a la del que
 * da de baja.
 *
 * <p>
 * <b>Sin {@code expectedVersion}, y es deliberado.</b> El endpoint que alimenta
 * este caso de uso es un {@code PATCH /{id}/enable} <em>sin cuerpo</em>, asi
 * que no hay donde viajar la version que vio el front sin cambiar el contrato
 * de la API. Es la misma decision que se tomo en #239 para los cargos, anotada
 * alli como #248; el conflicto se sigue detectando, pero tarde: el
 * {@code UPDATE} nativo de reactivar mueve la {@code version} del abono, asi
 * que un {@code save} concurrente cargado antes ya no encuentra fila y sale un
 * 409.
 *
 * @param reactivatedById
 *            empleado que reactiva; su caja OPEN es la que recibe de vuelta el
 *            ingreso (OPEN_ACCOUNT_IN)
 */
public record ReactivateDebtOpenAccountCommand(Long id, Long companyId, Long reactivatedById) {
}
