package com.vetsoftware.app.spatype.infrastructure.persistence;

import com.vetsoftware.app.spa.infrastructure.persistence.SpaJpaRepository;
import com.vetsoftware.app.spatype.application.port.out.SpaChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaSpaChildrenQueryPort implements SpaChildrenQueryPort {
    private final SpaJpaRepository jpaRepository;

    public JpaSpaChildrenQueryPort(SpaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveBySpaTypeId(Long parentId) {
        return jpaRepository.existsBySpaType_Id(parentId);
    }
}
