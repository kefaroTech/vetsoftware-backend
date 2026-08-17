package com.vetsoftware.app.owner.application.usecase;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.owner.application.port.in.ListOwnersUseCase;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "owner.list")
@Service
public class ListOwnersService implements ListOwnersUseCase {
    private final OwnerRepository repository;

    public ListOwnersService(OwnerRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<OwnerDto> listAll(Long companyId, int page, int pageSize) {
        return repository.findAllByCompanyId(companyId, page, pageSize).map(OwnerDto::from);
    }
}
