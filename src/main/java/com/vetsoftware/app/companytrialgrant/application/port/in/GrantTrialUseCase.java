package com.vetsoftware.app.companytrialgrant.application.port.in;

import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Concede la prueba de un artículo dentro de la ventana viva de la empresa.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, y esto no es rigidez.
 * Regalar software es una decisión comercial de plataforma; si el gate
 * admitiera al empleado de la clínica, la administradora podría concederse los
 * veintiséis artículos del catálogo y toda la capa I dejaría de servir para
 * nada.
 */
public interface GrantTrialUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyTrialGrantDto execute(GrantTrialCommand command);
}
