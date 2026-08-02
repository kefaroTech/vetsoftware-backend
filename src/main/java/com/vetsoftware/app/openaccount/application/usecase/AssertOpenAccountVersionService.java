package com.vetsoftware.app.openaccount.application.usecase;

import com.vetsoftware.app.openaccount.application.port.in.AssertOpenAccountVersionUseCase;
import com.vetsoftware.app.openaccount.application.port.out.OpenAccountRepository;
import com.vetsoftware.app.openaccount.domain.OpenAccount;
import com.vetsoftware.app.openaccount.domain.OpenAccountNotFoundException;
import com.vetsoftware.app.openaccount.domain.OpenAccountVersionConflictException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "open.account.assert.version")
@Service
public class AssertOpenAccountVersionService implements AssertOpenAccountVersionUseCase {
  private final OpenAccountRepository repository;

  public AssertOpenAccountVersionService(OpenAccountRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public void assertVersion(Long companyId, Long openAccountId, Long expectedVersion) {
    // Opt-in: sin versión esperada no hay chequeo temprano (se conserva el optimistic lock al
    // flush).
    if (expectedVersion == null) return;
    // Lectura scoped a la empresa: una cuenta ajena lanza NotFound en vez de comparar/filtrar su
    // versión.
    OpenAccount account =
        repository
            .findByIdAndCompanyId(openAccountId, companyId)
            .orElseThrow(() -> new OpenAccountNotFoundException(openAccountId));
    if (!expectedVersion.equals(account.getVersion())) {
      throw new OpenAccountVersionConflictException(
          openAccountId, expectedVersion, account.getVersion());
    }
  }
}
