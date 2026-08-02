package com.vetsoftware.app.generalchargeopenaccount.application.usecase;

import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ReactivateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.OpenAccountRefresher;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "general.charge.open.account.reactivate")
@Service
public class ReactivateGeneralChargeOpenAccountService
    implements ReactivateGeneralChargeOpenAccountUseCase {
  private final GeneralChargeOpenAccountRepository repository;
  private final OpenAccountRefresher refresher;

  public ReactivateGeneralChargeOpenAccountService(
      GeneralChargeOpenAccountRepository repository, OpenAccountRefresher refresher) {
    this.repository = repository;
    this.refresher = refresher;
  }

  @Override
  @Transactional
  public GeneralChargeOpenAccountDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id, companyId);
    if (rows == 0) throw new GeneralChargeOpenAccountNotFoundException(id);
    GeneralChargeOpenAccount charge =
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new GeneralChargeOpenAccountNotFoundException(id));
    refresher.refresh(companyId, charge.getOpenAccount().id());
    return GeneralChargeOpenAccountDto.from(charge);
  }
}
