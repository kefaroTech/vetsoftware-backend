package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.port.in.DeleteHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.delete")
@Service
public class DeleteHospitalizationService implements DeleteHospitalizationUseCase {
    private final HospitalizationRepository repository;

    public DeleteHospitalizationService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    /**
     * La existencia se comprueba acotada por empresa: una hospitalizacion de otro
     * tenant es indistinguible de una inexistente y sale como 404, sin llegar al
     * delete.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationNotFoundException(id));
        repository.delete(id);
    }
}
