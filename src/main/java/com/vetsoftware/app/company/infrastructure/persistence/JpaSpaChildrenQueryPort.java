package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSpaChildrenQueryPort implements SpaChildrenQueryPort {
    private final SpaJpaRepository jpaRepository;

    public JpaSpaChildrenQueryPort(SpaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByCompanyId(Long parentId) {
        return jpaRepository.existsByCompany_Id(parentId);
    }
}
