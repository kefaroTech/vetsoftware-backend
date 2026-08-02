package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ListHospitalizationObservationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.observation.list.by.hospitalization")
@Service
public class ListHospitalizationObservationsByHospitalizationService
        implements
            ListHospitalizationObservationsByHospitalizationUseCase {
    private final HospitalizationObservationRepository repository;

    public ListHospitalizationObservationsByHospitalizationService(
            HospitalizationObservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationObservationDto> listByHospitalization(Long hospitalizationId) {
        return repository.findAllByHospitalizationId(hospitalizationId).stream()
                .map(HospitalizationObservationDto::from).toList();
    }
}
