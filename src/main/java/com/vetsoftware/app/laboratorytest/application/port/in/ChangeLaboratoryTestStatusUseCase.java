package com.vetsoftware.app.laboratorytest.application.port.in;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ChangeLaboratoryTestStatusUseCase {
    /**
     * El {@code id} lo escribe el cliente en la URL, así que el permiso
     * {@code laboratoryTest.update} no basta: dice <em>qué</em> puede hacer el
     * empleado, no <em>sobre qué muestra</em>. Sin el {@code companyId} este puerto
     * dejaba cambiar el estado —y firmar la validación— de una orden de laboratorio
     * de otro tenant (BE-31).
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('laboratoryTest.update')"
            + " and @authz.isMyCompany(#command.companyId))")
    LaboratoryTestDto execute(ChangeLaboratoryTestStatusCommand command);
}
