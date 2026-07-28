package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.port.in.DeleteOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.delete")
@Service
public class DeleteOpenAccountService implements DeleteOpenAccountUseCase {
    private final OpenAccountRepository repository;

    public DeleteOpenAccountService(OpenAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        OpenAccount openAccount = repository.findById(id)
            .orElseThrow(() -> new OpenAccountNotFoundException(id));
        if (!openAccount.getCompany().id().equals(companyId)) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        repository.delete(id);
    }
}
