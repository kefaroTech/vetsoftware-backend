package com.vetsoftware.app.clinicalhistory.application.usecase;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.in.ListCompanyClinicalEventsUseCase;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.ListCompanyClinicalEventsQuery;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "clinical.history.list.by.company")
@Service
public class ListCompanyClinicalEventsService implements ListCompanyClinicalEventsUseCase {
    private final ClinicalEventRepository repository;

    public ListCompanyClinicalEventsService(ClinicalEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalEventDto> execute(ListCompanyClinicalEventsQuery query) {
        return repository.findByCompany(query).stream().map(ClinicalEventDto::from).toList();
    }
}
