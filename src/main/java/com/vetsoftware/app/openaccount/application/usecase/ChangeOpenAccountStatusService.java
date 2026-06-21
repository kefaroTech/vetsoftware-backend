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
        // el saldo. La emisión del documento al cierre ocurre en esta misma transacción bajo el lock.
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

        // Cobro/cierre de la venta: emite el documento electrónico directamente (síncrono, misma transacción).
        // La emisión ve la cuenta ya CLOSE y el documento se guarda atómicamente con el cierre. Un fallo de
        // configuración (p. ej. sin perfil fiscal) hace fallar el cierre con ese error, en vez de cerrar sin
        // documento; las caídas transitorias de la DIAN no lanzan (el proveedor las marca CONTINGENCIA).
        if (newStatus == OpenAccountStatus.CLOSE) {
            emissionPort.emitForClosedAccount(
                saved.getId(), saved.getCompany().id(), command.documentType(), command.finalConsumer());
        }
        return OpenAccountDto.from(saved);
    }
}
