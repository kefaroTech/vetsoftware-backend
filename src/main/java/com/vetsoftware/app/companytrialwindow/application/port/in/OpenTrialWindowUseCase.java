package com.vetsoftware.app.companytrialwindow.application.port.in;

import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Abre el reloj de la empresa. Se dispara al aceptar la cotización, que es el
 * único camino de alta (D-55): todo cliente nace con su ventana, la use o no.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas, y esto es una decisión de
 * negocio antes que técnica. Conceder días de prueba es una decisión comercial
 * de plataforma; si el gate admitiera al empleado de la clínica, la
 * administradora podría abrirse ventanas y el abuso que toda la capa I existe
 * para cerrar entraría por la puerta principal.
 */
public interface OpenTrialWindowUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyTrialWindowDto execute(OpenTrialWindowCommand command);
}
