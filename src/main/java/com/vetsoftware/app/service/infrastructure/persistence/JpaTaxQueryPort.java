package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.service.application.port.out.TaxQueryPort;
import com.vetsoftware.app.service.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("serviceJpaTaxQueryPort")
public class JpaTaxQueryPort implements TaxQueryPort {
    private final TaxJpaRepository taxJpaRepository;

    public JpaTaxQueryPort(TaxJpaRepository taxJpaRepository) {
        this.taxJpaRepository = taxJpaRepository;
    }

    @Override
    public Optional<TaxRef> findById(Long taxId, Long companyId) {
        return taxJpaRepository.findByIdAndCompany_Id(taxId, companyId)
            .map(e -> new TaxRef(e.getId(), e.getName(), e.getPercentage()));
    }
}
