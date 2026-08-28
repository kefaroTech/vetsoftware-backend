package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.command.RecordCompanyUsageEventCommand;
import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RecordCompanyUsageEventUseCase {

    /**
     * Anota un hecho de consumo de una empresa.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> Quien escribe aqui es el medidor de la
     * plataforma, no la clinica: un tenant capaz de anotar sus propios hechos
     * podria <em>no</em> anotarlos y dejar de pagar el excedente, o anotar los de
     * otro. Esta tabla es la prueba de un cobro, y la prueba no la escribe la parte
     * a la que se le cobra.
     *
     * <p>
     * Cerrarlo a {@code SYSTEM} a secas es ademas lo que mantiene coherente el gate
     * de toda la feature: una {@code hasAuthority} suelta aqui seria un endpoint
     * que se abre sembrando un permiso
     * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyUsageEventDto execute(RecordCompanyUsageEventCommand command);
}
