package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.command.PurchaseModulesCommand;
import com.vetsoftware.app.quote.application.dto.PurchaseModulesResultDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Compra uno o varios módulos desde el escaparate del tenant.
 *
 * <p>
 * <strong>El {@code @PreAuthorize} es defensa en profundidad de tenant, no la
 * autorización completa.</strong> Reutiliza {@code quote.request} —el mismo
 * permiso que ya abre la autocontratación— porque comprar módulos genera y
 * acepta exactamente esa clase de cotización. Pero {@code quote.request} lo
 * puede tener cualquier empleado con ese permiso granular, y esta operación es
 * exclusiva del rol base {@code ADMIN} de la empresa: esa comprobación —«el
 * permiso solo no basta»— vive en {@code PurchaseModulesService} contra
 * {@link com.vetsoftware.app.quote.application.port.out.EmployeeAdminCheckPort},
 * porque {@code ADMIN} no es una autoridad de Spring Security que un SpEL pueda
 * mirar: es un rol de negocio resuelto contra {@code roles.code}.
 */
public interface PurchaseModulesUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.request') "
            + "and @authz.isMyCompany(#command.companyId))")
    PurchaseModulesResultDto execute(PurchaseModulesCommand command);
}
