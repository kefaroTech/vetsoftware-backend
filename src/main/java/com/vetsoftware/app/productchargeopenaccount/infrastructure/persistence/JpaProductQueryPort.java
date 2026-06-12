package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.product.domain.TaxTreatment;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
import com.vetsoftware.app.productchargeopenaccount.domain.TaxRef;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("productChargeOpenAccountJpaProductQueryPort")
public class JpaProductQueryPort implements ProductQueryPort {
    private final ProductJpaRepository productJpaRepository;

    public JpaProductQueryPort(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Optional<ProductRef> findById(Long productId) {
        return productJpaRepository.findById(productId).map(JpaProductQueryPort::toRef);
    }

    private static ProductRef toRef(ProductJpaEntity e) {
        boolean hasTax = e.getTaxTreatment() == TaxTreatment.GRAVADO;
        TaxJpaEntity t = e.getTax();
        TaxRef tax = hasTax && t != null ? new TaxRef(t.getId(), t.getName(), t.getPercentage()) : null;
        return new ProductRef(e.getId(), e.getName(), e.getCode(), e.getSalePrice(), hasTax, tax);
    }
}
