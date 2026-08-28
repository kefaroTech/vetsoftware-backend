package com.vetsoftware.app.companyusageevent.application.port.in;

import com.vetsoftware.app.companyusageevent.application.dto.CompanyUsageEventDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListCompanyUsageEventsUseCase {

    /**
     * El barrido de plataforma: todos los hechos, de todas las clinicas.
     *
     * <p>
     * <strong>No filtra por empresa, asi que solo lo puede servir
     * {@code hasRole('SYSTEM')} a secas</strong> ({@code
     * LISTADOS_SIN_EMPRESA_SOLO_SYSTEM}, BE-29). No es una limitacion: es la
     * consulta del cierre mensual, que por definicion cruza tenants.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyUsageEventDto> listAll(int page, int pageSize);

    /**
     * Los hechos de <b>una</b> clinica: el hermano acotado, que es lo que se ensena
     * cuando un cliente discute un excedente.
     *
     * <p>
     * Tambien {@code SYSTEM} a secas, por coherencia del gate de la feature: hoy no
     * hay pantalla de tenant sobre esta tabla. El dia que la haya, este es el
     * metodo que se abre —tiene por donde acotar— y el de arriba <b>no</b>.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CompanyUsageEventDto> listByCompany(Long companyId, int page, int pageSize);
}
