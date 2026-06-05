package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.CreateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt_open_account.create")
@Service
public class CreateDebtOpenAccountService implements CreateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;

    public CreateDebtOpenAccountService(DebtOpenAccountRepository repository,
                                        OpenAccountQueryPort openAccountQueryPort,
                                        EmployeeQueryPort employeeQueryPort,
                                        OpenAccountRefresher refresher) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(CreateDebtOpenAccountCommand command) {
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        DebtOpenAccount debtOpenAccount = DebtOpenAccount.create(
            command.amount(), PaymentMethod.valueOf(command.paymentMethod()), openAccount, createdBy);
        DebtOpenAccountDto dto = DebtOpenAccountDto.from(repository.save(debtOpenAccount));
        refresher.refresh(command.openAccountId());
        return dto;
    }
}
