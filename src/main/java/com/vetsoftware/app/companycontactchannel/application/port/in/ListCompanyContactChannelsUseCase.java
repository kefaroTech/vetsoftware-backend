package com.vetsoftware.app.companycontactchannel.application.port.in;

import com.vetsoftware.app.companycontactchannel.application.dto.CompanyContactChannelDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * La bitacora completa de la empresa, revocados incluidos.
 *
 * <p>
 * <strong>Es el hermano probatorio de
 * {@link ListUsableCompanyContactChannelsUseCase}, y por eso son dos casos de
 * uso y no un parametro.</strong> Aquel contesta por donde se puede escribir
 * hoy; este, por donde se podia escribir en marzo. Fundirlos en uno con un
 * {@code boolean includeRevoked} dejaria la respuesta legal dependiendo de un
 * flag que es facil no mandar, y el valor por defecto decidiria en silencio si
 * la empresa puede o no demostrar lo que hizo.
 */
public interface ListCompanyContactChannelsUseCase {

    /**
     * Sin hermano sin acotar: un listado que no filtra por empresa devuelve filas
     * de todos los tenants ({@code LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}), y aqui esas
     * filas son las direcciones de contacto de todas las clinicas.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('companyContactChannel.read')"
            + " and @authz.isMyCompany(#companyId))")
    PageResult<CompanyContactChannelDto> listByCompany(Long companyId, int page, int pageSize);
}
