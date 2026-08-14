package com.vetsoftware.app.hospitalizationobservation.application.port.in;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationObservationsByHospitalizationUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('hospitalization.read') and @authz.isMyCompany(#companyId))")
    PageResult<HospitalizationObservationDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize);
}
