package com.vetsoftware.app.hospitalizationmedication.application.port.out;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedication;
import com.vetsoftware.app.hospitalizationmedication.application.dto.PageResult;
import java.util.Optional;

public interface HospitalizationMedicationRepository {
    HospitalizationMedication save(HospitalizationMedication medication);

    Optional<HospitalizationMedication> findById(Long id);

    Optional<HospitalizationMedication> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<HospitalizationMedication> findAllByHospitalizationId(Long hospitalizationId,
            int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
