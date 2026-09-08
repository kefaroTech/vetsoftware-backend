package com.vetsoftware.app.quote.application.port.out;

/**
 * Si el empleado tiene, en esa empresa, el rol base {@code ADMIN} (D-9): el
 * único que puede comprar módulos desde el escaparate. El permiso de cotizar
 * ({@code quote.request}) no distingue esto — por eso el {@code @PreAuthorize}
 * de
 * {@link com.vetsoftware.app.quote.application.port.in.PurchaseModulesUseCase}
 * no basta solo, y este puerto es la comprobación que falta.
 */
public interface EmployeeAdminCheckPort {

    boolean isCompanyAdmin(Long employeeId, Long companyId);
}
