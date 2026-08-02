package com.vetsoftware.app.servicechargeopenaccount.application.usecase;

import com.vetsoftware.app.servicechargeopenaccount.application.command.UpdateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.UpdateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.servicechargeopenaccount.application.port.out.OpenAccountVersionGuard;
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

@Observed(name = "service.charge.open.account.update")
@Service
public class UpdateServiceChargeOpenAccountService
    implements UpdateServiceChargeOpenAccountUseCase {
  private final ServiceChargeOpenAccountRepository repository;
  private final AnimalQueryPort animalQueryPort;
  private final ServiceQueryPort serviceQueryPort;
  private final OpenAccountQueryPort openAccountQueryPort;
  private final OpenAccountRefresher refresher;
  private final OpenAccountVersionGuard versionGuard;

  public UpdateServiceChargeOpenAccountService(
      ServiceChargeOpenAccountRepository repository,
      AnimalQueryPort animalQueryPort,
      ServiceQueryPort serviceQueryPort,
      OpenAccountQueryPort openAccountQueryPort,
      OpenAccountRefresher refresher,
      OpenAccountVersionGuard versionGuard) {
    this.repository = repository;
    this.animalQueryPort = animalQueryPort;
    this.serviceQueryPort = serviceQueryPort;
    this.openAccountQueryPort = openAccountQueryPort;
    this.refresher = refresher;
    this.versionGuard = versionGuard;
  }

  @Override
  @Transactional
  public ServiceChargeOpenAccountDto execute(UpdateServiceChargeOpenAccountCommand command) {
    ServiceChargeOpenAccount charge =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new ServiceChargeOpenAccountNotFoundException(command.id()));
    Long previousOpenAccountId = charge.getOpenAccount().id();

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
    // Detección temprana de conflicto sobre la cuenta destino del cargo.
    versionGuard.assertVersion(
        command.companyId(), command.openAccountId(), command.expectedVersion());
    AnimalRef animal =
        animalQueryPort
            .findByIdAndCompanyId(command.animalId(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Animal not found: " + command.animalId()));
    ServiceRef service =
        serviceQueryPort
            .findByIdAndCompanyId(command.serviceId(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Service not found: " + command.serviceId()));

    charge.update(animal, service, openAccount);
    ServiceChargeOpenAccountDto dto = ServiceChargeOpenAccountDto.from(repository.save(charge));
    refresher.refresh(command.companyId(), command.openAccountId());
    if (!command.openAccountId().equals(previousOpenAccountId)) {
      refresher.refresh(command.companyId(), previousOpenAccountId);
    }
    return dto;
  }
}
