package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.SearchOwnersUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "owner.search")
@Service
public class SearchOwnersService implements SearchOwnersUseCase {
    private final OwnerRepository repository;

    public SearchOwnersService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnerDto> search(Long companyId, String query) {
        return repository.searchByCompanyAndNameOrEmail(companyId, query)
            .stream().map(OwnerDto::from).toList();
    }
}
