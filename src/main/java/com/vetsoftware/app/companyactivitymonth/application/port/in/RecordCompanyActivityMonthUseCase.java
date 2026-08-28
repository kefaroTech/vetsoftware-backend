package com.vetsoftware.app.companyactivitymonth.application.port.in;

import com.vetsoftware.app.companyactivitymonth.application.command.RecordCompanyActivityMonthCommand;
import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RecordCompanyActivityMonthUseCase {

    /**
     * Da de alta la fila de actividad de una clinica en un mes.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas, y la ausencia de un camino de
     * tenant es la decision.</strong> Esta serie es el instrumento con el que
     * plataforma mide si un cliente se esta yendo; abrirla por permiso dejaria que
     * la clinica escribiera sus propios numeros de actividad —cuantos dias entro,
     * cuanto MRR aportaba— justo sobre el dato que se usa para decidir sobre ella.
     * No es que hoy no haga falta el camino de tenant: es que no debe existir.
     *
     * <p>
     * Ni siquiera una {@code hasAuthority} suelta como alternativa: en una feature
     * cerrada a {@code SYSTEM}, una autoridad suelta es un endpoint que se abre
     * sembrando un permiso ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyActivityMonthDto execute(RecordCompanyActivityMonthCommand command);
}
