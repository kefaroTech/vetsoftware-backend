package com.vetsoftware.app.promotion.infrastructure.persistence;

import com.vetsoftware.app.product.infrastructure.persistence.ProductJpaRepository;
import com.vetsoftware.app.productcategory.infrastructure.persistence.ProductCategoryJpaRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionTargetQueryPort;
import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaPromotionTargetQueryPort implements PromotionTargetQueryPort {
    private final ProductJpaRepository productJpaRepository;
    private final ServiceJpaRepository serviceJpaRepository;
    private final ProductCategoryJpaRepository productCategoryJpaRepository;
    private final ServiceCategoryJpaRepository serviceCategoryJpaRepository;

    public JpaPromotionTargetQueryPort(ProductJpaRepository productJpaRepository,
            ServiceJpaRepository serviceJpaRepository,
            ProductCategoryJpaRepository productCategoryJpaRepository,
            ServiceCategoryJpaRepository serviceCategoryJpaRepository) {
        this.productJpaRepository = productJpaRepository;
        this.serviceJpaRepository = serviceJpaRepository;
        this.productCategoryJpaRepository = productCategoryJpaRepository;
        this.serviceCategoryJpaRepository = serviceCategoryJpaRepository;
    }

    @Override
    public boolean exists(ApplicationType type, Long itemId, Long companyId) {
        if (type == null || itemId == null || companyId == null)
            return false;
        return switch (type) {
            case PRODUCT -> productJpaRepository.existsByIdAndCompany_Id(itemId, companyId);
            case SERVICE -> serviceJpaRepository.existsByIdAndCompany_Id(itemId, companyId);
            case CATEGORY -> productCategoryJpaRepository.existsByIdAndCompany_Id(itemId, companyId)
                    || serviceCategoryJpaRepository.existsByIdAndCompany_Id(itemId, companyId);
        };
    }
}
