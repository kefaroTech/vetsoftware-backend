package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.port.in.DeleteTaxUseCase;
import com.vetsoftware.app.tax.application.port.out.TaxChildrenQueryPort;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import com.vetsoftware.app.tax.domain.TaxHasActiveChildrenException;
import com.vetsoftware.app.tax.domain.TaxNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "tax.delete")
@Service
public class DeleteTaxService implements DeleteTaxUseCase {
  private final TaxRepository repository;
  private final TaxChildrenQueryPort taxChildrenQueryPort;

  public DeleteTaxService(TaxRepository repository, TaxChildrenQueryPort taxChildrenQueryPort) {
    this.repository = repository;
    this.taxChildrenQueryPort = taxChildrenQueryPort;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    repository.findByIdAndCompanyId(id, companyId).orElseThrow(() -> new TaxNotFoundException(id));
    if (taxChildrenQueryPort.existsActiveByTaxId(id)) {
      throw new TaxHasActiveChildrenException(id, "product/service");
    }
    repository.delete(id);
  }
}
