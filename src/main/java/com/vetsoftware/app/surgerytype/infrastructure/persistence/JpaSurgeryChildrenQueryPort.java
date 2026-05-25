package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaSurgeryChildrenQueryPort implements SurgeryChildrenQueryPort {
    private final SurgeryJpaRepository jpaRepository;

    public JpaSurgeryChildrenQueryPort(SurgeryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveBySurgeryTypeId(Long parentId) {
        return jpaRepository.existsBySurgeryType_Id(parentId);
    }
}
