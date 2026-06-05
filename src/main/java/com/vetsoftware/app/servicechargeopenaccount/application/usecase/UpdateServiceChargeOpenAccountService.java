package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.command.UpdateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.UpdateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceChargeOpenAccountRepository;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.ServiceQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccount;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceChargeOpenAccountNotFoundException;
import com.vetsoftware.app.servicechargeopenaccount.domain.ServiceRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "service_charge_open_account.update")
@Service
public class UpdateServiceChargeOpenAccountService implements UpdateServiceChargeOpenAccountUseCase {
    private final ServiceChargeOpenAccountRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final ServiceQueryPort serviceQueryPort;
    private final OpenAccountQueryPort openAccountQueryPort;
    private final OpenAccountRefresher refresher;

    public UpdateServiceChargeOpenAccountService(ServiceChargeOpenAccountRepository repository,
                                                 AnimalQueryPort animalQueryPort,
                                                 ServiceQueryPort serviceQueryPort,
                                                 OpenAccountQueryPort openAccountQueryPort,
                                                 OpenAccountRefresher refresher) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.serviceQueryPort = serviceQueryPort;
        this.openAccountQueryPort = openAccountQueryPort;
        this.refresher = refresher;
    }

    @Override
    @Transactional
    public ServiceChargeOpenAccountDto execute(UpdateServiceChargeOpenAccountCommand command) {
        ServiceChargeOpenAccount charge = repository.findById(command.id())
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(command.id()));
        Long previousOpenAccountId = charge.getOpenAccount().id();

        OpenAccountRef openAccount = openAccountQueryPort.findById(command.openAccountId())
            .orElseThrow(() -> new IllegalArgumentException("OpenAccount not found: " + command.openAccountId()));
        if (!openAccount.companyId().equals(command.companyId())) {
            throw new IllegalArgumentException("open account does not belong to company");
        }
        AnimalRef animal = animalQueryPort.findById(command.animalId())
            .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        ServiceRef service = serviceQueryPort.findById(command.serviceId())
            .orElseThrow(() -> new IllegalArgumentException("Service not found: " + command.serviceId()));

        charge.update(animal, service, openAccount);
        ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(repository.save(charge));
        refresher.refresh(command.openAccountId());
        if (!command.openAccountId().equals(previousOpenAccountId)) {
            refresher.refresh(previousOpenAccountId);
        }
        return dto;
    }
}
