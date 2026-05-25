package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.PrescriptionChildrenQueryPort;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaPrescriptionChildrenQueryPort implements PrescriptionChildrenQueryPort {
    private final PrescriptionJpaRepository jpaRepository;

    public JpaPrescriptionChildrenQueryPort(PrescriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
