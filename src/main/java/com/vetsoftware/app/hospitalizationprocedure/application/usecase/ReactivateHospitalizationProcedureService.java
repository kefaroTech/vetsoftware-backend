package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ReactivateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.procedure.reactivate")
@Service
public class ReactivateHospitalizationProcedureService implements ReactivateHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public ReactivateHospitalizationProcedureService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationProcedureDto execute(Long id) {
        int updated = repository.reactivate(id);
        if (updated == 0) throw new HospitalizationProcedureNotFoundException(id);
        return HospitalizationProcedureDto.from(repository.findById(id)
            .orElseThrow(() -> new HospitalizationProcedureNotFoundException(id)));
    }
}
