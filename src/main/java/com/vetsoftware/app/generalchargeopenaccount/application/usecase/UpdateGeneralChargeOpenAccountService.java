package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.UpdateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountVersionGuard;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import com.vetsoftware.app.generalchargeopenaccount.domain.OpenAccountRef;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general.charge.open.account.update")
@Service
public class UpdateGeneralChargeOpenAccountService
    implements UpdateGeneralChargeOpenAccountUseCase {
  private final GeneralChargeOpenAccountRepository repository;
  private final OpenAccountQueryPort openAccountQueryPort;
  private final TaxQueryPort taxQueryPort;
  private final OpenAccountRefresher refresher;
  private final OpenAccountVersionGuard versionGuard;

  public UpdateGeneralChargeOpenAccountService(
      GeneralChargeOpenAccountRepository repository,
      OpenAccountQueryPort openAccountQueryPort,
      TaxQueryPort taxQueryPort,
      OpenAccountRefresher refresher,
      OpenAccountVersionGuard versionGuard) {
    this.repository = repository;
    this.openAccountQueryPort = openAccountQueryPort;
    this.taxQueryPort = taxQueryPort;
    this.refresher = refresher;
    this.versionGuard = versionGuard;
  }

  @Override
  @Transactional
  public GeneralChargeOpenAccountDto execute(UpdateGeneralChargeOpenAccountCommand command) {
    GeneralChargeOpenAccount charge =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(command.id()));
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
    TaxRef tax =
        command.taxId() == null
            ? null
            : taxQueryPort
                .findById(command.taxId(), command.companyId())
                .orElseThrow(
                    () -> new IllegalArgumentException("Tax not found: " + command.taxId()));

    charge.update(command.name(), command.unitAmount(), command.quantity(), tax, openAccount);
    GeneralChargeOpenAccountDto dto = GeneralChargeOpenAccountDto.from(repository.save(charge));
    refresher.refresh(command.companyId(), openAccount.id());
    if (!openAccount.id().equals(previousOpenAccountId)) {
      refresher.refresh(command.companyId(), previousOpenAccountId);
    }
    return dto;
  }
}
