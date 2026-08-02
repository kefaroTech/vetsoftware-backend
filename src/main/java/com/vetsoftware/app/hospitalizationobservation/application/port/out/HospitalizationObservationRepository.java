package com.vetsoftware.app.hospitalizationobservation.application.port.out;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import java.util.List;
import java.util.Optional;

public interface HospitalizationObservationRepository {
    HospitalizationObservation save(HospitalizationObservation observation);

    Optional<HospitalizationObservation> findById(Long id);

    Optional<HospitalizationObservation> findByIdAndCompanyId(Long id, Long companyId);

    List<HospitalizationObservation> findAllByHospitalizationId(Long hospitalizationId);

    void delete(Long id);

    int reactivate(Long id);
}
