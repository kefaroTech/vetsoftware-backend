package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.port.in.DeleteHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.procedure.delete")
@Service
public class DeleteHospitalizationProcedureService implements DeleteHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public DeleteHospitalizationProcedureService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new HospitalizationProcedureNotFoundException(id));
        repository.delete(id);
    }
}
