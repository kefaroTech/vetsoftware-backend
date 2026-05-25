package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.HospitalizationChildrenQueryPort;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaHospitalizationChildrenQueryPort implements HospitalizationChildrenQueryPort {
    private final HospitalizationJpaRepository jpaRepository;

    public JpaHospitalizationChildrenQueryPort(HospitalizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
