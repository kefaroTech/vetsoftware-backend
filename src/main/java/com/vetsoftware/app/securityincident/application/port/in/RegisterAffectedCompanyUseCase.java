package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.securityincident.application.dto.AffectedCompanyDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterAffectedCompanyUseCase {

    /**
     * Anota que una clinica quedo alcanzada, y por que ambito.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas aunque el command lleve un
     * {@code companyId}.</strong> No es el tenant que llama —un principal SYSTEM no
     * tiene empresa y {@code @authz.isMyCompany} seria falso siempre— sino a que
     * clinica alcanzo el incidente. La empresa llega por la URL, no por el cuerpo:
     * ver {@link RegisterAffectedCompanyCommand}.
     *
     * <p>
     * <strong>Y no hay operacion inversa.</strong> Quitar una clinica de la lista
     * de afectados es destruir la prueba de que se le notifico, asi que no existe
     * ni el caso de uso, ni el metodo en el puerto de salida, ni el endpoint.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    AffectedCompanyDto execute(RegisterAffectedCompanyCommand command);
}
