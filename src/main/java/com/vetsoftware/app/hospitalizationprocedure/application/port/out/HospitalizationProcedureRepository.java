package com.vetsoftware.app.hospitalizationprocedure.application.port.out;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.PageResult;
import java.util.Optional;

public interface HospitalizationProcedureRepository {
    HospitalizationProcedure save(HospitalizationProcedure procedure);

    Optional<HospitalizationProcedure> findById(Long id);

    Optional<HospitalizationProcedure> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<HospitalizationProcedure> findAllByHospitalizationIdAndCompanyId(
            Long hospitalizationId, Long companyId, int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
