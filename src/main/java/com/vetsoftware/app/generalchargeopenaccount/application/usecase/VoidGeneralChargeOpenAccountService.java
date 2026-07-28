package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.VoidGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general.charge.open.account.void")
@Service
public class VoidGeneralChargeOpenAccountService implements VoidGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public VoidGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository,
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
    public GeneralChargeOpenAccountDto execute(VoidGeneralChargeOpenAccountCommand command) {
        GeneralChargeOpenAccount charge = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(command.id()));
        Long openAccountId = charge.getOpenAccount().id();
        if (!charge.getOpenAccount().companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("general charge does not belong to company");
        }
        // Lock pesimista de la cuenta antes de leer su estado/saldo: serializa la anulación frente a
        // cargos/abonos/cierre concurrentes (cierra el TOCTOU del isOpen/saldo), no solo en el recálculo.
        openAccountQueryPort.lockForUpdate(openAccountId);
        // Detección temprana de conflicto sobre la cuenta del cargo.
        versionGuard.assertVersion(command.companyId(), openAccountId, command.expectedVersion());
        if (!openAccountQueryPort.isOpen(openAccountId)) {
            throw new IllegalStateException("open account is not OPEN");
        }
        // No se puede anular un cargo si eso dejaría el saldo pendiente negativo (hay abonos que
        // ya cubren más de lo que quedaría). Regla: monto efectivo del cargo <= saldo pendiente.
        BigDecimal outstanding = openAccountQueryPort.outstandingAmount(openAccountId);
        if (charge.effectiveAmount().compareTo(outstanding) > 0) {
            throw new IllegalStateException(
                "No se puede anular el cargo: el saldo pendiente quedaría negativo. "
                + "Hay abonos que lo cubren; anula primero los abonos necesarios.");
        }
        EmployeeRef voidedBy = employeeQueryPort.findByIdAndCompanyId(command.voidedById(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.voidedById()));

        charge.voidCharge(voidedBy, command.reason());
        GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.companyId(), openAccountId);
        return dto;
    }
}
