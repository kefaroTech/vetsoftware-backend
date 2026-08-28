package com.vetsoftware.app.accountingperiod.application.command;

/**
 * Reabrir un mes cerrado.
 *
 * @param systemUserId
 *            quien firma la reapertura. Lo pone el controller desde
 *            {@code authz.currentSystemUserId()}, nunca el cuerpo
 * @param reason
 *            <strong>obligatorio, y es el unico campo de esta ficha que existe
 *            solo para que alguien lo lea despues.</strong> Un cierre que
 *            cualquiera deshace sin decir por que no significa nada: la
 *            reapertura es la operacion que un revisor mira primero, y sin
 *            motivo escrito lo unico que puede reconstruir es que el mes se
 *            abrio otra vez
 */
public record ReopenAccountingPeriodCommand(Long id, Long systemUserId, String reason) {
}
