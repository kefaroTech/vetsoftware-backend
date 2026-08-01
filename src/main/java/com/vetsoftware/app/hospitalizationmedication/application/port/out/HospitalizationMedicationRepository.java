package com.vetsoftware.app.hospitalizationmedication.application.port.out;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import java.util.List;
import java.util.Optional;

public interface HospitalizationMedicationRepository {
    HospitalizationMedication save(HospitalizationMedication medication);
    Optional<HospitalizationMedication> findById(Long id);
    Optional<HospitalizationMedication> findByIdAndCompanyId(Long id, Long companyId);
    List<HospitalizationMedication> findAllByHospitalizationId(Long hospitalizationId);
    void delete(Long id);
    int reactivate(Long id);
}
