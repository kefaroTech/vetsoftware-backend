package com.vetsoftware.app.hospitalizationmedication.application.port.in;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationMedicationsByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    PageResult<HospitalizationMedicationDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize);
}
