package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.FindHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.find")
@Service
public class FindHospitalizationService implements FindHospitalizationUseCase {
    private final HospitalizationRepository repository;

    public FindHospitalizationService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public HospitalizationDto findById(Long id) {
        return HospitalizationDto.from(repository.findById(id)
            .orElseThrow(() -> new HospitalizationNotFoundException(id)));
    }
}
