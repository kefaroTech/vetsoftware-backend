package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ListOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "open_account.list")
@Service
public class ListOpenAccountsService implements ListOpenAccountsUseCase {
    private final OpenAccountRepository repository;

    public ListOpenAccountsService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OpenAccountDto> listAll() {
        return repository.findAll().stream().map(OpenAccountDto::from).toList();
    }
}
