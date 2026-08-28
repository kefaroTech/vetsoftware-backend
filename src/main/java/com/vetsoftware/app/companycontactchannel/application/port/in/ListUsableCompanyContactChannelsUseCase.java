package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.companycontactchannel.domain.ContactPurpose;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La consulta caliente de la feature: por donde se le puede escribir hoy a esta
 * empresa para este fin.
 */
public interface ListUsableCompanyContactChannelsUseCase {

    /**
     * <strong>El proposito es obligatorio y no tiene valor por defecto.</strong>
     * Autorizar un proposito no autoriza los demas, asi que un defecto silencioso
     * —devolver todos los canales vivos, por ejemplo— convertiria esta consulta en
     * la lista de a quien se puede escribir <em>para cualquier cosa</em>, que es
     * justo la confusion que la columna {@code purpose} existe para evitar: el
     * correo que el cliente dio para su factura no es permiso para mandarle una
     * promocion.
     *
     * <p>
     * Sin hermano sin acotar: un listado que no filtra por empresa devuelve filas
     * de todos los tenants ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<CompanyContactChannelDto> listUsable(Long companyId, ContactPurpose purpose,
            int page, int pageSize);
}
