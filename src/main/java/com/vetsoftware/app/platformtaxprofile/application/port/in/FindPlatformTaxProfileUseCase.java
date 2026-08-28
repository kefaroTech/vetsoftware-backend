package com.vetsoftware.app.platformtaxprofile.application.port.in;

import com.vetsoftware.app.platformtaxprofile.application.dto.PlatformTaxProfileDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Una identidad fiscal concreta del historico, por id. */
public interface FindPlatformTaxProfileUseCase {

    /**
     * <strong>Recibe un {@code Long} y no lleva {@code companyId}, y eso lo mira
     * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}</strong> (dura, BE-COV).
     * La regla solo se activa si alguna entidad JPA de la feature alcanza
     * {@code CompanyJpaEntity} por asociaciones, y esta no lo hace — la tabla es
     * global—; aun asi el gate es {@code hasRole('SYSTEM')} a secas, que es lo que
     * la regla exigiria de todos modos.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PlatformTaxProfileDto findById(Long id);
}
