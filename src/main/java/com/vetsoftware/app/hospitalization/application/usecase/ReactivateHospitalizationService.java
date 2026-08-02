package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.ReactivateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.reactivate")
@Service
public class ReactivateHospitalizationService implements ReactivateHospitalizationUseCase {
    private final HospitalizationRepository repository;

    public ReactivateHospitalizationService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new HospitalizationNotFoundException(id);
        return HospitalizationDto.from(repository.findById(id)
                .orElseThrow(() -> new HospitalizationNotFoundException(id)));
    }
}
