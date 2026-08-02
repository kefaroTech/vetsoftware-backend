package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.UpdateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt.open.account.update")
@Service
public class UpdateDebtOpenAccountService implements UpdateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public UpdateDebtOpenAccountService(DebtOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, OpenAccountRefresher refresher,
            OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(UpdateDebtOpenAccountCommand command) {
        DebtOpenAccount debtOpenAccount = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        Long previousOpenAccountId = debtOpenAccount.getOpenAccount().id();

        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        // Detección temprana de conflicto sobre la cuenta destino del abono.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(),
                command.expectedVersion());

        debtOpenAccount.update(command.amount(), PaymentMethod.valueOf(command.paymentMethod()),
                openAccount);
        DebtOpenAccountDto dto = DebtOpenAccountDto.from(repository.save(debtOpenAccount));
        refresher.refresh(command.companyId(), command.openAccountId());
        if (!command.openAccountId().equals(previousOpenAccountId)) {
            refresher.refresh(command.companyId(), previousOpenAccountId);
        }
        return dto;
    }
}
