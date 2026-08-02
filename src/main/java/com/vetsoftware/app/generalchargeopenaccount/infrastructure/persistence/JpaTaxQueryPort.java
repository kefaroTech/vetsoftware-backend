package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.generalchargeopenaccount.application.port.out.TaxQueryPort;
import com.vetsoftware.app.generalchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("generalChargeOpenAccountJpaTaxQueryPort")
public class JpaTaxQueryPort implements TaxQueryPort {
  private final TaxJpaRepository taxJpaRepository;

  public JpaTaxQueryPort(TaxJpaRepository taxJpaRepository) {
    this.taxJpaRepository = taxJpaRepository;
  }

  @Override
  public Optional<TaxRef> findById(Long taxId, Long companyId) {
    return taxJpaRepository
        .findByIdAndCompany_Id(taxId, companyId)
        .map(
            e ->
                new TaxRef(
                    e.getId(),
                    e.getName(),
                    e.getPercentage(),
                    e.getTaxScheme() == null ? null : e.getTaxScheme().name()));
  }
}
