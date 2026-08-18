package com.vetsoftware.app.medicationschedule.application.port.in;

import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListMedicationSchedulesByHospitalizationUseCase {
    /**
     * El {@code hospitalizationId} lo escribe el cliente en la URL y la
     * hospitalización es de alguien: sin {@code companyId} este listado entregaba
     * la hoja de medicación completa del paciente de otro tenant. El
     * {@code companyId} no viaja en el request: lo pone el controller desde el
     * contexto autenticado.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read')"
            + " and @authz.isMyCompany(#companyId))")
    List<MedicationScheduleDto> listByHospitalization(Long hospitalizationId, Long companyId);
}
