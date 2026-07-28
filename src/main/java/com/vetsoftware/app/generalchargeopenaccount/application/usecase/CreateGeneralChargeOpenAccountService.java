package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.CreateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general.charge.open.account.create")
@Service
public class CreateGeneralChargeOpenAccountService implements CreateGeneralChargeOpenAccountUseCase {
    private final GeneralChargeOpenAccountRepository repository;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final TaxQueryPort taxQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public CreateGeneralChargeOpenAccountService(GeneralChargeOpenAccountRepository repository,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 TaxQueryPort taxQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher,
                                                 OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.openAccountQueryPort = openAccountQueryPort;
        this.taxQueryPort = taxQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public GeneralChargeOpenAccountDto execute(CreateGeneralChargeOpenAccountCommand command) {
        // Lock pesimista como PRIMERA sentencia: serializa cargos/abonos concurrentes desde la validación de
        // estado hasta el recálculo (cierra el TOCTOU del isOpen/recálculo), no solo durante el recálculo final.
        openAccountQueryPort.lockForUpdate(command.openAccountId());
        // Idempotencia: si el cargo ya se registró con esta clave (reintento/doble-submit), devolverlo sin
        // duplicar. Va DESPUÉS del lock (no antes): así un reintento concurrente que llega segundo lee —ya dentro
        // del lock— el cargo committeado por el rival y lo devuelve, en vez de chocar con la constraint única
        // (500). Mismo orden que el abono (CreateDebtOpenAccountService).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<GeneralChargeOpenAccount> existing = repository.findByOpenAccountIdAndClientRequestId(
                command.openAccountId(), command.clientRequestId());
            if (existing.isPresent()) {
                return GeneralChargeOpenAccountDto.from(existing.get());
            }
        }
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        // Detección temprana de conflicto: dentro del lock, antes de crear el cargo.
        versionGuard.assertVersion(command.companyId(), command.openAccountId(), command.expectedVersion());
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        TaxRef tax = command.taxId() == null ? null
            : taxQueryPort.findById(command.taxId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Tax not found: " + command.taxId()));
        EmployeeRef createdBy = employeeQueryPort.findByIdAndCompanyId(command.createdById(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        GeneralChargeOpenAccount charge = GeneralChargeOpenAccount.create(
            command.name(), command.unitAmount(), command.quantity(), tax,
            openAccount, createdBy, command.clientRequestId());
        GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.companyId(), openAccount.id());
        return dto;
    }
}
