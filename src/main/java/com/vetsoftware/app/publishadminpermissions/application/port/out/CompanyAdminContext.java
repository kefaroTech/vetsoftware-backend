package com.vetsoftware.app.publishadminpermissions.application.port.out;

/**
 * La empresa ya no aporta {@code membershipId}: lo que decide que submodulos
 * alcanza la republicacion es su contrato, y eso se resuelve por
 * {@code companyId} contra {@code company_entitlements}.
 */
public record CompanyAdminContext(Long companyId, Long adminRoleId) {
}
