package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ReactivateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.observation.reactivate")
@Service
public class ReactivateHospitalizationObservationService implements ReactivateHospitalizationObservationUseCase {
    private final HospitalizationObservationRepository repository;

    public ReactivateHospitalizationObservationService(HospitalizationObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationObservationDto execute(Long id) {
        int updated = repository.reactivate(id);
        if (updated == 0) throw new HospitalizationObservationNotFoundException(id);
        return HospitalizationObservationDto.from(repository.findById(id)
            .orElseThrow(() -> new HospitalizationObservationNotFoundException(id)));
    }
}
