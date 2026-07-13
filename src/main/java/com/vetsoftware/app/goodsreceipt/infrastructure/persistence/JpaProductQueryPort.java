package com.vetsoftware.app.goodsreceipt.infrastructure.persistence;

import com.vetsoftware.app.goodsreceipt.application.port.out.ProductQueryPort;
import com.vetsoftware.app.goodsreceipt.domain.ProductRef;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("goodsReceiptJpaProductQueryPort")
public class JpaProductQueryPort implements ProductQueryPort {
    private final ProductJpaRepository productJpaRepository;

    public JpaProductQueryPort(ProductJpaRepository productJpaRepository) {
        this.productJpaRepository = productJpaRepository;
    }

    @Override
    public Optional<ProductRef> findById(Long productId, Long companyId) {
        return productJpaRepository.findByIdAndCompany_Id(productId, companyId)
            .map(e -> new ProductRef(e.getId(), e.getName(), e.getCode()));
    }
}
