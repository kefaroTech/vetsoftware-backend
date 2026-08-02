package com.vetsoftware.app.tax.application.usecase;

import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.ListTaxesUseCase;
import com.vetsoftware.app.tax.application.port.out.TaxRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "tax.list")
@Service
public class ListTaxesService implements ListTaxesUseCase {
  private final TaxRepository repository;

  public ListTaxesService(TaxRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<TaxDto> listByCompany(Long companyId) {
    return repository.findAllByCompanyId(companyId).stream().map(TaxDto::from).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<TaxDto> listDisabledByCompany(Long companyId) {
    // readOnly tx: la query nativa trae los pausados y el mapper hidrata la asociación company LAZY
    // aquí.
    return repository.findAllDisabledByCompanyId(companyId).stream().map(TaxDto::from).toList();
  }
}
