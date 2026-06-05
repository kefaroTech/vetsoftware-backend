package com.vetsoftware.app.product.infrastructure.persistence;

import com.vetsoftware.app.product.application.port.out.TaxQueryPort;
import com.vetsoftware.app.product.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productJpaTaxQueryPort")
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
