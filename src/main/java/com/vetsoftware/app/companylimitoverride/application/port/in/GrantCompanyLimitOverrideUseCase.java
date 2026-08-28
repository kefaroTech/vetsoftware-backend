package com.vetsoftware.app.companylimitoverride.application.port.in;

import com.vetsoftware.app.companylimitoverride.application.command.GrantCompanyLimitOverrideCommand;
import com.vetsoftware.app.companylimitoverride.application.dto.CompanyLimitOverrideDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Negocia una excepción de techo para una empresa.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, y aquí no cabe otra cosa. Si
 * el gate admitiera al empleado de la clínica, la administradora se subiría el
 * techo cada vez que topa y el cupo dejaría de ser un cupo. Subir un techo es
 * una decisión comercial de plataforma, con firma.
 */
public interface GrantCompanyLimitOverrideUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyLimitOverrideDto execute(GrantCompanyLimitOverrideCommand command);
}
