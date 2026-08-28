package com.vetsoftware.app.accountmapping.application.usecase;

import com.vetsoftware.app.accountmapping.application.dto.AccountMappingDto;
import com.vetsoftware.app.accountmapping.application.port.in.FindAccountMappingUseCase;
import com.vetsoftware.app.accountmapping.application.port.out.AccountMappingRepository;
import com.vetsoftware.app.accountmapping.domain.AccountMappingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/** Un mapeo por su id. */
@Observed(name = "account.mapping.find")
@Service
public class FindAccountMappingService implements FindAccountMappingUseCase {

    private final AccountMappingRepository repository;

    public FindAccountMappingService(AccountMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccountMappingDto findById(Long id) {
        return repository.findById(id).map(AccountMappingDto::from)
                .orElseThrow(() -> new AccountMappingNotFoundException(id));
    }
}
