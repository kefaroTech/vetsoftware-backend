package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.CreateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.create")
@Service
public class CreateServiceChargeOpenAccountService implements CreateServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ServiceQueryPort serviceQueryPort;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final OpenAccountRefresher refresher;
    private final OpenAccountVersionGuard versionGuard;

    public CreateServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ServiceQueryPort serviceQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher,
                                                 OpenAccountVersionGuard versionGuard) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.serviceQueryPort = serviceQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
        this.versionGuard = versionGuard;
    }

    @Override
    @Transactional
    public ServiceChargeOpenAccountDto execute(CreateServiceChargeOpenAccountCommand command) {
        // Lock pesimista como PRIMERA sentencia: serializa cargos/abonos concurrentes desde la validación de
        // estado hasta el recálculo (cierra el TOCTOU del isOpen/recálculo), no solo durante el recálculo final.
        openAccountQueryPort.lockForUpdate(command.openAccountId());
        // Idempotencia: si el cargo ya se registró con esta clave (reintento/doble-submit), devolverlo sin
        // duplicar. Va DESPUÉS del lock (no antes): así un reintento concurrente que llega segundo lee —ya dentro
        // del lock— el cargo committeado por el rival y lo devuelve, en vez de chocar con la constraint única
        // (500). Mismo orden que el abono (CreateDebtOpenAccountService).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ServiceChargeOpenAccount> existing = repository.findByOpenAccountIdAndClientRequestId(
                command.openAccountId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ServiceChargeOpenAccountDto.from(existing.get());
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
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ServiceRef service = serviceQueryPort.findById(command.serviceId())
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + command.serviceId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(animal, service, openAccount, createdBy,
            command.clientRequestId());
        ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.companyId(), command.openAccountId());
        return dto;
    }
}
