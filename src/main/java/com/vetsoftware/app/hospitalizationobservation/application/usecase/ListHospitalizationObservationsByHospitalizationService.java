package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.PageResult;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ListHospitalizationObservationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import io.micrometer.observation.annotation.Observed;
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
    public PageResult<HospitalizationObservationDto> listByHospitalization(Long hospitalizationId,
            Long companyId, int page, int pageSize) {
        return repository.findAllByHospitalizationIdAndCompanyId(hospitalizationId, companyId, page,
                pageSize).map(HospitalizationObservationDto::from);
    }
}
