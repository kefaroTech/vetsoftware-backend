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
        // El filtro por empresa va EN la consulta, no en un if posterior: el
        // companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId(), nunca null), asi que la cuenta de otro tenant
        // ni se carga. Un 404 no revela que la fila existe en otra empresa.
        OpenAccount openAccount = repository.findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new OpenAccountNotFoundException(command.id()));
        if (command.expectedVersion() != null
                && !command.expectedVersion().equals(openAccount.getVersion())) {
            throw new OpenAccountVersionConflictException(command.id(), command.expectedVersion(),
                    openAccount.getVersion());
        }
        // El ownerId llega en el request: acotado por empresa, o la cuenta quedaria
        // reapuntada al propietario de otro tenant.
        OwnerRef owner = ownerQueryPort.findByIdAndCompanyId(command.ownerId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Owner not found: " + command.ownerId()));

        openAccount.update(owner);
        return OpenAccountDto.from(repository.save(openAccount));
    }
}
