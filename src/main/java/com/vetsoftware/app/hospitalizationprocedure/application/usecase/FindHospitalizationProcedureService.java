package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.FindHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.procedure.find")
@Service
public class FindHospitalizationProcedureService implements FindHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public FindHospitalizationProcedureService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    public HospitalizationProcedureDto findById(Long id, Long companyId) {
        return HospitalizationProcedureDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new HospitalizationProcedureNotFoundException(id)));
    }
}
