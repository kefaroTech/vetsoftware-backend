package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.laboratorytest.infrastructure.persistence.LaboratoryTestJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaLaboratoryTestChildrenQueryPort implements LaboratoryTestChildrenQueryPort {
    private final LaboratoryTestJpaRepository jpaRepository;

    public JpaLaboratoryTestChildrenQueryPort(LaboratoryTestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
