package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ReactivateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open_account.reactivate")
@Service
public class ReactivateOpenAccountService implements ReactivateOpenAccountUseCase {
    private final OpenAccountRepository repository;

    public ReactivateOpenAccountService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public OpenAccountDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0) throw new OpenAccountNotFoundException(id);
        return OpenAccountDto.from(repository.findById(id)
            .orElseThrow(() -> new OpenAccountNotFoundException(id)));
    }
}
