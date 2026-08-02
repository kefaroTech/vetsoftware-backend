package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.FindHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.observation.find")
@Service
public class FindHospitalizationObservationService implements FindHospitalizationObservationUseCase {
    private final HospitalizationObservationRepository repository;

    public FindHospitalizationObservationService(HospitalizationObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public HospitalizationObservationDto findById(Long id, Long companyId) {
        return HospitalizationObservationDto.from(repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new HospitalizationObservationNotFoundException(id)));
    }
}
