package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.in.SearchOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "open.account.search")
@Service
public class SearchOpenAccountsService implements SearchOpenAccountsUseCase {
    private final OpenAccountRepository repository;

    public SearchOpenAccountsService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<OpenAccountDto> execute(SearchOpenAccountsCommand command) {
        return repository.search(command).map(OpenAccountDto::from);
    }
}
