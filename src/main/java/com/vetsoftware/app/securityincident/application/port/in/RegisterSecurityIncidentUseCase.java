package com.vetsoftware.app.securityincident.application.port.in;

import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.dto.SecurityIncidentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterSecurityIncidentUseCase {

    /**
     * Da de alta un incidente y <strong>fija su vencimiento</strong>.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> Un incidente de seguridad es de la
     * plataforma: alcanza a varias clinicas y ninguna de ellas puede darlo de alta,
     * ni verlo entero, ni saber a quien mas alcanzo. Abrirlo por permiso pondria el
     * censo de afectados —cuantas clinicas, cuantos titulares de cada una— al
     * alcance de cualquiera con una autoridad sembrada.
     *
     * <p>
     * El plazo de reporte lo calcula el caso de uso contra el calendario laboral,
     * contando quince dias habiles desde el escalamiento interno. Si el tramo del
     * calendario no cubre el recorrido, la operacion <b>falla</b> con
     * {@code HolidayCalendarGapException} en vez de caer a dias corridos.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SecurityIncidentDto execute(RegisterSecurityIncidentCommand command);
}
