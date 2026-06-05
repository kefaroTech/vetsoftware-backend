package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.service.application.port.out.ServiceCategoryQueryPort;
import com.vetsoftware.app.service.domain.ServiceCategoryRef;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component("serviceJpaServiceCategoryQueryPort")
public class JpaServiceCategoryQueryPort implements ServiceCategoryQueryPort {
    private final ServiceCategoryJpaRepository serviceCategoryJpaRepository;

    public JpaServiceCategoryQueryPort(ServiceCategoryJpaRepository serviceCategoryJpaRepository) {
        this.serviceCategoryJpaRepository = serviceCategoryJpaRepository;
    }

    @Override
    public Optional<ServiceCategoryRef> findById(Long serviceCategoryId) {
        return serviceCategoryJpaRepository.findById(serviceCategoryId)
            .map(e -> new ServiceCategoryRef(e.getId(), e.getName()));
    }
}
