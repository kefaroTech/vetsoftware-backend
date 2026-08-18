package com.vetsoftware.app.hospitalizationobservation.application.port.out;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

public interface HospitalizationObservationRepository {
    HospitalizationObservation save(HospitalizationObservation observation);

    Optional<HospitalizationObservation> findById(Long id);

    Optional<HospitalizationObservation> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<HospitalizationObservation> findAllByHospitalizationIdAndCompanyId(
            Long hospitalizationId, Long companyId, int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id, Long companyId);
}
