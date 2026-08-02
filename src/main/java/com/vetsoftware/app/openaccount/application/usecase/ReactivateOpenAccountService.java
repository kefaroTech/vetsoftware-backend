package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.port.in.ReactivateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.reactivate")
@Service
public class ReactivateOpenAccountService implements ReactivateOpenAccountUseCase {
  private final OpenAccountRepository repository;

  public ReactivateOpenAccountService(OpenAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public OpenAccountDto execute(Long id, Long companyId) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new OpenAccountNotFoundException(id);
    OpenAccount openAccount =
        repository.findById(id).orElseThrow(() -> new OpenAccountNotFoundException(id));
    if (!openAccount.getCompany().id().equals(companyId)) {
      throw new IllegalArgumentException("open account does not belong to company");
    }
    return OpenAccountDto.from(openAccount);
  }
}
