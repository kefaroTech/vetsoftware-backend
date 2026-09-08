package com.vetsoftware.app.quote.application.command;

import java.util.List;

/**
 * Compra de módulos desde el escaparate del tenant.
 *
 * @param companyId
 *            la empresa que compra. Lo inyecta el controller desde el
 *            principal; nunca viaja en el cuerpo REST.
 * @param employeeId
 *            quien compra. El servicio exige que sea el rol base ADMIN de esa
 *            empresa: el permiso de cotizar solo no basta.
 * @param catalogItemCodes
 *            rótulos del catálogo público a comprar, uno por línea.
 * @param billingCycle
 *            {@code MONTHLY} o {@code ANNUAL}.
 * @param paymentSourceId
 *            el medio de pago ya registrado con el que se autoriza el cobro.
 * @param clientRequestId
 *            llave de idempotencia de la cotización de autoservicio.
 * @param acceptedIp
 *            la IP de la petición, prueba de la aceptación — igual que en
 *            {@code POST /quotes/{id}/accept}, nunca del cuerpo.
 */
public record PurchaseModulesCommand(Long companyId, Long employeeId, List<String> catalogItemCodes,
        String billingCycle, Long paymentSourceId, String clientRequestId, String acceptedIp) {
}
