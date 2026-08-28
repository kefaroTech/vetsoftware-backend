package com.vetsoftware.app.companyactivitymonth.application.port.in;

import com.vetsoftware.app.companyactivitymonth.application.dto.CompanyActivityMonthDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindCompanyActivityMonthUseCase {

    /**
     * Una fila de actividad por su identificador.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Recibe un {@code Long} y
     * no recibe empresa: es la forma literal de la familia «por id» de BE-COV, y
     * para ella la unica autorizacion admisible es {@code SYSTEM} sin alternativa.
     * El permiso dice <em>que</em> puede hacer alguien, nunca <em>sobre que
     * filas</em>, y un {@code id} lo escribe el cliente en la URL.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyActivityMonthDto findById(Long id);

    /**
     * La fila de una clinica en un mes concreto.
     *
     * <p>
     * Devuelve <b>una o ninguna</b>: {@code uq_cam_month (company_id, period_key)}
     * lo garantiza en la base.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    CompanyActivityMonthDto findByCompanyIdAndPeriodKey(Long companyId, String periodKey);
}
