package com.vetsoftware.app.dunning.application.usecase;

import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.FindDunningEventUseCase;
import com.vetsoftware.app.dunning.application.port.out.DunningEventRepository;
import com.vetsoftware.app.dunning.domain.DunningEventNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "dunning.event.find")
@Service
public class FindDunningEventService implements FindDunningEventUseCase {

    private final DunningEventRepository repository;

    public FindDunningEventService(DunningEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public DunningEventDto findById(Long id, Long companyId) {
        return repository.findByIdAndCompanyId(id, companyId).map(DunningEventDto::from)
                .orElseThrow(() -> new DunningEventNotFoundException(id));
    }
}
