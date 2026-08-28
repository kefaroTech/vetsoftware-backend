package com.vetsoftware.app.accountmapping.application.usecase;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.ListAccountMappingsUseCase;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.shared.pagination.PageResult;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Los mapeos, paginados. No hay hermano acotado por empresa porque la tabla no
 * tiene empresa; el puerto va cerrado a {@code hasRole('SYSTEM')} por eso
 * mismo.
 */
@Observed(name = "account.mapping.list")
@Service
public class ListAccountMappingsService implements ListAccountMappingsUseCase {

    private final AccountMappingRepository repository;

    public ListAccountMappingsService(AccountMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AccountMappingDto> listAll(int page, int pageSize) {
        return repository.findAllEnabled(page, pageSize).map(AccountMappingDto::from);
    }
}
