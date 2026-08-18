package com.vetsoftware.app.debtopenaccount.application.usecase;

import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.CreateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.out.CashPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.DebtOpenAccountRepository;
import com.vetsoftware.app.debtopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.debtopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.debtopenaccount.domain.DebtOpenAccount;
import com.vetsoftware.app.debtopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.debtopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.debtopenaccount.domain.PaymentMethod;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "debt.open.account.create")
@Service
public class CreateDebtOpenAccountService implements CreateDebtOpenAccountUseCase {
    private final DebtOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;
    private final CashPort cashPort;

    public CreateDebtOpenAccountService(DebtOpenAccountRepository repository,
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
    public DebtOpenAccountDto execute(CreateDebtOpenAccountCommand command) {
        // Lock pesimista ACOTADO por empresa como PRIMERA sentencia: serializa
        // abonos/cargos concurrentes sobre la cuenta desde el read-modify-write
        // completo, y va antes de cualquier lectura consistente para que el guard de
        // sobrepago lea el saldo ya committeado por una operacion rival. Acotado porque
        // la variante ancha tomaba un PESSIMISTIC_WRITE sobre la fila de OTRO tenant
        // antes de cualquier comprobacion: lo soltaba el rollback, pero se concedia.
        openAccountQueryPort.lockForUpdate(command.openAccountId(), command.companyId());
        // Carga ACOTADA por empresa: la cuenta de otro tenant no se resuelve, asi que
        // el
        // abono no puede colgarse de ella. Antes se cargaba ancha y la empresa se
        // comparaba despues en Java: ese if era toda la barrera entre un cobro propio y
        // un importe escrito en la cuenta de un cliente ajeno.
        OpenAccountRef openAccount = openAccountQueryPort
                .findByIdAndCompanyId(command.openAccountId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "OpenAccount not found: " + command.openAccountId()));
        // Idempotencia: si el cobro ya se registro con esta clave
        // (reintento/doble-submit), devolverlo sin duplicar. Va DESPUES de la
        // resolucion
        // acotada, y eso es aislamiento, no estilo: el abono no tiene company_id propio
        // —el tenant se alcanza navegando open_account.company_id—, asi que con el id
        // de
        // una cuenta ajena y la clave exacta este finder devolvia el DTO del abono del
        // otro tenant sin pasar por ninguna comprobacion de empresa. Sigue ANTES del
        // versionGuard y del guard de sobrepago: tras el 1er abono el saldo bajo y la
        // version subio, y el reintento legitimo del mismo cliente tiene que devolver
        // el
        // mismo abono en vez de fallar o duplicarlo.
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<DebtOpenAccount> existing = repository.findByOpenAccountIdAndClientRequestId(
                    command.openAccountId(), command.clientRequestId());
            if (existing.isPresent()) {
                return DebtOpenAccountDto.from(existing.get());
            }
        }
        // Detección temprana de conflicto: dentro del lock, antes de registrar el
        // abono.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(),
                command.expectedVersion());
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        // El abono no puede exceder el saldo pendiente (no se maneja vuelto en cuenta;
        // el cambio en
        // efectivo se da en el POS). Evita dejar el outstanding negativo.
        BigDecimal outstanding = openAccountQueryPort.outstandingAmount(command.openAccountId());
        if (command.amount().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("El abono (" + command.amount()
                    + ") no puede exceder el saldo pendiente (" + outstanding + ").");
        }
        EmployeeRef createdBy = employeeQueryPort
                .findByIdAndCompanyId(command.createdById(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Employee not found: " + command.createdById()));

        // El cobro siempre pertenece a la caja propia del empleado y a la misma sede de
        // la cuenta.
        cashPort.requireOpenSession(command.companyId(), command.openAccountId(),
                command.createdById());

        DebtOpenAccount debtOpenAccount = DebtOpenAccount.create(command.amount(),
                PaymentMethod.valueOf(command.paymentMethod()), openAccount, createdBy,
                command.clientRequestId());
        DebtOpenAccount saved = repository.save(debtOpenAccount);
        refresher.refresh(command.companyId(), command.openAccountId());
        // Registra el abono en la caja OPEN de la sede de la cuenta (OPEN_ACCOUNT_IN).
        // Idempotente y
        // no-op si no hay
        // caja abierta. Misma transacción del abono.
        cashPort.registerPayment(command.companyId(), command.openAccountId(), saved.getId(),
                saved.getPaymentMethod(), saved.getAmount(), command.createdById());
        return DebtOpenAccountDto.from(saved);
    }
}
