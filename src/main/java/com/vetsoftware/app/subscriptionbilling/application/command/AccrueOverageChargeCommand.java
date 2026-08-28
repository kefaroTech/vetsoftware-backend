package com.vetsoftware.app.subscriptionbilling.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Devengar el <b>excedente</b>: la clínica consumió por encima del cupo que
 * contrató, y su línea de contrato declaró que puede hacerlo y a qué precio.
 *
 * <p>
 * <b>Es un command aparte del de alta general y no un {@code chargeType} más
 * dentro de aquél</b>, porque lo que cambia entre los dos no es la forma sino
 * <b>quién puede dispararlos</b>. Ver
 * {@link com.vetsoftware.app.subscriptionbilling.application.port.in.AccrueOverageChargeUseCase}.
 *
 * <p>
 * No lleva ni {@code chargeType} ni {@code prorationDays}: el tipo es siempre
 * {@code OVERAGE} —dejarlo abierto convertiría este camino, que un empleado del
 * tenant puede disparar, en una vía para devengar cualquier cosa— y un
 * excedente no es una fracción de periodo, es servicio prestado de más.
 *
 * @param overageUnits
 *            unidades por encima del techo, nunca el delta entero. Un cliente
 *            con el cupo en 98 de 100 que pide 5 paga 3
 * @param unitAmount
 *            precio por unidad <b>copiado</b> de
 *            {@code subscription_item_limits.overage_unit_amount}: dentro de un
 *            año la tarifa habrá cambiado y este cargo tiene que seguir
 *            explicándose solo
 */
public record AccrueOverageChargeCommand(Long companyId, Long subscriptionId,
        Long subscriptionItemId, String description, LocalDate servicePeriodStart,
        LocalDate servicePeriodEnd, int overageUnits, BigDecimal unitAmount) {
}
