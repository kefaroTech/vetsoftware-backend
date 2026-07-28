package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.GetClinicalHistoryUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "clinical.history.get")
@Service
public class GetClinicalHistoryService implements GetClinicalHistoryUseCase {
    private final ClinicalEventRepository repository;

    public GetClinicalHistoryService(ClinicalEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ClinicalEventDto> execute(GetClinicalHistoryQuery query) {
        return repository.findHistory(query).stream()
                .map(ClinicalEventDto::from)
                .toList();
    }
}
