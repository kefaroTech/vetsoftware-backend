package com.vetsoftware.app.hospitalizationprocedure.application.usecase;

import com.vetsoftware.app.hospitalizationprocedure.application.port.in.DeleteHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedureNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.procedure.delete")
@Service
public class DeleteHospitalizationProcedureService
        implements
            DeleteHospitalizationProcedureUseCase {
    private final HospitalizationProcedureRepository repository;

    public DeleteHospitalizationProcedureService(HospitalizationProcedureRepository repository) {
        this.repository = repository;
    }

    /**
     * La existencia se comprueba acotada por empresa: una orden de otro tenant es
     * indistinguible de una inexistente y sale como 404, sin llegar al delete.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationProcedureNotFoundException(id));
        repository.delete(id);
    }
}
