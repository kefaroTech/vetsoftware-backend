package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.command.DesignatePrimaryCompanyContactChannelCommand;
import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Designar el canal principal de un proposito.
 *
 * <p>
 * <strong>Es un caso de uso propio y no un campo del alta.</strong> Marcar el
 * primario decide por donde sale la factura o el aviso de mora de la empresa;
 * escondido dentro de un {@code POST} de alta, un alta rutinaria desviaria esa
 * comunicacion sin que nadie lo lea como lo que es. Aqui ademas se ve en el
 * permiso: designar exige {@code update}, no {@code create}.
 */
public interface DesignatePrimaryCompanyContactChannelUseCase {

    /**
     * <strong>Hay un primario por empresa Y PROPOSITO.</strong> Designar el correo
     * de facturacion no toca el movil de mora: el indice unico del esquema es
     * {@code (primary_marker, purpose)}, y este caso de uso solo libera al
     * incumbente <em>del mismo proposito</em> que el canal senalado.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    CompanyContactChannelDto execute(DesignatePrimaryCompanyContactChannelCommand command);
}
