package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.UpdateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import com.vetsoftware.app.openaccount.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.update")
@Service
public class UpdateOpenAccountService implements UpdateOpenAccountUseCase {
    private final OpenAccountRepository repository;
    private final OwnerQueryPort ownerQueryPort;

    public UpdateOpenAccountService(OpenAccountRepository repository,
                                    OwnerQueryPort ownerQueryPort) {
        this.repository = repository;
        this.ownerQueryPort = ownerQueryPort;
    }

    @Override
    @Transactional
    public OpenAccountDto execute(UpdateOpenAccountCommand command) {
        OpenAccount openAccount = repository.findById(command.id())
            .orElseThrow(() -> new OpenAccountNotFoundException(command.id()));
        if (!openAccount.getCompany().id().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        if (command.expectedVersion() != null
                && !command.expectedVersion().equals(openAccount.getVersion())) {
            throw new OpenAccountVersionConflictException(
                command.id(), command.expectedVersion(), openAccount.getVersion());
        }
        OwnerRef owner = ownerQueryPort.findById(command.ownerId())
            .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));

        openAccount.update(owner);
        return OpenAccountDto.from(repository.save(openAccount));
    }
}
