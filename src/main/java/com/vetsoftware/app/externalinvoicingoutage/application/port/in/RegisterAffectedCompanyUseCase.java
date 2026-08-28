package com.vetsoftware.app.externalinvoicingoutage.application.port.in;

import com.vetsoftware.app.externalinvoicingoutage.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.dto.OutageAffectedCompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterAffectedCompanyUseCase {

    /**
     * Mete a una clinica en el reparto de una caida.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y el {@code companyId} del command
     * NO se valida contra el principal.</strong> No es un descuido: un principal
     * SYSTEM no tiene empresa propia contra la que comparar —{@code isMyCompany}
     * lanzaria {@code AccessDeniedException}— y el reparto consiste precisamente en
     * elegir a que clinicas alcanzo, igual que en tesoreria.
     *
     * <p>
     * Y por eso <b>no hay hermano de tenant</b>: una clinica no puede declararse a
     * si misma alcanzada por una caida, porque de esa declaracion cuelga la
     * justificacion de su numeracion de contingencia ante la autoridad.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    OutageAffectedCompanyDto execute(RegisterAffectedCompanyCommand command);
}
