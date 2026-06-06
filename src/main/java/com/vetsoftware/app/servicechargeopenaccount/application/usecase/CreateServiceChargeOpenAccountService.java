package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.CreateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.EmployeeRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import io.micrometer.observation.annotation.Observed;
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

    public CreateServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ServiceQueryPort serviceQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 EmployeeQueryPort employeeQueryPort,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.serviceQueryPort = serviceQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ServiceChargeOpenAccountDto execute(CreateServiceChargeOpenAccountCommand command) {
        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        if (!openAccountQueryPort.isOpen(command.openAccountId())) {
            throw new IllegalStateException("open account is not OPEN");
        }
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ServiceRef service = serviceQueryPort.findById(command.serviceId())
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + command.serviceId()));
        EmployeeRef createdBy = employeeQueryPort.findById(command.createdById())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.createdById()));

        ServiceChargeOpenAccount charge = ServiceChargeOpenAccount.create(animal, service, openAccount, createdBy);
        ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.openAccountId());
        return dto;
    }
}
