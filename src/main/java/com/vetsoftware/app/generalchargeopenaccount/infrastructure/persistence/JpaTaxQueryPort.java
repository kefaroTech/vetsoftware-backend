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
    public Optional<TaxRef> findById(Long taxId) {
        return taxJpaRepository.findById(taxId)
            .map(e -> new TaxRef(e.getId(), e.getName(), e.getPercentage()));
    }
}
