package com.vetsoftware.app.module.infrastructure.persistence;

import com.vetsoftware.app.module.application.port.out.SubModuleChildrenQueryPort;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSubModuleChildrenQueryPort implements SubModuleChildrenQueryPort {
    private final SubModuleJpaRepository jpaRepository;

    public JpaSubModuleChildrenQueryPort(SubModuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsActiveByModuleId(Long parentId) {
        return jpaRepository.existsByModule_Id(parentId);
    }
}
