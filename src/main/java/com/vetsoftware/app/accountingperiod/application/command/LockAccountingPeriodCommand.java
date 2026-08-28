package com.vetsoftware.app.accountingperiod.application.command;

/**
 * Declarar un mes: pasa a {@code LOCKED} y ya no se toca.
 *
 * <p>
 * <strong>Es un command propio y no un campo del cierre.</strong> Un
 * {@code CloseAccountingPeriodCommand(id, status)} dejaria que el estado de
 * destino viajara como dato, y con el la diferencia entre «cerrado y
 * corregible» y «declarado y definitivo» quedaria a un caracter de distancia en
 * un JSON. Son dos decisiones de negocio distintas, con dos rutas distintas y
 * dos casos de uso distintos.
 *
 * @param systemUserId
 *            quien firma. Lo pone el controller desde
 *            {@code authz.currentSystemUserId()}, nunca el cuerpo. Solo se
 *            escribe si el mes se declara estando abierto: desde
 *            {@code SOFT_CLOSED} el cierre ya tiene firma y no se sobrescribe
 */
public record LockAccountingPeriodCommand(Long id, Long systemUserId) {
}
