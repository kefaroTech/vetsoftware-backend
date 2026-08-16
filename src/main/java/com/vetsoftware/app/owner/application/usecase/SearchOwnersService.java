package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.owner.application.port.in.SearchOwnersUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "owner.search")
@Service
public class SearchOwnersService implements SearchOwnersUseCase {
    private final OwnerRepository repository;

    public SearchOwnersService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<OwnerDto> search(Long companyId, String query, int page, int pageSize) {
        return repository.searchByCompanyAndTerm(companyId, query, page, pageSize)
                .map(OwnerDto::from);
    }
}
