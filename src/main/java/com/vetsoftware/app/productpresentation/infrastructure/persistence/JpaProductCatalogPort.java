package com.vetsoftware.app.productpresentation.infrastructure.persistence;

import com.vetsoftware.app.product.application.port.out.DefaultProductPresentationPort;
import com.vetsoftware.app.product.application.port.out.UnitMeasureQueryPort;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaEntity;
import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaEntity;
import com.vetsoftware.app.unitmeasure.infrastructure.persistence.UnitMeasureCatalogJpaRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProductCatalogPort implements DefaultProductPresentationPort, UnitMeasureQueryPort {
    private final ProductJpaRepository products;
    private final ProductPresentationJpaRepository presentations;
    private final UnitMeasureCatalogJpaRepository units;

    public JpaProductCatalogPort(ProductJpaRepository products,
            ProductPresentationJpaRepository presentations, UnitMeasureCatalogJpaRepository units) {
        this.products = products;
        this.presentations = presentations;
        this.units = units;
    }

    @Override
    public boolean exists(String code) {
        return code != null && units.existsById(code);
    }

    @Override
    public void ensureDefault(Long productId, Long companyId, String unitMeasureCode,
            BigDecimal salePrice) {
        if (presentations
                .findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(companyId, productId)
                .isPresent()) {
            return;
        }
        ProductJpaEntity product = requireProduct(productId, companyId);
        UnitMeasureCatalogJpaEntity unit = units.getReferenceById(unitMeasureCode);
        presentations.save(ProductPresentationJpaEntity.create(product.getCompany(), product,
                "Unidad", unit, 1, salePrice, true, null));
    }

    @Override
    public void synchronizeDefault(Long productId, Long companyId, String unitMeasureCode,
            BigDecimal salePrice, Long actorId) {
        ProductPresentationJpaEntity presentation = presentations
                .findByCompany_IdAndProduct_IdAndDefaultPresentationTrue(companyId, productId)
                .orElse(null);
        if (presentation == null) {
            ensureDefault(productId, companyId, unitMeasureCode, salePrice);
            return;
        }
        presentation.update(presentation.getName(), units.getReferenceById(unitMeasureCode), 1,
                salePrice, true, actorId);
    }

    private ProductJpaEntity requireProduct(Long productId, Long companyId) {
        return products.findByIdAndCompany_Id(productId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }
}
