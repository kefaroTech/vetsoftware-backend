package com.vetsoftware.app.companybillingprofile.application.port.in;

import com.vetsoftware.app.companybillingprofile.application.command.SucceedCompanyBillingProfileCommand;
import com.vetsoftware.app.companybillingprofile.application.dto.CompanyBillingProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambia los datos de facturacion de una empresa cerrando la ficha vigente y
 * abriendo su sucesora, en una sola transaccion.
 *
 * <p>
 * <strong>Este puerto es el que ocupa el sitio del {@code update} que no
 * existe.</strong> Quien busque «como se corrige el NIT» acabara aqui, y lo que
 * tiene que leer es que no se corrige: se sucede. La ficha vieja se queda
 * intacta porque una factura ya emitida apunta a ella y tiene que seguir
 * diciendo a quien se emitio.
 */
public interface SucceedCompanyBillingProfileUseCase {

    /**
     * Devuelve la ficha <strong>nueva</strong>, no la cerrada: es la que rige a
     * partir de {@code effectiveFrom} y la que el front tiene que pintar.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('company.update') and"
            + " @authz.isMyCompany(#command.companyId))")
    CompanyBillingProfileDto execute(SucceedCompanyBillingProfileCommand command);
}
