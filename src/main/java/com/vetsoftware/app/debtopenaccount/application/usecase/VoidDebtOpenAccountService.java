package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.VoidDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccountNotFoundException;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt_open_account.void")
@Service
public class VoidDebtOpenAccountService implements VoidDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public VoidDebtOpenAccountService(DebtOpenAccountRepository repository,
                                      OpenAccountQueryPort openAccountQueryPort,
                                      EmployeeQueryPort employeeQueryPort,
                                      OpenAccountRefresher refresher,
                                      OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(VoidDebtOpenAccountCommand command) {
        DebtOpenAccount debtOpenAccount = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        Long openAccountId = debtOpenAccount.getOpenAccount().id();
        if (!debtOpenAccount.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("debt open account does not belong to company");
        }
        // Lock pesimista de la cuenta antes de leer su estado: serializa la anulación del abono frente a
        // cargos/abonos/cierre concurrentes (cierra el TOCTOU del isOpen), no solo en el recálculo.
        openAccountQueryPort.lockForUpdate(openAccountId);
        // Detección temprana de conflicto sobre la cuenta del abono.
        versionGuard.assertVersion(command.companyId(), openAccountId, command.expectedVersion());
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        EmployeeRef voidedBy = employeeQueryPort.findByIdAndCompanyId(command.voidedById(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.voidedById()));

        debtOpenAccount.voidPayment(voidedBy, command.reason());
        DebtOpenAccountDto dto = DebtOpenAccountDto.from(repository.save(debtOpenAccount));
        refresher.refresh(command.companyId(), openAccountId);
        return dto;
    }
}
