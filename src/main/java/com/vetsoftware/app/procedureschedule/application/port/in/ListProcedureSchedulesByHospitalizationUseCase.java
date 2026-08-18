package com.vetsoftware.app.procedureschedule.application.port.in;

import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListProcedureSchedulesByHospitalizationUseCase {
    /**
     * El {@code hospitalizationId} lo escribe el cliente en la URL y la
     * hospitalización es de alguien: sin {@code companyId} este listado entregaba
     * el plan de procedimientos completo del paciente de otro tenant. El
     * {@code companyId} no viaja en el request: lo pone el controller desde el
     * contexto autenticado.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<ProcedureScheduleDto> listByHospitalization(Long hospitalizationId, Long companyId);
}
