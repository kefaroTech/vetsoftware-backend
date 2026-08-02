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

  public CreateDebtOpenAccountService(
      DebtOpenAccountRepository repository,
      OpenAccountQueryPort openAccountQueryPort,
      EmployeeQueryPort employeeQueryPort,
      OpenAccountRefresher refresher,
      OpenAccountVersionGuard versionGuard,
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
    // Lock pesimista como PRIMERA sentencia: serializa abonos/cargos concurrentes sobre la cuenta
    // desde
    // el read-modify-write completo. Debe ir antes de cualquier lectura consistente (incluida la
    // idempotencia) para que el guard de sobrepago lea el saldo ya committeado por una operación
    // rival.
    openAccountQueryPort.lockForUpdate(command.openAccountId());
    // Idempotencia: si el cobro ya se registró con esta clave (reintento/doble-submit), devolverlo
    // sin
    // duplicar. Va ANTES del guard de sobrepago: tras el 1er abono el saldo bajó y el reintento
    // fallaría.
    if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
      Optional<DebtOpenAccount> existing =
          repository.findByOpenAccountIdAndClientRequestId(
              command.openAccountId(), command.clientRequestId());
      if (existing.isPresent()) {
        return DebtOpenAccountDto.from(existing.get());
      }
    }
    OpenAccountRef openAccount =
        openAccountQueryPort
            .findById(command.openAccountId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "OpenAccount not found: " + command.openAccountId()));
    if (!openAccount.companyId().equals(command.companyId())) {
      throw new IllegalArgumentException("open account does not belong to company");
    }
    // Detección temprana de conflicto: dentro del lock, antes de registrar el abono.
    versionGuard.assertVersion(
        command.companyId(), command.openAccountId(), command.expectedVersion());
    if (!openAccountQueryPort.isOpen(command.openAccountId())) {
      throw new IllegalStateException("open account is not OPEN");
    }
    // El abono no puede exceder el saldo pendiente (no se maneja vuelto en cuenta; el cambio en
    // efectivo se da en el POS). Evita dejar el outstanding negativo.
    BigDecimal outstanding = openAccountQueryPort.outstandingAmount(command.openAccountId());
    if (command.amount().compareTo(outstanding) > 0) {
      throw new IllegalArgumentException(
          "El abono ("
              + command.amount()
              + ") no puede exceder el saldo pendiente ("
              + outstanding
              + ").");
    }
    EmployeeRef createdBy =
        employeeQueryPort
            .findByIdAndCompanyId(command.createdById(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.createdById()));

    // El cobro siempre pertenece a la caja propia del empleado y a la misma sede de la cuenta.
    cashPort.requireOpenSession(
        command.companyId(), command.openAccountId(), command.createdById());

    DebtOpenAccount debtOpenAccount =
        DebtOpenAccount.create(
            command.amount(),
            PaymentMethod.valueOf(command.paymentMethod()),
            openAccount,
            createdBy,
            command.clientRequestId());
    DebtOpenAccount saved = repository.save(debtOpenAccount);
    refresher.refresh(command.companyId(), command.openAccountId());
    // Registra el abono en la caja OPEN de la sede de la cuenta (OPEN_ACCOUNT_IN). Idempotente y
    // no-op si no hay
    // caja abierta. Misma transacción del abono.
    cashPort.registerPayment(
        command.companyId(),
        command.openAccountId(),
        saved.getId(),
        saved.getPaymentMethod(),
        saved.getAmount(),
        command.createdById());
    return DebtOpenAccountDto.from(saved);
  }
}
