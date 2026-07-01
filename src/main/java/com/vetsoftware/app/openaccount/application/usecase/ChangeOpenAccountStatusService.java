package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.out.ClosedAccountEmissionPort;
import com.vetsoftware.app.openaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.EmployeeRef;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open_account.change_status")
@Service
public class ChangeOpenAccountStatusService implements ChangeOpenAccountStatusUseCase {
    private final OpenAccountRepository repository;
    private final EmployeeQueryPort employeeQueryPort;
    private final ClosedAccountEmissionPort emissionPort;

    public ChangeOpenAccountStatusService(OpenAccountRepository repository,
                                          EmployeeQueryPort employeeQueryPort,
                                          ClosedAccountEmissionPort emissionPort) {
        this.repository = repository;
        this.employeeQueryPort = employeeQueryPort;
        this.emissionPort = emissionPort;
    }

    @Override
    @Transactional
    public OpenAccountDto execute(ChangeOpenAccountStatusCommand command) {
        // Lock pesimista de la cuenta al inicio: serializa el cierre/cancelación frente a cargos/abonos
        // concurrentes (un cargo no se puede colar mientras se cierra), cerrando el TOCTOU sobre el estado y
        // el saldo. Bajo el lock solo se persiste el documento PENDIENTE; la transmisión a la DIAN + entrega
        // (I/O externo ~60s) se difiere a afterCommit (A1), fuera del lock.
        OpenAccount openAccount = repository.findByIdForUpdate(command.id())
            .orElseThrow(() -> new OpenAccountNotFoundException(command.id()));
        if (!openAccount.getCompany().id().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        if (command.expectedVersion() != null
                && !command.expectedVersion().equals(openAccount.getVersion())) {
            throw new OpenAccountVersionConflictException(
                command.id(), command.expectedVersion(), openAccount.getVersion());
        }
        EmployeeRef closedBy = employeeQueryPort.findById(command.employeeId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        OpenAccountStatus newStatus = OpenAccountStatus.valueOf(command.status().toUpperCase());
        openAccount.changeStatus(newStatus, closedBy, command.reason());
        OpenAccount saved = repository.save(openAccount);

        // Cobro/cierre de la venta: construye+persiste el documento PENDIENTE bajo el lock (un fallo de config
        // como "sin perfil fiscal" que valida el builder hace fallar el cierre atómicamente) y programa la
        // transmisión a la DIAN + entrega para DESPUÉS del commit, ya sin el lock (ver A1 / EmitOnClose).
        if (newStatus == OpenAccountStatus.CLOSE) {
            emissionPort.emitForClosedAccount(
                saved.getId(), saved.getCompany().id(), command.documentType(), command.finalConsumer());
        }
        return OpenAccountDto.from(saved);
    }
}
