package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductQueryPort;
import com.vetsoftware.app.productchargeopenaccount.domain.ProductRef;
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
        return productJpaRepository.findById(productId)
            .map(e -> new ProductRef(e.getId(), e.getName(), e.getCode(), e.getSalePrice()));
    }
}
