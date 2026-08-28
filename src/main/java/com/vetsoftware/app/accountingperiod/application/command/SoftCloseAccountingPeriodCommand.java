package com.vetsoftware.app.accountingperiod.application.command;

/**
 * Cerrar un mes dejandolo corregible.
 *
 * @param systemUserId
 *            quien firma el cierre. <strong>Lo pone el controller desde
 *            {@code authz.currentSystemUserId()}, nunca el cuerpo de la
 *            peticion.</strong> Es el mismo criterio que impide que un
 *            {@code companyId} viaje en el cuerpo, y aqui la consecuencia de
 *            saltarselo seria peor: un cliente podria firmar el cierre del mes
 *            con el id de otra persona, y la firma es justo el dato por el que
 *            un auditor pregunta
 */
public record SoftCloseAccountingPeriodCommand(Long id, Long systemUserId) {
}
