package com.vetsoftware.app.subscriptionmodule.application.command;

/**
 * @param companyId
 *            la empresa que consulta. Lo inyecta el controller desde el
 *            principal; nunca viaja en la petición.
 * @param employeeId
 *            quien consulta, para resolver {@code canPurchase} (rol ADMIN).
 */
public record ListModuleShowcaseQuery(Long companyId, Long employeeId) {
}
