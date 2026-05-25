package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.DewormingChildrenQueryPort;
import com.vetsoftware.app.deworming.infrastructure.persistence.DewormingJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDewormingChildrenQueryPort implements DewormingChildrenQueryPort {
    private final DewormingJpaRepository jpaRepository;

    public JpaDewormingChildrenQueryPort(DewormingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
