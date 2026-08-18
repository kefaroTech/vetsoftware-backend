package com.vetsoftware.app.employeerole.application.command;

/**
 * {@code companyId} es la empresa del principal que asigna, no un dato del
 * cliente: lo pone el controller desde el {@code AuthContext}. Viaja en el
 * comando porque las DOS referencias entrantes —el empleado titular y el rol—
 * hay que resolverlas acotadas, y sin la empresa aqui el servicio no tiene con
 * que acotarlas.
 *
 * <p>
 * {@code null} es el principal cross-tenant (SYSTEM), que si opera global: por
 * ahi entra el alta de una empresa nueva, que se auto-asigna el rol ADMIN
 * cuando todavia no existe ningun empleado con sesion.
 */
public record CreateEmployeeRoleCommand(Long employeeId, Long roleId, Long companyId) {
}
