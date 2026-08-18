package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.port.in.DeleteHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.observation.delete")
@Service
public class DeleteHospitalizationObservationService
        implements
            DeleteHospitalizationObservationUseCase {
    private final HospitalizationObservationRepository repository;

    public DeleteHospitalizationObservationService(
            HospitalizationObservationRepository repository) {
        this.repository = repository;
    }

    /**
     * La existencia se comprueba acotada por empresa: una observacion de otro
     * tenant es indistinguible de una inexistente y sale como 404, sin llegar al
     * delete.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationObservationNotFoundException(id));
        repository.delete(id);
    }
}
