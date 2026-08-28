package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyContactChannelUseCase {

    /**
     * Un {@code id} lo escribe el cliente en la URL, asi que el {@code companyId}
     * viaja siempre y la carga va acotada por el en el puerto de salida. No existe
     * la variante ancha a proposito (BE-COV,
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA}).
     *
     * <p>
     * El canal de otra empresa sale como <strong>no encontrado</strong> y no como
     * prohibido: un 403 confirmaria que la fila existe, y con ids consecutivos eso
     * es un censo de por donde se le escribe a la competencia.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.read')"
            + " and @authz.isMyCompany(#companyId))")
    CompanyContactChannelDto findById(Long id, Long companyId);
}
