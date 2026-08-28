package com.vetsoftware.app.companylimitevent.application.usecase;

import com.vetsoftware.app.companylimitevent.application.dto.CompanyLimitEventDto;
import com.vetsoftware.app.companylimitevent.application.port.in.ListCompanyLimitEventsUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Los hechos de cupo de una empresa. Acotado siempre. */
@Service
public class ListCompanyLimitEventsService implements ListCompanyLimitEventsUseCase {

    private final CompanyLimitEventRepository repository;

    public ListCompanyLimitEventsService(CompanyLimitEventRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyLimitEventDto> listByCompanyId(Long companyId, LocalDateTime from,
            LocalDateTime to) {
        return repository.findAllByCompanyIdBetween(companyId, from, to).stream()
                .map(CompanyLimitEventDto::from).toList();
    }
}
