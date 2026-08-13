package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventTypeCountDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistorySummaryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "clinical.history.summary")
@Service
public class GetClinicalHistorySummaryService implements GetClinicalHistorySummaryUseCase {
    private final ClinicalEventRepository repository;

    public GetClinicalHistorySummaryService(ClinicalEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ClinicalEventTypeCountDto> countByType(Long animalId, Long companyId) {
        return repository.countByType(animalId, companyId);
    }
}
