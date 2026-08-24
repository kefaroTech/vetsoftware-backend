package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.ListAllDunningEventsUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dunning.event.list.all")
@Service
public class ListAllDunningEventsService implements ListAllDunningEventsUseCase {

    private final DunningEventRepository repository;

    public ListAllDunningEventsService(DunningEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<DunningEventDto> listAll(Long companyId, int page, int pageSize) {
        return (companyId == null
                ? repository.findAll(page, pageSize)
                : repository.findAllByCompanyId(companyId, page, pageSize))
                .map(DunningEventDto::from);
    }
}
