package com.vetsoftware.app.productchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.productchargeopenaccount.application.port.out.ProductStockPort;
import org.springframework.stereotype.Component;

@Component("productChargeOpenAccountJpaProductStockPort")
public class JpaProductStockPort implements ProductStockPort {
    private final ProductStockJpaRepository repository;

    public JpaProductStockPort(ProductStockJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void decreaseStock(Long productId, Long companyId, int quantity) {
        repository.decreaseStock(productId, companyId, quantity);
    }

    @Override
    public void increaseStock(Long productId, Long companyId, int quantity) {
        repository.increaseStock(productId, companyId, quantity);
    }
}
