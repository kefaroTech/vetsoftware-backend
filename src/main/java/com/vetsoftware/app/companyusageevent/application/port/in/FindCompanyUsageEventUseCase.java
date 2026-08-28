package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyUsageEventUseCase {

    /**
     * Un hecho concreto, para el expediente de una reclamacion.
     *
     * <p>
     * <strong>Recibe un {@code id} y no recibe {@code companyId}, y por eso el
     * unico gate posible es {@code hasRole('SYSTEM')} a secas</strong>
     * ({@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}, BE-COV): el permiso dice
     * que puede hacer un empleado, nunca sobre que filas, y un {@code id} lo
     * escribe el cliente en la URL. Como {@code CompanyUsageEventJpaEntity} alcanza
     * {@code companies} por su {@code company_id}, esa regla esta activa sobre toda
     * la feature.
     *
     * <p>
     * Que la carga sea ancha —{@code findById} y no {@code findByIdAndCompanyId}—
     * es correcto <em>aqui y solo aqui</em>: un principal {@code SYSTEM} no tiene
     * empresa contra la que acotar. Es la exencion que
     * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} contempla para el servicio que solo
     * alcanza {@code SYSTEM}. El dia que esta operacion se abra a un tenant, la
     * carga tiene que pasar a la variante acotada <b>antes</b> de tocar el
     * {@code @PreAuthorize}.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyUsageEventDto findById(Long id);
}
