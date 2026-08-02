package com.vetsoftware.app.tax.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.tax.application.port.out.TaxChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaTaxChildrenQueryPort implements TaxChildrenQueryPort {
    private final ProductJpaRepository productJpaRepository;
    private final ServiceJpaRepository serviceJpaRepository;

    public JpaTaxChildrenQueryPort(ProductJpaRepository productJpaRepository,
            ServiceJpaRepository serviceJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.serviceJpaRepository = serviceJpaRepository;
    }

    @Override
    public boolean existsActiveByTaxId(Long taxId) {
        return productJpaRepository.existsByTax_Id(taxId)
                || serviceJpaRepository.existsByTax_Id(taxId);
    }
}
