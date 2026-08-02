package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.VoidDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
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

@Observed(name = "debt.open.account.void")
@Service
public class VoidDebtOpenAccountService implements VoidDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;
    private final CashPort cashPort;

    public VoidDebtOpenAccountService(DebtOpenAccountRepository repository,
            OpenAccountQueryPort openAccountQueryPort, EmployeeQueryPort employeeQueryPort,
            OpenAccountRefresher refresher, OpenAccountVersionGuard versionGuard,
            CashPort cashPort) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
        this.cashPort = cashPort;
    }

    @Override
    @Transactional
    public DebtOpenAccountDto execute(VoidDebtOpenAccountCommand command) {
        DebtOpenAccount debtOpenAccount = repository
                .findByIdAndCompanyId(command.id(), command.companyId())
                .orElseThrow(() -> new DebtOpenAccountNotFoundException(command.id()));
        Long openAccountId = debtOpenAccount.getOpenAccount().id();
        if (!debtOpenAccount.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("debt open account does not belong to company");
        }
        // Lock pesimista de la cuenta antes de leer su estado: serializa la anulación
        // del abono frente
        // a
        // cargos/abonos/cierre concurrentes (cierra el TOCTOU del isOpen), no solo en
        // el recálculo.
        openAccountQueryPort.lockForUpdate(openAccountId);
        // Detección temprana de conflicto sobre la cuenta del abono.
        versionGuard.assertVersion(command.companyId(), openAccountId, command.expectedVersion());
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        EmployeeRef voidedBy = employeeQueryPort
                .findByIdAndCompanyId(command.voidedById(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.voidedById()));

        // La anulación mueve dinero en la caja propia del actor y exige que esté
        // abierta en la sede de
        // la cuenta.
        cashPort.requireOpenSession(command.companyId(), openAccountId, command.voidedById());
        debtOpenAccount.voidPayment(voidedBy, command.reason());
        DebtOpenAccount saved = repository.save(debtOpenAccount);
        refresher.refresh(command.companyId(), openAccountId);
        // Compensa el abono en la caja OPEN del actor (VOID_OUT). voidPayment ya
        // garantizó que no
        // estaba anulado.
        cashPort.reversePayment(command.companyId(), openAccountId, saved.getId(),
                saved.getPaymentMethod(), saved.getAmount(), command.voidedById());
        return DebtOpenAccountDto.from(saved);
    }
}
