package com.vetsoftware.app.servicecategory.infrastructure.persistence;

import com.vetsoftware.app.service.infrastructure.persistence.ServiceJpaRepository;
import com.vetsoftware.app.servicecategory.application.port.out.ServiceChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaServiceChildrenQueryPort implements ServiceChildrenQueryPort {
    private final ServiceJpaRepository serviceJpaRepository;

    public JpaServiceChildrenQueryPort(ServiceJpaRepository serviceJpaRepository) {
        this.serviceJpaRepository = serviceJpaRepository;
    }

    @Override
    public boolean existsActiveByServiceCategoryId(Long categoryId) {
        return serviceJpaRepository.existsByServiceCategory_Id(categoryId);
    }
}
